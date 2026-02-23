package top.scfd.mcplugins.csmc.core;

import top.scfd.mcplugins.csmc.api.CSMCPlatform;
import top.scfd.mcplugins.csmc.core.config.CSMCConfig;
import top.scfd.mcplugins.csmc.core.i18n.MessageService;
import top.scfd.mcplugins.csmc.core.map.MapRegistry;
import top.scfd.mcplugins.csmc.core.rules.ModeRulesRegistry;
import top.scfd.mcplugins.csmc.core.session.GameSessionManager;
import top.scfd.mcplugins.csmc.storage.StorageManager;

public final class CSMCCore {
    private final CSMCPlatform platform;
    private final CSMCConfig config;
    private final MessageService messages;
    private final StorageManager storageManager;
    private final GameSessionManager sessionManager;
    private final ModeRulesRegistry rulesRegistry;
    private final MapRegistry mapRegistry;

    public CSMCCore(CSMCPlatform platform, CSMCConfig config, MessageService messages, StorageManager storageManager) {
        this.platform = platform;
        this.config = config;
        this.messages = messages;
        this.storageManager = storageManager;
        this.rulesRegistry = new ModeRulesRegistry();
        this.sessionManager = new GameSessionManager(rulesRegistry);
        this.mapRegistry = new MapRegistry();
    }

    public void start() {
        platform.logInfo("CSMC core starting on " + platform.platformName());
        platform.logInfo("Default mode: " + config.game().defaultMode());
    }

    public void stop() {
        platform.logInfo("CSMC core stopping");
        storageManager.shutdown();
    }

    public GameSessionManager sessions() {
        return sessionManager;
    }

    public CSMCConfig config() {
        return config;
    }

    public MessageService messages() {
        return messages;
    }

    public ModeRulesRegistry rulesRegistry() {
        return rulesRegistry;
    }

    public MapRegistry mapRegistry() {
        return mapRegistry;
    }
}
