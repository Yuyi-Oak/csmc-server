package top.scfd.mcplugins.csmc.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.core.rules.EconomyRules;
import top.scfd.mcplugins.csmc.core.rules.ModeRules;
import top.scfd.mcplugins.csmc.core.rules.RoundDurations;
import top.scfd.mcplugins.csmc.core.shop.ShopCatalog;

final class GameSessionTest {
    @Test
    void autoStartsFirstRoundCountdownWhenTeamsReady() {
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rules(13), new ShopCatalog());
        session.joinPlayer(UUID.randomUUID());
        session.joinPlayer(UUID.randomUUID());

        assertEquals(SessionState.WAITING, session.state());
        assertEquals(3, session.nextRoundCountdownSeconds());

        session.tickRound();
        assertEquals(2, session.nextRoundCountdownSeconds());
        session.tickRound();
        assertEquals(1, session.nextRoundCountdownSeconds());
        session.tickRound();

        assertEquals(SessionState.LIVE, session.state());
        assertEquals(1, session.roundEngine().matchState().roundNumber());
        assertEquals(-1, session.nextRoundCountdownSeconds());
    }

    @Test
    void startRoundDoesNotRestartWhenAlreadyLive() {
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rules(13), new ShopCatalog());
        session.joinPlayer(UUID.randomUUID());
        session.joinPlayer(UUID.randomUUID());

        session.startRound();
        assertEquals(SessionState.LIVE, session.state());
        assertEquals(1, session.roundEngine().matchState().roundNumber());

        session.startRound();
        assertEquals(1, session.roundEngine().matchState().roundNumber());
    }

    @Test
    void sessionBecomesFinishedWhenMatchWinnerExists() {
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rules(1), new ShopCatalog());
        session.joinPlayer(UUID.randomUUID());
        session.joinPlayer(UUID.randomUUID());

        session.startRound();
        session.tickRound();
        session.tickRound();

        assertEquals(SessionState.FINISHED, session.state());
        assertEquals(-1, session.nextRoundCountdownSeconds());
    }

    private ModeRules rules(int roundsToWin) {
        return new ModeRules(
            GameMode.COMPETITIVE,
            10,
            roundsToWin,
            24,
            false,
            0,
            0,
            new RoundDurations(0, 0, 1, 40, 10, 3),
            new EconomyRules(800, 16000, 300, 150, 300, 300, 3250, 1400, 500, 3400)
        );
    }
}
