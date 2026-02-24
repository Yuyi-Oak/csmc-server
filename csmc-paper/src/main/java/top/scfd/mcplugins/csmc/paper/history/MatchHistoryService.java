package top.scfd.mcplugins.csmc.paper.history;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MatchHistoryService {
    private static final int MAX_ENTRIES = 5000;

    private final JavaPlugin plugin;
    private final File file;
    private final List<MatchHistoryEntry> entries = new ArrayList<>();

    public MatchHistoryService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/match-history.yml");
        this.file.getParentFile().mkdirs();
        load();
    }

    public synchronized void append(MatchHistoryEntry entry) {
        if (entry == null) {
            return;
        }
        entries.add(entry);
        entries.sort(Comparator.comparingLong(MatchHistoryEntry::endedAtEpochSeconds).reversed());
        if (entries.size() > MAX_ENTRIES) {
            entries.subList(MAX_ENTRIES, entries.size()).clear();
        }
        save();
    }

    public synchronized List<MatchHistoryEntry> findByPlayer(UUID playerId, int limit) {
        if (playerId == null || limit <= 0) {
            return List.of();
        }
        List<MatchHistoryEntry> result = new ArrayList<>();
        for (MatchHistoryEntry entry : entries) {
            if (entry.players() != null && entry.players().contains(playerId)) {
                result.add(entry);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return List.copyOf(result);
    }

    private void load() {
        entries.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rawEntries = yaml.getMapList("matches");
        for (Map<?, ?> raw : rawEntries) {
            MatchHistoryEntry entry = parseEntry(raw);
            if (entry != null) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator.comparingLong(MatchHistoryEntry::endedAtEpochSeconds).reversed());
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (MatchHistoryEntry entry : entries) {
            serialized.add(Map.of(
                "sessionId", entry.sessionId().toString(),
                "mode", entry.mode(),
                "mapId", entry.mapId(),
                "winner", entry.winner(),
                "terroristScore", entry.terroristScore(),
                "counterTerroristScore", entry.counterTerroristScore(),
                "startedAt", entry.startedAtEpochSeconds(),
                "endedAt", entry.endedAtEpochSeconds(),
                "players", entry.players().stream().map(UUID::toString).toList()
            ));
        }
        yaml.set("matches", serialized);
        try {
            yaml.save(file);
        } catch (IOException error) {
            plugin.getLogger().warning("Failed to save match history: " + error.getMessage());
        }
    }

    private MatchHistoryEntry parseEntry(Map<?, ?> raw) {
        if (raw == null) {
            return null;
        }
        UUID sessionId = parseUuid(raw.get("sessionId"));
        if (sessionId == null) {
            return null;
        }
        String mode = text(raw.get("mode"), "UNKNOWN");
        String mapId = text(raw.get("mapId"), "unknown");
        String winner = text(raw.get("winner"), "NONE");
        int tScore = number(raw.get("terroristScore"));
        int ctScore = number(raw.get("counterTerroristScore"));
        long startedAt = longNumber(raw.get("startedAt"));
        long endedAt = longNumber(raw.get("endedAt"));
        List<UUID> players = parsePlayers(raw.get("players"));
        return new MatchHistoryEntry(
            sessionId,
            mode,
            mapId,
            winner,
            tScore,
            ctScore,
            startedAt,
            endedAt,
            players
        );
    }

    private List<UUID> parsePlayers(Object raw) {
        if (!(raw instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<UUID> parsed = new ArrayList<>();
        for (Object value : iterable) {
            UUID id = parseUuid(value);
            if (id != null) {
                parsed.add(id);
            }
        }
        return List.copyOf(parsed);
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private long longNumber(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
