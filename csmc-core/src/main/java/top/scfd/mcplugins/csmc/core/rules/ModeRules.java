package top.scfd.mcplugins.csmc.core.rules;

import top.scfd.mcplugins.csmc.api.GameMode;

public record ModeRules(
    GameMode mode,
    int maxPlayers,
    int roundsToWin,
    int maxRounds,
    boolean friendlyFireEnabled,
    boolean overtimeEnabled,
    int overtimeRoundsToWin,
    int overtimeMaxRounds,
    RoundDurations durations,
    EconomyRules economy
) {
}
