package top.scfd.mcplugins.csmc.paper.sync;

import java.util.UUID;
import java.util.function.Consumer;

public interface ClusterSyncService extends AutoCloseable {
    default void start() {
    }

    void onStatsUpdated(Consumer<UUID> listener);

    void publishStatsUpdated(UUID playerId);

    @Override
    default void close() {
    }
}
