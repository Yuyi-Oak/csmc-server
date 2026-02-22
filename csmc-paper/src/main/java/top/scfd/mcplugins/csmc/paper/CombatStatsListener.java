package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.entity.Projectile;

public final class CombatStatsListener implements Listener {
    private final SessionRegistry sessions;

    public CombatStatsListener(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (sessions.findSession(victim) == null) {
            return;
        }
        if (sessions.findSession(attacker) == null) {
            return;
        }
        // Placeholder for future damage tracking (headshots, weapon type, etc.)
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }
        UUID killerId = killer.getUniqueId();
        if (sessions.findSession(killer) == null) {
            return;
        }
        sessions.findSession(killer).onKill(killerId);
        // Assist logic will be added later with damage tracking.
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
