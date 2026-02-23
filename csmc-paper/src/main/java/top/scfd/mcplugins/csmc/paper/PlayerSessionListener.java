package top.scfd.mcplugins.csmc.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import top.scfd.mcplugins.csmc.api.TeamSide;

public final class PlayerSessionListener implements Listener {
    private final SessionRegistry sessions;
    private final TeamEliminationResolver eliminations;

    public PlayerSessionListener(SessionRegistry sessions, TeamEliminationResolver eliminations) {
        this.sessions = sessions;
        this.eliminations = eliminations;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var session = sessions.findSession(event.getPlayer());
        TeamSide side = session == null ? TeamSide.SPECTATOR : session.getSide(event.getPlayer().getUniqueId());
        sessions.leaveSession(event.getPlayer());
        if (session != null) {
            eliminations.resolveIfEliminated(session, side);
        }
    }
}
