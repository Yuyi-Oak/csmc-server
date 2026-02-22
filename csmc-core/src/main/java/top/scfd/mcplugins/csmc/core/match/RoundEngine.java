package top.scfd.mcplugins.csmc.core.match;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.economy.EconomyReason;
import top.scfd.mcplugins.csmc.core.economy.EconomyService;
import top.scfd.mcplugins.csmc.core.rules.ModeRules;
import top.scfd.mcplugins.csmc.core.rules.RoundDurations;

public final class RoundEngine {
    private final ModeRules rules;
    private final RoundDurations durations;
    private final EconomyService economy;
    private final Supplier<Map<UUID, TeamSide>> players;
    private final MatchState matchState;
    private final RoundEventListener listener;

    private RoundPhase phase = RoundPhase.END;
    private int phaseRemainingSeconds;
    private int bombRemainingSeconds;
    private int roundRemainingSeconds;
    private int buyRemainingSeconds;

    public RoundEngine(ModeRules rules, EconomyService economy, Supplier<Map<UUID, TeamSide>> players, RoundEventListener listener) {
        this.rules = rules;
        this.durations = rules.durations();
        this.economy = economy;
        this.players = players;
        this.matchState = new MatchState();
        this.listener = listener == null ? RoundEventListener.NO_OP : listener;
    }

    public MatchState matchState() {
        return matchState;
    }

    public RoundPhase phase() {
        return phase;
    }

    public int phaseRemainingSeconds() {
        return phaseRemainingSeconds;
    }

    public int bombRemainingSeconds() {
        return bombRemainingSeconds;
    }

    public int roundRemainingSeconds() {
        return roundRemainingSeconds;
    }

    public int buyRemainingSeconds() {
        return buyRemainingSeconds;
    }

    public boolean isBuyTime() {
        return (phase == RoundPhase.FREEZE || phase == RoundPhase.BUY || phase == RoundPhase.LIVE)
            && buyRemainingSeconds > 0;
    }

    public void startRound() {
        matchState.nextRound();
        RoundPhase previous = phase;
        phase = RoundPhase.FREEZE;
        phaseRemainingSeconds = durations.freezeTimeSeconds();
        bombRemainingSeconds = 0;
        roundRemainingSeconds = durations.roundTimeSeconds();
        buyRemainingSeconds = durations.buyTimeSeconds();
        listener.onRoundStart(matchState.roundNumber());
        listener.onPhaseChanged(previous, phase, matchState.roundNumber(), phaseRemainingSeconds);
    }

    public void tick() {
        if (phase == RoundPhase.END || matchState.winner().isPresent()) {
            return;
        }

        switch (phase) {
            case FREEZE -> advanceFromFreeze();
            case BUY -> tickBuyPhase();
            case LIVE -> {
                roundRemainingSeconds = Math.max(0, roundRemainingSeconds - 1);
                phaseRemainingSeconds = roundRemainingSeconds;
                if (roundRemainingSeconds == 0) {
                    endRound(TeamSide.COUNTER_TERRORIST, RoundEndReason.TIME_EXPIRED);
                }
            }
            case BOMB_PLANTED -> {
                bombRemainingSeconds = Math.max(0, bombRemainingSeconds - 1);
                if (bombRemainingSeconds == 0) {
                    listener.onBombExploded(matchState.roundNumber());
                    endRound(TeamSide.TERRORIST, RoundEndReason.BOMB_EXPLODED);
                }
            }
            case END -> {
                // no-op
            }
        }
    }

    public void onBombPlanted() {
        if (phase != RoundPhase.LIVE && phase != RoundPhase.BUY) {
            return;
        }
        RoundPhase previous = phase;
        phase = RoundPhase.BOMB_PLANTED;
        bombRemainingSeconds = durations.bombTimerSeconds();
        phaseRemainingSeconds = bombRemainingSeconds;
        listener.onBombPlanted(matchState.roundNumber());
        listener.onPhaseChanged(previous, phase, matchState.roundNumber(), bombRemainingSeconds);
    }

    public void onBombDefused() {
        if (phase != RoundPhase.BOMB_PLANTED) {
            return;
        }
        listener.onBombDefused(matchState.roundNumber());
        endRound(TeamSide.COUNTER_TERRORIST, RoundEndReason.BOMB_DEFUSED);
    }

    public void onTeamEliminated(TeamSide eliminatedSide) {
        if (phase != RoundPhase.LIVE && phase != RoundPhase.BUY && phase != RoundPhase.BOMB_PLANTED) {
            return;
        }
        TeamSide winner = eliminatedSide == TeamSide.TERRORIST
            ? TeamSide.COUNTER_TERRORIST
            : TeamSide.TERRORIST;
        listener.onTeamEliminated(matchState.roundNumber(), eliminatedSide, winner);
        endRound(winner, RoundEndReason.ELIMINATION);
    }

    private void advanceFromFreeze() {
        if (phaseRemainingSeconds > 0) {
            phaseRemainingSeconds -= 1;
            buyRemainingSeconds = Math.max(0, buyRemainingSeconds - 1);
            if (phaseRemainingSeconds > 0) {
                return;
            }
        }
        RoundPhase next = buyRemainingSeconds > 0 ? RoundPhase.BUY : RoundPhase.LIVE;
        RoundPhase previous = phase;
        phase = next;
        phaseRemainingSeconds = next == RoundPhase.BUY ? buyRemainingSeconds : roundRemainingSeconds;
        listener.onPhaseChanged(previous, phase, matchState.roundNumber(), phaseRemainingSeconds);
    }

    private void tickBuyPhase() {
        buyRemainingSeconds = Math.max(0, buyRemainingSeconds - 1);
        roundRemainingSeconds = Math.max(0, roundRemainingSeconds - 1);
        phaseRemainingSeconds = buyRemainingSeconds;
        if (roundRemainingSeconds == 0) {
            endRound(TeamSide.COUNTER_TERRORIST, RoundEndReason.TIME_EXPIRED);
            return;
        }
        if (buyRemainingSeconds == 0) {
            RoundPhase previous = phase;
            phase = RoundPhase.LIVE;
            phaseRemainingSeconds = roundRemainingSeconds;
            listener.onPhaseChanged(previous, phase, matchState.roundNumber(), phaseRemainingSeconds);
        }
    }

    private void endRound(TeamSide winner, RoundEndReason reason) {
        phase = RoundPhase.END;
        matchState.score().award(winner);
        applyEconomy(winner);
        matchState.evaluateWinner(rules);
        listener.onRoundEnd(
            matchState.roundNumber(),
            winner,
            reason,
            matchState.score().terrorist(),
            matchState.score().counterTerrorist()
        );
    }

    private void applyEconomy(TeamSide winner) {
        Map<UUID, TeamSide> snapshot = players.get();
        for (Map.Entry<UUID, TeamSide> entry : snapshot.entrySet()) {
            TeamSide side = entry.getValue();
            if (side == TeamSide.SPECTATOR) {
                continue;
            }
            if (side == winner) {
                economy.award(entry.getKey(), EconomyReason.ROUND_WIN);
            } else {
                economy.award(entry.getKey(), EconomyReason.ROUND_LOSS);
            }
        }
    }
}
