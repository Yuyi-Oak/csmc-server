package top.scfd.mcplugins.csmc.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class QueuePlayerListener implements Listener {
    private final MatchQueueService queue;

    public QueuePlayerListener(MatchQueueService queue) {
        this.queue = queue;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        queue.leave(event.getPlayer().getUniqueId());
    }
}
