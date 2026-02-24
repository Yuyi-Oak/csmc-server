package top.scfd.mcplugins.csmc.paper.sync;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class RedisClusterSyncService implements ClusterSyncService {
    private final String serverId = UUID.randomUUID().toString();
    private final String channel;
    private final RedisClient client;
    private final StatefulRedisPubSubConnection<String, String> connection;
    private final RedisPubSubCommands<String, String> commands;
    private final List<Consumer<UUID>> statsListeners = new CopyOnWriteArrayList<>();
    private volatile boolean started;

    public RedisClusterSyncService(String uri, String namespace) {
        String resolvedNamespace = (namespace == null || namespace.isBlank()) ? "csmc" : namespace;
        this.channel = resolvedNamespace + ":cluster:stats-updated";
        this.client = RedisClient.create(uri);
        this.connection = client.connectPubSub();
        this.commands = connection.sync();
    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        connection.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                if (!RedisClusterSyncService.this.channel.equals(channel) || message == null || message.isBlank()) {
                    return;
                }
                int separator = message.indexOf('|');
                if (separator <= 0 || separator >= message.length() - 1) {
                    return;
                }
                String sourceServerId = message.substring(0, separator);
                if (serverId.equals(sourceServerId)) {
                    return;
                }
                String uuidText = message.substring(separator + 1);
                UUID playerId;
                try {
                    playerId = UUID.fromString(uuidText);
                } catch (IllegalArgumentException ignored) {
                    return;
                }
                for (Consumer<UUID> listener : statsListeners) {
                    listener.accept(playerId);
                }
            }
        });
        commands.subscribe(channel);
        started = true;
    }

    @Override
    public void onStatsUpdated(Consumer<UUID> listener) {
        if (listener != null) {
            statsListeners.add(listener);
        }
    }

    @Override
    public void publishStatsUpdated(UUID playerId) {
        if (playerId == null || !started) {
            return;
        }
        commands.publish(channel, serverId + "|" + playerId);
    }

    @Override
    public void close() {
        try {
            if (started) {
                commands.unsubscribe(channel);
            }
        } catch (RuntimeException ignored) {
            // no-op
        } finally {
            started = false;
            try {
                connection.close();
            } finally {
                client.shutdown();
            }
        }
    }
}
