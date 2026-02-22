package top.scfd.mcplugins.csmc.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerSessionListener implements Listener {
    private final SessionRegistry sessions;

    public PlayerSessionListener(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.leaveSession(event.getPlayer());
    }
}
