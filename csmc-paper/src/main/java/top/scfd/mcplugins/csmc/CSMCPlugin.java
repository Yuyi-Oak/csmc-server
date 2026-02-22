package top.scfd.mcplugins.csmc;

import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.config.PaperConfigLoader;
import top.scfd.mcplugins.csmc.core.CSMCCore;
import top.scfd.mcplugins.csmc.core.config.CSMCConfig;
import top.scfd.mcplugins.csmc.core.i18n.MessageService;
import top.scfd.mcplugins.csmc.i18n.PaperMessageService;
import top.scfd.mcplugins.csmc.platform.PaperPlatform;
import top.scfd.mcplugins.csmc.paper.PaperEconomyNotifier;
import top.scfd.mcplugins.csmc.paper.PaperRoundNotifier;
import top.scfd.mcplugins.csmc.paper.PaperSessionTicker;
import top.scfd.mcplugins.csmc.paper.PlayerSessionListener;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;
import top.scfd.mcplugins.csmc.paper.command.SessionCommand;
import top.scfd.mcplugins.csmc.storage.PaperStorageProviderFactory;
import top.scfd.mcplugins.csmc.storage.StorageManager;
import top.scfd.mcplugins.csmc.storage.StorageProvider;

public final class CSMCPlugin extends JavaPlugin {
    private CSMCCore core;
    private PaperSessionTicker ticker;
    private SessionRegistry sessionRegistry;

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

        sessionRegistry = new SessionRegistry(core.sessions());

        PaperRoundNotifier roundNotifier = new PaperRoundNotifier(messages, sessionRegistry);
        PaperEconomyNotifier economyNotifier = new PaperEconomyNotifier(sessionRegistry);

        sessionRegistry.addRoundListener(roundNotifier);
        sessionRegistry.addEconomyListener(economyNotifier);

        ticker = new PaperSessionTicker(core.sessions());
        ticker.runTaskTimer(this, 20L, 20L);

        getCommand("csmc").setExecutor(new SessionCommand(sessionRegistry));
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(sessionRegistry), this);
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.stop();
        }
        if (ticker != null) {
            ticker.cancel();
        }
    }
}
