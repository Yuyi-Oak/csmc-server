package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;
import org.bukkit.Location;

public final class BombState {
    private final Location location;
    private final UUID planterId;
    private UUID defuserId;

    public BombState(Location location, UUID planterId) {
        this.location = location;
        this.planterId = planterId;
    }

    public Location location() {
        return location;
    }

    public UUID planterId() {
        return planterId;
    }

    public UUID defuserId() {
        return defuserId;
    }

    public void setDefuserId(UUID defuserId) {
        this.defuserId = defuserId;
    }
}
