package top.scfd.mcplugins.csmc.paper;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.player.WeaponSlot;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class WeaponSelectionListener implements Listener {
    private final SessionRegistry sessions;

    public WeaponSelectionListener(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        GameSession session = sessions.findSession(player);
        if (session == null) {
            return;
        }
        PlayerLoadout loadout = session.loadout(player.getUniqueId());
        if (loadout == null) {
            return;
        }
        WeaponSlot slot = slotFromIndex(event.getNewSlot());
        if (slot != null) {
            loadout.setActiveSlot(slot);
        }
    }

    private WeaponSlot slotFromIndex(int index) {
        return switch (index) {
            case 0 -> WeaponSlot.PRIMARY;
            case 1 -> WeaponSlot.SECONDARY;
            case 2 -> WeaponSlot.MELEE;
            default -> null;
        };
    }
}
