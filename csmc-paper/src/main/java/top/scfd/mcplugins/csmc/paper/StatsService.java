package top.scfd.mcplugins.csmc.paper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import top.scfd.mcplugins.csmc.paper.sync.ClusterSyncService;
import top.scfd.mcplugins.csmc.storage.LeaderboardEntry;
import top.scfd.mcplugins.csmc.storage.PlayerStats;
import top.scfd.mcplugins.csmc.storage.StorageManager;

public final class StatsService {
    private static final int MAX_FLUSH_BATCH = 1024;

    private final StorageManager storageManager;
    private final ClusterSyncService clusterSync;
    private final Logger logger;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    public StatsService(StorageManager storageManager, ClusterSyncService clusterSync, Logger logger) {
        this.storageManager = storageManager;
        this.clusterSync = clusterSync;
        this.logger = logger;
        this.clusterSync.onStatsUpdated(this::invalidate);
    }

    public PlayerStats get(UUID playerId) {
        return cache.computeIfAbsent(playerId, storageManager::loadPlayerStats);
    }

    public List<LeaderboardEntry> topByKills(int limit) {
        flushDirty(MAX_FLUSH_BATCH);
        return storageManager.topPlayersByKills(limit);
    }

    public void recordKill(UUID playerId) {
        update(playerId, stats -> new PlayerStats(
            stats.kills() + 1,
            stats.deaths(),
            stats.assists(),
            stats.headshots(),
            stats.roundsPlayed(),
            stats.roundsWon()
        ));
    }

    public void recordDeath(UUID playerId) {
        update(playerId, stats -> new PlayerStats(
            stats.kills(),
            stats.deaths() + 1,
            stats.assists(),
            stats.headshots(),
            stats.roundsPlayed(),
            stats.roundsWon()
        ));
    }

    public void recordAssist(UUID playerId) {
        update(playerId, stats -> new PlayerStats(
            stats.kills(),
            stats.deaths(),
            stats.assists() + 1,
            stats.headshots(),
            stats.roundsPlayed(),
            stats.roundsWon()
        ));
    }

    public void recordHeadshot(UUID playerId) {
        update(playerId, stats -> new PlayerStats(
            stats.kills(),
            stats.deaths(),
            stats.assists(),
            stats.headshots() + 1,
            stats.roundsPlayed(),
            stats.roundsWon()
        ));
    }

    public void recordRoundPlayed(UUID playerId) {
        update(playerId, stats -> new PlayerStats(
            stats.kills(),
            stats.deaths(),
            stats.assists(),
            stats.headshots(),
            stats.roundsPlayed() + 1,
            stats.roundsWon()
        ));
    }

    public void recordRoundWon(UUID playerId) {
        update(playerId, stats -> new PlayerStats(
            stats.kills(),
            stats.deaths(),
            stats.assists(),
            stats.headshots(),
            stats.roundsPlayed(),
            stats.roundsWon() + 1
        ));
    }

    private void update(UUID playerId, UnaryOperator<PlayerStats> updater) {
        PlayerStats updated = updater.apply(get(playerId));
        cache.put(playerId, updated);
        dirty.add(playerId);
    }

    public int flushDirty(int maxEntries) {
        if (maxEntries <= 0 || dirty.isEmpty()) {
            return 0;
        }
        int flushed = 0;
        for (UUID playerId : dirty) {
            if (flushed >= maxEntries) {
                break;
            }
            if (!dirty.remove(playerId)) {
                continue;
            }
            PlayerStats stats = cache.get(playerId);
            if (stats == null) {
                continue;
            }
            try {
                storageManager.savePlayerStats(playerId, stats);
                clusterSync.publishStatsUpdated(playerId);
                flushed++;
            } catch (RuntimeException error) {
                dirty.add(playerId);
                if (logger != null) {
                    logger.warning("Failed to flush stats for " + playerId + ": " + error.getMessage());
                }
            }
        }
        return flushed;
    }

    public void flushAll() {
        while (flushDirty(MAX_FLUSH_BATCH) > 0) {
            // continue until no dirty stats remain
        }
    }

    private void invalidate(UUID playerId) {
        if (playerId != null) {
            cache.remove(playerId);
        }
    }
}
