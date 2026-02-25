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
            case "flashbang" -> flash(location, ownerId);
            case "smoke" -> smoke(location);
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
            if (target.getLocation().distanceSquared(location) > 25.0) {
                continue;
            }
            if (ownerSession != null && sessions.findSession(target) != ownerSession) {
                continue;
            }
            if (ownerSession != null && ownerId != null) {
                TeamSide attackerSide = ownerSession.getSide(ownerId);
                TeamSide targetSide = ownerSession.getSide(target.getUniqueId());
                if (attackerSide == targetSide && attackerSide != TeamSide.SPECTATOR) {
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

    private void flash(Location location, UUID ownerId) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
        GameSession ownerSession = ownerId == null ? null : sessions.getSessionForPlayer(ownerId);
        for (Player target : world.getPlayers()) {
            if (target.getLocation().distanceSquared(location) > 36.0) {
                continue;
            }
            if (ownerSession != null && sessions.findSession(target) != ownerSession) {
                continue;
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1, true, false));
        }
    }

    private void smoke(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        AreaEffectCloud cloud = world.spawn(location, AreaEffectCloud.class);
        cloud.setDuration(200);
        cloud.setRadius(4.0f);
        cloud.setParticle(Particle.CLOUD);
        world.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.0f);
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
        for (Player target : world.getPlayers()) {
            if (target.getLocation().distanceSquared(location) > 16.0) {
                continue;
            }
            if (ownerSession != null && sessions.findSession(target) != ownerSession) {
                continue;
            }
            if (ownerSession != null && ownerId != null) {
                TeamSide attackerSide = ownerSession.getSide(ownerId);
                TeamSide targetSide = ownerSession.getSide(target.getUniqueId());
                if (attackerSide == targetSide && attackerSide != TeamSide.SPECTATOR) {
                    continue;
                }
            }
            target.setFireTicks(80);
        }
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
}
