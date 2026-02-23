package top.scfd.mcplugins.csmc.storage;

import java.util.UUID;

public record LeaderboardEntry(UUID playerId, PlayerStats stats) {
}
