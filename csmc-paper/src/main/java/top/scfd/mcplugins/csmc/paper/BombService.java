package top.scfd.mcplugins.csmc.paper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class BombService {
    private final Map<UUID, BombState> bombs = new ConcurrentHashMap<>();

    public BombState state(GameSession session) {
        if (session == null) {
            return null;
        }
        return bombs.get(session.id());
    }

    public boolean isBombPlanted(GameSession session) {
        return state(session) != null;
    }

    public boolean plant(GameSession session, Player player, Location location) {
        if (session == null || player == null || location == null) {
            return false;
        }
        if (bombs.containsKey(session.id())) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        location.getBlock().setType(Material.TNT, false);
        BombState state = new BombState(location, player.getUniqueId());
        bombs.put(session.id(), state);
        session.onBombPlanted(player.getUniqueId());
        return true;
    }

    public boolean startDefuse(GameSession session, Player player) {
        BombState state = state(session);
        if (state == null || player == null) {
            return false;
        }
        state.setDefuserId(player.getUniqueId());
        boolean hasKit = hasDefuseKit(session, player.getUniqueId());
        session.startDefuse(player.getUniqueId(), hasKit);
        return true;
    }

    public void cancelDefuse(GameSession session, UUID playerId) {
        BombState state = state(session);
        if (state == null || playerId == null) {
            return;
        }
        if (state.defuserId() == null || !state.defuserId().equals(playerId)) {
            return;
        }
        state.setDefuserId(null);
        session.cancelDefuse(playerId);
    }

    public void clear(GameSession session) {
        BombState state = bombs.remove(session.id());
        if (state == null) {
            return;
        }
        Location location = state.location();
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (location.getBlock().getType() == Material.TNT) {
            location.getBlock().setType(Material.AIR, false);
        }
    }

    public boolean isBombBlock(GameSession session, Location location) {
        BombState state = state(session);
        if (state == null || location == null) {
            return false;
        }
        Location bomb = state.location();
        if (bomb == null || bomb.getWorld() == null || location.getWorld() == null) {
            return false;
        }
        return bomb.getWorld().equals(location.getWorld())
            && bomb.getBlockX() == location.getBlockX()
            && bomb.getBlockY() == location.getBlockY()
            && bomb.getBlockZ() == location.getBlockZ();
    }

    public void assignBomb(GameSession session) {
        if (session == null) {
            return;
        }
        List<UUID> terrorists = new ArrayList<>();
        for (UUID playerId : session.players()) {
            if (session.getSide(playerId) == TeamSide.TERRORIST) {
                terrorists.add(playerId);
            }
        }
        if (terrorists.isEmpty()) {
            return;
        }
        UUID pick = terrorists.get(ThreadLocalRandom.current().nextInt(terrorists.size()));
        Player player = org.bukkit.Bukkit.getPlayer(pick);
        if (player == null) {
            return;
        }
        if (hasBombItem(player)) {
            return;
        }
        ItemStack bomb = createBombItem();
        player.getInventory().addItem(bomb);
    }

    public boolean hasBombItem(Player player) {
        if (player == null) {
            return false;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (isBombItem(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBombItem(ItemStack item) {
        if (item == null || item.getType() != Material.TNT) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) {
            return true;
        }
        return Component.text("C4").equals(meta.displayName());
    }

    public ItemStack createBombItem() {
        ItemStack stack = new ItemStack(Material.TNT, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("C4").color(NamedTextColor.RED));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean hasDefuseKit(GameSession session, UUID playerId) {
        if (session == null || playerId == null) {
            return false;
        }
        PlayerLoadout loadout = session.loadout(playerId);
        if (loadout == null) {
            return false;
        }
        return loadout.armor().defuseKit();
    }
}
