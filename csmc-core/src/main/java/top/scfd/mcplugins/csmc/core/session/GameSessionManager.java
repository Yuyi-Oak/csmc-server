package top.scfd.mcplugins.csmc.core.session;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import top.scfd.mcplugins.csmc.api.GameMode;

public final class GameSessionManager {
    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession createSession(GameMode mode, int maxPlayers) {
        UUID id = UUID.randomUUID();
        GameSession session = new GameSession(id, mode, maxPlayers);
        sessions.put(id, session);
        return session;
    }

    public GameSession getSession(UUID id) {
        return sessions.get(id);
    }

    public Collection<GameSession> allSessions() {
        return sessions.values();
    }

    public void removeSession(UUID id) {
        sessions.remove(id);
    }
}
