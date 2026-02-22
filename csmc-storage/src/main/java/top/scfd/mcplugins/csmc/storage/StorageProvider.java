package top.scfd.mcplugins.csmc.storage;

import java.util.UUID;

public interface StorageProvider {
    StorageType type();

    void savePlayerStats(UUID playerId, PlayerStats stats);

    PlayerStats loadPlayerStats(UUID playerId);
}
