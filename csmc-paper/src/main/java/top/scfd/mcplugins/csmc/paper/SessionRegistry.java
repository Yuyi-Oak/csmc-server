package top.scfd.mcplugins.csmc.paper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.economy.EconomyEventListener;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.core.session.GameSessionManager;
import top.scfd.mcplugins.csmc.core.shop.ShopCatalog;

public final class SessionRegistry {
    private final GameSessionManager sessionManager;
    private final ShopCatalog catalog;
    private final StatsService statsService;
    private final Map<UUID, UUID> playerSessions = new ConcurrentHashMap<>();
    private final List<RoundEventListener> roundListeners = new CopyOnWriteArrayList<>();
    private final List<EconomyEventListener> economyListeners = new CopyOnWriteArrayList<>();

    public SessionRegistry(GameSessionManager sessionManager, ShopCatalog catalog, StatsService statsService) {
        this.sessionManager = sessionManager;
        this.catalog = catalog;
        this.statsService = statsService;
    }

    public GameSession createSession(GameMode mode) {
        GameSession session = sessionManager.createSession(mode, 0, catalog);
        roundListeners.forEach(session::addRoundListener);
        economyListeners.forEach(session::addEconomyListener);
        if (statsService != null) {
            session.addRoundListener(new StatsRoundListener(session, statsService));
        }
        return session;
    }

    public void assignPlayer(Player player, GameSession session) {
        playerSessions.put(player.getUniqueId(), session.id());
    }

    public GameSession findSession(Player player) {
        UUID sessionId = playerSessions.get(player.getUniqueId());
        return sessionId == null ? null : sessionManager.getSession(sessionId);
    }

    public GameSession getSession(UUID sessionId) {
        return sessionManager.getSession(sessionId);
    }

    public TeamSide joinSession(Player player, GameSession session) {
        TeamSide side = session.joinPlayer(player.getUniqueId());
        if (side != TeamSide.SPECTATOR) {
            assignPlayer(player, session);
        }
        return side;
    }

    public void leaveSession(Player player) {
        UUID sessionId = playerSessions.remove(player.getUniqueId());
        if (sessionId == null) {
            return;
        }
        GameSession session = sessionManager.getSession(sessionId);
        if (session != null) {
            session.removePlayer(player.getUniqueId());
        }
    }

    public void addRoundListener(RoundEventListener listener) {
        if (listener != null) {
            roundListeners.add(listener);
        }
    }

    public void addEconomyListener(EconomyEventListener listener) {
        if (listener != null) {
            economyListeners.add(listener);
        }
    }
}
