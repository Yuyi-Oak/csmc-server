package top.scfd.mcplugins.csmc.core.session;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;

public final class GameSession {
    private final UUID id;
    private final GameMode mode;
    private final int maxPlayers;
    private volatile SessionState state;
    private final Map<UUID, TeamSide> players = new ConcurrentHashMap<>();

    public GameSession(UUID id, GameMode mode, int maxPlayers) {
        this.id = id;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.state = SessionState.WAITING;
    }

    public UUID id() {
        return id;
    }

    public GameMode mode() {
        return mode;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public SessionState state() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public boolean addPlayer(UUID playerId, TeamSide side) {
        if (players.size() >= maxPlayers) {
            return false;
        }
        players.put(playerId, side);
        return true;
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    public TeamSide getSide(UUID playerId) {
        return players.getOrDefault(playerId, TeamSide.SPECTATOR);
    }

    public Set<UUID> players() {
        return Set.copyOf(players.keySet());
    }
}
