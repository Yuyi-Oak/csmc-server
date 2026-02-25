package top.scfd.mcplugins.csmc.core.shop;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import top.scfd.mcplugins.csmc.api.TeamSide;

public final class ShopCatalog {
    private final Map<String, ShopItem> items = new LinkedHashMap<>();

    public ShopCatalog() {
        Set<TeamSide> tSide = EnumSet.of(TeamSide.TERRORIST);
        Set<TeamSide> ctSide = EnumSet.of(TeamSide.COUNTER_TERRORIST);
        register(new ShopItem("glock", "Glock-18", 200, ShopCategory.SECONDARY, tSide));
        register(new ShopItem("usp", "USP-S", 200, ShopCategory.SECONDARY, ctSide));
        register(new ShopItem("deagle", "Desert Eagle", 700, ShopCategory.SECONDARY));
        register(new ShopItem("p250", "P250", 300, ShopCategory.SECONDARY));
        register(new ShopItem("fiveseven", "Five-SeveN", 500, ShopCategory.SECONDARY, ctSide));
        register(new ShopItem("tec9", "Tec-9", 500, ShopCategory.SECONDARY, tSide));
        register(new ShopItem("ak47", "AK-47", 2700, ShopCategory.PRIMARY, tSide));
        register(new ShopItem("m4a1s", "M4A1-S", 2900, ShopCategory.PRIMARY, ctSide));
        register(new ShopItem("famas", "FAMAS", 2050, ShopCategory.PRIMARY, ctSide));
        register(new ShopItem("galil", "Galil AR", 1800, ShopCategory.PRIMARY, tSide));
        register(new ShopItem("awp", "AWP", 4750, ShopCategory.PRIMARY));
        register(new ShopItem("ssg08", "SSG 08", 1700, ShopCategory.PRIMARY));
        register(new ShopItem("mac10", "MAC-10", 1050, ShopCategory.PRIMARY, tSide));
        register(new ShopItem("mp9", "MP9", 1250, ShopCategory.PRIMARY, ctSide));
        register(new ShopItem("ump45", "UMP-45", 1200, ShopCategory.PRIMARY));
        register(new ShopItem("nova", "Nova", 1050, ShopCategory.PRIMARY));
        register(new ShopItem("xm1014", "XM1014", 2000, ShopCategory.PRIMARY));
        register(new ShopItem("kevlar", "Kevlar", 650, ShopCategory.ARMOR));
        register(new ShopItem("kevlar_helmet", "Kevlar + Helmet", 1000, ShopCategory.ARMOR));
        register(new ShopItem("hegrenade", "HE Grenade", 300, ShopCategory.GRENADE));
        register(new ShopItem("flashbang", "Flashbang", 200, ShopCategory.GRENADE));
        register(new ShopItem("smoke", "Smoke Grenade", 300, ShopCategory.GRENADE));
        register(new ShopItem("molotov", "Molotov", 400, ShopCategory.GRENADE, tSide));
        register(new ShopItem("incgrenade", "Incendiary Grenade", 600, ShopCategory.GRENADE, ctSide));
        register(new ShopItem("decoy", "Decoy", 50, ShopCategory.GRENADE));
        register(new ShopItem("defuse_kit", "Defuse Kit", 400, ShopCategory.UTILITY, ctSide));
    }

    public ShopCatalog(Map<String, ShopItem> items) {
        if (items != null) {
            this.items.putAll(items);
        }
    }

    public Optional<ShopItem> find(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(key.toLowerCase()));
    }

    public Map<String, ShopItem> items() {
        return Collections.unmodifiableMap(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public ShopCatalog copy() {
        return new ShopCatalog(items);
    }

    public void register(ShopItem item) {
        items.put(item.key().toLowerCase(), item);
    }
}
