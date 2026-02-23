package top.scfd.mcplugins.csmc.storage.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.Map;
import java.util.UUID;
import top.scfd.mcplugins.csmc.storage.PlayerStats;
import top.scfd.mcplugins.csmc.storage.StorageException;
import top.scfd.mcplugins.csmc.storage.StorageProvider;
import top.scfd.mcplugins.csmc.storage.StorageType;

public final class RedisStorageProvider implements StorageProvider {
    private final String namespace;
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    public RedisStorageProvider(String uri, String namespace) {
        this.namespace = (namespace == null || namespace.isBlank()) ? "csmc" : namespace;
        this.client = RedisClient.create(uri);
        this.connection = client.connect();
        this.commands = connection.sync();
    }

    @Override
    public StorageType type() {
        return StorageType.REDIS;
    }

    @Override
    public void savePlayerStats(UUID playerId, PlayerStats stats) {
        String key = key(playerId);
        Map<String, String> values = Map.of(
            "kills", Long.toString(stats.kills()),
            "deaths", Long.toString(stats.deaths()),
            "assists", Long.toString(stats.assists()),
            "headshots", Long.toString(stats.headshots()),
            "roundsPlayed", Long.toString(stats.roundsPlayed()),
            "roundsWon", Long.toString(stats.roundsWon())
        );
        try {
            commands.hset(key, values);
        } catch (RuntimeException error) {
            throw new StorageException("Failed to save Redis stats for player " + playerId, error);
        }
    }

    @Override
    public PlayerStats loadPlayerStats(UUID playerId) {
        try {
            Map<String, String> values = commands.hgetall(key(playerId));
            if (values == null || values.isEmpty()) {
                return PlayerStats.empty();
            }
            return new PlayerStats(
                parseLong(values.get("kills")),
                parseLong(values.get("deaths")),
                parseLong(values.get("assists")),
                parseLong(values.get("headshots")),
                parseLong(values.get("roundsPlayed")),
                parseLong(values.get("roundsWon"))
            );
        } catch (RuntimeException error) {
            throw new StorageException("Failed to load Redis stats for player " + playerId, error);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } finally {
            client.shutdown();
        }
    }

    private String key(UUID playerId) {
        return namespace + ":player-stats:" + playerId;
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
