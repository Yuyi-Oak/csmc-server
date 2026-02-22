package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.core.economy.EconomyEventListener;
import top.scfd.mcplugins.csmc.core.economy.EconomyReason;

public final class PaperEconomyNotifier implements EconomyEventListener {
    private final SessionRegistry sessions;

    public PaperEconomyNotifier(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @Override
    public void onReward(UUID playerId, EconomyReason reason, int amount, int newBalance) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || sessions.findSession(player) == null) {
            return;
        }
        Component component = Component.text("+$" + amount + " (" + newBalance + ")")
            .color(NamedTextColor.GREEN);
        player.sendActionBar(component);
    }

    @Override
    public void onSpend(UUID playerId, int amount, int newBalance) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || sessions.findSession(player) == null) {
            return;
        }
        Component component = Component.text("-$" + amount + " (" + newBalance + ")")
            .color(NamedTextColor.RED);
        player.sendActionBar(component);
    }
}
