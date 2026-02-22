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
import top.scfd.mcplugins.csmc.core.rules.ModeRules;
import top.scfd.mcplugins.csmc.core.shop.BuyResult;
import top.scfd.mcplugins.csmc.core.shop.ShopCatalog;
import top.scfd.mcplugins.csmc.core.shop.ShopItem;

public final class GameSession {
    private final UUID id;
    private final GameMode mode;
    private final int maxPlayers;
    private final ModeRules rules;
    private final EconomyService economy;
    private final RoundEngine roundEngine;
    private volatile SessionState state;
    private final Map<UUID, TeamSide> players = new ConcurrentHashMap<>();
    private final ShopCatalog catalog = new ShopCatalog();
    private final List<RoundEventListener> roundListeners = new CopyOnWriteArrayList<>();
    private final List<EconomyEventListener> economyListeners = new CopyOnWriteArrayList<>();

    public GameSession(UUID id, GameMode mode, int maxPlayers, ModeRules rules) {
        this.id = id;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.rules = rules;
        this.economy = new EconomyService(rules.economy(), economyListener());
        this.roundEngine = new RoundEngine(rules, this.economy, this::playersSnapshot, roundListener());
        this.state = SessionState.WAITING;
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
    }

    public TeamSide joinPlayer(UUID playerId) {
        TeamSide assigned = assignTeam();
        if (!addPlayer(playerId, assigned)) {
            return TeamSide.SPECTATOR;
        }
        economy.reset(playerId);
        return assigned;
    }

    public TeamSide getSide(UUID playerId) {
        return players.getOrDefault(playerId, TeamSide.SPECTATOR);
    }

    public Set<UUID> players() {
        return Set.copyOf(players.keySet());
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
        boolean success = economy.spend(playerId, item.price());
        return success ? BuyResult.SUCCESS : BuyResult.INSUFFICIENT_FUNDS;
    }

    public void startRound() {
        roundEngine.startRound();
        state = SessionState.LIVE;
    }

    public void tickRound() {
        roundEngine.tick();
        if (roundEngine.phase() == RoundPhase.END && state == SessionState.LIVE) {
            state = SessionState.WAITING;
        }
    }

    public void onBombPlanted() {
        roundEngine.onBombPlanted();
    }

    public void onBombDefused() {
        roundEngine.onBombDefused();
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
