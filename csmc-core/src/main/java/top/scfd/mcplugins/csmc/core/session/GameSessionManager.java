package top.scfd.mcplugins.csmc.core.session;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.core.rules.ModeRules;
import top.scfd.mcplugins.csmc.core.rules.ModeRulesRegistry;

public final class GameSessionManager {
    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final ModeRulesRegistry rulesRegistry;

    public GameSessionManager(ModeRulesRegistry rulesRegistry) {
        this.rulesRegistry = rulesRegistry;
    }

    public GameSession createSession(GameMode mode, int maxPlayers) {
        UUID id = UUID.randomUUID();
        ModeRules rules = rulesRegistry.rulesFor(mode);
        int resolvedMaxPlayers = maxPlayers > 0 ? maxPlayers : rules.maxPlayers();
        GameSession session = new GameSession(id, mode, resolvedMaxPlayers, rules);
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
