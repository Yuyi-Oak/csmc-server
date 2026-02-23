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
import top.scfd.mcplugins.csmc.api.TeamSide;

public final class CombatStatsListener implements Listener {
    private final SessionRegistry sessions;
    private final StatsService statsService;
    private final TeamEliminationResolver eliminations;
    private final java.util.Map<java.util.UUID, java.util.UUID> lastHit = new java.util.concurrent.ConcurrentHashMap<>();

    public CombatStatsListener(SessionRegistry sessions, StatsService statsService, TeamEliminationResolver eliminations) {
        this.sessions = sessions;
        this.statsService = statsService;
        this.eliminations = eliminations;
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
        var victimSession = sessions.findSession(victim);
        if (victimSession == null) {
            return;
        }
        var attackerSession = sessions.findSession(attacker);
        if (attackerSession == null || attackerSession != victimSession) {
            return;
        }
        // Placeholder for future damage tracking (headshots, weapon type, etc.)
        lastHit.put(victim.getUniqueId(), attacker.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        var victimSession = sessions.findSession(victim);
        if (victimSession == null) {
            return;
        }
        TeamSide victimSide = victimSession.getSide(victim.getUniqueId());
        statsService.recordDeath(victim.getUniqueId());
        UUID assisterId = lastHit.remove(victim.getUniqueId());

        Player killer = victim.getKiller();
        if (killer != null) {
            UUID killerId = killer.getUniqueId();
            var killerSession = sessions.findSession(killer);
            if (killerSession != null && killerSession == victimSession && !killerId.equals(victim.getUniqueId())) {
                killerSession.onKill(killerId);
                statsService.recordKill(killerId);
                if (assisterId != null && !assisterId.equals(killerId)) {
                    killerSession.onAssist(assisterId);
                    statsService.recordAssist(assisterId);
                }
            }
        }
        eliminations.resolveIfEliminated(victimSession, victimSide);
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
