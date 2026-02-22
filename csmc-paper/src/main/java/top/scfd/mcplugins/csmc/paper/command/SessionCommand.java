package top.scfd.mcplugins.csmc.paper.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;

public final class SessionCommand implements CommandExecutor {
    private final SessionRegistry sessions;

    public SessionCommand(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /csmc create <mode> | /csmc start");
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "start" -> handleStart(player);
            default -> {
                sender.sendMessage("Unknown subcommand.");
                yield true;
            }
        };
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /csmc create <mode>");
            return true;
        }
        GameMode mode;
        try {
            mode = GameMode.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException ex) {
            player.sendMessage("Invalid mode.");
            return true;
        }
        GameSession session = sessions.createSession(mode);
        sessions.assignPlayer(player, session);
        player.sendMessage("Created session " + session.id() + " for " + mode);
        return true;
    }

    private boolean handleStart(Player player) {
        GameSession session = sessions.findSession(player);
        if (session == null) {
            player.sendMessage("You are not in a session.");
            return true;
        }
        session.startRound();
        player.sendMessage("Round started.");
        return true;
    }
}
