package top.scfd.mcplugins.csmc.core.match;

import top.scfd.mcplugins.csmc.api.TeamSide;

public interface RoundEventListener {
    RoundEventListener NO_OP = new RoundEventListener() {};

    default void onRoundStart(int roundNumber) {}

    default void onPhaseChanged(RoundPhase from, RoundPhase to, int roundNumber, int durationSeconds) {}

    default void onBombPlanted(int roundNumber) {}

    default void onBombDefused(int roundNumber) {}

    default void onBombExploded(int roundNumber) {}

    default void onTeamEliminated(int roundNumber, TeamSide eliminatedSide, TeamSide winner) {}

    default void onRoundEnd(int roundNumber, TeamSide winner, RoundEndReason reason, int terroristScore, int counterTerroristScore) {}
}
