package top.scfd.mcplugins.csmc.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import top.scfd.mcplugins.csmc.core.player.WeaponInstance;
import top.scfd.mcplugins.csmc.core.weapon.WeaponSpec;
import top.scfd.mcplugins.csmc.core.weapon.WeaponType;

public final class WeaponItemService {
    private final NamespacedKey weaponKey;

    public WeaponItemService(Plugin plugin) {
        this.weaponKey = new NamespacedKey(plugin, "csmc_weapon");
    }

    public ItemStack createWeaponItem(WeaponInstance instance) {
        if (instance == null) {
            return null;
        }
        WeaponSpec spec = instance.spec();
        Material material = materialFor(spec);
        if (material == null) {
            return null;
        }
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(weaponKey, PersistentDataType.STRING, spec.key());
            meta.displayName(buildDisplayName(spec, instance));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void updateWeaponItem(ItemStack stack, WeaponInstance instance) {
        if (stack == null || instance == null) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(weaponKey, PersistentDataType.STRING, instance.spec().key());
        meta.displayName(buildDisplayName(instance.spec(), instance));
        stack.setItemMeta(meta);
    }

    public boolean matches(ItemStack stack, WeaponSpec spec) {
        if (stack == null || spec == null) {
            return false;
        }
        String key = getWeaponKey(stack);
        return key != null && key.equalsIgnoreCase(spec.key());
    }

    public String getWeaponKey(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(weaponKey, PersistentDataType.STRING);
    }

    private Component buildDisplayName(WeaponSpec spec, WeaponInstance instance) {
        String ammo = instance.magazine() + "/" + instance.reserve();
        return Component.text(spec.displayName() + " [" + ammo + "]")
            .color(NamedTextColor.WHITE);
    }

    private Material materialFor(WeaponSpec spec) {
        if (spec == null) {
            return null;
        }
        return switch (spec.type()) {
            case PISTOL -> Material.BOW;
            case RIFLE -> Material.CROSSBOW;
            case SNIPER -> Material.SPYGLASS;
            case KNIFE -> Material.IRON_SWORD;
            case SMG -> Material.CARROT_ON_A_STICK;
            case SHOTGUN -> Material.TRIDENT;
            case LMG -> Material.BLAZE_ROD;
            case GRENADE -> Material.SNOWBALL;
        };
    }
}
