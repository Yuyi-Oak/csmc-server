package top.scfd.mcplugins.csmc.paper.sync;

import java.util.UUID;
import java.util.function.Consumer;

public final class NoopClusterSyncService implements ClusterSyncService {
    @Override
    public void onStatsUpdated(Consumer<UUID> listener) {
        // no-op
    }

    @Override
    public void publishStatsUpdated(UUID playerId) {
        // no-op
    }
}
