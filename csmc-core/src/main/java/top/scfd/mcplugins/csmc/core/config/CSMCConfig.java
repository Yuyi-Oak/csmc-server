package top.scfd.mcplugins.csmc.core.config;

import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.storage.StorageType;

public record CSMCConfig(
    General general,
    Storage storage,
    Server server,
    Game game
) {
    public record General(String language, boolean debug) {}

    public record Storage(
        StorageType type,
        Yaml yaml,
        Sqlite sqlite,
        Mysql mysql,
        Postgres postgresql,
        Mongo mongodb,
        Redis redis
    ) {
        public record Yaml(String path) {}
        public record Sqlite(String file) {}
        public record Mysql(String host, int port, String database, String username, String password, int maxPoolSize) {}
        public record Postgres(String host, int port, String database, String username, String password, int maxPoolSize) {}
        public record Mongo(String uri, String database) {}
        public record Redis(String uri, String namespace) {}
    }

    public record Server(int maxSessions, int maxPlayersPerSession) {}

    public record Game(GameMode defaultMode) {}
}
