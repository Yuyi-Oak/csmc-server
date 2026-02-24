package top.scfd.mcplugins.csmc.paper.history;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.match.RoundEndReason;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class SessionHistoryRoundListener implements RoundEventListener {
    private final GameSession session;
    private final MatchHistoryService history;
    private final String mapId;
    private final long startedAtEpochSeconds;
    private volatile boolean recorded;

    public SessionHistoryRoundListener(GameSession session, MatchHistoryService history, String mapId, long startedAtEpochSeconds) {
        this.session = session;
        this.history = history;
        this.mapId = mapId == null || mapId.isBlank() ? "unknown" : mapId;
        this.startedAtEpochSeconds = Math.max(0L, startedAtEpochSeconds);
    }

    @Override
    public void onRoundEnd(int roundNumber, TeamSide winner, RoundEndReason reason, int terroristScore, int counterTerroristScore) {
        if (recorded) {
            return;
        }
        TeamSide matchWinner = session.roundEngine().matchState().winner().orElse(null);
        if (matchWinner == null) {
            return;
        }
        recorded = true;
        List<UUID> players = new ArrayList<>(session.players());
        history.append(new MatchHistoryEntry(
            session.id(),
            session.mode().name(),
            mapId,
            matchWinner.name(),
            terroristScore,
            counterTerroristScore,
            startedAtEpochSeconds,
            Instant.now().getEpochSecond(),
            List.copyOf(players)
        ));
    }
}
