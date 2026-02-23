package top.scfd.mcplugins.csmc.paper;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class FreezePhaseListener implements Listener {
    private final SessionRegistry sessions;

    public FreezePhaseListener(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameSession session = sessions.findSession(player);
        if (session == null || session.roundEngine().phase() != RoundPhase.FREEZE) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }
        Location locked = from.clone();
        locked.setYaw(to.getYaw());
        locked.setPitch(to.getPitch());
        event.setTo(locked);
    }
}
