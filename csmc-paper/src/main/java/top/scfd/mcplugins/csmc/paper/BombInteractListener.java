package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.map.BombSite;
import top.scfd.mcplugins.csmc.core.map.MapDefinition;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class BombInteractListener implements Listener {
    private final SessionRegistry sessions;
    private final BombService bombs;

    public BombInteractListener(SessionRegistry sessions, BombService bombs) {
        this.sessions = sessions;
        this.bombs = bombs;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        GameSession session = sessions.findSession(player);
        if (session == null) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        ItemStack item = event.getItem();
        if (item != null && bombs.isBombItem(item)) {
            if (session.getSide(player.getUniqueId()) != TeamSide.TERRORIST) {
                return;
            }
            RoundPhase phase = session.roundEngine().phase();
            if (phase != RoundPhase.LIVE && phase != RoundPhase.BUY) {
                return;
            }
            MapDefinition map = sessions.mapForSession(session);
            if (map == null || !isInBombSite(clicked.getLocation(), map)) {
                return;
            }
            Location plantLocation = clicked.getLocation().add(0, 1, 0);
            if (plantLocation.getBlock().getType() != Material.AIR) {
                return;
            }
            if (bombs.plant(session, player, plantLocation)) {
                item.setAmount(item.getAmount() - 1);
                event.setCancelled(true);
            }
            return;
        }
        if (bombs.isBombBlock(session, clicked.getLocation())) {
            if (session.getSide(player.getUniqueId()) != TeamSide.COUNTER_TERRORIST) {
                return;
            }
            if (bombs.startDefuse(session, player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameSession session = sessions.findSession(player);
        if (session == null) {
            return;
        }
        BombState state = bombs.state(session);
        if (state == null || state.defuserId() == null) {
            return;
        }
        if (!state.defuserId().equals(player.getUniqueId())) {
            return;
        }
        Location bomb = state.location();
        if (bomb == null || bomb.getWorld() == null || player.getWorld() != bomb.getWorld()) {
            bombs.cancelDefuse(session, player.getUniqueId());
            return;
        }
        if (!player.isSneaking()) {
            bombs.cancelDefuse(session, player.getUniqueId());
            return;
        }
        if (player.getLocation().distanceSquared(bomb) > 9.0) {
            bombs.cancelDefuse(session, player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameSession session = sessions.findSession(player);
        if (session != null) {
            bombs.cancelDefuse(session, player.getUniqueId());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameSession session = sessions.findSession(player);
        if (session != null) {
            bombs.cancelDefuse(session, player.getUniqueId());
        }
    }

    private boolean isInBombSite(Location location, MapDefinition map) {
        if (location == null || map == null) {
            return false;
        }
        for (BombSite site : map.bombSites()) {
            double dx = location.getX() - site.x();
            double dy = location.getY() - site.y();
            double dz = location.getZ() - site.z();
            double radius = site.radius();
            if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                return true;
            }
        }
        return false;
    }
}
