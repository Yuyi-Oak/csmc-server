package top.scfd.mcplugins.csmc.config;

import java.io.File;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.core.shop.ShopCatalog;
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
                if (price <= 0) {
                    continue;
                }
                catalog.register(new ShopItem(key, name, price));
            }
        }
        return catalog;
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
