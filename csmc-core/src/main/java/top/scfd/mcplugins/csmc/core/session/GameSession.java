package top.scfd.mcplugins.csmc.core.session;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.economy.EconomyEventListener;
import top.scfd.mcplugins.csmc.core.economy.EconomyReason;
import top.scfd.mcplugins.csmc.core.economy.EconomyService;
import top.scfd.mcplugins.csmc.core.match.RoundEndReason;
import top.scfd.mcplugins.csmc.core.match.RoundEngine;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.player.WeaponInstance;
import top.scfd.mcplugins.csmc.core.player.WeaponSlot;
import top.scfd.mcplugins.csmc.core.rules.ModeRules;
import top.scfd.mcplugins.csmc.core.shop.BuyResult;
import top.scfd.mcplugins.csmc.core.shop.ShopCatalog;
import top.scfd.mcplugins.csmc.core.shop.ShopCategory;
import top.scfd.mcplugins.csmc.core.shop.ShopItem;
import top.scfd.mcplugins.csmc.core.weapon.WeaponRegistry;
import top.scfd.mcplugins.csmc.core.weapon.WeaponSpec;

public final class GameSession {
    private static final int FIRST_ROUND_COUNTDOWN_SECONDS = 3;
    private static final int NEXT_ROUND_COUNTDOWN_SECONDS = 5;
    private static final int HALFTIME_COUNTDOWN_SECONDS = 10;
    private final UUID id;
    private final GameMode mode;
    private final int maxPlayers;
    private final ModeRules rules;
    private final EconomyService economy;
    private final RoundEngine roundEngine;
    private volatile SessionState state;
    private final Map<UUID, TeamSide> players = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerLoadout> loadouts = new ConcurrentHashMap<>();
    private final ShopCatalog catalog;
    private final WeaponRegistry weaponRegistry;
    private final List<RoundEventListener> roundListeners = new CopyOnWriteArrayList<>();
    private final List<EconomyEventListener> economyListeners = new CopyOnWriteArrayList<>();
    private volatile int nextRoundCountdownSeconds = -1;
    private volatile boolean sidesSwapped;

    public GameSession(UUID id, GameMode mode, int maxPlayers, ModeRules rules, ShopCatalog catalog) {
        this.id = id;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.rules = rules;
        this.economy = new EconomyService(rules.economy(), economyListener());
        this.roundEngine = new RoundEngine(rules, this.economy, this::playersSnapshot, roundListener());
        this.state = SessionState.WAITING;
        this.weaponRegistry = new WeaponRegistry();
        if (catalog == null || catalog.isEmpty()) {
            this.catalog = new ShopCatalog();
        } else {
            this.catalog = catalog.copy();
        }
    }

    public UUID id() {
        return id;
    }

    public GameMode mode() {
        return mode;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public ModeRules rules() {
        return rules;
    }

    public EconomyService economy() {
        return economy;
    }

    public RoundEngine roundEngine() {
        return roundEngine;
    }

    public void addRoundListener(RoundEventListener listener) {
        if (listener != null) {
            roundListeners.add(listener);
        }
    }

    public void removeRoundListener(RoundEventListener listener) {
        roundListeners.remove(listener);
    }

    public void addEconomyListener(EconomyEventListener listener) {
        if (listener != null) {
            economyListeners.add(listener);
        }
    }

    public void removeEconomyListener(EconomyEventListener listener) {
        economyListeners.remove(listener);
    }

    public SessionState state() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public boolean addPlayer(UUID playerId, TeamSide side) {
        if (players.size() >= maxPlayers) {
            return false;
        }
        players.put(playerId, side);
        return true;
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
        loadouts.remove(playerId);
        if (!canStartRound()) {
            nextRoundCountdownSeconds = -1;
        }
    }

    public TeamSide joinPlayer(UUID playerId) {
        TeamSide assigned = assignTeam();
        if (!addPlayer(playerId, assigned)) {
            return TeamSide.SPECTATOR;
        }
        economy.reset(playerId);
        loadouts.put(playerId, createDefaultLoadout(assigned));
        if (state == SessionState.WAITING && nextRoundCountdownSeconds < 0 && canStartRound()) {
            nextRoundCountdownSeconds = roundEngine.matchState().roundNumber() == 0
                ? FIRST_ROUND_COUNTDOWN_SECONDS
                : NEXT_ROUND_COUNTDOWN_SECONDS;
        }
        return assigned;
    }

    public TeamSide getSide(UUID playerId) {
        return players.getOrDefault(playerId, TeamSide.SPECTATOR);
    }

    public Set<UUID> players() {
        return Set.copyOf(players.keySet());
    }

    public PlayerLoadout loadout(UUID playerId) {
        return loadouts.get(playerId);
    }

    public BuyResult buy(UUID playerId, String itemKey) {
        if (!players.containsKey(playerId)) {
            return BuyResult.NOT_IN_SESSION;
        }
        if (!roundEngine.isBuyTime()) {
            return BuyResult.BUY_TIME_OVER;
        }
        ShopItem item = catalog.find(itemKey).orElse(null);
        if (item == null) {
            return BuyResult.UNKNOWN_ITEM;
        }
        PlayerLoadout loadout = loadouts.get(playerId);
        if (loadout == null) {
            return BuyResult.NOT_IN_SESSION;
        }
        TeamSide side = players.getOrDefault(playerId, TeamSide.SPECTATOR);
        BuyResult validation = validatePurchase(item, loadout, side);
        if (validation != BuyResult.SUCCESS) {
            return validation;
        }
        boolean success = economy.spend(playerId, item.price());
        if (!success) {
            return BuyResult.INSUFFICIENT_FUNDS;
        }
        grantPurchase(item, loadout);
        return BuyResult.SUCCESS;
    }

    public ShopItem findItem(String itemKey) {
        return catalog.find(itemKey).orElse(null);
    }

    public Map<String, ShopItem> shopItems() {
        return catalog.items();
    }

    public WeaponSpec findWeapon(String weaponKey) {
        return weaponRegistry.find(weaponKey).orElse(null);
    }

    public Map<String, WeaponSpec> weapons() {
        return weaponRegistry.all();
    }

    public void startRound() {
        if (state != SessionState.WAITING && state != SessionState.WARMUP) {
            return;
        }
        if (!canStartRound()) {
            return;
        }
        roundEngine.startRound();
        state = SessionState.LIVE;
        nextRoundCountdownSeconds = -1;
    }

    public void tickRound() {
        if (state == SessionState.FINISHED) {
            return;
        }
        if (state == SessionState.LIVE) {
            roundEngine.tick();
            if (roundEngine.phase() == RoundPhase.END) {
                if (roundEngine.matchState().winner().isPresent()) {
                    state = SessionState.FINISHED;
                    nextRoundCountdownSeconds = -1;
                    return;
                }
                state = SessionState.WAITING;
                if (shouldSwapSidesForHalftime()) {
                    swapSidesForHalftime();
                    nextRoundCountdownSeconds = HALFTIME_COUNTDOWN_SECONDS;
                } else {
                    nextRoundCountdownSeconds = NEXT_ROUND_COUNTDOWN_SECONDS;
                }
            }
            return;
        }
        if (state != SessionState.WAITING) {
            return;
        }
        if (!canStartRound()) {
            nextRoundCountdownSeconds = -1;
            return;
        }
        if (nextRoundCountdownSeconds < 0) {
            nextRoundCountdownSeconds = roundEngine.matchState().roundNumber() == 0
                ? FIRST_ROUND_COUNTDOWN_SECONDS
                : NEXT_ROUND_COUNTDOWN_SECONDS;
            return;
        }
        nextRoundCountdownSeconds = Math.max(0, nextRoundCountdownSeconds - 1);
        if (nextRoundCountdownSeconds == 0) {
            startRound();
        }
    }

    public int nextRoundCountdownSeconds() {
        return nextRoundCountdownSeconds;
    }

    public boolean sidesSwapped() {
        return sidesSwapped;
    }

    public void onBombPlanted() {
        roundEngine.onBombPlanted();
    }

    public void onBombDefused() {
        roundEngine.onBombDefused();
    }

    public void startDefuse(UUID defuserId, boolean hasKit) {
        roundEngine.startDefuse(defuserId, hasKit);
    }

    public void cancelDefuse(UUID defuserId) {
        roundEngine.cancelDefuse(defuserId);
    }

    public void onBombPlanted(UUID planterId) {
        if (planterId != null) {
            economy.award(planterId, EconomyReason.PLANT);
        }
        roundEngine.onBombPlanted();
    }

    public void onBombDefused(UUID defuserId) {
        if (defuserId != null) {
            economy.award(defuserId, EconomyReason.DEFUSE);
        }
        roundEngine.onBombDefused();
    }

    public void onTeamEliminated(TeamSide eliminatedSide) {
        roundEngine.onTeamEliminated(eliminatedSide);
    }

    public void onKill(UUID killerId) {
        if (killerId != null) {
            economy.award(killerId, EconomyReason.KILL);
        }
    }

    public void onAssist(UUID assistantId) {
        if (assistantId != null) {
            economy.award(assistantId, EconomyReason.ASSIST);
        }
    }

    private TeamSide assignTeam() {
        int tCount = 0;
        int ctCount = 0;
        for (TeamSide side : players.values()) {
            if (side == TeamSide.TERRORIST) {
                tCount++;
            } else if (side == TeamSide.COUNTER_TERRORIST) {
                ctCount++;
            }
        }
        return tCount <= ctCount ? TeamSide.TERRORIST : TeamSide.COUNTER_TERRORIST;
    }

    private boolean canStartRound() {
        int tCount = 0;
        int ctCount = 0;
        for (TeamSide side : players.values()) {
            if (side == TeamSide.TERRORIST) {
                tCount++;
            } else if (side == TeamSide.COUNTER_TERRORIST) {
                ctCount++;
            }
        }
        return tCount > 0 && ctCount > 0;
    }

    private boolean shouldSwapSidesForHalftime() {
        if (sidesSwapped || rules.maxRounds() <= 1) {
            return false;
        }
        int halfRounds = rules.maxRounds() / 2;
        if (halfRounds <= 0) {
            return false;
        }
        return roundEngine.matchState().roundNumber() >= halfRounds;
    }

    private void swapSidesForHalftime() {
        for (Map.Entry<UUID, TeamSide> entry : players.entrySet()) {
            UUID playerId = entry.getKey();
            TeamSide side = entry.getValue();
            if (side == TeamSide.TERRORIST) {
                players.put(playerId, TeamSide.COUNTER_TERRORIST);
            } else if (side == TeamSide.COUNTER_TERRORIST) {
                players.put(playerId, TeamSide.TERRORIST);
            }
            TeamSide newSide = players.get(playerId);
            if (newSide == TeamSide.TERRORIST || newSide == TeamSide.COUNTER_TERRORIST) {
                loadouts.put(playerId, createDefaultLoadout(newSide));
                economy.reset(playerId);
            }
        }
        sidesSwapped = true;
        notifyRound(listener -> listener.onSideSwap(roundEngine.matchState().roundNumber()));
    }

    private PlayerLoadout createDefaultLoadout(TeamSide side) {
        PlayerLoadout loadout = new PlayerLoadout();
        WeaponSpec pistol = weaponRegistry.find(side == TeamSide.COUNTER_TERRORIST ? "usp" : "glock").orElse(null);
        WeaponSpec knife = weaponRegistry.find("knife").orElse(null);
        if (pistol != null) {
            loadout.setSecondary(WeaponInstance.full(pistol));
            loadout.setActiveSlot(WeaponSlot.SECONDARY);
        }
        if (knife != null) {
            loadout.setMelee(WeaponInstance.full(knife));
        }
        return loadout;
    }

    private BuyResult validatePurchase(ShopItem item, PlayerLoadout loadout, TeamSide side) {
        if (!item.isAllowedFor(side)) {
            return BuyResult.SIDE_RESTRICTED;
        }
        ShopCategory category = item.category();
        if (category == null) {
            category = ShopCategory.UNKNOWN;
        }
        return switch (category) {
            case PRIMARY, SECONDARY, MELEE -> weaponRegistry.find(item.key()).isPresent()
                ? BuyResult.SUCCESS
                : BuyResult.UNKNOWN_ITEM;
            case GRENADE -> loadout.canAddGrenade(item.key(), grenadeMaxTotal(), grenadeMaxPerType(item.key()))
                ? BuyResult.SUCCESS
                : BuyResult.INVENTORY_FULL;
            case ARMOR -> validateArmorPurchase(item.key(), loadout);
            case UTILITY -> {
                if (isDefuseKit(item.key())) {
                    yield loadout.armor().defuseKit() ? BuyResult.INVENTORY_FULL : BuyResult.SUCCESS;
                }
                yield BuyResult.UNKNOWN_ITEM;
            }
            case UNKNOWN -> BuyResult.UNKNOWN_ITEM;
        };
    }

    private void grantPurchase(ShopItem item, PlayerLoadout loadout) {
        ShopCategory category = item.category();
        if (category == null) {
            category = ShopCategory.UNKNOWN;
        }
        switch (category) {
            case PRIMARY -> weaponRegistry.find(item.key())
                .ifPresent(spec -> loadout.setPrimary(WeaponInstance.full(spec)));
            case SECONDARY -> weaponRegistry.find(item.key())
                .ifPresent(spec -> loadout.setSecondary(WeaponInstance.full(spec)));
            case MELEE -> weaponRegistry.find(item.key())
                .ifPresent(spec -> loadout.setMelee(WeaponInstance.full(spec)));
            case GRENADE -> loadout.addGrenade(item.key(), grenadeMaxTotal(), grenadeMaxPerType(item.key()));
            case ARMOR -> grantArmorPurchase(item.key(), loadout);
            case UTILITY -> {
                if (isDefuseKit(item.key())) {
                    loadout.armor().grantDefuseKit();
                }
            }
            case UNKNOWN -> {
            }
        }
    }

    private int grenadeMaxTotal() {
        return 4;
    }

    private int grenadeMaxPerType(String key) {
        if (key == null) {
            return 1;
        }
        return "flashbang".equalsIgnoreCase(key) ? 2 : 1;
    }

    private boolean isDefuseKit(String key) {
        return key != null && "defuse_kit".equalsIgnoreCase(key);
    }

    private BuyResult validateArmorPurchase(String key, PlayerLoadout loadout) {
        if (isKevlarHelmet(key)) {
            return loadout.armor().armor() >= 100 && loadout.armor().helmet()
                ? BuyResult.INVENTORY_FULL
                : BuyResult.SUCCESS;
        }
        if (isKevlar(key)) {
            return loadout.armor().armor() >= 100 ? BuyResult.INVENTORY_FULL : BuyResult.SUCCESS;
        }
        return BuyResult.UNKNOWN_ITEM;
    }

    private void grantArmorPurchase(String key, PlayerLoadout loadout) {
        if (isKevlarHelmet(key)) {
            loadout.armor().grantKevlarHelmet();
            return;
        }
        if (isKevlar(key)) {
            loadout.armor().grantKevlar();
        }
    }

    private boolean isKevlar(String key) {
        return key != null && "kevlar".equalsIgnoreCase(key);
    }

    private boolean isKevlarHelmet(String key) {
        return key != null && "kevlar_helmet".equalsIgnoreCase(key);
    }

    private Map<UUID, TeamSide> playersSnapshot() {
        return Map.copyOf(players);
    }

    private RoundEventListener roundListener() {
        return new RoundEventListener() {
            @Override
            public void onRoundStart(int roundNumber) {
                notifyRound(listener -> listener.onRoundStart(roundNumber));
            }

            @Override
            public void onPhaseChanged(RoundPhase from, RoundPhase to, int roundNumber, int durationSeconds) {
                notifyRound(listener -> listener.onPhaseChanged(from, to, roundNumber, durationSeconds));
            }

            @Override
            public void onBombPlanted(int roundNumber) {
                notifyRound(listener -> listener.onBombPlanted(roundNumber));
            }

            @Override
            public void onBombDefused(int roundNumber) {
                notifyRound(listener -> listener.onBombDefused(roundNumber));
            }

            @Override
            public void onDefuseStarted(int roundNumber, UUID defuserId, int durationSeconds) {
                notifyRound(listener -> listener.onDefuseStarted(roundNumber, defuserId, durationSeconds));
            }

            @Override
            public void onDefuseStopped(int roundNumber, UUID defuserId, int remainingSeconds) {
                notifyRound(listener -> listener.onDefuseStopped(roundNumber, defuserId, remainingSeconds));
            }

            @Override
            public void onBombExploded(int roundNumber) {
                notifyRound(listener -> listener.onBombExploded(roundNumber));
            }

            @Override
            public void onTeamEliminated(int roundNumber, TeamSide eliminatedSide, TeamSide winner) {
                notifyRound(listener -> listener.onTeamEliminated(roundNumber, eliminatedSide, winner));
            }

            @Override
            public void onRoundEnd(int roundNumber, TeamSide winner, RoundEndReason reason, int terroristScore, int counterTerroristScore) {
                notifyRound(listener -> listener.onRoundEnd(roundNumber, winner, reason, terroristScore, counterTerroristScore));
            }
        };
    }

    private EconomyEventListener economyListener() {
        return new EconomyEventListener() {
            @Override
            public void onReward(UUID playerId, EconomyReason reason, int amount, int newBalance) {
                notifyEconomy(listener -> listener.onReward(playerId, reason, amount, newBalance));
            }

            @Override
            public void onSpend(UUID playerId, int amount, int newBalance) {
                notifyEconomy(listener -> listener.onSpend(playerId, amount, newBalance));
            }
        };
    }

    private void notifyRound(Consumer<RoundEventListener> consumer) {
        for (RoundEventListener listener : roundListeners) {
            consumer.accept(listener);
        }
    }

    private void notifyEconomy(Consumer<EconomyEventListener> consumer) {
        for (EconomyEventListener listener : economyListeners) {
            consumer.accept(listener);
        }
    }
}
