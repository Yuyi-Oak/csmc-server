package top.scfd.mcplugins.csmc.core.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import top.scfd.mcplugins.csmc.core.economy.EconomyReason;

final class EconomyRulesTest {
    @Test
    void lossRewardCapsAtConfiguredMaximum() {
        EconomyRules rules = new EconomyRules(
            800,
            16000,
            300,
            150,
            300,
            300,
            3250,
            1400,
            500,
            3400
        );

        assertEquals(1400, rules.rewardFor(EconomyReason.ROUND_LOSS, 0));
        assertEquals(1900, rules.rewardFor(EconomyReason.ROUND_LOSS, 1));
        assertEquals(2400, rules.rewardFor(EconomyReason.ROUND_LOSS, 2));
        assertEquals(2900, rules.rewardFor(EconomyReason.ROUND_LOSS, 3));
        assertEquals(3400, rules.rewardFor(EconomyReason.ROUND_LOSS, 4));
        assertEquals(3400, rules.rewardFor(EconomyReason.ROUND_LOSS, 8));
    }

    @Test
    void malformedLossCapLowerThanBaseFallsBackToBase() {
        EconomyRules rules = new EconomyRules(
            800,
            16000,
            300,
            150,
            300,
            300,
            3250,
            1400,
            500,
            1000
        );

        assertEquals(1400, rules.rewardFor(EconomyReason.ROUND_LOSS, 0));
        assertEquals(1400, rules.rewardFor(EconomyReason.ROUND_LOSS, 10));
    }
}
