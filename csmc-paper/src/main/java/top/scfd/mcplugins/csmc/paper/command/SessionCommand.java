package top.scfd.mcplugins.csmc.paper.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import top.scfd.mcplugins.csmc.paper.history.MatchHistoryEntry;
import top.scfd.mcplugins.csmc.paper.history.MatchHistoryService;
import top.scfd.mcplugins.csmc.storage.LeaderboardEntry;
import top.scfd.mcplugins.csmc.storage.PlayerStats;

public final class SessionCommand implements CommandExecutor {
    private static final int DEFAULT_HISTORY_LIMIT = 5;
    private static final int MAX_HISTORY_LIMIT = 20;
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());

    private final SessionRegistry sessions;
    private final PaperShopService shopService;
    private final LoadoutInventoryService loadoutInventory;
    private final StatsService stats;
    private final MatchQueueService queue;
    private final MatchHistoryService history;

    public SessionCommand(
        SessionRegistry sessions,
        PaperShopService shopService,
        LoadoutInventoryService loadoutInventory,
        StatsService stats,
        MatchQueueService queue,
        MatchHistoryService history
    ) {
        this.sessions = sessions;
        this.shopService = shopService;
        this.loadoutInventory = loadoutInventory;
        this.stats = stats;
        this.queue = queue;
        this.history = history;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /csmc create <mode> [mapId] | /csmc maps | /csmc info | /csmc join <id> | /csmc leave | /csmc start | /csmc buy <item> | /csmc view <free|player> | /csmc stats [player] | /csmc history [player|uuid] [limit] | /csmc top | /csmc queue <join|leave|status|list> [mode] [mapId]");
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "maps" -> handleMaps(player);
            case "info" -> handleInfo(player);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player);
            case "buy" -> handleBuy(player, args);
            case "view" -> handleView(player, args);
            case "stats" -> handleStats(player, args);
            case "history" -> handleHistory(player, args);
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
            player.sendMessage("Usage: /csmc create <mode> [mapId]");
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
        String requestedMap = args.length >= 3 ? normalizeAutoMap(args[2]) : null;
        if (requestedMap != null && !sessions.hasMap(requestedMap)) {
            player.sendMessage("Unknown map: " + requestedMap);
            player.sendMessage("Use /csmc maps to list available maps.");
            return true;
        }
        GameSession session = sessions.createSession(mode, requestedMap);
        TeamSide side = sessions.joinSession(player, session);
        var map = sessions.mapForSession(session);
        player.sendMessage(
            "Created session " + session.id() + " for " + mode + " as " + side
                + (map == null ? "" : " on " + map.id())
        );
        if (side != TeamSide.SPECTATOR) {
            loadoutInventory.applyWeapons(player, session.loadout(player.getUniqueId()));
        }
        return true;
    }

    private boolean handleMaps(Player player) {
        var maps = sessions.availableMaps();
        if (maps.isEmpty()) {
            player.sendMessage("No maps loaded.");
            return true;
        }
        StringBuilder builder = new StringBuilder("Maps: ");
        for (int index = 0; index < maps.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(maps.get(index).id());
        }
        player.sendMessage(builder.toString());
        return true;
    }

    private boolean handleInfo(Player player) {
        GameSession session = sessions.findSession(player);
        if (session == null) {
            player.sendMessage("You are not in a session.");
            return true;
        }
        int t = 0;
        int ct = 0;
        for (UUID playerId : session.players()) {
            TeamSide side = session.getSide(playerId);
            if (side == TeamSide.TERRORIST) {
                t++;
            } else if (side == TeamSide.COUNTER_TERRORIST) {
                ct++;
            }
        }
        var map = sessions.mapForSession(session);
        var round = session.roundEngine();
        var score = round.matchState().score();
        player.sendMessage("Session " + session.id()
            + " | mode=" + session.mode()
            + " | state=" + session.state()
            + " | map=" + (map == null ? "none" : map.id()));
        player.sendMessage("Round " + round.matchState().roundNumber()
            + " | phase=" + round.phase()
            + " | score T/CT=" + score.terrorist() + "/" + score.counterTerrorist()
            + " | players T/CT=" + t + "/" + ct);
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

    private boolean handleView(Player viewer, String[] args) {
        GameSession session = sessions.findSession(viewer);
        if (session == null) {
            viewer.sendMessage("You are not in a session.");
            return true;
        }
        if (viewer.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            viewer.sendMessage("You must be in spectator mode.");
            return true;
        }
        if (args.length < 2) {
            viewer.sendMessage("Usage: /csmc view <free|player>");
            return true;
        }
        String targetText = args[1];
        if ("free".equalsIgnoreCase(targetText) || "self".equalsIgnoreCase(targetText)) {
            viewer.setSpectatorTarget(null);
            viewer.sendMessage("Now in free spectator camera.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(targetText);
        if (target == null || !target.isOnline()) {
            viewer.sendMessage("Player not found.");
            return true;
        }
        if (target.getUniqueId().equals(viewer.getUniqueId())) {
            viewer.sendMessage("Use /csmc view free to leave player camera.");
            return true;
        }
        GameSession targetSession = sessions.findSession(target);
        if (targetSession == null || !targetSession.id().equals(session.id())) {
            viewer.sendMessage("Target is not in your session.");
            return true;
        }
        if (target.getGameMode() == org.bukkit.GameMode.SPECTATOR || target.isDead()) {
            viewer.sendMessage("Target is not currently alive.");
            return true;
        }
        viewer.setSpectatorTarget(target);
        viewer.sendMessage("Now spectating " + target.getName() + ".");
        return true;
    }

    private boolean handleStats(Player sender, String[] args) {
        UUID targetId = sender.getUniqueId();
        String targetName = sender.getName();
        if (args.length >= 2) {
            UUID parsedId = parseUuid(args[1]);
            if (parsedId != null) {
                targetId = parsedId;
                OfflinePlayer offline = Bukkit.getOfflinePlayer(parsedId);
                targetName = resolveName(offline, parsedId);
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                if (offline.getUniqueId() == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                if (!offline.isOnline() && !offline.hasPlayedBefore()) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                targetId = offline.getUniqueId();
                targetName = resolveName(offline, targetId);
            }
        }
        PlayerStats playerStats = stats.get(targetId);
        double kd = playerStats.deaths() == 0
            ? playerStats.kills()
            : (double) playerStats.kills() / playerStats.deaths();
        sender.sendMessage("Stats - " + targetName
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

    private boolean handleHistory(Player sender, String[] args) {
        if (history == null) {
            sender.sendMessage("Match history is unavailable.");
            return true;
        }
        UUID targetId = sender.getUniqueId();
        String targetName = sender.getName();
        int limit = DEFAULT_HISTORY_LIMIT;
        if (args.length >= 2) {
            UUID parsedId = parseUuid(args[1]);
            if (parsedId != null) {
                targetId = parsedId;
                OfflinePlayer offline = Bukkit.getOfflinePlayer(parsedId);
                targetName = resolveName(offline, parsedId);
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                if (offline.getUniqueId() == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                if (!offline.isOnline() && !offline.hasPlayedBefore()) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                targetId = offline.getUniqueId();
                targetName = resolveName(offline, targetId);
            }
        }
        if (args.length >= 3) {
            Integer parsed = parsePositiveInt(args[2]);
            if (parsed == null) {
                sender.sendMessage("Invalid limit. Use an integer between 1 and " + MAX_HISTORY_LIMIT + ".");
                return true;
            }
            limit = Math.min(MAX_HISTORY_LIMIT, parsed);
        }
        List<MatchHistoryEntry> matches = history.findByPlayer(targetId, limit);
        if (matches.isEmpty()) {
            sender.sendMessage("No match history found for " + targetName + ".");
            return true;
        }
        sender.sendMessage("Recent matches - " + targetName + " (showing " + matches.size() + "):");
        int index = 1;
        for (MatchHistoryEntry entry : matches) {
            String endedAt = HISTORY_TIME_FORMAT.format(Instant.ofEpochSecond(Math.max(0L, entry.endedAtEpochSeconds())));
            sender.sendMessage(
                index + ". " + endedAt
                    + " | " + entry.mode()
                    + " | " + entry.mapId()
                    + " | W:" + winnerLabel(entry.winner())
                    + " | T/CT " + entry.terroristScore() + "/" + entry.counterTerroristScore()
            );
            index++;
        }
        return true;
    }

    private boolean handleQueue(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /csmc queue <join|leave|status|list> [mode] [mapId]");
            return true;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "join" -> handleQueueJoin(player, args);
            case "leave" -> {
                boolean removed = queue.leave(player.getUniqueId());
                player.sendMessage(removed ? "You left the queue." : "You are not queued.");
                yield true;
            }
            case "list" -> handleQueueList(player);
            case "status" -> {
                GameMode mode = queue.queuedMode(player.getUniqueId());
                if (mode == null) {
                    player.sendMessage("You are not queued.");
                } else {
                    int size = queue.queueSize(mode);
                    int position = queue.queuePosition(player.getUniqueId());
                    String map = queue.queuedMap(player.getUniqueId());
                    String mapText = map == null ? "auto" : map;
                    player.sendMessage("Queue " + mode + " (" + mapText + ") | position " + position + " / " + size);
                }
                yield true;
            }
            default -> {
                player.sendMessage("Usage: /csmc queue <join|leave|status|list> [mode] [mapId]");
                yield true;
            }
        };
    }

    private boolean handleQueueJoin(Player player, String[] args) {
        GameMode mode = GameMode.COMPETITIVE;
        String mapId = null;
        if (args.length >= 3) {
            try {
                mode = GameMode.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                player.sendMessage("Invalid mode.");
                return true;
            }
        }
        if (args.length >= 4) {
            mapId = normalizeAutoMap(args[3]);
            if (mapId != null && !sessions.hasMap(mapId)) {
                player.sendMessage("Unknown map: " + mapId);
                player.sendMessage("Use /csmc maps to list available maps.");
                return true;
            }
        }
        MatchQueueService.JoinResult result = queue.join(player.getUniqueId(), mode, mapId);
        String mapText = mapId == null || mapId.isBlank() ? "auto" : mapId.toLowerCase(Locale.ROOT);
        switch (result) {
            case QUEUED -> player.sendMessage("Queued for " + mode + " (" + mapText + ").");
            case MOVED -> player.sendMessage("Queue switched to " + mode + " (" + mapText + ").");
            case ALREADY_IN_QUEUE -> player.sendMessage("Already queued for " + mode + " (" + mapText + ").");
            case ALREADY_IN_SESSION -> player.sendMessage("You are already in a session.");
        }
        if (result == MatchQueueService.JoinResult.QUEUED || result == MatchQueueService.JoinResult.MOVED) {
            int size = queue.queueSize(mode);
            int position = queue.queuePosition(player.getUniqueId());
            player.sendMessage("Queue " + mode + " (" + mapText + ") | position " + position + " / " + size);
        }
        return true;
    }

    private boolean handleQueueList(Player player) {
        var sizes = queue.queueSizes();
        StringBuilder builder = new StringBuilder("Queue sizes: ");
        int shown = 0;
        for (GameMode mode : GameMode.values()) {
            int size = sizes.getOrDefault(mode, 0);
            if (shown > 0) {
                builder.append(" | ");
            }
            builder.append(mode.name()).append("=").append(size);
            shown++;
        }
        player.sendMessage(builder.toString());
        return true;
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolveName(OfflinePlayer offline, UUID fallbackId) {
        if (offline != null && offline.getName() != null && !offline.getName().isBlank()) {
            return offline.getName();
        }
        return fallbackId.toString();
    }

    private String winnerLabel(String winner) {
        if (winner == null) {
            return "NONE";
        }
        return switch (winner.toUpperCase(Locale.ROOT)) {
            case "TERRORIST" -> "T";
            case "COUNTER_TERRORIST" -> "CT";
            default -> winner.toUpperCase(Locale.ROOT);
        };
    }

    private String normalizeAutoMap(String mapId) {
        if (mapId == null) {
            return null;
        }
        String normalized = mapId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || "auto".equals(normalized)) {
            return null;
        }
        return normalized;
    }
}
