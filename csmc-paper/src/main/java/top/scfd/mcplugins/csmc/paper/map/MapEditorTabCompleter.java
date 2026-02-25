package top.scfd.mcplugins.csmc.paper.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import top.scfd.mcplugins.csmc.api.TeamSide;

public final class MapEditorTabCompleter implements TabCompleter {
    private static final List<String> ROOT = List.of(
        "list", "create", "clone", "info", "setname", "setworld", "listpoints", "tp",
        "addspawn", "removespawn", "clearspawns",
        "setbomb", "removebomb",
        "addbuy", "removebuy", "clearbuy",
        "validate", "save", "saveall", "reload"
    );
    private static final List<String> SIDES = List.of("t", "ct");
    private static final List<String> BOMB_SITES = List.of("A", "B");
    private static final List<String> TP_TARGETS = List.of("spawn", "buy", "bomb");

    private final MapEditorService maps;

    public MapEditorTabCompleter(MapEditorService maps) {
        this.maps = maps;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return match(args[0], ROOT);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("clone".equals(sub)) {
            if (args.length == 2) {
                return match(args[1], maps.listMaps());
            }
            return List.of();
        }
        return switch (sub) {
            case "info", "setname", "setworld", "listpoints", "addspawn", "removespawn", "clearspawns",
                "setbomb", "removebomb", "addbuy", "removebuy", "clearbuy", "validate", "save", "tp" -> completeMapScoped(sub, args);
            default -> List.of();
        };
    }

    private List<String> completeMapScoped(String sub, String[] args) {
        if (args.length == 2) {
            return match(args[1], maps.listMaps());
        }
        if (args.length == 3) {
            return switch (sub) {
                case "addspawn", "removespawn", "clearspawns", "addbuy", "removebuy", "clearbuy" -> match(args[2], SIDES);
                case "setbomb", "removebomb" -> match(args[2], BOMB_SITES);
                case "tp" -> match(args[2], TP_TARGETS);
                case "setworld" -> {
                    List<String> worlds = new ArrayList<>();
                    Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
                    yield match(args[2], worlds);
                }
                default -> List.of();
            };
        }
        if (args.length == 4) {
            return switch (sub) {
                case "removespawn" -> completeSpawnIndex(args);
                case "removebuy" -> completeBuyIndex(args);
                case "setbomb" -> match(args[3], List.of("3.0", "4.5", "5.0"));
                case "addbuy" -> match(args[3], List.of("4.0", "6.0", "8.0"));
                case "tp" -> completeTeleportType(args);
                default -> List.of();
            };
        }
        if (args.length == 5 && "tp".equals(sub)) {
            return completeTeleportIndex(args);
        }
        if (args.length == 3 && "save".equals(sub)) {
            return match(args[2], List.of("force"));
        }
        if (args.length == 2 && "saveall".equals(sub)) {
            return match(args[1], List.of("force"));
        }
        return List.of();
    }

    private List<String> completeSpawnIndex(String[] args) {
        EditableMap map = maps.get(args[1]);
        TeamSide side = parseSide(args[2]);
        if (map == null || side == null) {
            return List.of();
        }
        int size = side == TeamSide.TERRORIST
            ? map.terroristSpawns().size()
            : map.counterTerroristSpawns().size();
        return match(args[3], indexes(size));
    }

    private List<String> completeBuyIndex(String[] args) {
        EditableMap map = maps.get(args[1]);
        TeamSide side = parseSide(args[2]);
        if (map == null || side == null) {
            return List.of();
        }
        int size = side == TeamSide.TERRORIST
            ? map.terroristBuyZones().size()
            : map.counterTerroristBuyZones().size();
        return match(args[3], indexes(size));
    }

    private List<String> completeTeleportType(String[] args) {
        String target = args[2].toLowerCase(Locale.ROOT);
        return switch (target) {
            case "spawn", "buy" -> match(args[3], SIDES);
            case "bomb" -> match(args[3], BOMB_SITES);
            default -> List.of();
        };
    }

    private List<String> completeTeleportIndex(String[] args) {
        String target = args[2].toLowerCase(Locale.ROOT);
        if (!"spawn".equals(target) && !"buy".equals(target)) {
            return List.of();
        }
        EditableMap map = maps.get(args[1]);
        TeamSide side = parseSide(args[3]);
        if (map == null || side == null) {
            return List.of();
        }
        int size;
        if ("spawn".equals(target)) {
            size = side == TeamSide.TERRORIST
                ? map.terroristSpawns().size()
                : map.counterTerroristSpawns().size();
        } else {
            size = side == TeamSide.TERRORIST
                ? map.terroristBuyZones().size()
                : map.counterTerroristBuyZones().size();
        }
        return match(args[4], indexes(size));
    }

    private List<String> indexes(int size) {
        if (size <= 0) {
            return List.of("1");
        }
        List<String> values = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            values.add(Integer.toString(i));
        }
        return values;
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

    private List<String> match(String token, List<String> candidates) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, candidates, matches);
        return matches;
    }
}
