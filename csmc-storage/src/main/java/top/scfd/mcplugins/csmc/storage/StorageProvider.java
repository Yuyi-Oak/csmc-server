package top.scfd.mcplugins.csmc.storage;

import java.util.List;
import java.util.UUID;

public interface StorageProvider extends AutoCloseable {
    StorageType type();

    void savePlayerStats(UUID playerId, PlayerStats stats);

    PlayerStats loadPlayerStats(UUID playerId);

    default List<LeaderboardEntry> topPlayersByKills(int limit) {
        return List.of();
    }

    @Override
    default void close() {
        // no-op
    }
}
