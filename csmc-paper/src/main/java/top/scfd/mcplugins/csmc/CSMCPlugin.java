package top.scfd.mcplugins.csmc;

import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.config.PaperConfigLoader;
import top.scfd.mcplugins.csmc.config.PaperMapLoader;
import top.scfd.mcplugins.csmc.config.PaperShopCatalogLoader;
import top.scfd.mcplugins.csmc.core.CSMCCore;
import top.scfd.mcplugins.csmc.core.config.CSMCConfig;
import top.scfd.mcplugins.csmc.core.i18n.MessageService;
import top.scfd.mcplugins.csmc.i18n.PaperMessageService;
import top.scfd.mcplugins.csmc.platform.PaperPlatform;
import top.scfd.mcplugins.csmc.paper.PaperEconomyNotifier;
import top.scfd.mcplugins.csmc.paper.PaperRoundNotifier;
import top.scfd.mcplugins.csmc.paper.PaperShopService;
import top.scfd.mcplugins.csmc.paper.PaperSessionTicker;
import top.scfd.mcplugins.csmc.paper.BombInteractListener;
import top.scfd.mcplugins.csmc.paper.BombService;
import top.scfd.mcplugins.csmc.paper.CombatStatsListener;
import top.scfd.mcplugins.csmc.paper.GrenadeItemService;
import top.scfd.mcplugins.csmc.paper.GrenadeThrowListener;
import top.scfd.mcplugins.csmc.paper.HudTicker;
import top.scfd.mcplugins.csmc.paper.LoadoutInventoryService;
import top.scfd.mcplugins.csmc.paper.MapProtectionListener;
import top.scfd.mcplugins.csmc.paper.PlayerSessionListener;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;
import top.scfd.mcplugins.csmc.paper.StatsService;
import top.scfd.mcplugins.csmc.paper.WeaponFireListener;
import top.scfd.mcplugins.csmc.paper.WeaponItemService;
import top.scfd.mcplugins.csmc.paper.WeaponReloadListener;
import top.scfd.mcplugins.csmc.paper.WeaponSelectionListener;
import top.scfd.mcplugins.csmc.paper.command.SessionCommand;
import top.scfd.mcplugins.csmc.storage.PaperStorageProviderFactory;
import top.scfd.mcplugins.csmc.storage.StorageManager;
import top.scfd.mcplugins.csmc.storage.StorageProvider;

public final class CSMCPlugin extends JavaPlugin {
    private CSMCCore core;
    private PaperSessionTicker ticker;
    private HudTicker hudTicker;
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

        PaperMapLoader mapLoader = new PaperMapLoader(this);
        mapLoader.loadInto(core.mapRegistry());

        PaperShopCatalogLoader shopLoader = new PaperShopCatalogLoader(this);
        StatsService statsService = new StatsService(storageManager);
        BombService bombService = new BombService();
        WeaponItemService weaponItems = new WeaponItemService(this);
        LoadoutInventoryService loadoutInventory = new LoadoutInventoryService(weaponItems);
        GrenadeItemService grenadeItems = new GrenadeItemService(this);
        sessionRegistry = new SessionRegistry(core.sessions(), core.mapRegistry(), shopLoader.load(), statsService, bombService, loadoutInventory);

        PaperRoundNotifier roundNotifier = new PaperRoundNotifier(messages, sessionRegistry);
        PaperEconomyNotifier economyNotifier = new PaperEconomyNotifier(sessionRegistry);

        sessionRegistry.addRoundListener(roundNotifier);
        sessionRegistry.addEconomyListener(economyNotifier);

        ticker = new PaperSessionTicker(core.sessions());
        ticker.runTaskTimer(this, 20L, 20L);

        hudTicker = new HudTicker(sessionRegistry);
        hudTicker.runTaskTimer(this, 20L, 20L);

        PaperShopService shopService = new PaperShopService(grenadeItems);
        getCommand("csmc").setExecutor(new SessionCommand(sessionRegistry, shopService, loadoutInventory));
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(sessionRegistry), this);
        getServer().getPluginManager().registerEvents(new CombatStatsListener(sessionRegistry, statsService), this);
        getServer().getPluginManager().registerEvents(new BombInteractListener(sessionRegistry, bombService), this);
        getServer().getPluginManager().registerEvents(new WeaponSelectionListener(sessionRegistry), this);
        getServer().getPluginManager().registerEvents(new WeaponFireListener(sessionRegistry, weaponItems, loadoutInventory), this);
        getServer().getPluginManager().registerEvents(new WeaponReloadListener(sessionRegistry, weaponItems, loadoutInventory, this), this);
        getServer().getPluginManager().registerEvents(new GrenadeThrowListener(sessionRegistry, grenadeItems, this), this);
        getServer().getPluginManager().registerEvents(new MapProtectionListener(sessionRegistry), this);
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.stop();
        }
        if (ticker != null) {
            ticker.cancel();
        }
        if (hudTicker != null) {
            hudTicker.cancel();
        }
    }
}
