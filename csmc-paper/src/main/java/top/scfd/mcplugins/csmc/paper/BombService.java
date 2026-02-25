package top.scfd.mcplugins.csmc.paper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.match.RoundPhase;
import top.scfd.mcplugins.csmc.core.player.PlayerLoadout;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class BombService {
    private final NamespacedKey bombItemKey;
    private final NamespacedKey bombSessionKey;
    private final Map<UUID, BombState> bombs = new ConcurrentHashMap<>();
    private final Map<UUID, PlantState> plants = new ConcurrentHashMap<>();

    public BombService(Plugin plugin) {
        this.bombItemKey = new NamespacedKey(plugin, "csmc_bomb");
        this.bombSessionKey = new NamespacedKey(plugin, "csmc_bomb_session");
    }

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
        plants.remove(session.id());
        session.onBombPlanted(player.getUniqueId());
        return true;
    }

    public boolean startPlant(GameSession session, Player player, Location location, int durationSeconds) {
        if (session == null || player == null || location == null) {
            return false;
        }
        if (bombs.containsKey(session.id())) {
            return false;
        }
        UUID sessionId = session.id();
        UUID planterId = player.getUniqueId();
        PlantState existing = plants.get(sessionId);
        if (existing != null) {
            return existing.planterId().equals(planterId);
        }
        int seconds = Math.max(1, durationSeconds);
        plants.put(sessionId, new PlantState(location, planterId, seconds));
        player.sendActionBar(Component.text("Planting C4... " + seconds + "s").color(NamedTextColor.RED));
        return true;
    }

    public boolean startDefuse(GameSession session, Player player) {
        BombState state = state(session);
        if (state == null || player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        UUID activeDefuser = state.defuserId();
        if (activeDefuser != null && !activeDefuser.equals(playerId)) {
            return false;
        }
        if (session.roundEngine().defuseRemainingSeconds() > 0) {
            return activeDefuser != null && activeDefuser.equals(playerId);
        }
        state.setDefuserId(playerId);
        boolean hasKit = hasDefuseKit(session, playerId);
        session.startDefuse(playerId, hasKit);
        if (session.roundEngine().defuseRemainingSeconds() <= 0) {
            state.setDefuserId(null);
            return false;
        }
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

    public void cancelPlant(GameSession session, UUID playerId) {
        if (session == null || playerId == null) {
            return;
        }
        UUID sessionId = session.id();
        PlantState state = plants.get(sessionId);
        if (state == null || !state.planterId().equals(playerId)) {
            return;
        }
        plants.remove(sessionId);
        Player planter = Bukkit.getPlayer(playerId);
        if (planter != null && planter.isOnline()) {
            planter.sendActionBar(Component.text("Plant cancelled.").color(NamedTextColor.GRAY));
        }
    }

    public boolean isPlantingBy(GameSession session, UUID playerId) {
        if (session == null || playerId == null) {
            return false;
        }
        PlantState state = plants.get(session.id());
        return state != null && state.planterId().equals(playerId);
    }

    public void validatePlanting(GameSession session, Player player) {
        if (session == null || player == null) {
            return;
        }
        PlantState state = plants.get(session.id());
        if (state == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!state.planterId().equals(playerId)) {
            return;
        }
        if (!canContinuePlanting(session, player, state.location())) {
            cancelPlant(session, playerId);
        }
    }

    public void tick(GameSession session) {
        if (session == null) {
            return;
        }
        PlantState state = plants.get(session.id());
        if (state == null) {
            return;
        }
        Player planter = Bukkit.getPlayer(state.planterId());
        if (planter == null || !planter.isOnline()) {
            plants.remove(session.id());
            return;
        }
        if (!canContinuePlanting(session, planter, state.location())) {
            cancelPlant(session, state.planterId());
            return;
        }
        int remaining = state.tick();
        if (remaining > 0) {
            planter.sendActionBar(Component.text("Planting C4... " + remaining + "s").color(NamedTextColor.RED));
            return;
        }
        if (!consumeBombItem(planter, session)) {
            cancelPlant(session, state.planterId());
            return;
        }
        boolean planted = plant(session, planter, state.location());
        if (!planted) {
            cancelPlant(session, state.planterId());
            return;
        }
        planter.sendActionBar(Component.text("C4 planted.").color(NamedTextColor.RED));
    }

    public void clear(GameSession session) {
        if (session == null) {
            return;
        }
        plants.remove(session.id());
        clearDroppedBombItems(session);
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
        stripBombItems(session);
        clearDroppedBombItems(session);
        List<Player> terrorists = new ArrayList<>();
        for (UUID playerId : session.players()) {
            if (session.getSide(playerId) == TeamSide.TERRORIST) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null
                    && player.isOnline()
                    && !player.isDead()
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR
                    && player.getHealth() > 0.0) {
                    terrorists.add(player);
                }
            }
        }
        if (terrorists.isEmpty()) {
            return;
        }
        Player player = terrorists.get(ThreadLocalRandom.current().nextInt(terrorists.size()));
        ItemStack bomb = createBombItem(session);
        player.getInventory().addItem(bomb);
    }

    public boolean dropBombAt(GameSession session, Location location) {
        if (session == null || location == null || location.getWorld() == null) {
            return false;
        }
        if (isBombPlanted(session)) {
            return false;
        }
        clearDroppedBombItems(session);
        var dropped = location.getWorld().dropItemNaturally(location, createBombItem(session));
        dropped.setPickupDelay(10);
        return true;
    }

    public boolean hasBombItem(Player player) {
        return hasBombItem(player, null);
    }

    public boolean hasBombItem(Player player, GameSession session) {
        if (player == null) {
            return false;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (session == null) {
                if (isBombItem(item)) {
                    return true;
                }
                continue;
            }
            if (isBombItemForSession(item, session)) {
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
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(bombItemKey, PersistentDataType.BYTE);
    }

    public UUID resolveBombSessionId(ItemStack item) {
        if (!isBombItem(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(bombSessionKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public boolean isBombItemForSession(ItemStack item, GameSession session) {
        if (session == null || !isBombItem(item)) {
            return false;
        }
        UUID taggedSession = resolveBombSessionId(item);
        return taggedSession != null && session.id().equals(taggedSession);
    }

    public ItemStack createBombItem() {
        ItemStack stack = new ItemStack(Material.TNT, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(bombItemKey, PersistentDataType.BYTE, (byte) 1);
            meta.displayName(Component.text("C4").color(NamedTextColor.RED));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack createBombItem(GameSession session) {
        ItemStack stack = createBombItem();
        if (session == null) {
            return stack;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.getPersistentDataContainer().set(bombSessionKey, PersistentDataType.STRING, session.id().toString());
        stack.setItemMeta(meta);
        return stack;
    }

    public Player findBombCarrier(GameSession session) {
        if (session == null) {
            return null;
        }
        for (UUID playerId : session.players()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (hasBombItem(player, session)) {
                return player;
            }
        }
        return null;
    }

    public int droppedBombCount(GameSession session) {
        if (session == null) {
            return 0;
        }
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (isBombItemForSession(item.getItemStack(), session)) {
                    count++;
                }
            }
        }
        return count;
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

    private boolean canContinuePlanting(GameSession session, Player player, Location plantLocation) {
        if (session.getSide(player.getUniqueId()) != TeamSide.TERRORIST) {
            return false;
        }
        if (!player.isSneaking()) {
            return false;
        }
        if (!hasBombItem(player, session)) {
            return false;
        }
        var phase = session.roundEngine().phase();
        if (phase != RoundPhase.LIVE && phase != RoundPhase.BUY) {
            return false;
        }
        Location playerLocation = player.getLocation();
        if (playerLocation.getWorld() == null || plantLocation.getWorld() == null) {
            return false;
        }
        if (!playerLocation.getWorld().equals(plantLocation.getWorld())) {
            return false;
        }
        if (playerLocation.distanceSquared(plantLocation) > 9.0) {
            return false;
        }
        return plantLocation.getBlock().getType() == Material.AIR;
    }

    private boolean consumeBombItem(Player player, GameSession session) {
        if (player == null) {
            return false;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (session != null) {
                if (!isBombItemForSession(item, session)) {
                    continue;
                }
            } else if (!isBombItem(item)) {
                continue;
            }
            int amount = item.getAmount();
            if (amount <= 1) {
                player.getInventory().setItem(slot, null);
            } else {
                item.setAmount(amount - 1);
            }
            return true;
        }
        return false;
    }

    public void stripBombItems(Player player) {
        if (player == null) {
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (isBombItem(item)) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private void stripBombItems(GameSession session) {
        if (session == null) {
            return;
        }
        for (UUID playerId : session.players()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            stripBombItems(player);
        }
    }

    private void clearDroppedBombItems(GameSession session) {
        if (session == null) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                ItemStack stack = item.getItemStack();
                if (!isBombItem(stack)) {
                    continue;
                }
                UUID taggedSession = resolveBombSessionId(stack);
                if (taggedSession != null && !taggedSession.equals(session.id())) {
                    continue;
                }
                item.remove();
            }
        }
    }

    private static final class PlantState {
        private final Location location;
        private final UUID planterId;
        private int remainingSeconds;

        private PlantState(Location location, UUID planterId, int remainingSeconds) {
            this.location = location;
            this.planterId = planterId;
            this.remainingSeconds = remainingSeconds;
        }

        private Location location() {
            return location;
        }

        private UUID planterId() {
            return planterId;
        }

        private int tick() {
            remainingSeconds = Math.max(0, remainingSeconds - 1);
            return remainingSeconds;
        }
    }
}
