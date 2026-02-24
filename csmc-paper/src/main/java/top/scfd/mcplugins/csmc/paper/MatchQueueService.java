package top.scfd.mcplugins.csmc.paper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final Map<UUID, String> queuedMaps = new ConcurrentHashMap<>();

    public MatchQueueService(SessionRegistry sessions, int maxSessions) {
        this.sessions = sessions;
        this.maxSessions = Math.max(1, maxSessions);
        for (GameMode mode : GameMode.values()) {
            queues.put(mode, new LinkedHashSet<>());
        }
    }

    public synchronized JoinResult join(UUID playerId, GameMode mode) {
        return join(playerId, mode, null);
    }

    public synchronized JoinResult join(UUID playerId, GameMode mode, String mapId) {
        if (sessions.getSessionForPlayer(playerId) != null) {
            return JoinResult.ALREADY_IN_SESSION;
        }
        GameMode oldMode = queuedModes.get(playerId);
        String normalizedMap = normalizeMapId(mapId);
        if (oldMode == mode && java.util.Objects.equals(queuedMaps.get(playerId), normalizedMap)) {
            return JoinResult.ALREADY_IN_QUEUE;
        }
        if (oldMode != null) {
            queues.get(oldMode).remove(playerId);
        }
        queues.get(mode).add(playerId);
        queuedModes.put(playerId, mode);
        if (normalizedMap == null) {
            queuedMaps.remove(playerId);
        } else {
            queuedMaps.put(playerId, normalizedMap);
        }
        return oldMode == null ? JoinResult.QUEUED : JoinResult.MOVED;
    }

    public synchronized boolean leave(UUID playerId) {
        GameMode mode = queuedModes.remove(playerId);
        if (mode == null) {
            return false;
        }
        queuedMaps.remove(playerId);
        return queues.get(mode).remove(playerId);
    }

    public synchronized GameMode queuedMode(UUID playerId) {
        return queuedModes.get(playerId);
    }

    public synchronized int queueSize(GameMode mode) {
        return queues.get(mode).size();
    }

    public synchronized int playersNeeded(GameMode mode) {
        int queueSize = queues.get(mode).size();
        int neededForNewSession = Math.max(0, 2 - queueSize);
        int neededForOpenSession = Integer.MAX_VALUE;
        for (GameSession session : sessions.allSessions()) {
            if (session.mode() != mode) {
                continue;
            }
            if (session.state() != top.scfd.mcplugins.csmc.api.SessionState.WAITING) {
                continue;
            }
            int available = session.maxPlayers() - session.players().size();
            if (available <= 0) {
                continue;
            }
            int needed = queueSize > 0 ? 0 : 1;
            neededForOpenSession = Math.min(neededForOpenSession, needed);
        }
        if (neededForOpenSession == Integer.MAX_VALUE) {
            return neededForNewSession;
        }
        return Math.min(neededForNewSession, neededForOpenSession);
    }

    public synchronized Map<GameMode, Integer> queueSizes() {
        Map<GameMode, Integer> snapshot = new EnumMap<>(GameMode.class);
        for (GameMode mode : GameMode.values()) {
            snapshot.put(mode, queues.get(mode).size());
        }
        return snapshot;
    }

    public synchronized Map<String, Integer> mapVotes(GameMode mode) {
        Map<String, Integer> votes = new HashMap<>();
        if (mode == null) {
            return votes;
        }
        for (UUID playerId : queues.get(mode)) {
            String map = queuedMaps.get(playerId);
            String key = (map == null || map.isBlank()) ? "auto" : map;
            votes.merge(key, 1, Integer::sum);
        }
        return votes;
    }

    public synchronized String queuedMap(UUID playerId) {
        return queuedMaps.get(playerId);
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
        for (GameMode mode : GameMode.values()) {
            sanitize(mode);
            fillExistingSessions(mode);
            while (sessions.sessionCount() < maxSessions && queues.get(mode).size() >= 2) {
                String selectedMap = pickMapPreference(mode);
                GameSession session = sessions.createSession(mode, selectedMap);
                List<QueuedPlayer> matched = fillSession(mode, selectedMap, session);
                if (matched.size() < 2) {
                    for (QueuedPlayer queued : matched) {
                        sessions.leaveSession(queued.player);
                        join(queued.player.getUniqueId(), mode, queued.mapId);
                        queued.player.sendMessage("Matchmaking paused: not enough online players.");
                    }
                    sessions.removeSession(session);
                    break;
                }
                String mapLabel = selectedMap == null ? "auto" : selectedMap;
                for (QueuedPlayer queued : matched) {
                    TeamSide side = session.getSide(queued.player.getUniqueId());
                    queued.player.sendMessage("Matched to " + mode + " (" + mapLabel + ") session " + session.id() + " as " + side + ".");
                }
            }
        }
        updateQueueActionBar();
    }

    private void fillExistingSessions(GameMode mode) {
        for (GameSession session : sessions.allSessions()) {
            if (session.mode() != mode) {
                continue;
            }
            if (session.state() != top.scfd.mcplugins.csmc.api.SessionState.WAITING) {
                continue;
            }
            int available = session.maxPlayers() - session.players().size();
            if (available <= 0) {
                continue;
            }
            var map = sessions.mapForSession(session);
            String selectedMap = map == null ? null : map.id();
            List<QueuedPlayer> matched = fillSession(mode, selectedMap, session);
            if (matched.isEmpty()) {
                continue;
            }
            String mapLabel = selectedMap == null ? "auto" : selectedMap;
            for (QueuedPlayer queued : matched) {
                TeamSide side = session.getSide(queued.player.getUniqueId());
                queued.player.sendMessage("Matched to existing " + mode + " (" + mapLabel + ") session " + session.id() + " as " + side + ".");
            }
        }
    }

    private List<QueuedPlayer> fillSession(GameMode mode, String selectedMap, GameSession session) {
        List<QueuedPlayer> matched = new ArrayList<>();
        int maxPlayers = Math.max(0, session.maxPlayers() - session.players().size());
        if (maxPlayers <= 0) {
            return matched;
        }
        boolean requireExactMap = selectedMap != null;
        fillByRule(mode, session, matched, maxPlayers, selectedMap, requireExactMap, false);
        if (selectedMap != null && matched.size() < maxPlayers) {
            fillByRule(mode, session, matched, maxPlayers, selectedMap, false, true);
        }
        return matched;
    }

    private void fillByRule(
        GameMode mode,
        GameSession session,
        List<QueuedPlayer> matched,
        int maxPlayers,
        String selectedMap,
        boolean requireExactMap,
        boolean allowAutoMap
    ) {
        var iterator = queues.get(mode).iterator();
        while (iterator.hasNext() && matched.size() < maxPlayers) {
            UUID playerId = iterator.next();
            String queuedMap = queuedMaps.get(playerId);
            boolean exactMatch = selectedMap != null && selectedMap.equalsIgnoreCase(queuedMap);
            boolean autoMap = queuedMap == null || queuedMap.isBlank();
            if (requireExactMap && !exactMatch) {
                continue;
            }
            if (allowAutoMap && !autoMap) {
                continue;
            }
            if (!requireExactMap && !allowAutoMap && selectedMap != null && !exactMatch && !autoMap) {
                continue;
            }

            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                queuedModes.remove(playerId);
                queuedMaps.remove(playerId);
                continue;
            }
            if (sessions.getSessionForPlayer(playerId) != null) {
                iterator.remove();
                queuedModes.remove(playerId);
                queuedMaps.remove(playerId);
                continue;
            }
            TeamSide side = sessions.joinSession(player, session);
            if (side == TeamSide.SPECTATOR) {
                break;
            }
            iterator.remove();
            queuedModes.remove(playerId);
            queuedMaps.remove(playerId);
            matched.add(new QueuedPlayer(player, queuedMap));
        }
    }

    private void sanitize(GameMode mode) {
        var queue = queues.get(mode);
        var iterator = queue.iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            Player player = Bukkit.getPlayer(playerId);
            String queuedMap = queuedMaps.get(playerId);
            boolean invalidMap = queuedMap != null && !sessions.hasMap(queuedMap);
            if (player == null || !player.isOnline() || sessions.getSessionForPlayer(playerId) != null || invalidMap) {
                iterator.remove();
                queuedModes.remove(playerId);
                queuedMaps.remove(playerId);
            }
        }
    }

    private String pickMapPreference(GameMode mode) {
        Map<String, Integer> votes = new HashMap<>();
        for (UUID playerId : queues.get(mode)) {
            String map = queuedMaps.get(playerId);
            if (map == null || map.isBlank()) {
                continue;
            }
            votes.merge(map, 1, Integer::sum);
        }
        String best = null;
        int bestVotes = 0;
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            String map = entry.getKey();
            int value = entry.getValue();
            if (value > bestVotes || (value == bestVotes && best != null && map.compareToIgnoreCase(best) < 0)) {
                best = map;
                bestVotes = value;
            }
        }
        return best;
    }

    private String normalizeMapId(String mapId) {
        if (mapId == null) {
            return null;
        }
        String normalized = mapId.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private void updateQueueActionBar() {
        for (Map.Entry<UUID, GameMode> entry : queuedModes.entrySet()) {
            UUID playerId = entry.getKey();
            GameMode mode = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            int size = queueSize(mode);
            int needed = playersNeeded(mode);
            int position = queuePosition(playerId);
            String map = queuedMaps.get(playerId);
            String mapText = map == null ? "auto" : map;
            player.sendActionBar(
                Component.text("Queue " + mode + " (" + mapText + ") " + position + "/" + size + " need " + needed)
                    .color(NamedTextColor.AQUA)
            );
        }
    }

    private static final class QueuedPlayer {
        private final Player player;
        private final String mapId;

        private QueuedPlayer(Player player, String mapId) {
            this.player = player;
            this.mapId = mapId;
        }
    }
}
