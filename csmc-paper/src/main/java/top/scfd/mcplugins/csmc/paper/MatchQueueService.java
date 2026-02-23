package top.scfd.mcplugins.csmc.paper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class MatchQueueService {
    public enum JoinResult {
        QUEUED,
        MOVED,
        ALREADY_IN_QUEUE,
        ALREADY_IN_SESSION
    }

    private final SessionRegistry sessions;
    private final int maxSessions;
    private final Map<GameMode, LinkedHashSet<UUID>> queues = new EnumMap<>(GameMode.class);
    private final Map<UUID, GameMode> queuedModes = new ConcurrentHashMap<>();

    public MatchQueueService(SessionRegistry sessions, int maxSessions) {
        this.sessions = sessions;
        this.maxSessions = Math.max(1, maxSessions);
        for (GameMode mode : GameMode.values()) {
            queues.put(mode, new LinkedHashSet<>());
        }
    }

    public synchronized JoinResult join(UUID playerId, GameMode mode) {
        if (sessions.getSessionForPlayer(playerId) != null) {
            return JoinResult.ALREADY_IN_SESSION;
        }
        GameMode oldMode = queuedModes.get(playerId);
        if (oldMode == mode) {
            return JoinResult.ALREADY_IN_QUEUE;
        }
        if (oldMode != null) {
            queues.get(oldMode).remove(playerId);
        }
        queues.get(mode).add(playerId);
        queuedModes.put(playerId, mode);
        return oldMode == null ? JoinResult.QUEUED : JoinResult.MOVED;
    }

    public synchronized boolean leave(UUID playerId) {
        GameMode mode = queuedModes.remove(playerId);
        if (mode == null) {
            return false;
        }
        return queues.get(mode).remove(playerId);
    }

    public synchronized GameMode queuedMode(UUID playerId) {
        return queuedModes.get(playerId);
    }

    public synchronized int queueSize(GameMode mode) {
        return queues.get(mode).size();
    }

    public synchronized int queuePosition(UUID playerId) {
        GameMode mode = queuedModes.get(playerId);
        if (mode == null) {
            return -1;
        }
        int index = 1;
        for (UUID queued : queues.get(mode)) {
            if (queued.equals(playerId)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public synchronized void tick() {
        if (sessions.sessionCount() >= maxSessions) {
            return;
        }
        for (GameMode mode : GameMode.values()) {
            sanitize(mode);
            while (sessions.sessionCount() < maxSessions && queues.get(mode).size() >= 2) {
                GameSession session = sessions.createSession(mode);
                List<Player> matched = fillSession(mode, session);
                if (matched.size() < 2) {
                    for (Player player : matched) {
                        sessions.leaveSession(player);
                        join(player.getUniqueId(), mode);
                        player.sendMessage("Matchmaking paused: not enough online players.");
                    }
                    sessions.removeSession(session);
                    break;
                }
                for (Player player : matched) {
                    TeamSide side = session.getSide(player.getUniqueId());
                    player.sendMessage("Matched to " + mode + " session " + session.id() + " as " + side + ".");
                }
            }
        }
    }

    private List<Player> fillSession(GameMode mode, GameSession session) {
        List<Player> matched = new ArrayList<>();
        int maxPlayers = Math.max(2, session.maxPlayers());
        var iterator = queues.get(mode).iterator();
        while (iterator.hasNext() && matched.size() < maxPlayers) {
            UUID playerId = iterator.next();
            iterator.remove();
            queuedModes.remove(playerId);

            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (sessions.getSessionForPlayer(playerId) != null) {
                continue;
            }
            TeamSide side = sessions.joinSession(player, session);
            if (side == TeamSide.SPECTATOR) {
                continue;
            }
            matched.add(player);
        }
        return matched;
    }

    private void sanitize(GameMode mode) {
        var queue = queues.get(mode);
        var iterator = queue.iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || sessions.getSessionForPlayer(playerId) != null) {
                iterator.remove();
                queuedModes.remove(playerId);
            }
        }
    }
}
