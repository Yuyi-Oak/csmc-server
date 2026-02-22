package top.scfd.mcplugins.csmc;

import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.config.PaperConfigLoader;
import top.scfd.mcplugins.csmc.core.CSMCCore;
import top.scfd.mcplugins.csmc.core.config.CSMCConfig;
import top.scfd.mcplugins.csmc.core.i18n.MessageService;
import top.scfd.mcplugins.csmc.i18n.PaperMessageService;
import top.scfd.mcplugins.csmc.platform.PaperPlatform;
import top.scfd.mcplugins.csmc.storage.PaperStorageProviderFactory;
import top.scfd.mcplugins.csmc.storage.StorageManager;
import top.scfd.mcplugins.csmc.storage.StorageProvider;

public final class CSMCPlugin extends JavaPlugin {
    private CSMCCore core;

    @Override
    public void onEnable() {
        PaperPlatform platform = new PaperPlatform(this);
        PaperConfigLoader loader = new PaperConfigLoader(this);
        CSMCConfig config = loader.load();
        MessageService messages = new PaperMessageService(this, config.general().language());
        StorageProvider provider = new PaperStorageProviderFactory(this).create(config.storage());
        StorageManager storageManager = new StorageManager(provider);
        core = new CSMCCore(platform, config, messages, storageManager);
        core.start();
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.stop();
        }
    }
}
