package top.scfd.mcplugins.csmc.core.session;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.economy.EconomyService;
import top.scfd.mcplugins.csmc.core.match.RoundEngine;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.rules.ModeRules;

public final class GameSession {
    private final UUID id;
    private final GameMode mode;
    private final int maxPlayers;
    private final ModeRules rules;
    private final EconomyService economy;
    private final RoundEngine roundEngine;
    private volatile SessionState state;
    private final Map<UUID, TeamSide> players = new ConcurrentHashMap<>();

    public GameSession(UUID id, GameMode mode, int maxPlayers, ModeRules rules, EconomyService economy) {
        this.id = id;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.rules = rules;
        this.economy = economy;
        this.roundEngine = new RoundEngine(rules, economy, this::playersSnapshot);
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

    public TeamSide getSide(UUID playerId) {
        return players.getOrDefault(playerId, TeamSide.SPECTATOR);
    }

    public Set<UUID> players() {
        return Set.copyOf(players.keySet());
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

    public void onTeamEliminated(TeamSide eliminatedSide) {
        roundEngine.onTeamEliminated(eliminatedSide);
    }

    private Map<UUID, TeamSide> playersSnapshot() {
        return Map.copyOf(players);
    }
}
