package top.scfd.mcplugins.csmc.core.rules;

public record RoundDurations(
    int freezeTimeSeconds,
    int buyTimeSeconds,
    int roundTimeSeconds,
    int bombTimerSeconds,
    int defuseTimeSeconds,
    int plantTimeSeconds
) {
}
