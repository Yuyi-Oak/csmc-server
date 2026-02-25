package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class GrenadeThrowListener implements Listener {
    private final SessionRegistry sessions;
    private final GrenadeItemService grenades;
    private final Plugin plugin;

    public GrenadeThrowListener(SessionRegistry sessions, GrenadeItemService grenades, Plugin plugin) {
        this.sessions = sessions;
        this.grenades = grenades;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        GameSession session = sessions.findSession(player);
        if (session == null || session.state() != SessionState.LIVE) {
            return;
        }
        ItemStack item = event.getItem();
        String key = grenades.getGrenadeKey(item);
        if (key == null) {
            return;
        }
        event.setCancelled(true);
        PlayerLoadout loadout = session.loadout(player.getUniqueId());
        if (loadout != null) {
            loadout.consumeGrenade(key);
        }
        consumeItem(player, item, event.getHand());
        Projectile projectile = launch(player, key);
        if (projectile == null) {
            return;
        }
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        container.set(grenades.grenadeKey(), PersistentDataType.STRING, key.toLowerCase());
        container.set(new org.bukkit.NamespacedKey(plugin, "csmc_grenade_owner"), PersistentDataType.STRING, player.getUniqueId().toString());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        String key = container.get(grenades.grenadeKey(), PersistentDataType.STRING);
        if (key == null) {
            return;
        }
        Location location = projectile.getLocation();
        UUID ownerId = null;
        String ownerValue = container.get(new org.bukkit.NamespacedKey(plugin, "csmc_grenade_owner"), PersistentDataType.STRING);
        if (ownerValue != null) {
            try {
                ownerId = UUID.fromString(ownerValue);
            } catch (IllegalArgumentException ignored) {
            }
        }
        switch (key) {
            case "hegrenade" -> explodeHE(location, ownerId);
            case "flashbang" -> flash(projectile, location, ownerId);
            case "smoke" -> smoke(location, ownerId);
            case "molotov", "incgrenade" -> molotov(location, ownerId);
            case "decoy" -> decoy(location);
            default -> {
            }
        }
    }

    private Projectile launch(Player player, String key) {
        return switch (key) {
            case "hegrenade", "smoke", "decoy" -> player.launchProjectile(Snowball.class);
            case "flashbang" -> player.launchProjectile(Egg.class);
            case "molotov", "incgrenade" -> player.launchProjectile(SmallFireball.class);
            default -> null;
        };
    }

    private void consumeItem(Player player, ItemStack stack, EquipmentSlot hand) {
        if (stack == null) {
            return;
        }
        int amount = stack.getAmount();
        if (amount <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else {
            stack.setAmount(amount - 1);
        }
    }

    private void explodeHE(Location location, UUID ownerId) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.EXPLOSION, location, 1);
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        GameSession ownerSession = ownerId == null ? null : sessions.getSessionForPlayer(ownerId);
        Player owner = ownerId == null ? null : Bukkit.getPlayer(ownerId);
        for (Player target : world.getPlayers()) {
            if (!isDamageablePlayer(target)) {
                continue;
            }
            if (target.getLocation().distanceSquared(location) > 25.0) {
                continue;
            }
            if (ownerSession != null && sessions.findSession(target) != ownerSession) {
                continue;
            }
            if (ownerSession != null && ownerId != null) {
                if (shouldBlockFriendlyDamage(ownerSession, ownerId, target.getUniqueId())) {
                    continue;
                }
            }
            double distance = target.getLocation().distance(location);
            double damage = Math.max(0.0, 60.0 - distance * 12.0);
            if (damage > 0) {
                if (owner != null) {
                    target.damage(damage, owner);
                } else {
                    target.damage(damage);
                }
            }
        }
    }

    private void flash(Projectile projectile, Location location, UUID ownerId) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        double maxRange = 12.0;
        double maxRangeSquared = maxRange * maxRange;
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
        GameSession ownerSession = ownerId == null ? null : sessions.getSessionForPlayer(ownerId);
        for (Player target : world.getPlayers()) {
            if (!isDamageablePlayer(target)) {
                continue;
            }
            if (target.getLocation().distanceSquared(location) > maxRangeSquared) {
                continue;
            }
            if (ownerSession != null && sessions.findSession(target) != ownerSession) {
                continue;
            }
            Location eye = target.getEyeLocation();
            Vector toFlash = location.toVector().subtract(eye.toVector());
            double distance = toFlash.length();
            if (distance <= 0.001) {
                distance = 0.001;
            }
            toFlash.multiply(1.0 / distance);
            double facing = eye.getDirection().normalize().dot(toFlash);
            double facingFactor = clamp01((facing + 1.0) * 0.5);
            double distanceFactor = clamp01(1.0 - distance / maxRange);
            double intensity = distanceFactor * (0.25 + 0.75 * facingFactor);
            if (projectile != null && !target.hasLineOfSight(projectile)) {
                intensity *= 0.35;
            }
            if (intensity < 0.08) {
                continue;
            }
            int durationTicks = clampInt((int) Math.round(20 + intensity * 120), 20, 140);
            int amplifier = intensity >= 0.75 ? 1 : 0;
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, amplifier, true, false));
        }
    }

    private void smoke(Location location, UUID ownerId) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        AreaEffectCloud cloud = world.spawn(location, AreaEffectCloud.class);
        cloud.setDuration(200);
        cloud.setRadius(4.0f);
        cloud.setParticle(Particle.CLOUD);
        world.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.0f);
        GameSession ownerSession = ownerId == null ? null : sessions.getSessionForPlayer(ownerId);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 200 || !cloud.isValid()) {
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.CLOUD, location, 20, 2.8, 1.6, 2.8, 0.01);
                world.spawnParticle(Particle.SMOKE, location, 10, 2.2, 1.2, 2.2, 0.001);
                for (Player target : world.getPlayers()) {
                    if (!target.isOnline()) {
                        continue;
                    }
                    if (target.getGameMode() == org.bukkit.GameMode.SPECTATOR || target.isDead()) {
                        continue;
                    }
                    if (ownerSession != null && sessions.findSession(target) != ownerSession) {
                        continue;
                    }
                    if (target.getLocation().distanceSquared(location) > 16.0) {
                        continue;
                    }
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 12, 0, true, false));
                }
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void molotov(Location location, UUID ownerId) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        AreaEffectCloud cloud = world.spawn(location, AreaEffectCloud.class);
        cloud.setDuration(120);
        cloud.setRadius(3.0f);
        cloud.setParticle(Particle.FLAME);
        world.playSound(location, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.1f);
        GameSession ownerSession = ownerId == null ? null : sessions.getSessionForPlayer(ownerId);
        Player owner = ownerId == null ? null : Bukkit.getPlayer(ownerId);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 120 || !cloud.isValid()) {
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.FLAME, location, 12, 2.2, 0.4, 2.2, 0.003);
                for (Player target : world.getPlayers()) {
                    if (!isDamageablePlayer(target)) {
                        continue;
                    }
                    if (target.getLocation().distanceSquared(location) > 16.0) {
                        continue;
                    }
                    if (ownerSession != null && sessions.findSession(target) != ownerSession) {
                        continue;
                    }
                    if (ownerSession != null && ownerId != null) {
                        if (shouldBlockFriendlyDamage(ownerSession, ownerId, target.getUniqueId())) {
                            continue;
                        }
                    }
                    target.setFireTicks(Math.max(target.getFireTicks(), 60));
                    if (owner != null) {
                        target.damage(1.5, owner);
                    } else {
                        target.damage(1.5);
                    }
                }
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void decoy(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.ENTITY_SNOWBALL_THROW, 0.6f, 1.0f);
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 200) {
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.SMOKE, location, 6, 0.4, 0.2, 0.4, 0.01);
                world.playSound(location, Sound.ENTITY_SNOWBALL_THROW, 0.4f, 0.8f);
                ticks += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private boolean shouldBlockFriendlyDamage(GameSession ownerSession, UUID ownerId, UUID targetId) {
        if (ownerSession == null || ownerId == null || targetId == null) {
            return false;
        }
        TeamSide attackerSide = ownerSession.getSide(ownerId);
        TeamSide targetSide = ownerSession.getSide(targetId);
        return attackerSide == targetSide
            && attackerSide != TeamSide.SPECTATOR
            && !ownerSession.rules().friendlyFireEnabled();
    }

    private boolean isDamageablePlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        return player.getGameMode() != org.bukkit.GameMode.SPECTATOR && !player.isDead() && player.getHealth() > 0.0;
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
