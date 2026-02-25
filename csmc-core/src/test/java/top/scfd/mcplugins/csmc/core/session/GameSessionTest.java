package top.scfd.mcplugins.csmc.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.rules.EconomyRules;
import top.scfd.mcplugins.csmc.core.rules.ModeRules;
import top.scfd.mcplugins.csmc.core.rules.RoundDurations;
import top.scfd.mcplugins.csmc.core.shop.BuyResult;
import top.scfd.mcplugins.csmc.core.shop.ShopCatalog;

final class GameSessionTest {
    @Test
    void autoStartsFirstRoundCountdownWhenTeamsReady() {
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rules(13, 24), new ShopCatalog());
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
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rules(13, 24), new ShopCatalog());
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
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rules(1, 24), new ShopCatalog());
        session.joinPlayer(UUID.randomUUID());
        session.joinPlayer(UUID.randomUUID());

        session.startRound();
        session.tickRound();
        session.tickRound();

        assertEquals(SessionState.FINISHED, session.state());
        assertEquals(-1, session.nextRoundCountdownSeconds());
    }

    @Test
    void sidesSwapAndEconomyResetAtHalftime() {
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rules(3, 4), new ShopCatalog());
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        session.joinPlayer(first);
        session.joinPlayer(second);

        assertEquals(TeamSide.TERRORIST, session.getSide(first));
        assertEquals(TeamSide.COUNTER_TERRORIST, session.getSide(second));

        session.onKill(first);
        assertEquals(1100, session.economy().balance(first));

        session.startRound();
        session.tickRound();
        session.tickRound();
        session.startRound();
        session.tickRound();
        session.tickRound();

        assertEquals(SessionState.WAITING, session.state());
        assertTrue(session.sidesSwapped());
        assertEquals(10, session.nextRoundCountdownSeconds());
        assertEquals(TeamSide.COUNTER_TERRORIST, session.getSide(first));
        assertEquals(TeamSide.TERRORIST, session.getSide(second));
        assertEquals(800, session.economy().balance(first));
    }

    @Test
    void teamRestrictedWeaponsAreEnforced() {
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rulesWithBuyTime(13, 24), new ShopCatalog());
        UUID terrorist = UUID.randomUUID();
        UUID counterTerrorist = UUID.randomUUID();
        session.joinPlayer(terrorist);
        session.joinPlayer(counterTerrorist);

        session.startRound();

        assertEquals(BuyResult.SIDE_RESTRICTED, session.buy(terrorist, "usp"));
        assertEquals(BuyResult.SIDE_RESTRICTED, session.buy(counterTerrorist, "ak47"));
        assertEquals(BuyResult.SUCCESS, session.buy(terrorist, "glock"));
        assertEquals(BuyResult.SUCCESS, session.buy(counterTerrorist, "usp"));
    }

    @Test
    void defuseKitIsCounterTerroristOnly() {
        GameSession session = new GameSession(UUID.randomUUID(), GameMode.COMPETITIVE, 10, rulesWithBuyTime(13, 24), new ShopCatalog());
        UUID terrorist = UUID.randomUUID();
        UUID counterTerrorist = UUID.randomUUID();
        session.joinPlayer(terrorist);
        session.joinPlayer(counterTerrorist);

        session.startRound();

        assertEquals(BuyResult.SIDE_RESTRICTED, session.buy(terrorist, "defuse_kit"));
        assertEquals(BuyResult.SUCCESS, session.buy(counterTerrorist, "defuse_kit"));
    }

    private ModeRules rules(int roundsToWin, int maxRounds) {
        return new ModeRules(
            GameMode.COMPETITIVE,
            10,
            roundsToWin,
            maxRounds,
            true,
            false,
            0,
            0,
            new RoundDurations(0, 0, 1, 40, 10, 3),
            new EconomyRules(800, 16000, 300, 150, 300, 300, 3250, 1400, 500, 3400)
        );
    }

    private ModeRules rulesWithBuyTime(int roundsToWin, int maxRounds) {
        return new ModeRules(
            GameMode.COMPETITIVE,
            10,
            roundsToWin,
            maxRounds,
            true,
            false,
            0,
            0,
            new RoundDurations(0, 20, 60, 40, 10, 3),
            new EconomyRules(800, 16000, 300, 150, 300, 300, 3250, 1400, 500, 3400)
        );
    }
}
