package top.scfd.mcplugins.csmc.paper.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;

public final class SessionTabCompleter implements TabCompleter {
    private static final List<String> ROOT = List.of(
        "create", "maps", "sessions", "rules", "info", "scoreboard", "join", "leave", "start",
        "buy", "view", "stats", "history", "top", "queue", "ac"
    );
    private static final List<String> QUEUE = List.of("join", "leave", "status", "list", "votes", "global");
    private static final List<String> VIEW = List.of("free", "next", "prev");
    private static final List<String> AC = List.of("status", "reset", "top");

    private final SessionRegistry sessions;

    public SessionTabCompleter(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return match(args[0], ROOT);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> completeCreate(args);
            case "rules" -> completeRules(args);
            case "join" -> completeJoin(args);
            case "view" -> completeView(sender, args);
            case "stats" -> completePlayerName(args);
            case "history" -> completeHistory(args);
            case "scoreboard", "board" -> completeScoreboard(args);
            case "queue" -> completeQueue(args);
            case "ac", "anticheat" -> completeAntiCheat(args);
            default -> List.of();
        };
    }

    private List<String> completeCreate(String[] args) {
        if (args.length == 2) {
            return match(args[1], modeNames());
        }
        if (args.length == 3) {
            return match(args[2], mapNames(false));
        }
        return List.of();
    }

    private List<String> completeRules(String[] args) {
        if (args.length == 2) {
            return match(args[1], modeNames());
        }
        return List.of();
    }

    private List<String> completeJoin(String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (GameSession session : sessions.allSessions()) {
            ids.add(session.id().toString());
        }
        return match(args[1], ids);
    }

    private List<String> completeView(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        List<String> options = new ArrayList<>(VIEW);
        if (sender instanceof Player viewer) {
            GameSession session = sessions.findSession(viewer);
            if (session != null) {
                for (UUID playerId : session.players()) {
                    Player target = Bukkit.getPlayer(playerId);
                    if (target != null && target.isOnline() && !target.getUniqueId().equals(viewer.getUniqueId())) {
                        options.add(target.getName());
                    }
                }
            }
        }
        return match(args[1], options);
    }

    private List<String> completePlayerName(String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            names.add(online.getName());
        }
        return match(args[1], names);
    }

    private List<String> completeHistory(String[] args) {
        if (args.length == 2) {
            return completePlayerName(args);
        }
        if (args.length == 3) {
            return match(args[2], List.of("5", "10", "20"));
        }
        return List.of();
    }

    private List<String> completeScoreboard(String[] args) {
        if (args.length == 2) {
            return match(args[1], List.of("5", "10", "20"));
        }
        return List.of();
    }

    private List<String> completeQueue(String[] args) {
        if (args.length == 2) {
            return match(args[1], QUEUE);
        }
        if ("votes".equalsIgnoreCase(args[1])) {
            if (args.length == 3) {
                return match(args[2], modeNames());
            }
            return List.of();
        }
        if ("global".equalsIgnoreCase(args[1])) {
            if (args.length == 3) {
                return match(args[2], modeNames());
            }
            return List.of();
        }
        if (!"join".equalsIgnoreCase(args[1])) {
            return List.of();
        }
        if (args.length == 3) {
            return match(args[2], modeNames());
        }
        if (args.length == 4) {
            return match(args[3], mapNames(true));
        }
        return List.of();
    }

    private List<String> completeAntiCheat(String[] args) {
        if (args.length == 2) {
            return match(args[1], AC);
        }
        if (args.length >= 3 && "top".equalsIgnoreCase(args[1])) {
            return List.of();
        }
        if (args.length == 3) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return match(args[2], names);
        }
        return List.of();
    }

    private List<String> modeNames() {
        List<String> modes = new ArrayList<>();
        for (GameMode mode : GameMode.values()) {
            modes.add(mode.name().toLowerCase(Locale.ROOT));
        }
        return modes;
    }

    private List<String> mapNames(boolean includeAuto) {
        List<String> maps = new ArrayList<>();
        if (includeAuto) {
            maps.add("auto");
        }
        sessions.availableMaps().forEach(map -> maps.add(map.id()));
        return maps;
    }

    private List<String> match(String token, List<String> candidates) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, candidates, matches);
        return matches;
    }
}
