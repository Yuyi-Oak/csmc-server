package top.scfd.mcplugins.csmc.paper;

import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.i18n.MessageService;
import top.scfd.mcplugins.csmc.core.match.RoundEndReason;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class PaperRoundNotifier implements RoundEventListener {
    private final MessageService messages;
    private final GameSession session;

    public PaperRoundNotifier(MessageService messages, GameSession session) {
        this.messages = messages;
        this.session = session;
    }

    @Override
    public void onRoundStart(int roundNumber) {
        broadcast(messages.message("ui.round_start", Map.of("round", String.valueOf(roundNumber))), NamedTextColor.GOLD);
    }

    @Override
    public void onPhaseChanged(RoundPhase from, RoundPhase to, int roundNumber, int durationSeconds) {
        switch (to) {
            case FREEZE -> broadcast(messages.message("ui.phase_freeze"), NamedTextColor.GRAY);
            case BUY -> broadcast(messages.message("ui.phase_buy"), NamedTextColor.GREEN);
            case LIVE -> broadcast(messages.message("ui.phase_live"), NamedTextColor.RED);
            case BOMB_PLANTED -> broadcast(messages.message("ui.bomb_planted"), NamedTextColor.RED);
            case END -> { }
        }
    }

    @Override
    public void onBombPlanted(int roundNumber) {
        broadcast(messages.message("ui.bomb_planted"), NamedTextColor.RED);
    }

    @Override
    public void onBombDefused(int roundNumber) {
        broadcast(messages.message("ui.bomb_defused"), NamedTextColor.AQUA);
    }

    @Override
    public void onBombExploded(int roundNumber) {
        broadcast(messages.message("ui.bomb_exploded"), NamedTextColor.RED);
    }

    @Override
    public void onTeamEliminated(int roundNumber, TeamSide eliminatedSide, TeamSide winner) {
        broadcast(messages.message("ui.team_eliminated"), NamedTextColor.YELLOW);
    }

    @Override
    public void onSideSwap(int roundNumber) {
        broadcast(messages.message("ui.sides_swapped"), NamedTextColor.AQUA);
    }

    @Override
    public void onRoundEnd(int roundNumber, TeamSide winner, RoundEndReason reason, int terroristScore, int counterTerroristScore) {
        String message = messages.message("ui.round_end");
        broadcast(message + " (" + terroristScore + "-" + counterTerroristScore + ")", NamedTextColor.YELLOW);
    }

    private void broadcast(String text, NamedTextColor color) {
        Component component = Component.text(text).color(color);
        for (UUID playerId : session.players()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendActionBar(component);
            }
        }
    }
}
