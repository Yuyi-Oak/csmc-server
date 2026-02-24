package top.scfd.mcplugins.csmc.paper.sync;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import top.scfd.mcplugins.csmc.api.GameMode;

public final class RedisClusterSyncService implements ClusterSyncService {
    private final String serverId = UUID.randomUUID().toString();
    private final String statsChannel;
    private final String queueChannel;
    private final RedisClient client;
    private final StatefulRedisPubSubConnection<String, String> connection;
    private final RedisPubSubCommands<String, String> commands;
    private final List<Consumer<UUID>> statsListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<QueueSnapshot>> queueListeners = new CopyOnWriteArrayList<>();
    private volatile boolean started;

    public RedisClusterSyncService(String uri, String namespace) {
        String resolvedNamespace = (namespace == null || namespace.isBlank()) ? "csmc" : namespace;
        this.statsChannel = resolvedNamespace + ":cluster:stats-updated";
        this.queueChannel = resolvedNamespace + ":cluster:queue-snapshot";
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
                if (message == null || message.isBlank()) {
                    return;
                }
                if (statsChannel.equals(channel)) {
                    handleStatsMessage(message);
                } else if (queueChannel.equals(channel)) {
                    handleQueueMessage(message);
                }
            }
        });
        commands.subscribe(statsChannel, queueChannel);
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
        commands.publish(statsChannel, serverId + "|" + playerId);
    }

    @Override
    public void onQueueSnapshot(Consumer<QueueSnapshot> listener) {
        if (listener != null) {
            queueListeners.add(listener);
        }
    }

    @Override
    public void publishQueueSnapshot(Map<GameMode, Integer> queueSizes) {
        if (!started) {
            return;
        }
        long epochSecond = System.currentTimeMillis() / 1000L;
        commands.publish(queueChannel, serverId + "|" + epochSecond + "|" + encodeQueueSizes(queueSizes));
    }

    private void handleStatsMessage(String message) {
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

    private void handleQueueMessage(String message) {
        int firstSeparator = message.indexOf('|');
        if (firstSeparator <= 0 || firstSeparator >= message.length() - 1) {
            return;
        }
        int secondSeparator = message.indexOf('|', firstSeparator + 1);
        if (secondSeparator <= firstSeparator + 1 || secondSeparator >= message.length() - 1) {
            return;
        }
        String sourceServerId = message.substring(0, firstSeparator);
        if (serverId.equals(sourceServerId)) {
            return;
        }
        long epochSecond;
        try {
            epochSecond = Long.parseLong(message.substring(firstSeparator + 1, secondSeparator));
        } catch (NumberFormatException ignored) {
            return;
        }
        QueueSnapshot snapshot;
        try {
            snapshot = new QueueSnapshot(
                sourceServerId,
                epochSecond,
                parseQueueSizes(message.substring(secondSeparator + 1))
            );
        } catch (IllegalArgumentException ignored) {
            return;
        }
        for (Consumer<QueueSnapshot> listener : queueListeners) {
            listener.accept(snapshot);
        }
    }

    private Map<GameMode, Integer> parseQueueSizes(String payload) {
        EnumMap<GameMode, Integer> queueSizes = new EnumMap<>(GameMode.class);
        for (GameMode mode : GameMode.values()) {
            queueSizes.put(mode, 0);
        }
        if (payload == null || payload.isBlank()) {
            return queueSizes;
        }
        String[] pairs = payload.split(",");
        for (String pair : pairs) {
            int separator = pair.indexOf('=');
            if (separator <= 0 || separator >= pair.length() - 1) {
                continue;
            }
            GameMode mode;
            try {
                mode = GameMode.valueOf(pair.substring(0, separator).trim());
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            int count;
            try {
                count = Integer.parseInt(pair.substring(separator + 1).trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            queueSizes.put(mode, Math.max(0, count));
        }
        return queueSizes;
    }

    private String encodeQueueSizes(Map<GameMode, Integer> queueSizes) {
        StringBuilder builder = new StringBuilder();
        for (GameMode mode : GameMode.values()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            int value = 0;
            if (queueSizes != null) {
                Integer provided = queueSizes.get(mode);
                if (provided != null) {
                    value = Math.max(0, provided);
                }
            }
            builder.append(mode.name()).append('=').append(value);
        }
        return builder.toString();
    }

    @Override
    public void close() {
        try {
            if (started) {
                commands.unsubscribe(statsChannel, queueChannel);
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
