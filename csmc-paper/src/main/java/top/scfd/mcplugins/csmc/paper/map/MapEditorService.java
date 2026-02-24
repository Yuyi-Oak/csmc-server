package top.scfd.mcplugins.csmc.paper.map;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.config.PaperMapLoader;
import top.scfd.mcplugins.csmc.core.map.BombSite;
import top.scfd.mcplugins.csmc.core.map.BuyZone;
import top.scfd.mcplugins.csmc.core.map.MapRegistry;
import top.scfd.mcplugins.csmc.core.map.MapSpawn;

public final class MapEditorService {
    private final JavaPlugin plugin;
    private final PaperMapLoader mapLoader;
    private final MapRegistry mapRegistry;
    private final File mapsDir;
    private final Map<String, EditableMap> cache = new ConcurrentHashMap<>();

    public MapEditorService(JavaPlugin plugin, PaperMapLoader mapLoader, MapRegistry mapRegistry) {
        this.plugin = plugin;
        this.mapLoader = mapLoader;
        this.mapRegistry = mapRegistry;
        this.mapsDir = new File(plugin.getDataFolder(), "maps");
        this.mapsDir.mkdirs();
    }

    public List<String> listMaps() {
        TreeSet<String> ids = new TreeSet<>();
        File[] files = mapsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                ids.add(name.substring(0, name.length() - 4));
            }
        }
        ids.addAll(cache.keySet());
        return List.copyOf(ids);
    }

    public EditableMap get(String id) {
        String key = normalizeId(id);
        if (key == null) {
            return null;
        }
        EditableMap cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        EditableMap loaded = loadFromFile(key);
        if (loaded != null) {
            cache.put(key, loaded);
        }
        return loaded;
    }

    public EditableMap create(String id, String name, String world) {
        String key = normalizeId(id);
        if (key == null) {
            return null;
        }
        if (cache.containsKey(key) || fileFor(key).exists()) {
            return null;
        }
        EditableMap map = new EditableMap(
            key,
            (name == null || name.isBlank()) ? key : name,
            (world == null || world.isBlank()) ? "world" : world
        );
        cache.put(key, map);
        return map;
    }

    public void addSpawn(EditableMap map, TeamSide side, MapSpawn spawn) {
        List<MapSpawn> spawns = side == TeamSide.TERRORIST
            ? map.terroristSpawns()
            : map.counterTerroristSpawns();
        spawns.add(spawn);
    }

    public int clearSpawns(EditableMap map, TeamSide side) {
        List<MapSpawn> spawns = side == TeamSide.TERRORIST
            ? map.terroristSpawns()
            : map.counterTerroristSpawns();
        int size = spawns.size();
        spawns.clear();
        return size;
    }

    public void addBuyZone(EditableMap map, TeamSide side, BuyZone zone) {
        List<BuyZone> zones = side == TeamSide.TERRORIST
            ? map.terroristBuyZones()
            : map.counterTerroristBuyZones();
        zones.add(zone);
    }

    public int clearBuyZones(EditableMap map, TeamSide side) {
        List<BuyZone> zones = side == TeamSide.TERRORIST
            ? map.terroristBuyZones()
            : map.counterTerroristBuyZones();
        int size = zones.size();
        zones.clear();
        return size;
    }

    public void setBombSite(EditableMap map, String siteId, BombSite bombSite) {
        map.bombSites().put(siteId, bombSite);
    }

    public boolean removeBombSite(EditableMap map, String siteId) {
        return map.bombSites().remove(siteId) != null;
    }

    public boolean save(EditableMap map) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("map.id", map.id());
        config.set("map.name", map.name());
        config.set("map.world", map.world());
        config.set("map.spawns.terrorist", spawnEntries(map.terroristSpawns()));
        config.set("map.spawns.counter_terrorist", spawnEntries(map.counterTerroristSpawns()));
        for (Map.Entry<String, BombSite> entry : map.bombSites().entrySet()) {
            String base = "map.bomb_sites." + entry.getKey();
            BombSite site = entry.getValue();
            config.set(base + ".x", site.x());
            config.set(base + ".y", site.y());
            config.set(base + ".z", site.z());
            config.set(base + ".radius", site.radius());
        }
        config.set("map.buy_zones.terrorist", buyZoneEntries(map.terroristBuyZones()));
        config.set("map.buy_zones.counter_terrorist", buyZoneEntries(map.counterTerroristBuyZones()));
        File target = fileFor(map.id());
        try {
            config.save(target);
            cache.put(map.id(), map);
            mapLoader.loadInto(mapRegistry);
            return true;
        } catch (IOException error) {
            plugin.getLogger().warning("Failed to save map " + map.id() + ": " + error.getMessage());
            return false;
        }
    }

    public void reload() {
        cache.clear();
        mapLoader.loadInto(mapRegistry);
    }

    private File fileFor(String id) {
        return new File(mapsDir, id + ".yml");
    }

    private EditableMap loadFromFile(String id) {
        File file = fileFor(id);
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String mapId = normalizeId(config.getString("map.id", id));
        if (mapId == null) {
            return null;
        }
        EditableMap map = new EditableMap(
            mapId,
            config.getString("map.name", mapId),
            config.getString("map.world", "world")
        );
        map.terroristSpawns().addAll(loadSpawns(config, "map.spawns.terrorist"));
        map.counterTerroristSpawns().addAll(loadSpawns(config, "map.spawns.counter_terrorist"));
        map.terroristBuyZones().addAll(loadBuyZones(config, "map.buy_zones.terrorist"));
        map.counterTerroristBuyZones().addAll(loadBuyZones(config, "map.buy_zones.counter_terrorist"));

        ConfigurationSection bombSites = config.getConfigurationSection("map.bomb_sites");
        if (bombSites != null) {
            for (String siteId : bombSites.getKeys(false)) {
                String base = siteId + ".";
                map.bombSites().put(siteId, new BombSite(
                    siteId,
                    bombSites.getDouble(base + "x"),
                    bombSites.getDouble(base + "y"),
                    bombSites.getDouble(base + "z"),
                    bombSites.getDouble(base + "radius")
                ));
            }
        }
        return map;
    }

    private List<MapSpawn> loadSpawns(YamlConfiguration config, String path) {
        List<MapSpawn> spawns = new ArrayList<>();
        for (Map<?, ?> entry : config.getMapList(path)) {
            spawns.add(new MapSpawn(
                toDouble(entry, "x"),
                toDouble(entry, "y"),
                toDouble(entry, "z"),
                (float) toDouble(entry, "yaw"),
                (float) toDouble(entry, "pitch")
            ));
        }
        return spawns;
    }

    private List<BuyZone> loadBuyZones(YamlConfiguration config, String path) {
        List<BuyZone> zones = new ArrayList<>();
        for (Map<?, ?> entry : config.getMapList(path)) {
            zones.add(new BuyZone(
                toDouble(entry, "x"),
                toDouble(entry, "y"),
                toDouble(entry, "z"),
                toDouble(entry, "radius")
            ));
        }
        return zones;
    }

    private List<Map<String, Object>> spawnEntries(List<MapSpawn> spawns) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (MapSpawn spawn : spawns) {
            entries.add(Map.of(
                "x", spawn.x(),
                "y", spawn.y(),
                "z", spawn.z(),
                "yaw", spawn.yaw(),
                "pitch", spawn.pitch()
            ));
        }
        return entries;
    }

    private List<Map<String, Object>> buyZoneEntries(List<BuyZone> zones) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (BuyZone zone : zones) {
            entries.add(Map.of(
                "x", zone.x(),
                "y", zone.y(),
                "z", zone.z(),
                "radius", zone.radius()
            ));
        }
        return entries;
    }

    private double toDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private String normalizeId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !normalized.matches("[a-z0-9_\\-]+")) {
            return null;
        }
        return normalized;
    }
}
