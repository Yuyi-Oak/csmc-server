package top.scfd.mcplugins.csmc.paper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.map.BuyZone;
import top.scfd.mcplugins.csmc.core.map.MapDefinition;
import top.scfd.mcplugins.csmc.core.map.MapRegistry;
import top.scfd.mcplugins.csmc.core.economy.EconomyEventListener;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.core.session.GameSessionManager;
import top.scfd.mcplugins.csmc.core.shop.ShopCatalog;

public final class SessionRegistry {
    private static final int FINISHED_SESSION_TTL_SECONDS = 15;
    private final GameSessionManager sessionManager;
    private final MapRegistry mapRegistry;
    private final ShopCatalog catalog;
    private final StatsService statsService;
    private final BombService bombs;
    private final LoadoutInventoryService loadoutInventory;
    private volatile PlayerRoundStateService playerRoundState;
    private final Map<UUID, UUID> playerSessions = new ConcurrentHashMap<>();
    private final Map<UUID, MapDefinition> sessionMaps = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> finishedSessionTtl = new ConcurrentHashMap<>();
    private final List<RoundEventListener> roundListeners = new CopyOnWriteArrayList<>();
    private final List<EconomyEventListener> economyListeners = new CopyOnWriteArrayList<>();

    public SessionRegistry(GameSessionManager sessionManager, MapRegistry mapRegistry, ShopCatalog catalog, StatsService statsService, BombService bombs, LoadoutInventoryService loadoutInventory) {
        this.sessionManager = sessionManager;
        this.mapRegistry = mapRegistry;
        this.catalog = catalog;
        this.statsService = statsService;
        this.bombs = bombs;
        this.loadoutInventory = loadoutInventory;
    }

    public GameSession createSession(GameMode mode) {
        return createSession(mode, null);
    }

    public GameSession createSession(GameMode mode, String mapId) {
        GameSession session = sessionManager.createSession(mode, 0, catalog);
        roundListeners.forEach(session::addRoundListener);
        economyListeners.forEach(session::addEconomyListener);
        if (statsService != null) {
            session.addRoundListener(new StatsRoundListener(session, statsService));
        }
        if (bombs != null) {
            session.addRoundListener(new BombRoundListener(session, bombs));
        }
        if (playerRoundState != null) {
            session.addRoundListener(new RoundPlayerStateListener(session, playerRoundState));
        }
        MapDefinition map = resolveMap(mapId);
        if (map != null) {
            sessionMaps.put(session.id(), map);
            session.addRoundListener(new RoundSpawnListener(session, map));
        }
        if (loadoutInventory != null) {
            session.addRoundListener(new LoadoutRoundListener(session, loadoutInventory));
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

    public GameSession getSessionForPlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        UUID sessionId = playerSessions.get(playerId);
        return sessionId == null ? null : sessionManager.getSession(sessionId);
    }

    public MapDefinition mapForSession(GameSession session) {
        if (session == null) {
            return null;
        }
        return sessionMaps.get(session.id());
    }

    public List<MapDefinition> availableMaps() {
        if (mapRegistry == null) {
            return List.of();
        }
        return mapRegistry.all().stream()
            .sorted((left, right) -> left.id().compareToIgnoreCase(right.id()))
            .toList();
    }

    public boolean hasMap(String mapId) {
        if (mapId == null || mapId.isBlank() || mapRegistry == null) {
            return false;
        }
        return mapRegistry.find(mapId).isPresent();
    }

    public boolean isInBuyZone(Player player, GameSession session) {
        if (player == null || session == null) {
            return false;
        }
        MapDefinition map = mapForSession(session);
        if (map == null) {
            return true;
        }
        Location location = player.getLocation();
        if (location.getWorld() == null || !location.getWorld().getName().equals(map.world())) {
            return false;
        }
        TeamSide side = session.getSide(player.getUniqueId());
        var zones = map.buyZones(side);
        if (zones == null || zones.isEmpty()) {
            return true;
        }
        for (BuyZone zone : zones) {
            double dx = location.getX() - zone.x();
            double dy = location.getY() - zone.y();
            double dz = location.getZ() - zone.z();
            double radius = zone.radius();
            if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    public TeamSide joinSession(Player player, GameSession session) {
        UUID playerId = player.getUniqueId();
        UUID previousSessionId = playerSessions.get(playerId);
        if (previousSessionId != null && !previousSessionId.equals(session.id())) {
            playerSessions.remove(playerId, previousSessionId);
            GameSession previousSession = sessionManager.getSession(previousSessionId);
            if (previousSession != null) {
                previousSession.removePlayer(playerId);
                if (previousSession.players().isEmpty()) {
                    removeSession(previousSession);
                }
            }
        }

        TeamSide side = session.joinPlayer(playerId);
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
            if (session.players().isEmpty()) {
                removeSession(session);
            }
        }
    }

    public int sessionCount() {
        return sessionManager.allSessions().size();
    }

    public void removeSession(GameSession session) {
        if (session == null) {
            return;
        }
        UUID sessionId = session.id();
        for (UUID playerId : session.players()) {
            playerSessions.remove(playerId, sessionId);
        }
        sessionMaps.remove(sessionId);
        finishedSessionTtl.remove(sessionId);
        sessionManager.removeSession(sessionId);
    }

    public void tickSessions() {
        List<GameSession> snapshot = new ArrayList<>(sessionManager.allSessions());
        for (GameSession session : snapshot) {
            session.tickRound();
            handleFinishedLifecycle(session);
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

    public void setPlayerRoundState(PlayerRoundStateService playerRoundState) {
        this.playerRoundState = playerRoundState;
    }

    private MapDefinition resolveDefaultMap() {
        if (mapRegistry == null) {
            return null;
        }
        return mapRegistry.all().stream()
            .sorted((left, right) -> left.id().compareToIgnoreCase(right.id()))
            .findFirst()
            .orElse(null);
    }

    private MapDefinition resolveMap(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return resolveDefaultMap();
        }
        if (mapRegistry == null) {
            return null;
        }
        return mapRegistry.find(mapId).orElseGet(this::resolveDefaultMap);
    }

    private void handleFinishedLifecycle(GameSession session) {
        if (session == null) {
            return;
        }
        UUID sessionId = session.id();
        if (session.state() != SessionState.FINISHED) {
            finishedSessionTtl.remove(sessionId);
            return;
        }
        int remaining = finishedSessionTtl.compute(
            sessionId,
            (id, ttl) -> ttl == null ? FINISHED_SESSION_TTL_SECONDS : ttl - 1
        );
        if (remaining > 0) {
            return;
        }
        closeFinishedSession(session);
    }

    private void closeFinishedSession(GameSession session) {
        UUID sessionId = session.id();
        for (UUID playerId : session.players()) {
            playerSessions.remove(playerId, sessionId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.setSpectatorTarget(null);
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                player.sendMessage("Match finished. Session closed.");
            }
            session.removePlayer(playerId);
        }
        sessionMaps.remove(sessionId);
        finishedSessionTtl.remove(sessionId);
        sessionManager.removeSession(sessionId);
    }
}
