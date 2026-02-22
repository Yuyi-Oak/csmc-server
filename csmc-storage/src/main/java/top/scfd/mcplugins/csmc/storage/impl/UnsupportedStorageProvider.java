package top.scfd.mcplugins.csmc.storage.impl;

import java.util.UUID;
import top.scfd.mcplugins.csmc.storage.PlayerStats;
import top.scfd.mcplugins.csmc.storage.StorageProvider;
import top.scfd.mcplugins.csmc.storage.StorageType;

public final class UnsupportedStorageProvider implements StorageProvider {
    private final StorageType type;
    private final String reason;

    public UnsupportedStorageProvider(StorageType type, String reason) {
        this.type = type;
        this.reason = reason;
    }

    @Override
    public StorageType type() {
        return type;
    }

    @Override
    public void savePlayerStats(UUID playerId, PlayerStats stats) {
        throw new UnsupportedOperationException(reason);
    }

    @Override
    public PlayerStats loadPlayerStats(UUID playerId) {
        throw new UnsupportedOperationException(reason);
    }
}
