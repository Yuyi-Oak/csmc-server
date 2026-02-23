package top.scfd.mcplugins.csmc.core.shop;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ShopCatalog {
    private final Map<String, ShopItem> items = new LinkedHashMap<>();

    public ShopCatalog() {
        register(new ShopItem("glock", "Glock-18", 200, ShopCategory.SECONDARY));
        register(new ShopItem("usp", "USP-S", 200, ShopCategory.SECONDARY));
        register(new ShopItem("ak47", "AK-47", 2700, ShopCategory.PRIMARY));
        register(new ShopItem("m4a1s", "M4A1-S", 2900, ShopCategory.PRIMARY));
        register(new ShopItem("awp", "AWP", 4750, ShopCategory.PRIMARY));
        register(new ShopItem("kevlar", "Kevlar", 650, ShopCategory.ARMOR));
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
