package top.scfd.mcplugins.csmc.storage;

import java.util.List;
import java.util.UUID;

public final class StorageManager {
    private final StorageProvider provider;

    public StorageManager(StorageProvider provider) {
        this.provider = provider;
    }

    public StorageProvider provider() {
        return provider;
    }

    public void savePlayerStats(UUID playerId, PlayerStats stats) {
        provider.savePlayerStats(playerId, stats);
    }

    public PlayerStats loadPlayerStats(UUID playerId) {
        return provider.loadPlayerStats(playerId);
    }

    public List<LeaderboardEntry> topPlayersByKills(int limit) {
        return provider.topPlayersByKills(limit);
    }

    public void shutdown() {
        try {
            provider.close();
        } catch (Exception ignored) {
            // no-op for now
        }
    }
}
