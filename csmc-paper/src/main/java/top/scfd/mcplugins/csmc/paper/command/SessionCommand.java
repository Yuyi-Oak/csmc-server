package top.scfd.mcplugins.csmc.paper.command;

import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.shop.BuyResult;
import top.scfd.mcplugins.csmc.core.shop.ShopCategory;
import top.scfd.mcplugins.csmc.core.shop.ShopItem;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.paper.LoadoutInventoryService;
import top.scfd.mcplugins.csmc.paper.PaperShopService;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;

public final class SessionCommand implements CommandExecutor {
    private final SessionRegistry sessions;
    private final PaperShopService shopService;
    private final LoadoutInventoryService loadoutInventory;

    public SessionCommand(SessionRegistry sessions, PaperShopService shopService, LoadoutInventoryService loadoutInventory) {
        this.sessions = sessions;
        this.shopService = shopService;
        this.loadoutInventory = loadoutInventory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /csmc create <mode> | /csmc join <id> | /csmc leave | /csmc start | /csmc buy <item>");
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player);
            case "buy" -> handleBuy(player, args);
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
        if (session == null) {
            player.sendMessage("You are not in a session.");
            return true;
        }
        sessions.leaveSession(player);
        player.sendMessage("You left the session.");
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
}
