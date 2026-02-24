package top.scfd.mcplugins.csmc.paper.history;

import java.util.List;
import java.util.UUID;

public record MatchHistoryEntry(
    UUID sessionId,
    String mode,
    String mapId,
    String winner,
    int terroristScore,
    int counterTerroristScore,
    long startedAtEpochSeconds,
    long endedAtEpochSeconds,
    List<UUID> players
) {
}
