package top.scfd.mcplugins.csmc.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.scfd.mcplugins.csmc.core.shop.ShopItem;

public final class PaperShopService {
    private final GrenadeItemService grenades;

    public PaperShopService(GrenadeItemService grenades) {
        this.grenades = grenades;
    }

    public void grant(Player player, ShopItem item) {
        ItemStack stack = toItemStack(item);
        if (stack == null) {
            return;
        }
        player.getInventory().addItem(stack);
    }

    private ItemStack toItemStack(ShopItem item) {
        Material material = switch (item.key().toLowerCase()) {
            case "glock", "usp", "deagle", "p250", "fiveseven", "tec9" -> Material.BOW;
            case "ak47", "m4a1s", "m4a1", "famas", "galil" -> Material.CROSSBOW;
            case "awp", "ssg08" -> Material.SPYGLASS;
            case "mac10", "mp9", "ump45" -> Material.CARROT_ON_A_STICK;
            case "nova", "xm1014" -> Material.TRIDENT;
            case "kevlar" -> Material.LEATHER_CHESTPLATE;
            case "hegrenade" -> Material.SNOWBALL;
            case "flashbang" -> Material.EGG;
            case "smoke" -> Material.FIREWORK_STAR;
            case "molotov", "incgrenade" -> Material.FIRE_CHARGE;
            case "decoy" -> Material.PAPER;
            case "defuse_kit" -> Material.SHEARS;
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
        if (grenades != null && item.category() == top.scfd.mcplugins.csmc.core.shop.ShopCategory.GRENADE) {
            grenades.tag(stack, item.key());
        }
        return stack;
    }
}
