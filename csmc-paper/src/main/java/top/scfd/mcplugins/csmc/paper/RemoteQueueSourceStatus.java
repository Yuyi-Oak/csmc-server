package top.scfd.mcplugins.csmc.paper;

import java.util.Map;
import top.scfd.mcplugins.csmc.api.GameMode;

public record RemoteQueueSourceStatus(
    String serverId,
    long ageSeconds,
    Map<GameMode, Integer> queueSizes
) {
    public int totalQueued() {
        int total = 0;
        if (queueSizes == null || queueSizes.isEmpty()) {
            return 0;
        }
        for (GameMode mode : GameMode.values()) {
            total += queueSizes.getOrDefault(mode, 0);
        }
        return total;
    }
}
