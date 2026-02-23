package top.scfd.mcplugins.csmc.core.shop;

public record ShopItem(
    String key,
    String displayName,
    int price,
    ShopCategory category
) {
    public ShopItem(String key, String displayName, int price) {
        this(key, displayName, price, ShopCategory.UNKNOWN);
    }
}
