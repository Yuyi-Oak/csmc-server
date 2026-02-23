package top.scfd.mcplugins.csmc.core.shop;

public enum ShopCategory {
    PRIMARY,
    SECONDARY,
    MELEE,
    GRENADE,
    ARMOR,
    UTILITY,
    UNKNOWN;

    public static ShopCategory fromConfig(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return ShopCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
