package top.scfd.mcplugins.csmc.paper.map;

import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.map.BombSite;
import top.scfd.mcplugins.csmc.core.map.BuyZone;
import top.scfd.mcplugins.csmc.core.map.MapSpawn;

public final class MapEditorCommand implements CommandExecutor {
    private final MapEditorService maps;

    public MapEditorCommand(MapEditorService maps) {
        this.maps = maps;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /csmcmap list | /csmcmap create <id> [name] | /csmcmap info <id> | /csmcmap setname <id> <name>");
            sender.sendMessage("       /csmcmap setworld <id> [world] | /csmcmap addspawn <id> <t|ct> | /csmcmap clearspawns <id> <t|ct>");
            sender.sendMessage("       /csmcmap setbomb <id> <A|B> <radius> | /csmcmap removebomb <id> <A|B>");
            sender.sendMessage("       /csmcmap addbuy <id> <t|ct> <radius> | /csmcmap clearbuy <id> <t|ct> | /csmcmap save <id> | /csmcmap reload");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> handleList(sender);
            case "create" -> handleCreate(sender, args);
            case "info" -> handleInfo(sender, args);
            case "setname" -> handleSetName(sender, args);
            case "setworld" -> handleSetWorld(sender, args);
            case "addspawn" -> handleAddSpawn(sender, args);
            case "clearspawns" -> handleClearSpawns(sender, args);
            case "setbomb" -> handleSetBomb(sender, args);
            case "removebomb" -> handleRemoveBomb(sender, args);
            case "addbuy" -> handleAddBuy(sender, args);
            case "clearbuy" -> handleClearBuy(sender, args);
            case "save" -> handleSave(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage("Unknown subcommand.");
                yield true;
            }
        };
    }

    private boolean handleList(CommandSender sender) {
        var ids = maps.listMaps();
        if (ids.isEmpty()) {
            sender.sendMessage("No map files found.");
            return true;
        }
        sender.sendMessage("Maps: " + String.join(", ", ids));
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /csmcmap create <id> [name]");
            return true;
        }
        String name = args.length >= 3 ? joinTail(args, 2) : args[1];
        String world = sender instanceof Player player ? player.getWorld().getName() : "world";
        EditableMap map = maps.create(args[1], name, world);
        if (map == null) {
            sender.sendMessage("Failed to create map (invalid id or already exists).");
            return true;
        }
        sender.sendMessage("Created map draft " + map.id() + " in world " + map.world() + ".");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /csmcmap info <id>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        sender.sendMessage(
            "Map " + map.id() + " (" + map.name() + ") world=" + map.world()
                + " | spawns T/CT=" + map.terroristSpawns().size() + "/" + map.counterTerroristSpawns().size()
                + " | buyzones T/CT=" + map.terroristBuyZones().size() + "/" + map.counterTerroristBuyZones().size()
                + " | bombSites=" + map.bombSites().keySet()
        );
        return true;
    }

    private boolean handleSetName(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /csmcmap setname <id> <name>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        map.setName(joinTail(args, 2));
        sender.sendMessage("Map name updated to: " + map.name());
        return true;
    }

    private boolean handleSetWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /csmcmap setworld <id> [world]");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        String world = args.length >= 3
            ? args[2]
            : (sender instanceof Player player ? player.getWorld().getName() : map.world());
        map.setWorld(world);
        sender.sendMessage("World set to " + world + ".");
        return true;
    }

    private boolean handleAddSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only command.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /csmcmap addspawn <id> <t|ct>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        TeamSide side = parseSide(args[2]);
        if (side == null) {
            sender.sendMessage("Side must be t or ct.");
            return true;
        }
        maps.addSpawn(map, side, spawnAt(player.getLocation()));
        int size = side == TeamSide.TERRORIST ? map.terroristSpawns().size() : map.counterTerroristSpawns().size();
        sender.sendMessage("Added " + side.name() + " spawn #" + size + ".");
        return true;
    }

    private boolean handleClearSpawns(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /csmcmap clearspawns <id> <t|ct>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        TeamSide side = parseSide(args[2]);
        if (side == null) {
            sender.sendMessage("Side must be t or ct.");
            return true;
        }
        int removed = maps.clearSpawns(map, side);
        sender.sendMessage("Removed " + removed + " " + side.name() + " spawns.");
        return true;
    }

    private boolean handleSetBomb(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only command.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /csmcmap setbomb <id> <A|B> <radius>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        String siteId = args[2].toUpperCase(Locale.ROOT);
        double radius = parsePositiveDouble(args[3]);
        if (radius <= 0.0) {
            sender.sendMessage("Radius must be > 0.");
            return true;
        }
        Location location = player.getLocation();
        maps.setBombSite(map, siteId, new BombSite(siteId, location.getX(), location.getY(), location.getZ(), radius));
        sender.sendMessage("Set bomb site " + siteId + " (radius " + radius + ").");
        return true;
    }

    private boolean handleRemoveBomb(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /csmcmap removebomb <id> <A|B>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        String siteId = args[2].toUpperCase(Locale.ROOT);
        boolean removed = maps.removeBombSite(map, siteId);
        sender.sendMessage(removed ? "Removed bomb site " + siteId + "." : "Bomb site " + siteId + " not present.");
        return true;
    }

    private boolean handleAddBuy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only command.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /csmcmap addbuy <id> <t|ct> <radius>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        TeamSide side = parseSide(args[2]);
        if (side == null) {
            sender.sendMessage("Side must be t or ct.");
            return true;
        }
        double radius = parsePositiveDouble(args[3]);
        if (radius <= 0.0) {
            sender.sendMessage("Radius must be > 0.");
            return true;
        }
        Location location = player.getLocation();
        maps.addBuyZone(map, side, new BuyZone(location.getX(), location.getY(), location.getZ(), radius));
        int size = side == TeamSide.TERRORIST ? map.terroristBuyZones().size() : map.counterTerroristBuyZones().size();
        sender.sendMessage("Added " + side.name() + " buy zone #" + size + " (radius " + radius + ").");
        return true;
    }

    private boolean handleClearBuy(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /csmcmap clearbuy <id> <t|ct>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        TeamSide side = parseSide(args[2]);
        if (side == null) {
            sender.sendMessage("Side must be t or ct.");
            return true;
        }
        int removed = maps.clearBuyZones(map, side);
        sender.sendMessage("Removed " + removed + " " + side.name() + " buy zones.");
        return true;
    }

    private boolean handleSave(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /csmcmap save <id>");
            return true;
        }
        EditableMap map = maps.get(args[1]);
        if (map == null) {
            sender.sendMessage("Map not found.");
            return true;
        }
        boolean success = maps.save(map);
        sender.sendMessage(success
            ? "Saved " + map.id() + " and reloaded map registry."
            : "Failed to save map " + map.id() + ".");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        maps.reload();
        sender.sendMessage("Map registry reloaded from disk.");
        return true;
    }

    private TeamSide parseSide(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "t", "terrorist", "terrorists" -> TeamSide.TERRORIST;
            case "ct", "counter_terrorist", "counter-terrorist", "counterterrorist" -> TeamSide.COUNTER_TERRORIST;
            default -> null;
        };
    }

    private MapSpawn spawnAt(Location location) {
        return new MapSpawn(
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }

    private String joinTail(String[] args, int start) {
        StringBuilder text = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                text.append(' ');
            }
            text.append(args[i]);
        }
        return text.toString();
    }

    private double parsePositiveDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return -1.0;
        }
    }
}
