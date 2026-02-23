package top.scfd.mcplugins.csmc.storage.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import top.scfd.mcplugins.csmc.storage.LeaderboardEntry;
import top.scfd.mcplugins.csmc.storage.PlayerStats;
import top.scfd.mcplugins.csmc.storage.StorageException;
import top.scfd.mcplugins.csmc.storage.StorageProvider;
import top.scfd.mcplugins.csmc.storage.StorageType;

public final class MongoStorageProvider implements StorageProvider {
    private static final String COLLECTION_NAME = "csmc_player_stats";

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoStorageProvider(String uri, String database) {
        String resolvedDatabase = (database == null || database.isBlank()) ? "csmc" : database;
        this.client = MongoClients.create(uri);
        this.collection = client.getDatabase(resolvedDatabase).getCollection(COLLECTION_NAME);
    }

    @Override
    public StorageType type() {
        return StorageType.MONGODB;
    }

    @Override
    public void savePlayerStats(UUID playerId, PlayerStats stats) {
        Document document = new Document("_id", playerId.toString())
            .append("kills", stats.kills())
            .append("deaths", stats.deaths())
            .append("assists", stats.assists())
            .append("headshots", stats.headshots())
            .append("roundsPlayed", stats.roundsPlayed())
            .append("roundsWon", stats.roundsWon());

        try {
            collection.replaceOne(
                Filters.eq("_id", playerId.toString()),
                document,
                new ReplaceOptions().upsert(true)
            );
        } catch (RuntimeException error) {
            throw new StorageException("Failed to save MongoDB stats for player " + playerId, error);
        }
    }

    @Override
    public PlayerStats loadPlayerStats(UUID playerId) {
        try {
            Document document = collection.find(Filters.eq("_id", playerId.toString())).first();
            if (document == null) {
                return PlayerStats.empty();
            }
            return toStats(document);
        } catch (RuntimeException error) {
            throw new StorageException("Failed to load MongoDB stats for player " + playerId, error);
        }
    }

    @Override
    public List<LeaderboardEntry> topPlayersByKills(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<LeaderboardEntry> entries = new ArrayList<>();
        try {
            for (Document document : collection.find()
                .sort(Sorts.orderBy(Sorts.descending("kills"), Sorts.descending("roundsWon")))
                .limit(limit)) {
                UUID playerId = parseUuid(document.getString("_id"));
                if (playerId == null) {
                    continue;
                }
                entries.add(new LeaderboardEntry(playerId, toStats(document)));
            }
            return entries;
        } catch (RuntimeException error) {
            throw new StorageException("Failed to load MongoDB leaderboard", error);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private long longValue(Document document, String key) {
        Object value = document.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private PlayerStats toStats(Document document) {
        return new PlayerStats(
            longValue(document, "kills"),
            longValue(document, "deaths"),
            longValue(document, "assists"),
            longValue(document, "headshots"),
            longValue(document, "roundsPlayed"),
            longValue(document, "roundsWon")
        );
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
