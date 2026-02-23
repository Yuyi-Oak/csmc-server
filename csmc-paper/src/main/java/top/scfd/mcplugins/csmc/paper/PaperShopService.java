package top.scfd.mcplugins.csmc.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.scfd.mcplugins.csmc.core.shop.ShopItem;

public final class PaperShopService {
    public void grant(Player player, ShopItem item) {
        ItemStack stack = toItemStack(item);
        if (stack == null) {
            return;
        }
        player.getInventory().addItem(stack);
    }

    private ItemStack toItemStack(ShopItem item) {
        Material material = switch (item.key().toLowerCase()) {
            case "glock", "usp" -> Material.BOW;
            case "ak47", "m4a1s" -> Material.CROSSBOW;
            case "awp" -> Material.SPYGLASS;
            case "kevlar" -> Material.LEATHER_CHESTPLATE;
            case "hegrenade" -> Material.SNOWBALL;
            case "flashbang" -> Material.EGG;
            case "smoke" -> Material.FIREWORK_STAR;
            case "molotov" -> Material.FIRE_CHARGE;
            case "decoy" -> Material.PAPER;
            default -> null;
        };
        if (material == null) {
            return null;
        }
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(item.displayName()).color(NamedTextColor.WHITE));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
