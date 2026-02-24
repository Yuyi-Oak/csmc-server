package top.scfd.mcplugins.csmc.core.match;

import java.util.UUID;
import top.scfd.mcplugins.csmc.api.TeamSide;

public interface RoundEventListener {
    RoundEventListener NO_OP = new RoundEventListener() {};

    default void onRoundStart(int roundNumber) {}

    default void onPhaseChanged(RoundPhase from, RoundPhase to, int roundNumber, int durationSeconds) {}

    default void onBombPlanted(int roundNumber) {}

    default void onBombDefused(int roundNumber) {}

    default void onDefuseStarted(int roundNumber, UUID defuserId, int durationSeconds) {}

    default void onDefuseStopped(int roundNumber, UUID defuserId, int remainingSeconds) {}

    default void onBombExploded(int roundNumber) {}

    default void onTeamEliminated(int roundNumber, TeamSide eliminatedSide, TeamSide winner) {}

    default void onSideSwap(int roundNumber) {}

    default void onRoundEnd(int roundNumber, TeamSide winner, RoundEndReason reason, int terroristScore, int counterTerroristScore) {}
}
