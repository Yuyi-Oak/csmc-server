package top.scfd.mcplugins.csmc.paper.sync;

import java.util.EnumMap;
import java.util.Map;
import top.scfd.mcplugins.csmc.api.GameMode;

public record QueueSnapshot(String serverId, long epochSecond, Map<GameMode, Integer> queueSizes) {
    public QueueSnapshot {
        String normalizedServerId = serverId == null ? "" : serverId.trim();
        if (normalizedServerId.isEmpty()) {
            throw new IllegalArgumentException("serverId must not be blank");
        }
        serverId = normalizedServerId;
        epochSecond = Math.max(0L, epochSecond);
        EnumMap<GameMode, Integer> normalizedSizes = new EnumMap<>(GameMode.class);
        for (GameMode mode : GameMode.values()) {
            int value = 0;
            if (queueSizes != null) {
                Integer provided = queueSizes.get(mode);
                if (provided != null) {
                    value = Math.max(0, provided);
                }
            }
            normalizedSizes.put(mode, value);
        }
        queueSizes = Map.copyOf(normalizedSizes);
    }

    public int queueSize(GameMode mode) {
        if (mode == null) {
            return 0;
        }
        return queueSizes.getOrDefault(mode, 0);
    }
}
