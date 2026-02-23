package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.match.RoundEndReason;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class StatsRoundListener implements RoundEventListener {
    private final GameSession session;
    private final StatsService statsService;

    public StatsRoundListener(GameSession session, StatsService statsService) {
        this.session = session;
        this.statsService = statsService;
    }

    @Override
    public void onRoundEnd(int roundNumber, TeamSide winner, RoundEndReason reason, int terroristScore, int counterTerroristScore) {
        for (UUID playerId : session.players()) {
            statsService.recordRoundPlayed(playerId);
            if (session.getSide(playerId) == winner) {
                statsService.recordRoundWon(playerId);
            }
        }
    }
}
