package top.scfd.mcplugins.csmc.config;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.shop.ShopCatalog;
import top.scfd.mcplugins.csmc.core.shop.ShopCategory;
import top.scfd.mcplugins.csmc.core.shop.ShopItem;

public final class PaperShopCatalogLoader {
    private final JavaPlugin plugin;

    public PaperShopCatalogLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ShopCatalog load() {
        ensureTemplates();
        File configFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!configFile.exists()) {
            copyResource("config/shop-en.yml", configFile);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection items = config.getConfigurationSection("shop.items");
        ShopCatalog catalog = new ShopCatalog();
        if (items != null) {
            catalog = new ShopCatalog(java.util.Map.of());
            for (String key : items.getKeys(false)) {
                String name = items.getString(key + ".name", key);
                int price = items.getInt(key + ".price", 0);
                ShopCategory category = ShopCategory.fromConfig(items.getString(key + ".category"));
                Set<TeamSide> sides = parseSides(items, key);
                if (price <= 0) {
                    continue;
                }
                catalog.register(new ShopItem(key, name, price, category, sides));
            }
        }
        return catalog;
    }

    private Set<TeamSide> parseSides(ConfigurationSection items, String key) {
        EnumSet<TeamSide> sides = EnumSet.noneOf(TeamSide.class);
        List<String> configured = items.getStringList(key + ".sides");
        if (configured.isEmpty()) {
            configured = List.of(items.getString(key + ".side", "both"));
        }
        for (String side : configured) {
            if (side == null) {
                continue;
            }
            String normalized = side.toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "t", "terrorist", "terrorists" -> sides.add(TeamSide.TERRORIST);
                case "ct", "counter_terrorist", "counter-terrorist", "counterterrorist", "counter_terrorists" ->
                    sides.add(TeamSide.COUNTER_TERRORIST);
                case "both", "all", "*" -> {
                    sides.add(TeamSide.TERRORIST);
                    sides.add(TeamSide.COUNTER_TERRORIST);
                }
                default -> {
                }
            }
        }
        if (sides.isEmpty()) {
            return Set.of(TeamSide.TERRORIST, TeamSide.COUNTER_TERRORIST);
        }
        return Set.copyOf(sides);
    }

    private void ensureTemplates() {
        saveTemplate("config/shop-en.yml");
        saveTemplate("config/shop-zh.yml");
        saveTemplate("config/shop-ru.yml");
        saveTemplate("config/shop-fr.yml");
        saveTemplate("config/shop-de.yml");
    }

    private void saveTemplate(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void copyResource(String resourcePath, File destination) {
        try (java.io.InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }
            destination.getParentFile().mkdirs();
            java.nio.file.Files.copy(input, destination.toPath());
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Failed to copy resource: " + resourcePath, error);
        }
    }
}
