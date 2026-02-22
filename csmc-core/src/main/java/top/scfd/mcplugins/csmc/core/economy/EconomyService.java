package top.scfd.mcplugins.csmc.core.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import top.scfd.mcplugins.csmc.core.rules.EconomyRules;

public final class EconomyService {
    private final EconomyRules rules;
    private final Map<UUID, EconomyAccount> accounts = new ConcurrentHashMap<>();
    private final EconomyEventListener listener;

    public EconomyService(EconomyRules rules) {
        this.rules = rules;
        this.listener = EconomyEventListener.NO_OP;
    }

    public EconomyService(EconomyRules rules, EconomyEventListener listener) {
        this.rules = rules;
        this.listener = listener == null ? EconomyEventListener.NO_OP : listener;
    }

    public EconomyAccount account(UUID playerId) {
        return accounts.computeIfAbsent(playerId, id -> new EconomyAccount(rules));
    }

    public int balance(UUID playerId) {
        return account(playerId).money();
    }

    public int award(UUID playerId, EconomyReason reason) {
        EconomyAccount account = account(playerId);
        int amount = account.award(reason);
        if (amount != 0) {
            listener.onReward(playerId, reason, amount, account.money());
        }
        return amount;
    }

    public boolean spend(UUID playerId, int amount) {
        EconomyAccount account = account(playerId);
        boolean success = account.spend(amount);
        if (success && amount > 0) {
            listener.onSpend(playerId, amount, account.money());
        }
        return success;
    }

    public void reset(UUID playerId) {
        account(playerId).reset();
    }

    public void clear(UUID playerId) {
        accounts.remove(playerId);
    }
}
