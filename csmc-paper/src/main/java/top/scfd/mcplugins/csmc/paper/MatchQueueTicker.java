package top.scfd.mcplugins.csmc.paper;

import org.bukkit.scheduler.BukkitRunnable;

public final class MatchQueueTicker extends BukkitRunnable {
    private final MatchQueueService queue;

    public MatchQueueTicker(MatchQueueService queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        queue.tick();
    }
}
