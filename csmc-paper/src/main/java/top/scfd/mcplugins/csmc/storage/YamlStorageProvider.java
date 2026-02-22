package top.scfd.mcplugins.csmc.storage;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import top.scfd.mcplugins.csmc.storage.PlayerStats;
import top.scfd.mcplugins.csmc.storage.StorageException;
import top.scfd.mcplugins.csmc.storage.StorageProvider;
import top.scfd.mcplugins.csmc.storage.StorageType;

public final class YamlStorageProvider implements StorageProvider {
    private final File file;
    private final YamlConfiguration config;

    public YamlStorageProvider(File file) {
        this.file = file;
        this.file.getParentFile().mkdirs();
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public StorageType type() {
        return StorageType.YAML;
    }

    @Override
    public synchronized void savePlayerStats(UUID playerId, PlayerStats stats) {
        String base = "players." + playerId;
        config.set(base + ".kills", stats.kills());
        config.set(base + ".deaths", stats.deaths());
        config.set(base + ".assists", stats.assists());
        config.set(base + ".headshots", stats.headshots());
        config.set(base + ".roundsPlayed", stats.roundsPlayed());
        config.set(base + ".roundsWon", stats.roundsWon());
        try {
            config.save(file);
        } catch (IOException error) {
            throw new StorageException("Failed to save YAML storage", error);
        }
    }

    @Override
    public synchronized PlayerStats loadPlayerStats(UUID playerId) {
        String base = "players." + playerId;
        if (!config.contains(base)) {
            return PlayerStats.empty();
        }
        return new PlayerStats(
            config.getLong(base + ".kills"),
            config.getLong(base + ".deaths"),
            config.getLong(base + ".assists"),
            config.getLong(base + ".headshots"),
            config.getLong(base + ".roundsPlayed"),
            config.getLong(base + ".roundsWon")
        );
    }
}
