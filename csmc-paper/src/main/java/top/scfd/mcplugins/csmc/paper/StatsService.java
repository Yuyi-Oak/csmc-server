package top.scfd.mcplugins.csmc.paper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import top.scfd.mcplugins.csmc.paper.sync.ClusterSyncService;
import top.scfd.mcplugins.csmc.storage.LeaderboardEntry;
import top.scfd.mcplugins.csmc.storage.PlayerStats;
import top.scfd.mcplugins.csmc.storage.StorageManager;

public final class StatsService {
    private final StorageManager storageManager;
    private final ClusterSyncService clusterSync;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();

    public StatsService(StorageManager storageManager, ClusterSyncService clusterSync) {
        this.storageManager = storageManager;
        this.clusterSync = clusterSync;
        this.clusterSync.onStatsUpdated(this::invalidate);
    }

    public PlayerStats get(UUID playerId) {
        return cache.computeIfAbsent(playerId, storageManager::loadPlayerStats);
    }

    public List<LeaderboardEntry> topByKills(int limit) {
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
        storageManager.savePlayerStats(playerId, updated);
        clusterSync.publishStatsUpdated(playerId);
    }

    private void invalidate(UUID playerId) {
        if (playerId != null) {
            cache.remove(playerId);
        }
    }
}
