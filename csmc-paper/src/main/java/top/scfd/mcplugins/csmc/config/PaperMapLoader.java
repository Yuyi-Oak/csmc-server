package top.scfd.mcplugins.csmc.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.core.map.BombSite;
import top.scfd.mcplugins.csmc.core.map.BuyZone;
import top.scfd.mcplugins.csmc.core.map.MapDefinition;
import top.scfd.mcplugins.csmc.core.map.MapRegistry;
import top.scfd.mcplugins.csmc.core.map.MapSpawn;

public final class PaperMapLoader {
    private final JavaPlugin plugin;

    public PaperMapLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadInto(MapRegistry registry) {
        File mapsDir = new File(plugin.getDataFolder(), "maps");
        ensureTemplates(mapsDir);
        File[] files = mapsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        registry.clear();
        for (File file : files) {
            MapDefinition definition = loadDefinition(file);
            if (definition != null) {
                registry.register(definition);
            }
        }
    }

    private MapDefinition loadDefinition(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String id = config.getString("map.id");
        if (id == null || id.isBlank()) {
            return null;
        }
        String name = config.getString("map.name", id);
        String world = config.getString("map.world", id);
        List<MapSpawn> terroristSpawns = loadSpawns(config, "map.spawns.terrorist");
        List<MapSpawn> counterTerroristSpawns = loadSpawns(config, "map.spawns.counter_terrorist");
        List<BombSite> bombSites = loadBombSites(config, "map.bomb_sites");
        List<BuyZone> terroristBuyZones = loadBuyZones(config, "map.buy_zones.terrorist");
        List<BuyZone> counterTerroristBuyZones = loadBuyZones(config, "map.buy_zones.counter_terrorist");
        return new MapDefinition(
            id,
            name,
            world,
            terroristSpawns,
            counterTerroristSpawns,
            bombSites,
            terroristBuyZones,
            counterTerroristBuyZones
        );
    }

    private List<MapSpawn> loadSpawns(YamlConfiguration config, String path) {
        List<MapSpawn> spawns = new ArrayList<>();
        for (Map<?, ?> entry : config.getMapList(path)) {
            double x = readDouble(entry, "x");
            double y = readDouble(entry, "y");
            double z = readDouble(entry, "z");
            float yaw = readFloat(entry, "yaw");
            float pitch = readFloat(entry, "pitch");
            spawns.add(new MapSpawn(x, y, z, yaw, pitch));
        }
        return spawns;
    }

    private List<BuyZone> loadBuyZones(YamlConfiguration config, String path) {
        List<BuyZone> zones = new ArrayList<>();
        for (Map<?, ?> entry : config.getMapList(path)) {
            double x = readDouble(entry, "x");
            double y = readDouble(entry, "y");
            double z = readDouble(entry, "z");
            double radius = readDouble(entry, "radius");
            zones.add(new BuyZone(x, y, z, radius));
        }
        return zones;
    }

    private List<BombSite> loadBombSites(YamlConfiguration config, String path) {
        List<BombSite> sites = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return sites;
        }
        for (String key : section.getKeys(false)) {
            double x = section.getDouble(key + ".x");
            double y = section.getDouble(key + ".y");
            double z = section.getDouble(key + ".z");
            double radius = section.getDouble(key + ".radius");
            sites.add(new BombSite(key, x, y, z, radius));
        }
        return sites;
    }

    private void ensureTemplates(File mapsDir) {
        if (!mapsDir.exists()) {
            mapsDir.mkdirs();
        }
        saveTemplate("maps/template.yml");
        saveTemplate("maps/dust2.yml");
        saveTemplate("maps/mirage.yml");
        saveTemplate("maps/inferno.yml");
        saveTemplate("maps/nuke.yml");
        saveTemplate("maps/anubis.yml");
        saveTemplate("maps/ancient.yml");
        saveTemplate("maps/vertigo.yml");
        saveTemplate("maps/overpass.yml");
        saveTemplate("maps/train.yml");
        saveTemplate("maps/cache.yml");
        saveTemplate("maps/cobblestone.yml");
        saveTemplate("maps/tuscan.yml");
        saveTemplate("maps/summit.yml");
    }

    private void saveTemplate(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private double readDouble(Map<?, ?> entry, String key) {
        Object value = entry.get(key);
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

    private float readFloat(Map<?, ?> entry, String key) {
        return (float) readDouble(entry, key);
    }
}
