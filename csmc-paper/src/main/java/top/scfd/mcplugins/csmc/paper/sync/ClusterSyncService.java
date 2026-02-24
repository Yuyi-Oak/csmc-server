package top.scfd.mcplugins.csmc.paper.sync;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.Map;
import top.scfd.mcplugins.csmc.api.GameMode;

public interface ClusterSyncService extends AutoCloseable {
    default void start() {
    }

    void onStatsUpdated(Consumer<UUID> listener);

    void publishStatsUpdated(UUID playerId);

    default void onQueueSnapshot(Consumer<QueueSnapshot> listener) {
    }

    default void publishQueueSnapshot(Map<GameMode, Integer> queueSizes) {
    }

    @Override
    default void close() {
    }
}
