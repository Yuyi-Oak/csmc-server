package top.scfd.mcplugins.csmc.storage;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.core.config.CSMCConfig;
import top.scfd.mcplugins.csmc.storage.StorageProvider;
import top.scfd.mcplugins.csmc.storage.StorageType;
import top.scfd.mcplugins.csmc.storage.impl.UnsupportedStorageProvider;
import top.scfd.mcplugins.csmc.storage.jdbc.JdbcDialect;
import top.scfd.mcplugins.csmc.storage.jdbc.JdbcStorageProvider;

public final class PaperStorageProviderFactory {
    private final JavaPlugin plugin;

    public PaperStorageProviderFactory(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public StorageProvider create(CSMCConfig.Storage storage) {
        return switch (storage.type()) {
            case YAML -> new YamlStorageProvider(resolveFile(storage.yaml().path()));
            case SQLITE -> new JdbcStorageProvider(
                StorageType.SQLITE,
                JdbcDialect.SQLITE,
                "jdbc:sqlite:" + resolveFile(storage.sqlite().file()).getAbsolutePath(),
                null,
                null
            );
            case MYSQL -> new JdbcStorageProvider(
                StorageType.MYSQL,
                JdbcDialect.MYSQL,
                mysqlUrl(storage.mysql()),
                storage.mysql().username(),
                storage.mysql().password()
            );
            case POSTGRESQL -> new JdbcStorageProvider(
                StorageType.POSTGRESQL,
                JdbcDialect.POSTGRESQL,
                postgresUrl(storage.postgresql()),
                storage.postgresql().username(),
                storage.postgresql().password()
            );
            case MONGODB -> new UnsupportedStorageProvider(
                StorageType.MONGODB,
                "MongoDB provider not initialized yet"
            );
            case REDIS -> new UnsupportedStorageProvider(
                StorageType.REDIS,
                "Redis provider not initialized yet"
            );
        };
    }

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(plugin.getDataFolder(), path);
    }

    private String mysqlUrl(CSMCConfig.Storage.Mysql mysql) {
        return "jdbc:mysql://" + mysql.host() + ":" + mysql.port() + "/" + mysql.database() +
            "?useSSL=false&allowPublicKeyRetrieval=true";
    }

    private String postgresUrl(CSMCConfig.Storage.Postgres postgres) {
        return "jdbc:postgresql://" + postgres.host() + ":" + postgres.port() + "/" + postgres.database();
    }
}
