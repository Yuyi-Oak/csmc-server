package top.scfd.mcplugins.csmc.paper.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntiCheatService {
    private static final int ALERT_THRESHOLD = 12;
    private static final int KICK_THRESHOLD = 24;
    private static final long ALERT_COOLDOWN_MILLIS = 30_000L;
    private static final long KICK_COOLDOWN_MILLIS = 5 * 60_000L;

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> violationLevels = new ConcurrentHashMap<>();
    private final Map<String, Integer> reasonCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerReasonCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAlertAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastKickAt = new ConcurrentHashMap<>();

    public AntiCheatService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void flag(Player player, String reason, int points) {
        if (player == null || points <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        String normalizedReason = normalizeReason(reason);
        reasonCounts.merge(normalizedReason, 1, Integer::sum);
        playerReasonCounts.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
            .merge(normalizedReason, 1, Integer::sum);
        int level = violationLevels.merge(playerId, points, Integer::sum);
        if (level >= KICK_THRESHOLD) {
            maybeKick(player, normalizedReason, level);
        }
        if (level < ALERT_THRESHOLD) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastAlertAt.getOrDefault(playerId, 0L);
        if (now - last < ALERT_COOLDOWN_MILLIS) {
            return;
        }
        lastAlertAt.put(playerId, now);
        alertOps(player.getName(), normalizedReason, level);
    }

    public void decay() {
        for (Map.Entry<UUID, Integer> entry : violationLevels.entrySet()) {
            int reduced = Math.max(0, entry.getValue() - 1);
            if (reduced == 0) {
                violationLevels.remove(entry.getKey(), entry.getValue());
                lastAlertAt.remove(entry.getKey());
                lastKickAt.remove(entry.getKey());
                playerReasonCounts.remove(entry.getKey());
            } else {
                violationLevels.put(entry.getKey(), reduced);
            }
        }
    }

    public int violationLevel(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return violationLevels.getOrDefault(playerId, 0);
    }

    public void reset(UUID playerId) {
        if (playerId == null) {
            return;
        }
        violationLevels.remove(playerId);
        playerReasonCounts.remove(playerId);
        lastAlertAt.remove(playerId);
        lastKickAt.remove(playerId);
    }

    public Map<UUID, Integer> violationSnapshot() {
        return Map.copyOf(violationLevels);
    }

    public Map<String, Integer> reasonSnapshot() {
        return Map.copyOf(reasonCounts);
    }

    public Map<String, Integer> playerReasonSnapshot(UUID playerId) {
        if (playerId == null) {
            return Map.of();
        }
        Map<String, Integer> reasons = playerReasonCounts.get(playerId);
        if (reasons == null || reasons.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(reasons);
    }

    public void clearReasonStats() {
        reasonCounts.clear();
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return "unknown";
        }
        String normalized = reason.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? "unknown" : normalized;
    }

    private void maybeKick(Player player, String reason, int level) {
        if (player.isOp() || player.hasPermission("csmc.anticheat.bypass")) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastKick = lastKickAt.getOrDefault(playerId, 0L);
        if (now - lastKick < KICK_COOLDOWN_MILLIS) {
            return;
        }
        lastKickAt.put(playerId, now);
        violationLevels.put(playerId, 0);
        String message = "Disconnected by CSMC Anti-Cheat (" + reason + ", vl=" + level + ")";
        if (Bukkit.isPrimaryThread()) {
            player.kick(Component.text(message).color(NamedTextColor.RED));
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> player.kick(Component.text(message).color(NamedTextColor.RED)));
        }
        plugin.getLogger().warning("[CSMC-AC] Kicked " + player.getName() + " for " + reason + ", vl=" + level);
    }

    private void alertOps(String playerName, String reason, int level) {
        String text = "[CSMC-AC] " + playerName + " flagged (" + reason + "), vl=" + level;
        plugin.getLogger().warning(text);
        Component message = Component.text(text).color(NamedTextColor.RED);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOp() || online.hasPermission("csmc.anticheat.alert")) {
                online.sendMessage(message);
            }
        }
    }
}
