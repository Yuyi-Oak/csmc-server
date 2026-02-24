package top.scfd.mcplugins.csmc.paper.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import top.scfd.mcplugins.csmc.core.map.BombSite;
import top.scfd.mcplugins.csmc.core.map.BuyZone;
import top.scfd.mcplugins.csmc.core.map.MapSpawn;

public final class EditableMap {
    private final String id;
    private String name;
    private String world;
    private final List<MapSpawn> terroristSpawns = new ArrayList<>();
    private final List<MapSpawn> counterTerroristSpawns = new ArrayList<>();
    private final Map<String, BombSite> bombSites = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<BuyZone> terroristBuyZones = new ArrayList<>();
    private final List<BuyZone> counterTerroristBuyZones = new ArrayList<>();

    public EditableMap(String id, String name, String world) {
        this.id = id;
        this.name = name;
        this.world = world;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String world() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public List<MapSpawn> terroristSpawns() {
        return terroristSpawns;
    }

    public List<MapSpawn> counterTerroristSpawns() {
        return counterTerroristSpawns;
    }

    public Map<String, BombSite> bombSites() {
        return bombSites;
    }

    public List<BuyZone> terroristBuyZones() {
        return terroristBuyZones;
    }

    public List<BuyZone> counterTerroristBuyZones() {
        return counterTerroristBuyZones;
    }
}
