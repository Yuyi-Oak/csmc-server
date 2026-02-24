package top.scfd.mcplugins.csmc.paper;

import java.util.Map;
import top.scfd.mcplugins.csmc.api.GameMode;

public record RemoteQueueSourceStatus(
    String serverId,
    long ageSeconds,
    Map<GameMode, Integer> queueSizes
) {
}
