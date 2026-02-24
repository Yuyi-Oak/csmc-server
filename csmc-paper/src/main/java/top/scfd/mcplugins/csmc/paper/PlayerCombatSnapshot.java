package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;

public record PlayerCombatSnapshot(
    UUID playerId,
    int kills,
    int deaths,
    int assists,
    int headshots
) {
}
