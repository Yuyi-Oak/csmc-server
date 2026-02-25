package top.scfd.mcplugins.csmc.paper;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class PlayerRoundStateService implements Listener {
    private final Plugin plugin;
    private final SessionRegistry sessions;
    private final BombService bombs;
    private final ConcurrentHashMap<UUID, Set<UUID>> roundDeaths = new ConcurrentHashMap<>();

    public PlayerRoundStateService(Plugin plugin, SessionRegistry sessions, BombService bombs) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.bombs = bombs;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameSession session = sessions.findSession(player);
        if (session == null || !isCombatPhase(session.roundEngine().phase())) {
            return;
        }
        boolean hadBomb = bombs != null && bombs.hasBombItem(player, session);
        roundDeaths.computeIfAbsent(session.id(), ignored -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
        event.setDroppedExp(0);
        event.getDrops().clear();
        if (hadBomb && bombs != null) {
            bombs.stripBombItems(player);
            if (!bombs.dropBombAt(session, player.getLocation())) {
                Bukkit.getScheduler().runTask(plugin, () -> bombs.assignBomb(session));
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameSession session = sessions.findSession(player);
        if (session == null) {
            return;
        }
        if (!isMarkedDead(session.id(), player.getUniqueId())) {
            return;
        }
        event.setRespawnLocation(resolveSpectatorRespawn(session, player));
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.setFlying(true);
            Player target = resolveSpectatorTarget(session, player);
            player.setSpectatorTarget(target);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        for (Set<UUID> deadPlayers : roundDeaths.values()) {
            deadPlayers.remove(playerId);
        }
    }

    public void onRoundStart(GameSession session) {
        if (session == null) {
            return;
        }
        roundDeaths.remove(session.id());
        for (UUID playerId : session.players()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);
            Attribute attribute = Attribute.MAX_HEALTH;
            var maxHealth = player.getAttribute(attribute);
            if (maxHealth != null) {
                player.setHealth(Math.max(1.0, maxHealth.getValue()));
            }
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setExhaustion(0.0f);
            player.setFireTicks(0);
            player.setFallDistance(0.0f);
        }
    }

    private boolean isCombatPhase(RoundPhase phase) {
        return phase == RoundPhase.BUY || phase == RoundPhase.LIVE || phase == RoundPhase.BOMB_PLANTED;
    }

    private boolean isMarkedDead(UUID sessionId, UUID playerId) {
        Set<UUID> deadPlayers = roundDeaths.get(sessionId);
        return deadPlayers != null && deadPlayers.contains(playerId);
    }

    private Location resolveSpectatorRespawn(GameSession session, Player player) {
        Player target = resolveSpectatorTarget(session, player);
        return target == null ? player.getLocation() : target.getLocation();
    }

    private Player resolveSpectatorTarget(GameSession session, Player player) {
        TeamSide playerSide = session.getSide(player.getUniqueId());
        Player aliveTeammate = findAlivePlayer(session, player.getUniqueId(), playerSide);
        if (aliveTeammate != null) {
            return aliveTeammate;
        }
        return findAlivePlayer(session, player.getUniqueId(), null);
    }

    private Player findAlivePlayer(GameSession session, UUID excludePlayer, TeamSide sideFilter) {
        for (UUID playerId : session.players()) {
            if (playerId.equals(excludePlayer)) {
                continue;
            }
            if (sideFilter != null && session.getSide(playerId) != sideFilter) {
                continue;
            }
            Player candidate = Bukkit.getPlayer(playerId);
            if (candidate == null || !candidate.isOnline()) {
                continue;
            }
            if (candidate.getGameMode() == GameMode.SPECTATOR || candidate.isDead() || candidate.getHealth() <= 0.0) {
                continue;
            }
            return candidate;
        }
        return null;
    }
}
