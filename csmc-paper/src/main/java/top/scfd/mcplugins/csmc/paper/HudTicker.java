package top.scfd.mcplugins.csmc.paper;

import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.match.RoundEngine;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class HudTicker extends BukkitRunnable {
    private final SessionRegistry sessions;

    public HudTicker(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            GameSession session = sessions.findSession(player);
            if (session == null) {
                continue;
            }
            RoundEngine round = session.roundEngine();
            int roundTime = round.roundRemainingSeconds();
            int buyTime = round.buyRemainingSeconds();
            int money = session.economy().balance(player.getUniqueId());
            TeamSide side = session.getSide(player.getUniqueId());

            String text = "R:" + format(roundTime) + " B:" + format(buyTime) + " $" + money + " " + side.name();
            player.sendActionBar(Component.text(text).color(NamedTextColor.WHITE));
        }
    }

    private String format(int seconds) {
        if (seconds < 0) {
            return "0:00";
        }
        int m = seconds / 60;
        int s = seconds % 60;
        return m + ":" + (s < 10 ? "0" + s : s);
    }
}
