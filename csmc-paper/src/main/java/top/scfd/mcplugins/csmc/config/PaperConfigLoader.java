package top.scfd.mcplugins.csmc.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.core.config.CSMCConfig;
import top.scfd.mcplugins.csmc.storage.StorageType;

public final class PaperConfigLoader {
    private final JavaPlugin plugin;

    public PaperConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CSMCConfig load() {
        ensureTemplates();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            copyResource("config/config-en.yml", configFile);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection general = config.getConfigurationSection("general");
        String language = readString(general, "language", "en-US");
        boolean debug = general != null && general.getBoolean("debug", false);

        ConfigurationSection storage = config.getConfigurationSection("storage");
        StorageType storageType = StorageType.valueOf(readString(storage, "type", "YAML")
            .toUpperCase(Locale.ROOT));

        CSMCConfig.Storage.Yaml yaml = new CSMCConfig.Storage.Yaml(readString(storage, "yaml.path", "data/players.yml"));
        CSMCConfig.Storage.Sqlite sqlite = new CSMCConfig.Storage.Sqlite(readString(storage, "sqlite.file", "data/csmc.sqlite"));
        CSMCConfig.Storage.Mysql mysql = new CSMCConfig.Storage.Mysql(
            readString(storage, "mysql.host", "localhost"),
            storage != null ? storage.getInt("mysql.port", 3306) : 3306,
            readString(storage, "mysql.database", "csmc"),
            readString(storage, "mysql.username", "root"),
            readString(storage, "mysql.password", "password"),
            storage != null ? storage.getInt("mysql.pool.maxSize", 10) : 10
        );
        CSMCConfig.Storage.Postgres postgresql = new CSMCConfig.Storage.Postgres(
            readString(storage, "postgresql.host", "localhost"),
            storage != null ? storage.getInt("postgresql.port", 5432) : 5432,
            readString(storage, "postgresql.database", "csmc"),
            readString(storage, "postgresql.username", "postgres"),
            readString(storage, "postgresql.password", "password"),
            storage != null ? storage.getInt("postgresql.pool.maxSize", 10) : 10
        );
        CSMCConfig.Storage.Mongo mongodb = new CSMCConfig.Storage.Mongo(
            readString(storage, "mongodb.uri", "mongodb://localhost:27017"),
            readString(storage, "mongodb.database", "csmc")
        );
        CSMCConfig.Storage.Redis redis = new CSMCConfig.Storage.Redis(
            readString(storage, "redis.uri", "redis://localhost:6379"),
            readString(storage, "redis.namespace", "csmc")
        );

        CSMCConfig.Storage storageConfig = new CSMCConfig.Storage(storageType, yaml, sqlite, mysql, postgresql, mongodb, redis);

        ConfigurationSection server = config.getConfigurationSection("server");
        CSMCConfig.Server serverConfig = new CSMCConfig.Server(
            server != null ? server.getInt("maxSessions", 2) : 2,
            server != null ? server.getInt("maxPlayersPerSession", 40) : 40
        );

        ConfigurationSection game = config.getConfigurationSection("game");
        GameMode defaultMode = GameMode.valueOf(readString(game, "defaultMode", "COMPETITIVE")
            .toUpperCase(Locale.ROOT));
        CSMCConfig.Game gameConfig = new CSMCConfig.Game(defaultMode);

        return new CSMCConfig(
            new CSMCConfig.General(language, debug),
            storageConfig,
            serverConfig,
            gameConfig
        );
    }

    private String readString(ConfigurationSection section, String path, String fallback) {
        if (section == null) {
            return fallback;
        }
        String value = section.getString(path);
        return value == null ? fallback : value;
    }

    private void ensureTemplates() {
        saveTemplate("config/config-en.yml");
        saveTemplate("config/config-zh.yml");
        saveTemplate("config/config-ru.yml");
        saveTemplate("config/config-fr.yml");
        saveTemplate("config/config-de.yml");
    }

    private void saveTemplate(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void copyResource(String resourcePath, File destination) {
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }
            destination.getParentFile().mkdirs();
            Files.copy(input, destination.toPath());
        } catch (IOException error) {
            throw new IllegalStateException("Failed to copy resource: " + resourcePath, error);
        }
    }
}
