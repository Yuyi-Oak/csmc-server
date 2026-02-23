package top.scfd.mcplugins.csmc.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.match.RoundEngine;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
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
            int bombTime = round.bombRemainingSeconds();
            int defuseTime = round.defuseRemainingSeconds();
            int money = session.economy().balance(player.getUniqueId());
            TeamSide side = session.getSide(player.getUniqueId());
            var score = round.matchState().score();
            var loadout = session.loadout(player.getUniqueId());
            var weapon = loadout == null ? null : loadout.activeWeapon();
            int armor = loadout == null ? 0 : loadout.armor().armor();
            SessionState state = session.state();

            StringBuilder text = new StringBuilder();
            if (state == SessionState.WAITING) {
                text.append("N:").append(formatCountdown(session.nextRoundCountdownSeconds()));
            } else if (state == SessionState.FINISHED) {
                text.append("MATCH END");
                round.matchState().winner().ifPresent(winner -> text.append(" ").append(shortSide(winner)));
            } else {
                text.append("R:").append(format(roundTime));
                if (round.phase() == RoundPhase.BOMB_PLANTED) {
                    text.append(" C4:").append(format(bombTime));
                }
                if (defuseTime > 0) {
                    text.append(" D:").append(format(defuseTime));
                }
                text.append(" B:").append(format(buyTime));
            }
            text.append(" T:").append(score.terrorist());
            text.append(" CT:").append(score.counterTerrorist());
            text.append(" $").append(money);
            text.append(" A:").append(armor);
            text.append(" ").append(side.name());
            if (weapon != null) {
                text.append(" ").append(weapon.magazine()).append("/").append(weapon.reserve());
            }
            player.sendActionBar(Component.text(text.toString()).color(NamedTextColor.WHITE));
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

    private String formatCountdown(int seconds) {
        if (seconds < 0) {
            return "--:--";
        }
        return format(seconds);
    }

    private String shortSide(TeamSide side) {
        return side == TeamSide.TERRORIST ? "T" : "CT";
    }
}
