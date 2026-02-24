package top.scfd.mcplugins.csmc.paper;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.core.match.RoundEndReason;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class RoundScoreboardNotifier implements RoundEventListener {
    private final GameSession session;
    private final MatchCombatTrackerService combatTracker;

    public RoundScoreboardNotifier(GameSession session, MatchCombatTrackerService combatTracker) {
        this.session = session;
        this.combatTracker = combatTracker;
    }

    @Override
    public void onRoundEnd(int roundNumber, top.scfd.mcplugins.csmc.api.TeamSide winner, RoundEndReason reason, int terroristScore, int counterTerroristScore) {
        List<PlayerCombatSnapshot> top = combatTracker.leaderboard(session, 3);
        if (top.isEmpty()) {
            return;
        }
        broadcastToSession("Round " + roundNumber + " scoreboard:");
        int position = 1;
        for (PlayerCombatSnapshot entry : top) {
            String name = resolveName(entry.playerId());
            double kd = entry.deaths() == 0
                ? entry.kills()
                : (double) entry.kills() / entry.deaths();
            broadcastToSession(
                position + ". " + name
                    + " | K:" + entry.kills()
                    + " D:" + entry.deaths()
                    + " A:" + entry.assists()
                    + " HS:" + entry.headshots()
                    + " KD:" + String.format(Locale.ROOT, "%.2f", kd)
            );
            position++;
        }
        if (session.roundEngine().matchState().winner().isPresent()) {
            PlayerCombatSnapshot mvp = top.get(0);
            broadcastToSession("Match MVP: " + resolveName(mvp.playerId()) + " (" + mvp.kills() + "K/" + mvp.assists() + "A)");
        }
    }

    private void broadcastToSession(String text) {
        for (UUID playerId : session.players()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(text);
            }
        }
    }

    private String resolveName(UUID playerId) {
        if (playerId == null) {
            return "unknown";
        }
        Player online = Bukkit.getPlayer(playerId);
        if (online != null && online.isOnline()) {
            return online.getName();
        }
        String offline = Bukkit.getOfflinePlayer(playerId).getName();
        return (offline == null || offline.isBlank()) ? playerId.toString() : offline;
    }
}
