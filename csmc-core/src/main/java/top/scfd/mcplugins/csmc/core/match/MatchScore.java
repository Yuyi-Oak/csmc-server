package top.scfd.mcplugins.csmc.core.match;

import top.scfd.mcplugins.csmc.api.TeamSide;

public final class MatchScore {
    private int terroristScore;
    private int counterTerroristScore;

    public int terrorist() {
        return terroristScore;
    }

    public int counterTerrorist() {
        return counterTerroristScore;
    }

    public void award(TeamSide winner) {
        if (winner == TeamSide.TERRORIST) {
            terroristScore += 1;
        } else if (winner == TeamSide.COUNTER_TERRORIST) {
            counterTerroristScore += 1;
        }
    }
}
