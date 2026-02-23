package top.scfd.mcplugins.csmc.paper.command;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.shop.BuyResult;
import top.scfd.mcplugins.csmc.core.shop.ShopCategory;
import top.scfd.mcplugins.csmc.core.shop.ShopItem;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.paper.LoadoutInventoryService;
import top.scfd.mcplugins.csmc.paper.MatchQueueService;
import top.scfd.mcplugins.csmc.paper.PaperShopService;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;
import top.scfd.mcplugins.csmc.paper.StatsService;
import top.scfd.mcplugins.csmc.storage.LeaderboardEntry;
import top.scfd.mcplugins.csmc.storage.PlayerStats;

public final class SessionCommand implements CommandExecutor {
    private final SessionRegistry sessions;
    private final PaperShopService shopService;
    private final LoadoutInventoryService loadoutInventory;
    private final StatsService stats;
    private final MatchQueueService queue;

    public SessionCommand(
        SessionRegistry sessions,
        PaperShopService shopService,
        LoadoutInventoryService loadoutInventory,
        StatsService stats,
        MatchQueueService queue
    ) {
        this.sessions = sessions;
        this.shopService = shopService;
        this.loadoutInventory = loadoutInventory;
        this.stats = stats;
        this.queue = queue;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /csmc create <mode> | /csmc join <id> | /csmc leave | /csmc start | /csmc buy <item> | /csmc stats [player] | /csmc top | /csmc queue <join|leave|status> [mode]");
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player);
            case "buy" -> handleBuy(player, args);
            case "stats" -> handleStats(player, args);
            case "top" -> handleTop(player);
            case "queue" -> handleQueue(player, args);
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
        queue.leave(player.getUniqueId());
        GameSession session = sessions.createSession(mode);
        TeamSide side = sessions.joinSession(player, session);
        player.sendMessage("Created session " + session.id() + " for " + mode + " as " + side);
        if (side != TeamSide.SPECTATOR) {
            loadoutInventory.applyWeapons(player, session.loadout(player.getUniqueId()));
        }
        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /csmc join <sessionId>");
            return true;
        }
        UUID sessionId;
        try {
            sessionId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException ex) {
            player.sendMessage("Invalid session id.");
            return true;
        }
        GameSession session = sessions.getSession(sessionId);
        if (session == null) {
            player.sendMessage("Session not found.");
            return true;
        }
        queue.leave(player.getUniqueId());
        TeamSide side = sessions.joinSession(player, session);
        if (side == TeamSide.SPECTATOR) {
            player.sendMessage("Session is full.");
            return true;
        }
        player.sendMessage("Joined session " + session.id() + " as " + side);
        loadoutInventory.applyWeapons(player, session.loadout(player.getUniqueId()));
        return true;
    }

    private boolean handleLeave(Player player) {
        GameSession session = sessions.findSession(player);
        boolean leftQueue = queue.leave(player.getUniqueId());
        if (session == null && !leftQueue) {
            player.sendMessage("You are not in a session or queue.");
            return true;
        }
        if (session != null) {
            sessions.leaveSession(player);
            player.sendMessage("You left the session.");
        }
        if (leftQueue) {
            player.sendMessage("You left the queue.");
        }
        return true;
    }

    private boolean handleStart(Player player) {
        GameSession session = sessions.findSession(player);
        if (session == null) {
            player.sendMessage("You are not in a session.");
            return true;
        }
        SessionState before = session.state();
        session.startRound();
        if (session.state() == SessionState.LIVE && before != SessionState.LIVE) {
            player.sendMessage("Round started.");
        } else if (before == SessionState.LIVE) {
            player.sendMessage("Round is already live.");
        } else {
            player.sendMessage("Cannot start round yet. Both teams need at least one player.");
        }
        return true;
    }

    private boolean handleBuy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /csmc buy <item>");
            return true;
        }
        GameSession session = sessions.findSession(player);
        if (session == null) {
            player.sendMessage("You are not in a session.");
            return true;
        }
        if (!sessions.isInBuyZone(player, session)) {
            player.sendMessage("You are not in a buy zone.");
            return true;
        }
        ShopItem item = session.findItem(args[1]);
        if (item == null) {
            player.sendMessage("Unknown item.");
            return true;
        }
        BuyResult result = session.buy(player.getUniqueId(), args[1]);
        switch (result) {
            case SUCCESS -> {
                ShopCategory category = item.category();
                if (category == null) {
                    category = ShopCategory.UNKNOWN;
                }
                switch (category) {
                    case PRIMARY, SECONDARY, MELEE -> loadoutInventory.applyWeapons(player, session.loadout(player.getUniqueId()));
                    default -> shopService.grant(player, item);
                }
                player.sendMessage("Purchase successful.");
            }
            case BUY_TIME_OVER -> player.sendMessage("Buy time is over.");
            case INSUFFICIENT_FUNDS -> player.sendMessage("Not enough money.");
            case UNKNOWN_ITEM -> player.sendMessage("Unknown item.");
            case NOT_IN_SESSION -> player.sendMessage("You are not in a session.");
            case INVENTORY_FULL -> player.sendMessage("Inventory full.");
        }
        return true;
    }

    private boolean handleStats(Player sender, String[] args) {
        Player target = sender;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found (must be online).");
                return true;
            }
        }
        PlayerStats playerStats = stats.get(target.getUniqueId());
        double kd = playerStats.deaths() == 0
            ? playerStats.kills()
            : (double) playerStats.kills() / playerStats.deaths();
        sender.sendMessage("Stats - " + target.getName()
            + " | K:" + playerStats.kills()
            + " D:" + playerStats.deaths()
            + " A:" + playerStats.assists()
            + " HS:" + playerStats.headshots()
            + " R:" + playerStats.roundsWon() + "/" + playerStats.roundsPlayed()
            + " KD:" + String.format(Locale.ROOT, "%.2f", kd));
        return true;
    }

    private boolean handleTop(Player sender) {
        List<LeaderboardEntry> ranking = stats.topByKills(10);
        if (ranking.isEmpty()) {
            sender.sendMessage("No leaderboard data found.");
            return true;
        }
        sender.sendMessage("Top 10 (storage, by kills):");
        int position = 1;
        for (LeaderboardEntry entry : ranking) {
            String name = Bukkit.getOfflinePlayer(entry.playerId()).getName();
            if (name == null || name.isBlank()) {
                name = entry.playerId().toString();
            }
            PlayerStats playerStats = entry.stats();
            sender.sendMessage(
                position + ". " + name
                    + " K:" + playerStats.kills()
                    + " W:" + playerStats.roundsWon()
                    + " R:" + playerStats.roundsPlayed()
            );
            position++;
        }
        return true;
    }

    private boolean handleQueue(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /csmc queue <join|leave|status> [mode]");
            return true;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "join" -> handleQueueJoin(player, args);
            case "leave" -> {
                boolean removed = queue.leave(player.getUniqueId());
                player.sendMessage(removed ? "You left the queue." : "You are not queued.");
                yield true;
            }
            case "status" -> {
                GameMode mode = queue.queuedMode(player.getUniqueId());
                if (mode == null) {
                    player.sendMessage("You are not queued.");
                } else {
                    int size = queue.queueSize(mode);
                    int position = queue.queuePosition(player.getUniqueId());
                    player.sendMessage("Queue " + mode + " | position " + position + " / " + size);
                }
                yield true;
            }
            default -> {
                player.sendMessage("Usage: /csmc queue <join|leave|status> [mode]");
                yield true;
            }
        };
    }

    private boolean handleQueueJoin(Player player, String[] args) {
        GameMode mode = GameMode.COMPETITIVE;
        if (args.length >= 3) {
            try {
                mode = GameMode.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                player.sendMessage("Invalid mode.");
                return true;
            }
        }
        MatchQueueService.JoinResult result = queue.join(player.getUniqueId(), mode);
        switch (result) {
            case QUEUED -> player.sendMessage("Queued for " + mode + ".");
            case MOVED -> player.sendMessage("Queue switched to " + mode + ".");
            case ALREADY_IN_QUEUE -> player.sendMessage("Already queued for " + mode + ".");
            case ALREADY_IN_SESSION -> player.sendMessage("You are already in a session.");
        }
        if (result == MatchQueueService.JoinResult.QUEUED || result == MatchQueueService.JoinResult.MOVED) {
            int size = queue.queueSize(mode);
            int position = queue.queuePosition(player.getUniqueId());
            player.sendMessage("Queue " + mode + " | position " + position + " / " + size);
        }
        return true;
    }
}
