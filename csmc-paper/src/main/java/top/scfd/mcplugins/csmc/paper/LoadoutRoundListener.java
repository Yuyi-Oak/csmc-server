package top.scfd.mcplugins.csmc.paper;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class LoadoutRoundListener implements RoundEventListener {
    private final GameSession session;
    private final LoadoutInventoryService loadoutInventory;

    public LoadoutRoundListener(GameSession session, LoadoutInventoryService loadoutInventory) {
        this.session = session;
        this.loadoutInventory = loadoutInventory;
    }

    @Override
    public void onRoundStart(int roundNumber) {
        for (UUID playerId : session.players()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            PlayerLoadout loadout = session.loadout(playerId);
            if (loadout != null) {
                loadoutInventory.applyWeapons(player, loadout);
            }
        }
    }
}
