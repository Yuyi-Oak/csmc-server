package top.scfd.mcplugins.csmc.paper.security;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;

public final class MovementAntiCheatListener implements Listener {
    private static final double HORIZONTAL_MAX_PER_EVENT = 3.5;
    private static final double VERTICAL_MAX_PER_EVENT = 1.8;

    private final SessionRegistry sessions;
    private final AntiCheatService antiCheat;

    public MovementAntiCheatListener(SessionRegistry sessions, AntiCheatService antiCheat) {
        this.sessions = sessions;
        this.antiCheat = antiCheat;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameSession session = sessions.findSession(player);
        if (session == null || !isCombatPhase(session.roundEngine().phase())) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.getAllowFlight() || player.isInsideVehicle()) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        if (isInFluid(from) || isInFluid(to)) {
            return;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > HORIZONTAL_MAX_PER_EVENT) {
            antiCheat.flag(player, "speed", 2);
        }

        double dy = to.getY() - from.getY();
        if (dy > VERTICAL_MAX_PER_EVENT && !isClimbable(to)) {
            antiCheat.flag(player, "fly", 2);
        }
    }

    private boolean isCombatPhase(RoundPhase phase) {
        return phase == RoundPhase.BUY || phase == RoundPhase.LIVE || phase == RoundPhase.BOMB_PLANTED;
    }

    private boolean isInFluid(Location location) {
        Block block = location.getBlock();
        Material material = block.getType();
        return material == Material.WATER || material == Material.LAVA;
    }

    private boolean isClimbable(Location location) {
        Material material = location.getBlock().getType();
        return material == Material.LADDER || material == Material.VINE || material == Material.SCAFFOLDING;
    }
}
