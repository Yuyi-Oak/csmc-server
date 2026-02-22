package top.scfd.mcplugins.csmc.core.match;

import java.util.Optional;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.rules.ModeRules;

public final class MatchState {
    private final MatchScore score = new MatchScore();
    private int roundNumber;
    private boolean overtime;
    private Optional<TeamSide> winner = Optional.empty();

    public int roundNumber() {
        return roundNumber;
    }

    public void nextRound() {
        roundNumber += 1;
    }

    public MatchScore score() {
        return score;
    }

    public boolean overtime() {
        return overtime;
    }

    public Optional<TeamSide> winner() {
        return winner;
    }

    public void evaluateWinner(ModeRules rules) {
        if (winner.isPresent()) {
            return;
        }
        int roundsToWin = overtime ? rules.overtimeRoundsToWin() : rules.roundsToWin();
        int maxRounds = overtime ? rules.overtimeMaxRounds() : rules.maxRounds();

        if (score.counterTerrorist() >= roundsToWin) {
            winner = Optional.of(TeamSide.COUNTER_TERRORIST);
            return;
        }
        if (score.terrorist() >= roundsToWin) {
            winner = Optional.of(TeamSide.TERRORIST);
            return;
        }

        if (maxRounds > 0 && roundNumber >= maxRounds) {
            if (score.counterTerrorist() > score.terrorist()) {
                winner = Optional.of(TeamSide.COUNTER_TERRORIST);
            } else if (score.terrorist() > score.counterTerrorist()) {
                winner = Optional.of(TeamSide.TERRORIST);
            } else if (rules.overtimeEnabled() && !overtime) {
                overtime = true;
                roundNumber = 0;
            }
        }
    }
}
