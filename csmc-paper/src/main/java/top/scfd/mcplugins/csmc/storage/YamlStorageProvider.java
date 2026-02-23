package top.scfd.mcplugins.csmc.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import top.scfd.mcplugins.csmc.storage.LeaderboardEntry;
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

    @Override
    public synchronized List<LeaderboardEntry> topPlayersByKills(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        var playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return List.of();
        }
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (String key : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            entries.add(new LeaderboardEntry(playerId, loadPlayerStats(playerId)));
        }
        entries.sort((left, right) -> {
            int byKills = Long.compare(right.stats().kills(), left.stats().kills());
            if (byKills != 0) {
                return byKills;
            }
            return Long.compare(right.stats().roundsWon(), left.stats().roundsWon());
        });
        if (entries.size() <= limit) {
            return entries;
        }
        return List.copyOf(entries.subList(0, limit));
    }
}
