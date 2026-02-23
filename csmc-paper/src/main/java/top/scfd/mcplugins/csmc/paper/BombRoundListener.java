package top.scfd.mcplugins.csmc.paper;

import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class BombRoundListener implements RoundEventListener {
    private final GameSession session;
    private final BombService bombs;

    public BombRoundListener(GameSession session, BombService bombs) {
        this.session = session;
        this.bombs = bombs;
    }

    @Override
    public void onRoundStart(int roundNumber) {
        bombs.clear(session);
        bombs.assignBomb(session);
    }

    @Override
    public void onBombDefused(int roundNumber) {
        bombs.clear(session);
    }

    @Override
    public void onBombExploded(int roundNumber) {
        bombs.clear(session);
    }

    @Override
    public void onRoundEnd(int roundNumber, top.scfd.mcplugins.csmc.api.TeamSide winner, top.scfd.mcplugins.csmc.core.match.RoundEndReason reason, int terroristScore, int counterTerroristScore) {
        bombs.clear(session);
    }
}
