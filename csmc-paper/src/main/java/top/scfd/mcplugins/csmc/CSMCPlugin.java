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
import top.scfd.mcplugins.csmc.paper.FreezePhaseListener;
import top.scfd.mcplugins.csmc.paper.GrenadeItemService;
import top.scfd.mcplugins.csmc.paper.GrenadeThrowListener;
import top.scfd.mcplugins.csmc.paper.HeadshotTrackerService;
import top.scfd.mcplugins.csmc.paper.HudTicker;
import top.scfd.mcplugins.csmc.paper.LoadoutInventoryService;
import top.scfd.mcplugins.csmc.paper.MapProtectionListener;
import top.scfd.mcplugins.csmc.paper.MatchCombatTrackerService;
import top.scfd.mcplugins.csmc.paper.MatchQueueService;
import top.scfd.mcplugins.csmc.paper.MatchQueueTicker;
import top.scfd.mcplugins.csmc.paper.PlayerRoundStateService;
import top.scfd.mcplugins.csmc.paper.PlayerSessionListener;
import top.scfd.mcplugins.csmc.paper.QueuePlayerListener;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;
import top.scfd.mcplugins.csmc.paper.StatsService;
import top.scfd.mcplugins.csmc.paper.StatsFlushTicker;
import top.scfd.mcplugins.csmc.paper.TeamEliminationResolver;
import top.scfd.mcplugins.csmc.paper.WeaponFireListener;
import top.scfd.mcplugins.csmc.paper.WeaponItemService;
import top.scfd.mcplugins.csmc.paper.WeaponReloadListener;
import top.scfd.mcplugins.csmc.paper.WeaponSelectionListener;
import top.scfd.mcplugins.csmc.paper.command.SessionCommand;
import top.scfd.mcplugins.csmc.paper.command.SessionTabCompleter;
import top.scfd.mcplugins.csmc.paper.history.MatchHistoryService;
import top.scfd.mcplugins.csmc.paper.map.MapEditorCommand;
import top.scfd.mcplugins.csmc.paper.map.MapEditorService;
import top.scfd.mcplugins.csmc.paper.map.MapEditorTabCompleter;
import top.scfd.mcplugins.csmc.paper.security.AntiCheatService;
import top.scfd.mcplugins.csmc.paper.security.AntiCheatTicker;
import top.scfd.mcplugins.csmc.paper.security.MovementAntiCheatListener;
import top.scfd.mcplugins.csmc.paper.sync.ClusterSyncService;
import top.scfd.mcplugins.csmc.paper.sync.NoopClusterSyncService;
import top.scfd.mcplugins.csmc.paper.sync.RedisClusterSyncService;
import top.scfd.mcplugins.csmc.storage.PaperStorageProviderFactory;
import top.scfd.mcplugins.csmc.storage.StorageManager;
import top.scfd.mcplugins.csmc.storage.StorageProvider;
import top.scfd.mcplugins.csmc.storage.StorageType;

public final class CSMCPlugin extends JavaPlugin {
    private CSMCCore core;
    private PaperSessionTicker ticker;
    private HudTicker hudTicker;
    private MatchQueueTicker queueTicker;
    private AntiCheatTicker antiCheatTicker;
    private StatsFlushTicker statsFlushTicker;
    private SessionRegistry sessionRegistry;
    private StatsService statsService;
    private ClusterSyncService clusterSync = new NoopClusterSyncService();

    @Override
    public void onEnable() {
        PaperPlatform platform = new PaperPlatform(this);
        PaperConfigLoader loader = new PaperConfigLoader(this);
        CSMCConfig config = loader.load();
        MessageService messages = new PaperMessageService(this, config.general().language());
        StorageProvider provider = new PaperStorageProviderFactory(this).create(config.storage());
        StorageManager storageManager = new StorageManager(provider);
        clusterSync = createClusterSync(config);
        clusterSync.start();
        core = new CSMCCore(platform, config, messages, storageManager);
        core.start();

        PaperMapLoader mapLoader = new PaperMapLoader(this);
        mapLoader.loadInto(core.mapRegistry());

        PaperShopCatalogLoader shopLoader = new PaperShopCatalogLoader(this);
        statsService = new StatsService(storageManager, clusterSync, getLogger());
        BombService bombService = new BombService();
        WeaponItemService weaponItems = new WeaponItemService(this);
        LoadoutInventoryService loadoutInventory = new LoadoutInventoryService(weaponItems);
        AntiCheatService antiCheat = new AntiCheatService(this);
        HeadshotTrackerService headshots = new HeadshotTrackerService();
        GrenadeItemService grenadeItems = new GrenadeItemService(this);
        TeamEliminationResolver eliminationResolver = new TeamEliminationResolver();
        MatchCombatTrackerService combatTracker = new MatchCombatTrackerService();
        MatchHistoryService matchHistory = new MatchHistoryService(this);
        sessionRegistry = new SessionRegistry(
            core.sessions(),
            core.mapRegistry(),
            shopLoader.load(),
            statsService,
            bombService,
            loadoutInventory,
            matchHistory,
            combatTracker
        );
        MatchQueueService queueService = new MatchQueueService(sessionRegistry, config.server().maxSessions());
        PlayerRoundStateService roundStateService = new PlayerRoundStateService(this, sessionRegistry);
        sessionRegistry.setPlayerRoundState(roundStateService);

        PaperRoundNotifier roundNotifier = new PaperRoundNotifier(messages, sessionRegistry);
        PaperEconomyNotifier economyNotifier = new PaperEconomyNotifier(sessionRegistry);

        sessionRegistry.addRoundListener(roundNotifier);
        sessionRegistry.addEconomyListener(economyNotifier);

        ticker = new PaperSessionTicker(sessionRegistry);
        ticker.runTaskTimer(this, 20L, 20L);

        hudTicker = new HudTicker(sessionRegistry);
        hudTicker.runTaskTimer(this, 20L, 20L);

        queueTicker = new MatchQueueTicker(queueService);
        queueTicker.runTaskTimer(this, 20L, 20L);

        antiCheatTicker = new AntiCheatTicker(antiCheat);
        antiCheatTicker.runTaskTimer(this, 20L, 20L);

        statsFlushTicker = new StatsFlushTicker(statsService, 256);
        statsFlushTicker.runTaskTimerAsynchronously(this, 20L, 20L);

        PaperShopService shopService = new PaperShopService(grenadeItems);
        getCommand("csmc").setExecutor(
            new SessionCommand(
                sessionRegistry,
                shopService,
                loadoutInventory,
                statsService,
                combatTracker,
                queueService,
                matchHistory,
                antiCheat
            )
        );
        getCommand("csmc").setTabCompleter(new SessionTabCompleter(sessionRegistry));
        MapEditorService mapEditor = new MapEditorService(this, mapLoader, core.mapRegistry());
        getCommand("csmcmap").setExecutor(new MapEditorCommand(mapEditor));
        getCommand("csmcmap").setTabCompleter(new MapEditorTabCompleter(mapEditor));
        getServer().getPluginManager().registerEvents(roundStateService, this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(sessionRegistry, eliminationResolver), this);
        getServer().getPluginManager().registerEvents(new QueuePlayerListener(queueService), this);
        getServer().getPluginManager().registerEvents(
            new CombatStatsListener(sessionRegistry, statsService, combatTracker, eliminationResolver, headshots),
            this
        );
        getServer().getPluginManager().registerEvents(new BombInteractListener(sessionRegistry, bombService), this);
        getServer().getPluginManager().registerEvents(new WeaponSelectionListener(sessionRegistry), this);
        getServer().getPluginManager().registerEvents(new WeaponFireListener(sessionRegistry, weaponItems, loadoutInventory, antiCheat, headshots), this);
        getServer().getPluginManager().registerEvents(new WeaponReloadListener(sessionRegistry, weaponItems, loadoutInventory, this), this);
        getServer().getPluginManager().registerEvents(new GrenadeThrowListener(sessionRegistry, grenadeItems, this), this);
        getServer().getPluginManager().registerEvents(new MapProtectionListener(sessionRegistry), this);
        getServer().getPluginManager().registerEvents(new FreezePhaseListener(sessionRegistry), this);
        getServer().getPluginManager().registerEvents(new MovementAntiCheatListener(sessionRegistry, antiCheat), this);
    }

    @Override
    public void onDisable() {
        if (ticker != null) {
            ticker.cancel();
        }
        if (hudTicker != null) {
            hudTicker.cancel();
        }
        if (queueTicker != null) {
            queueTicker.cancel();
        }
        if (antiCheatTicker != null) {
            antiCheatTicker.cancel();
        }
        if (statsFlushTicker != null) {
            statsFlushTicker.cancel();
        }
        if (statsService != null) {
            statsService.flushAll();
        }
        if (core != null) {
            core.stop();
        }
        if (clusterSync != null) {
            clusterSync.close();
        }
    }

    private ClusterSyncService createClusterSync(CSMCConfig config) {
        if (config.storage().type() != StorageType.REDIS) {
            return new NoopClusterSyncService();
        }
        try {
            return new RedisClusterSyncService(
                config.storage().redis().uri(),
                config.storage().redis().namespace()
            );
        } catch (RuntimeException error) {
            getLogger().warning("Redis cluster sync disabled: " + error.getMessage());
            return new NoopClusterSyncService();
        }
    }
}
