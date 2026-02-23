package top.scfd.mcplugins.csmc.paper;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.player.ArmorState;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.player.WeaponInstance;
import top.scfd.mcplugins.csmc.core.weapon.DamageResult;
import top.scfd.mcplugins.csmc.core.weapon.Hitbox;
import top.scfd.mcplugins.csmc.core.weapon.WeaponSimulator;
import top.scfd.mcplugins.csmc.core.weapon.WeaponSpec;

public final class WeaponFireListener implements Listener {
    private final SessionRegistry sessions;
    private final WeaponSimulator simulator = new WeaponSimulator();
    private final WeaponItemService weaponItems;
    private final LoadoutInventoryService loadoutInventory;

    public WeaponFireListener(SessionRegistry sessions, WeaponItemService weaponItems, LoadoutInventoryService loadoutInventory) {
        this.sessions = sessions;
        this.weaponItems = weaponItems;
        this.loadoutInventory = loadoutInventory;
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        var session = sessions.findSession(player);
        if (session == null || session.state() != SessionState.LIVE) {
            return;
        }
        PlayerLoadout loadout = session.loadout(player.getUniqueId());
        if (loadout == null) {
            return;
        }
        WeaponInstance weapon = loadout.activeWeapon();
        if (weapon == null) {
            return;
        }
        WeaponSpec spec = weapon.spec();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!weaponItems.matches(hand, spec)) {
            return;
        }
        long tick = player.getWorld().getFullTime();
        if (weapon.state().isReloading(tick)) {
            return;
        }
        double minTicks = Math.max(1.0, 20.0 / Math.max(0.1, spec.fireRate()));
        if (tick - weapon.state().lastShotTick() < minTicks) {
            return;
        }
        if (!weapon.consumeRound()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            return;
        }
        simulator.nextRecoil(spec, weaponPattern(spec), weapon.state(), tick);
        double[] spread = simulator.sampleSpread(spec, movementFactor(player), jumpFactor(player));
        shoot(player, session, spec, spread);
        loadoutInventory.updateActiveWeaponItem(player, loadout);
    }

    private void shoot(Player player, top.scfd.mcplugins.csmc.core.session.GameSession session, WeaponSpec spec, double[] spread) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Vector adjusted = applySpread(direction, spread[0], spread[1]);
        World world = player.getWorld();
        RayTraceResult result = world.rayTrace(
            eye,
            adjusted,
            spec.range(),
            FluidCollisionMode.NEVER,
            true,
            0.25,
            entity -> entity instanceof Player && entity != player
        );
        if (result == null || !(result.getHitEntity() instanceof Player victim)) {
            return;
        }
        TeamSide attackerSide = session.getSide(player.getUniqueId());
        TeamSide victimSide = session.getSide(victim.getUniqueId());
        if (attackerSide == victimSide && attackerSide != TeamSide.SPECTATOR) {
            return;
        }
        PlayerLoadout victimLoadout = session.loadout(victim.getUniqueId());
        boolean armored = victimLoadout != null && victimLoadout.armor().armor() > 0;
        Hitbox hitbox = resolveHitbox(victim, result.getHitPosition().toLocation(world));
        double distance = eye.distance(result.getHitPosition().toLocation(world));
        DamageResult damage = simulator.simulateHit(spec, distance, armored, hitbox);
        if (victimLoadout != null) {
            ArmorState armor = victimLoadout.armor();
            armor.applyArmorDamage(damage.armorDamage());
        }
        if (damage.healthDamage() > 0) {
            victim.damage(damage.healthDamage(), player);
        }
    }

    private double movementFactor(Player player) {
        Vector velocity = player.getVelocity().clone();
        velocity.setY(0);
        return velocity.length() * 0.08;
    }

    private double jumpFactor(Player player) {
        return player.isOnGround() ? 0.0 : 0.15;
    }

    private Vector applySpread(Vector direction, double spreadX, double spreadY) {
        Vector up = new Vector(0, 1, 0);
        Vector right = direction.clone().crossProduct(up);
        if (right.lengthSquared() < 1.0E-6) {
            right = new Vector(1, 0, 0);
        } else {
            right.normalize();
        }
        Vector realUp = right.clone().crossProduct(direction).normalize();
        Vector adjusted = direction.clone()
            .add(right.multiply(spreadX))
            .add(realUp.multiply(spreadY));
        return adjusted.normalize();
    }

    private Hitbox resolveHitbox(Player victim, Location hit) {
        double relative = hit.getY() - victim.getLocation().getY();
        double height = victim.getHeight();
        if (relative >= height * 0.85) {
            return Hitbox.HEAD;
        }
        if (relative >= height * 0.65) {
            return Hitbox.CHEST;
        }
        if (relative >= height * 0.45) {
            return Hitbox.STOMACH;
        }
        return Hitbox.LEG;
    }

    private top.scfd.mcplugins.csmc.core.weapon.RecoilPattern weaponPattern(WeaponSpec spec) {
        return top.scfd.mcplugins.csmc.core.weapon.RecoilPattern.empty();
    }
}
