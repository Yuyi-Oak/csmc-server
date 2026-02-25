package top.scfd.mcplugins.csmc.paper.security;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;

public final class MovementAntiCheatListener implements Listener {
    private static final double HORIZONTAL_MAX_PER_EVENT = 3.5;
    private static final double VERTICAL_MAX_PER_EVENT = 1.8;
    private static final double TELEPORT_IGNORE_DISTANCE_SQUARED = 64.0; // >= 8 blocks in one event is likely plugin teleport
    private static final double SPEED_EFFECT_BONUS_PER_LEVEL = 0.35;
    private static final double JUMP_EFFECT_BONUS_PER_LEVEL = 0.45;
    private static final double ICE_SURFACE_HORIZONTAL_BONUS = 1.2;
    private static final double SPECIAL_SURFACE_VERTICAL_BONUS = 2.4;

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
        if (player.getGameMode() == GameMode.SPECTATOR
            || player.getAllowFlight()
            || player.isInsideVehicle()
            || player.isGliding()
            || player.isRiptiding()) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        if (from.distanceSquared(to) >= TELEPORT_IGNORE_DISTANCE_SQUARED) {
            return;
        }
        if (isInFluid(from) || isInFluid(to)) {
            return;
        }
        double horizontalLimit = pingAdjustedHorizontalLimit(player, from, to);
        double verticalLimit = pingAdjustedVerticalLimit(player, from, to);

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > horizontalLimit) {
            antiCheat.flag(player, "speed", 2);
        }

        double dy = to.getY() - from.getY();
        if (dy > verticalLimit && !isClimbable(to)) {
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

    private double pingAdjustedHorizontalLimit(Player player, Location from, Location to) {
        int ping = Math.max(0, player.getPing());
        double bonus = Math.min(2.0, ping / 160.0);
        double speedBonus = potionLevel(player, PotionEffectType.SPEED) * SPEED_EFFECT_BONUS_PER_LEVEL;
        if (isIceSurface(surfaceMaterial(from)) || isIceSurface(surfaceMaterial(to))) {
            speedBonus += ICE_SURFACE_HORIZONTAL_BONUS;
        }
        return HORIZONTAL_MAX_PER_EVENT + bonus + speedBonus;
    }

    private double pingAdjustedVerticalLimit(Player player, Location from, Location to) {
        int ping = Math.max(0, player.getPing());
        double bonus = Math.min(1.2, ping / 220.0);
        double jumpBonus = potionLevel(player, PotionEffectType.JUMP_BOOST) * JUMP_EFFECT_BONUS_PER_LEVEL;
        Material fromSurface = surfaceMaterial(from);
        Material toSurface = surfaceMaterial(to);
        if (isSpecialVerticalSurface(fromSurface) || isSpecialVerticalSurface(toSurface)) {
            jumpBonus += SPECIAL_SURFACE_VERTICAL_BONUS;
        }
        return VERTICAL_MAX_PER_EVENT + bonus + jumpBonus;
    }

    private int potionLevel(Player player, PotionEffectType type) {
        if (player == null || type == null) {
            return 0;
        }
        PotionEffect effect = player.getPotionEffect(type);
        if (effect == null) {
            return 0;
        }
        return effect.getAmplifier() + 1;
    }

    private Material surfaceMaterial(Location location) {
        if (location == null) {
            return Material.AIR;
        }
        Location probe = location.clone().subtract(0.0, 1.0, 0.0);
        return probe.getBlock().getType();
    }

    private boolean isIceSurface(Material material) {
        return material == Material.ICE
            || material == Material.PACKED_ICE
            || material == Material.BLUE_ICE
            || material == Material.FROSTED_ICE;
    }

    private boolean isSpecialVerticalSurface(Material material) {
        if (material == Material.SLIME_BLOCK || material == Material.HONEY_BLOCK) {
            return true;
        }
        String name = material.name();
        return name.endsWith("_BED");
    }
}
