package top.scfd.mcplugins.csmc.storage.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import top.scfd.mcplugins.csmc.storage.LeaderboardEntry;
import top.scfd.mcplugins.csmc.storage.PlayerStats;
import top.scfd.mcplugins.csmc.storage.StorageException;
import top.scfd.mcplugins.csmc.storage.StorageProvider;
import top.scfd.mcplugins.csmc.storage.StorageType;

public final class JdbcStorageProvider implements StorageProvider {
    private final StorageType type;
    private final JdbcDialect dialect;
    private final String url;
    private final String username;
    private final String password;

    public JdbcStorageProvider(StorageType type, JdbcDialect dialect, String url, String username, String password) {
        this.type = type;
        this.dialect = dialect;
        this.url = url;
        this.username = username;
        this.password = password;
        initSchema();
    }

    @Override
    public StorageType type() {
        return type;
    }

    @Override
    public void savePlayerStats(UUID playerId, PlayerStats stats) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(upsertSql())) {
            statement.setString(1, playerId.toString());
            statement.setLong(2, stats.kills());
            statement.setLong(3, stats.deaths());
            statement.setLong(4, stats.assists());
            statement.setLong(5, stats.headshots());
            statement.setLong(6, stats.roundsPlayed());
            statement.setLong(7, stats.roundsWon());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new StorageException("Failed to save player stats", error);
        }
    }

    @Override
    public PlayerStats loadPlayerStats(UUID playerId) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(selectSql())) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return PlayerStats.empty();
                }
                return toStats(resultSet);
            }
        } catch (SQLException error) {
            throw new StorageException("Failed to load player stats", error);
        }
    }

    @Override
    public List<LeaderboardEntry> topPlayersByKills(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(topByKillsSql())) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID playerId = parseUuid(resultSet.getString("player_id"));
                    if (playerId == null) {
                        continue;
                    }
                    entries.add(new LeaderboardEntry(playerId, toStats(resultSet)));
                }
            }
            return entries;
        } catch (SQLException error) {
            throw new StorageException("Failed to load leaderboard", error);
        }
    }

    private Connection open() throws SQLException {
        if (username == null || username.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password == null ? "" : password);
    }

    private void initSchema() {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableSql());
        } catch (SQLException error) {
            throw new StorageException("Failed to initialize storage schema", error);
        }
    }

    private String createTableSql() {
        return "CREATE TABLE IF NOT EXISTS csmc_player_stats (" +
            "player_id VARCHAR(36) PRIMARY KEY, " +
            "kills BIGINT, " +
            "deaths BIGINT, " +
            "assists BIGINT, " +
            "headshots BIGINT, " +
            "rounds_played BIGINT, " +
            "rounds_won BIGINT" +
            ")";
    }

    private String selectSql() {
        return "SELECT kills, deaths, assists, headshots, rounds_played, rounds_won " +
            "FROM csmc_player_stats WHERE player_id=?";
    }

    private String topByKillsSql() {
        return "SELECT player_id, kills, deaths, assists, headshots, rounds_played, rounds_won " +
            "FROM csmc_player_stats ORDER BY kills DESC, rounds_won DESC LIMIT ?";
    }

    private String upsertSql() {
        return switch (dialect) {
            case MYSQL -> "INSERT INTO csmc_player_stats " +
                "(player_id, kills, deaths, assists, headshots, rounds_played, rounds_won) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "kills=VALUES(kills), deaths=VALUES(deaths), assists=VALUES(assists), " +
                "headshots=VALUES(headshots), rounds_played=VALUES(rounds_played), rounds_won=VALUES(rounds_won)";
            case SQLITE, POSTGRESQL -> "INSERT INTO csmc_player_stats " +
                "(player_id, kills, deaths, assists, headshots, rounds_played, rounds_won) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (player_id) DO UPDATE SET " +
                "kills=excluded.kills, deaths=excluded.deaths, assists=excluded.assists, " +
                "headshots=excluded.headshots, rounds_played=excluded.rounds_played, rounds_won=excluded.rounds_won";
        };
    }

    private PlayerStats toStats(ResultSet resultSet) throws SQLException {
        return new PlayerStats(
            resultSet.getLong("kills"),
            resultSet.getLong("deaths"),
            resultSet.getLong("assists"),
            resultSet.getLong("headshots"),
            resultSet.getLong("rounds_played"),
            resultSet.getLong("rounds_won")
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
