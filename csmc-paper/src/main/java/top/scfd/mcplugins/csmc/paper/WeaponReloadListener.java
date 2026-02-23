package top.scfd.mcplugins.csmc.paper;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.player.WeaponInstance;
import top.scfd.mcplugins.csmc.core.weapon.WeaponSpec;

public final class WeaponReloadListener implements Listener {
    private final SessionRegistry sessions;
    private final WeaponItemService weaponItems;
    private final LoadoutInventoryService loadoutInventory;

    public WeaponReloadListener(SessionRegistry sessions, WeaponItemService weaponItems, LoadoutInventoryService loadoutInventory) {
        this.sessions = sessions;
        this.weaponItems = weaponItems;
        this.loadoutInventory = loadoutInventory;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        var session = sessions.findSession(player);
        if (session == null || session.state() != SessionState.LIVE) {
            return;
        }
        PlayerLoadout loadout = session.loadout(player.getUniqueId());
        if (loadout == null) {
            return;
        }
        WeaponInstance weapon = loadout.activeWeapon();
        if (weapon == null) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        WeaponSpec spec = weapon.spec();
        if (!weaponItems.matches(hand, spec)) {
            return;
        }
        event.setCancelled(true);
        if (weapon.magazine() >= spec.magazineSize() || weapon.reserve() <= 0) {
            return;
        }
        weapon.reload();
        loadoutInventory.updateActiveWeaponItem(player, loadout);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.6f, 1.1f);
    }
}
