package top.scfd.mcplugins.csmc.paper;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.player.WeaponInstance;
import top.scfd.mcplugins.csmc.core.weapon.WeaponSpec;

public final class WeaponReloadListener implements Listener {
    private final SessionRegistry sessions;
    private final WeaponItemService weaponItems;
    private final LoadoutInventoryService loadoutInventory;
    private final Plugin plugin;

    public WeaponReloadListener(SessionRegistry sessions, WeaponItemService weaponItems, LoadoutInventoryService loadoutInventory, Plugin plugin) {
        this.sessions = sessions;
        this.weaponItems = weaponItems;
        this.loadoutInventory = loadoutInventory;
        this.plugin = plugin;
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
        long tick = player.getWorld().getFullTime();
        if (weapon.state().isReloading(tick)) {
            return;
        }
        if (weapon.magazine() >= spec.magazineSize() || weapon.reserve() <= 0) {
            return;
        }
        long durationTicks = Math.max(1L, Math.round(spec.reloadTime() * 20.0));
        weapon.state().startReload(tick, durationTicks);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.6f, 1.1f);
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            weapon.reload();
            weapon.state().finishReload();
            loadoutInventory.updateActiveWeaponItem(player, loadout);
        }, durationTicks);
    }
}
