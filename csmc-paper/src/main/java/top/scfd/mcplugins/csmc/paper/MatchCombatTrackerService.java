package top.scfd.mcplugins.csmc.paper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class MatchCombatTrackerService {
    private final Map<UUID, Map<UUID, SessionCombatStats>> sessionStats = new ConcurrentHashMap<>();

    public void recordKill(GameSession session, UUID playerId) {
        if (session == null || playerId == null) {
            return;
        }
        statsForPlayer(session.id(), playerId).recordKill();
    }

    public void recordDeath(GameSession session, UUID playerId) {
        if (session == null || playerId == null) {
            return;
        }
        statsForPlayer(session.id(), playerId).recordDeath();
    }

    public void recordAssist(GameSession session, UUID playerId) {
        if (session == null || playerId == null) {
            return;
        }
        statsForPlayer(session.id(), playerId).recordAssist();
    }

    public void recordHeadshot(GameSession session, UUID playerId) {
        if (session == null || playerId == null) {
            return;
        }
        statsForPlayer(session.id(), playerId).recordHeadshot();
    }

    public List<PlayerCombatSnapshot> leaderboard(GameSession session, int limit) {
        if (session == null || limit <= 0) {
            return List.of();
        }
        Map<UUID, SessionCombatStats> players = sessionStats.get(session.id());
        if (players == null || players.isEmpty()) {
            return List.of();
        }
        List<PlayerCombatSnapshot> snapshots = new ArrayList<>(players.size());
        for (Map.Entry<UUID, SessionCombatStats> entry : players.entrySet()) {
            SessionCombatStats stats = entry.getValue();
            snapshots.add(new PlayerCombatSnapshot(
                entry.getKey(),
                stats.kills(),
                stats.deaths(),
                stats.assists(),
                stats.headshots()
            ));
        }
        snapshots.sort(Comparator
            .comparingInt(PlayerCombatSnapshot::kills).reversed()
            .thenComparingInt(PlayerCombatSnapshot::assists).reversed()
            .thenComparingInt(PlayerCombatSnapshot::deaths)
            .thenComparing(snapshot -> snapshot.playerId().toString()));
        if (snapshots.size() <= limit) {
            return List.copyOf(snapshots);
        }
        return List.copyOf(snapshots.subList(0, limit));
    }

    public void clearSession(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        sessionStats.remove(sessionId);
    }

    private SessionCombatStats statsForPlayer(UUID sessionId, UUID playerId) {
        Map<UUID, SessionCombatStats> players = sessionStats.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>());
        return players.computeIfAbsent(playerId, ignored -> new SessionCombatStats());
    }
}
