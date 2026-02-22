package top.scfd.mcplugins.csmc.core.economy;

import java.util.UUID;

public interface EconomyEventListener {
    EconomyEventListener NO_OP = new EconomyEventListener() {};

    default void onReward(UUID playerId, EconomyReason reason, int amount, int newBalance) {}

    default void onSpend(UUID playerId, int amount, int newBalance) {}
}
