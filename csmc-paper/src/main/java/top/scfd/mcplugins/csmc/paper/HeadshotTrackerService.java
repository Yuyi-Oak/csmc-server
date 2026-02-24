package top.scfd.mcplugins.csmc.paper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeadshotTrackerService {
    private final Map<UUID, Marker> markers = new ConcurrentHashMap<>();

    public void markHeadshot(UUID victimId, UUID attackerId, long expiresAtTick) {
        if (victimId == null || attackerId == null || expiresAtTick <= 0L) {
            return;
        }
        markers.put(victimId, new Marker(attackerId, expiresAtTick));
    }

    public boolean consumeIfMatching(UUID victimId, UUID attackerId, long currentTick) {
        if (victimId == null || attackerId == null) {
            return false;
        }
        Marker marker = markers.remove(victimId);
        if (marker == null) {
            return false;
        }
        return marker.attackerId.equals(attackerId) && currentTick <= marker.expiresAtTick;
    }

    private static final class Marker {
        private final UUID attackerId;
        private final long expiresAtTick;

        private Marker(UUID attackerId, long expiresAtTick) {
            this.attackerId = attackerId;
            this.expiresAtTick = expiresAtTick;
        }
    }
}
