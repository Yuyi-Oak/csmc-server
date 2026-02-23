package top.scfd.mcplugins.csmc.paper;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.player.WeaponInstance;
import top.scfd.mcplugins.csmc.core.player.WeaponSlot;

public final class LoadoutInventoryService {
    private final WeaponItemService weaponItems;

    public LoadoutInventoryService(WeaponItemService weaponItems) {
        this.weaponItems = weaponItems;
    }

    public void applyWeapons(Player player, PlayerLoadout loadout) {
        if (player == null || loadout == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(0, toItem(loadout.primary()));
        inventory.setItem(1, toItem(loadout.secondary()));
        inventory.setItem(2, toItem(loadout.melee()));
        clearArmor(inventory);
        int slot = slotFor(loadout.activeSlot());
        if (slot >= 0) {
            inventory.setHeldItemSlot(slot);
        }
    }

    public void updateActiveWeaponItem(Player player, PlayerLoadout loadout) {
        if (player == null || loadout == null) {
            return;
        }
        WeaponInstance instance = loadout.activeWeapon();
        if (instance == null) {
            return;
        }
        int slot = slotFor(loadout.activeSlot());
        if (slot < 0) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        weaponItems.updateWeaponItem(stack, instance);
    }

    private ItemStack toItem(WeaponInstance instance) {
        if (instance == null) {
            return null;
        }
        return weaponItems.createWeaponItem(instance);
    }

    private int slotFor(WeaponSlot slot) {
        if (slot == null) {
            return -1;
        }
        return switch (slot) {
            case PRIMARY -> 0;
            case SECONDARY -> 1;
            case MELEE -> 2;
            default -> -1;
        };
    }

    private void clearArmor(PlayerInventory inventory) {
        inventory.setHelmet(null);
        inventory.setChestplate(null);
        inventory.setLeggings(null);
        inventory.setBoots(null);
        inventory.setItemInOffHand(null);
    }
}
