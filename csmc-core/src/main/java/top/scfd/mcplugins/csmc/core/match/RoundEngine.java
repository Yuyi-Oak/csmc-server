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

    private RoundPhase phase = RoundPhase.END;
    private int phaseRemainingSeconds;
    private int bombRemainingSeconds;

    public RoundEngine(ModeRules rules, EconomyService economy, Supplier<Map<UUID, TeamSide>> players) {
        this.rules = rules;
        this.durations = rules.durations();
        this.economy = economy;
        this.players = players;
        this.matchState = new MatchState();
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

    public void startRound() {
        matchState.nextRound();
        phase = RoundPhase.FREEZE;
        phaseRemainingSeconds = durations.freezeTimeSeconds();
        bombRemainingSeconds = 0;
    }

    public void tick() {
        if (phase == RoundPhase.END || matchState.winner().isPresent()) {
            return;
        }

        switch (phase) {
            case FREEZE -> advancePhase(RoundPhase.BUY, durations.buyTimeSeconds());
            case BUY -> advancePhase(RoundPhase.LIVE, durations.roundTimeSeconds());
            case LIVE -> {
                phaseRemainingSeconds = Math.max(0, phaseRemainingSeconds - 1);
                if (phaseRemainingSeconds == 0) {
                    endRound(TeamSide.COUNTER_TERRORIST, RoundEndReason.TIME_EXPIRED);
                }
            }
            case BOMB_PLANTED -> {
                bombRemainingSeconds = Math.max(0, bombRemainingSeconds - 1);
                if (bombRemainingSeconds == 0) {
                    endRound(TeamSide.TERRORIST, RoundEndReason.BOMB_EXPLODED);
                }
            }
            case END -> {
                // no-op
            }
        }
    }

    public void onBombPlanted() {
        if (phase != RoundPhase.LIVE) {
            return;
        }
        phase = RoundPhase.BOMB_PLANTED;
        bombRemainingSeconds = durations.bombTimerSeconds();
    }

    public void onBombDefused() {
        if (phase != RoundPhase.BOMB_PLANTED) {
            return;
        }
        endRound(TeamSide.COUNTER_TERRORIST, RoundEndReason.BOMB_DEFUSED);
    }

    public void onTeamEliminated(TeamSide eliminatedSide) {
        if (phase != RoundPhase.LIVE && phase != RoundPhase.BOMB_PLANTED) {
            return;
        }
        TeamSide winner = eliminatedSide == TeamSide.TERRORIST
            ? TeamSide.COUNTER_TERRORIST
            : TeamSide.TERRORIST;
        endRound(winner, RoundEndReason.ELIMINATION);
    }

    private void advancePhase(RoundPhase next, int durationSeconds) {
        if (phaseRemainingSeconds > 0) {
            phaseRemainingSeconds -= 1;
            return;
        }
        phase = next;
        phaseRemainingSeconds = Math.max(0, durationSeconds);
    }

    private void endRound(TeamSide winner, RoundEndReason reason) {
        phase = RoundPhase.END;
        matchState.score().award(winner);
        applyEconomy(winner);
        matchState.evaluateWinner(rules);
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
