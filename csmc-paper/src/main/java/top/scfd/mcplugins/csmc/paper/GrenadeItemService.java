package top.scfd.mcplugins.csmc.paper;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class GrenadeItemService {
    private final NamespacedKey grenadeKey;

    public GrenadeItemService(Plugin plugin) {
        this.grenadeKey = new NamespacedKey(plugin, "csmc_grenade");
    }

    public void tag(ItemStack stack, String key) {
        if (stack == null || key == null) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(grenadeKey, PersistentDataType.STRING, key.toLowerCase());
        stack.setItemMeta(meta);
    }

    public String getGrenadeKey(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(grenadeKey, PersistentDataType.STRING);
    }

    public NamespacedKey grenadeKey() {
        return grenadeKey;
    }
}
