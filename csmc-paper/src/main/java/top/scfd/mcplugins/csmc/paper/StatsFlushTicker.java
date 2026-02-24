package top.scfd.mcplugins.csmc.paper;

import org.bukkit.scheduler.BukkitRunnable;

public final class StatsFlushTicker extends BukkitRunnable {
    private final StatsService statsService;
    private final int batchSize;

    public StatsFlushTicker(StatsService statsService, int batchSize) {
        this.statsService = statsService;
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    public void run() {
        statsService.flushDirty(batchSize);
    }
}
