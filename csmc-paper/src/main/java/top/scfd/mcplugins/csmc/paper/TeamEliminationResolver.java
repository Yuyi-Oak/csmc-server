package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class TeamEliminationResolver {
    public void resolveIfEliminated(GameSession session, TeamSide side) {
        if (session == null || side == null || side == TeamSide.SPECTATOR) {
            return;
        }
        if (!canResolve(session.roundEngine().phase(), side)) {
            return;
        }
        if (aliveCount(session, side) > 0) {
            return;
        }
        session.onTeamEliminated(side);
    }

    private boolean canResolve(RoundPhase phase, TeamSide eliminatedSide) {
        if (phase == RoundPhase.LIVE || phase == RoundPhase.BUY) {
            return true;
        }
        return phase == RoundPhase.BOMB_PLANTED && eliminatedSide == TeamSide.COUNTER_TERRORIST;
    }

    private int aliveCount(GameSession session, TeamSide side) {
        int count = 0;
        for (UUID playerId : session.players()) {
            if (session.getSide(playerId) != side) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (!player.isDead() && player.getHealth() > 0.0) {
                count++;
            }
        }
        return count;
    }
}
