package top.scfd.mcplugins.csmc.paper;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class MapProtectionListener implements Listener {
    private final SessionRegistry sessions;

    public MapProtectionListener(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private boolean shouldBlock(Player player) {
        GameSession session = sessions.findSession(player);
        return session != null;
    }
}
