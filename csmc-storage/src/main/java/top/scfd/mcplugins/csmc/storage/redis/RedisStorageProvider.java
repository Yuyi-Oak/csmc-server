package top.scfd.mcplugins.csmc.storage.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import top.scfd.mcplugins.csmc.storage.LeaderboardEntry;
import top.scfd.mcplugins.csmc.storage.PlayerStats;
import top.scfd.mcplugins.csmc.storage.StorageException;
import top.scfd.mcplugins.csmc.storage.StorageProvider;
import top.scfd.mcplugins.csmc.storage.StorageType;

public final class RedisStorageProvider implements StorageProvider {
    private static final String LEADERBOARD_KILLS_SUFFIX = ":leaderboard:kills";

    private final String namespace;
    private final String leaderboardKillsKey;
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    public RedisStorageProvider(String uri, String namespace) {
        this.namespace = (namespace == null || namespace.isBlank()) ? "csmc" : namespace;
        this.leaderboardKillsKey = this.namespace + LEADERBOARD_KILLS_SUFFIX;
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
            commands.zadd(leaderboardKillsKey, stats.kills(), playerId.toString());
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
            return toStats(values);
        } catch (RuntimeException error) {
            throw new StorageException("Failed to load Redis stats for player " + playerId, error);
        }
    }

    @Override
    public List<LeaderboardEntry> topPlayersByKills(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<LeaderboardEntry> entries = topFromLeaderboardIndex(limit);
        if (!entries.isEmpty()) {
            return entries;
        }
        return topByScanningHashes(limit);
    }

    private List<LeaderboardEntry> topFromLeaderboardIndex(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        List<String> ids;
        try {
            ids = commands.zrevrange(leaderboardKillsKey, 0, Math.max(limit * 3L, limit) - 1L);
        } catch (RuntimeException error) {
            throw new StorageException("Failed to load Redis leaderboard index", error);
        }
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        for (String value : ids) {
            UUID playerId = parseUuid(value);
            if (playerId == null) {
                continue;
            }
            Map<String, String> stats = commands.hgetall(key(playerId));
            if (stats == null || stats.isEmpty()) {
                continue;
            }
            entries.add(new LeaderboardEntry(playerId, toStats(stats)));
            if (entries.size() >= limit) {
                break;
            }
        }
        entries.sort((left, right) -> {
            int byKills = Long.compare(right.stats().kills(), left.stats().kills());
            if (byKills != 0) {
                return byKills;
            }
            return Long.compare(right.stats().roundsWon(), left.stats().roundsWon());
        });
        return entries.size() <= limit ? entries : List.copyOf(entries.subList(0, limit));
    }

    private List<LeaderboardEntry> topByScanningHashes(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        String prefix = namespace + ":player-stats:";
        try {
            ScanCursor cursor = ScanCursor.INITIAL;
            do {
                KeyScanCursor<String> page = commands.scan(cursor, ScanArgs.Builder.matches(prefix + "*").limit(500));
                for (String key : page.getKeys()) {
                    UUID playerId = parseUuid(key, prefix);
                    if (playerId == null) {
                        continue;
                    }
                    Map<String, String> values = commands.hgetall(key);
                    if (values == null || values.isEmpty()) {
                        continue;
                    }
                    entries.add(new LeaderboardEntry(playerId, toStats(values)));
                }
                cursor = page;
            } while (!cursor.isFinished());
        } catch (RuntimeException error) {
            throw new StorageException("Failed to load Redis leaderboard via scan", error);
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

    private PlayerStats toStats(Map<String, String> values) {
        return new PlayerStats(
            parseLong(values.get("kills")),
            parseLong(values.get("deaths")),
            parseLong(values.get("assists")),
            parseLong(values.get("headshots")),
            parseLong(values.get("roundsPlayed")),
            parseLong(values.get("roundsWon"))
        );
    }

    private UUID parseUuid(String key, String prefix) {
        if (key == null || key.length() <= prefix.length() || !key.startsWith(prefix)) {
            return null;
        }
        try {
            return UUID.fromString(key.substring(prefix.length()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
