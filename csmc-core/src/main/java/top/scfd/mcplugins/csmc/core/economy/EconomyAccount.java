package top.scfd.mcplugins.csmc.core.economy;

import top.scfd.mcplugins.csmc.core.rules.EconomyRules;

public final class EconomyAccount {
    private int money;
    private int lossStreak;
    private final EconomyRules rules;

    public EconomyAccount(EconomyRules rules) {
        this.rules = rules;
        this.money = rules.startMoney();
        this.lossStreak = 0;
    }

    public int money() {
        return money;
    }

    public int lossStreak() {
        return lossStreak;
    }

    public void reset() {
        this.money = rules.startMoney();
        this.lossStreak = 0;
    }

    public void award(EconomyReason reason) {
        int reward = rules.rewardFor(reason, lossStreak);
        applyReward(reason, reward);
    }

    public void applyReward(EconomyReason reason, int amount) {
        if (reason == EconomyReason.ROUND_WIN) {
            lossStreak = 0;
        } else if (reason == EconomyReason.ROUND_LOSS) {
            lossStreak += 1;
        }
        addMoney(amount);
    }

    public void addMoney(int amount) {
        money = Math.min(rules.maxMoney(), Math.max(0, money + amount));
    }

    public boolean spend(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (money < amount) {
            return false;
        }
        money -= amount;
        return true;
    }
}
