package top.scfd.mcplugins.csmc.core.rules;

import top.scfd.mcplugins.csmc.core.economy.EconomyReason;

public record EconomyRules(
    int startMoney,
    int maxMoney,
    int killReward,
    int assistReward,
    int plantReward,
    int defuseReward,
    int roundWinReward,
    int roundLossBase,
    int lossBonusIncrement,
    int lossBonusMax
) {
    public int rewardFor(EconomyReason reason, int lossStreak) {
        return switch (reason) {
            case KILL -> killReward;
            case ASSIST -> assistReward;
            case PLANT -> plantReward;
            case DEFUSE -> defuseReward;
            case ROUND_WIN -> roundWinReward;
            case ROUND_LOSS -> roundLossBase + Math.min(lossBonusMax, lossStreak * lossBonusIncrement);
        };
    }
}
