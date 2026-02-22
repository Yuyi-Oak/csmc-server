package top.scfd.mcplugins.csmc.core.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import top.scfd.mcplugins.csmc.core.rules.EconomyRules;

public final class EconomyService {
    private final EconomyRules rules;
    private final Map<UUID, EconomyAccount> accounts = new ConcurrentHashMap<>();

    public EconomyService(EconomyRules rules) {
        this.rules = rules;
    }

    public EconomyAccount account(UUID playerId) {
        return accounts.computeIfAbsent(playerId, id -> new EconomyAccount(rules));
    }

    public int balance(UUID playerId) {
        return account(playerId).money();
    }

    public void award(UUID playerId, EconomyReason reason) {
        account(playerId).award(reason);
    }

    public boolean spend(UUID playerId, int amount) {
        return account(playerId).spend(amount);
    }

    public void reset(UUID playerId) {
        account(playerId).reset();
    }

    public void clear(UUID playerId) {
        accounts.remove(playerId);
    }
}
