package top.scfd.mcplugins.csmc.core.map;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import top.scfd.mcplugins.csmc.api.TeamSide;

public final class MapDefinition {
    private final String id;
    private final String displayName;
    private final String world;
    private final List<MapSpawn> terroristSpawns;
    private final List<MapSpawn> counterTerroristSpawns;
    private final List<BombSite> bombSites;
    private final List<BuyZone> terroristBuyZones;
    private final List<BuyZone> counterTerroristBuyZones;

    public MapDefinition(
        String id,
        String displayName,
        String world,
        List<MapSpawn> terroristSpawns,
        List<MapSpawn> counterTerroristSpawns,
        List<BombSite> bombSites,
        List<BuyZone> terroristBuyZones,
        List<BuyZone> counterTerroristBuyZones
    ) {
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.terroristSpawns = safeList(terroristSpawns);
        this.counterTerroristSpawns = safeList(counterTerroristSpawns);
        this.bombSites = safeList(bombSites);
        this.terroristBuyZones = safeList(terroristBuyZones);
        this.counterTerroristBuyZones = safeList(counterTerroristBuyZones);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String world() {
        return world;
    }

    public List<MapSpawn> spawns(TeamSide side) {
        if (side == TeamSide.TERRORIST) {
            return terroristSpawns;
        }
        if (side == TeamSide.COUNTER_TERRORIST) {
            return counterTerroristSpawns;
        }
        return List.of();
    }

    public List<BombSite> bombSites() {
        return bombSites;
    }

    public Optional<BombSite> bombSite(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String key = id.trim();
        return bombSites.stream()
            .filter(site -> site.id().equalsIgnoreCase(key))
            .findFirst();
    }

    public List<BuyZone> buyZones(TeamSide side) {
        if (side == TeamSide.TERRORIST) {
            return terroristBuyZones;
        }
        if (side == TeamSide.COUNTER_TERRORIST) {
            return counterTerroristBuyZones;
        }
        return List.of();
    }

    private <T> List<T> safeList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(List.copyOf(list));
    }
}
