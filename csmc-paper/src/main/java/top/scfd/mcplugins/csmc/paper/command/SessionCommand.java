package top.scfd.mcplugins.csmc.paper.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.GameMode;
import top.scfd.mcplugins.csmc.api.SessionState;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.rules.ModeRules;
import top.scfd.mcplugins.csmc.core.shop.BuyResult;
import top.scfd.mcplugins.csmc.core.shop.ShopCategory;
import top.scfd.mcplugins.csmc.core.shop.ShopItem;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.paper.LoadoutInventoryService;
import top.scfd.mcplugins.csmc.paper.MatchCombatTrackerService;
import top.scfd.mcplugins.csmc.paper.MatchQueueService;
import top.scfd.mcplugins.csmc.paper.PaperShopService;
import top.scfd.mcplugins.csmc.paper.PlayerCombatSnapshot;
import top.scfd.mcplugins.csmc.paper.RemoteQueueSourceStatus;
import top.scfd.mcplugins.csmc.paper.SessionRegistry;
import top.scfd.mcplugins.csmc.paper.StatsService;
import top.scfd.mcplugins.csmc.paper.history.MatchHistoryEntry;
import top.scfd.mcplugins.csmc.paper.history.MatchHistoryService;
import top.scfd.mcplugins.csmc.paper.security.AntiCheatService;
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
    private final MatchCombatTrackerService combatTracker;
    private final MatchQueueService queue;
    private final MatchHistoryService history;
    private final AntiCheatService antiCheat;

    public SessionCommand(
        SessionRegistry sessions,
        PaperShopService shopService,
        LoadoutInventoryService loadoutInventory,
        StatsService stats,
        MatchCombatTrackerService combatTracker,
        MatchQueueService queue,
        MatchHistoryService history,
        AntiCheatService antiCheat
    ) {
        this.sessions = sessions;
        this.shopService = shopService;
        this.loadoutInventory = loadoutInventory;
        this.stats = stats;
        this.combatTracker = combatTracker;
        this.queue = queue;
        this.history = history;
        this.antiCheat = antiCheat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /csmc create <mode> [mapId] | /csmc maps | /csmc sessions | /csmc rules [mode] | /csmc info | /csmc scoreboard [limit] | /csmc join <id> | /csmc leave | /csmc start | /csmc buy <item> | /csmc view <free|next|prev|player> | /csmc stats [player] | /csmc history [player|uuid] [limit] | /csmc top | /csmc queue <join|leave|status|list|votes|global> [mode|detail] [mapId] | /csmc ac <status|reset|top> [player|limit]");
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "maps" -> handleMaps(player);
            case "sessions" -> handleSessions(player);
            case "rules" -> handleRules(player, args);
            case "info" -> handleInfo(player);
            case "scoreboard", "board" -> handleScoreboard(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player);
            case "buy" -> handleBuy(player, args);
            case "view" -> handleView(player, args);
            case "stats" -> handleStats(player, args);
            case "history" -> handleHistory(player, args);
            case "top" -> handleTop(player);
            case "queue" -> handleQueue(player, args);
            case "ac", "anticheat" -> handleAntiCheat(player, args);
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

    private boolean handleSessions(Player player) {
        List<GameSession> active = sessions.allSessions();
        if (active.isEmpty()) {
            player.sendMessage("No active sessions.");
            return true;
        }
        player.sendMessage("Active sessions: " + active.size());
        for (GameSession session : active) {
            int t = 0;
            int ct = 0;
            int spec = 0;
            for (UUID playerId : session.players()) {
                TeamSide side = session.getSide(playerId);
                if (side == TeamSide.TERRORIST) {
                    t++;
                } else if (side == TeamSide.COUNTER_TERRORIST) {
                    ct++;
                } else {
                    spec++;
                }
            }
            var map = sessions.mapForSession(session);
            player.sendMessage(
                session.id() + " | "
                    + session.mode() + " | "
                    + session.state() + " | "
                    + (map == null ? "none" : map.id()) + " | "
                    + "T/CT/S=" + t + "/" + ct + "/" + spec
            );
        }
        return true;
    }

    private boolean handleRules(Player player, String[] args) {
        GameMode mode;
        if (args.length >= 2) {
            try {
                mode = GameMode.valueOf(args[1].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                player.sendMessage("Invalid mode.");
                return true;
            }
        } else {
            GameSession session = sessions.findSession(player);
            mode = session == null ? GameMode.COMPETITIVE : session.mode();
        }
        ModeRules rules = sessions.modeRules(mode);
        if (rules == null) {
            player.sendMessage("Rules not found for mode: " + mode);
            return true;
        }
        var durations = rules.durations();
        var economy = rules.economy();
        player.sendMessage("Rules for " + mode + ":");
        player.sendMessage("Players=" + rules.maxPlayers()
            + " | roundsToWin=" + rules.roundsToWin()
            + " | maxRounds=" + rules.maxRounds()
            + " | friendlyFire=" + rules.friendlyFireEnabled());
        player.sendMessage("Overtime=" + rules.overtimeEnabled()
            + " | otRoundsToWin=" + rules.overtimeRoundsToWin()
            + " | otMaxRounds=" + rules.overtimeMaxRounds());
        player.sendMessage("Durations freeze/buy/round/bomb/defuse/plant="
            + durations.freezeTimeSeconds() + "/"
            + durations.buyTimeSeconds() + "/"
            + durations.roundTimeSeconds() + "/"
            + durations.bombTimerSeconds() + "/"
            + durations.defuseTimeSeconds() + "/"
            + durations.plantTimeSeconds());
        player.sendMessage("Economy start/max/kill/assist/win/lossBase/lossInc/lossMax="
            + economy.startMoney() + "/"
            + economy.maxMoney() + "/"
            + economy.killReward() + "/"
            + economy.assistReward() + "/"
            + economy.roundWinReward() + "/"
            + economy.roundLossBase() + "/"
            + economy.lossBonusIncrement() + "/"
            + economy.lossBonusMax());
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

    private boolean handleScoreboard(Player player, String[] args) {
        GameSession session = sessions.findSession(player);
        if (session == null) {
            player.sendMessage("You are not in a session.");
            return true;
        }
        int limit = 10;
        if (args.length >= 2) {
            Integer parsed = parsePositiveInt(args[1]);
            if (parsed == null) {
                player.sendMessage("Invalid limit. Use a positive integer.");
                return true;
            }
            limit = Math.min(20, parsed);
        }
        List<PlayerCombatSnapshot> ranking = combatTracker.leaderboard(session, limit);
        if (ranking.isEmpty()) {
            player.sendMessage("No combat stats recorded yet.");
            return true;
        }
        player.sendMessage("Match scoreboard (top " + ranking.size() + "):");
        int position = 1;
        for (PlayerCombatSnapshot snapshot : ranking) {
            String name = resolveName(Bukkit.getOfflinePlayer(snapshot.playerId()), snapshot.playerId());
            double kd = snapshot.deaths() == 0
                ? snapshot.kills()
                : (double) snapshot.kills() / snapshot.deaths();
            player.sendMessage(
                position + ". " + name
                    + " | K:" + snapshot.kills()
                    + " D:" + snapshot.deaths()
                    + " A:" + snapshot.assists()
                    + " HS:" + snapshot.headshots()
                    + " KD:" + String.format(Locale.ROOT, "%.2f", kd)
            );
            position++;
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
            viewer.sendMessage("Usage: /csmc view <free|next|prev|player>");
            return true;
        }
        String targetText = args[1];
        if ("free".equalsIgnoreCase(targetText) || "self".equalsIgnoreCase(targetText)) {
            viewer.setSpectatorTarget(null);
            viewer.sendMessage("Now in free spectator camera.");
            return true;
        }
        if ("next".equalsIgnoreCase(targetText) || "prev".equalsIgnoreCase(targetText)) {
            boolean forward = "next".equalsIgnoreCase(targetText);
            Player target = pickSpectatorTarget(session, viewer, forward);
            if (target == null) {
                viewer.sendMessage("No alive players available to spectate.");
                return true;
            }
            viewer.setSpectatorTarget(target);
            viewer.sendMessage("Now spectating " + target.getName() + ".");
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

    private Player pickSpectatorTarget(GameSession session, Player viewer, boolean forward) {
        List<Player> candidates = new ArrayList<>();
        for (UUID playerId : session.players()) {
            if (playerId.equals(viewer.getUniqueId())) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR || player.isDead() || player.getHealth() <= 0.0) {
                continue;
            }
            candidates.add(player);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));

        int currentIndex = -1;
        Entity currentTarget = viewer.getSpectatorTarget();
        if (currentTarget instanceof Player currentPlayer) {
            UUID currentId = currentPlayer.getUniqueId();
            for (int index = 0; index < candidates.size(); index++) {
                if (candidates.get(index).getUniqueId().equals(currentId)) {
                    currentIndex = index;
                    break;
                }
            }
        }

        if (currentIndex < 0) {
            return candidates.get(0);
        }
        int offset = forward ? 1 : -1;
        int nextIndex = (currentIndex + offset + candidates.size()) % candidates.size();
        return candidates.get(nextIndex);
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
            player.sendMessage("Usage: /csmc queue <join|leave|status|list|votes|global> [mode|detail] [mapId]");
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
            case "votes" -> handleQueueVotes(player, args);
            case "global" -> handleQueueGlobal(player, args);
            case "status" -> {
                GameMode mode = queue.queuedMode(player.getUniqueId());
                if (mode == null) {
                    player.sendMessage("You are not queued.");
                } else {
                    int localSize = queue.queueSize(mode);
                    int globalSize = queue.queueSizeGlobal(mode);
                    int needed = queue.playersNeeded(mode);
                    int position = queue.queuePosition(player.getUniqueId());
                    String map = queue.queuedMap(player.getUniqueId());
                    String mapText = map == null ? "auto" : map;
                    int mapVotes = queue.mapVoteCount(mode, map);
                    double mapShare = localSize == 0 ? 0.0 : (mapVotes * 100.0) / localSize;
                    player.sendMessage(
                        "Queue " + mode + " (" + mapText + ") | position " + position + " / " + localSize
                            + " | global " + globalSize
                            + " | need " + needed
                            + " | map votes " + mapVotes + " (" + String.format(Locale.ROOT, "%.1f", mapShare) + "%)"
                    );
                }
                yield true;
            }
            default -> {
                player.sendMessage("Usage: /csmc queue <join|leave|status|list|votes|global> [mode|detail] [mapId]");
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
            int localSize = queue.queueSize(mode);
            int globalSize = queue.queueSizeGlobal(mode);
            int needed = queue.playersNeeded(mode);
            int position = queue.queuePosition(player.getUniqueId());
            int mapVotes = queue.mapVoteCount(mode, mapId);
            double mapShare = localSize == 0 ? 0.0 : (mapVotes * 100.0) / localSize;
            player.sendMessage(
                "Queue " + mode + " (" + mapText + ") | position " + position + " / " + localSize
                    + " | global " + globalSize
                    + " | need " + needed
                    + " | map votes " + mapVotes + " (" + String.format(Locale.ROOT, "%.1f", mapShare) + "%)"
            );
        }
        return true;
    }

    private boolean handleQueueList(Player player) {
        var localSizes = queue.queueSizes();
        var globalSizes = queue.queueSizesGlobal();
        StringBuilder builder = new StringBuilder("Queue sizes (local/global): ");
        int shown = 0;
        for (GameMode mode : GameMode.values()) {
            int localSize = localSizes.getOrDefault(mode, 0);
            int globalSize = globalSizes.getOrDefault(mode, localSize);
            int needed = queue.playersNeeded(mode);
            String voteHint = "";
            if (localSize > 0) {
                var votes = queue.mapVotes(mode);
                if (!votes.isEmpty()) {
                    var top = votes.entrySet().stream()
                        .sorted((left, right) -> {
                            int byCount = Integer.compare(right.getValue(), left.getValue());
                            if (byCount != 0) {
                                return byCount;
                            }
                            return left.getKey().compareToIgnoreCase(right.getKey());
                        })
                        .findFirst();
                    if (top.isPresent()) {
                        double share = (top.get().getValue() * 100.0) / localSize;
                        voteHint = ",top=" + top.get().getKey() + ":" + String.format(Locale.ROOT, "%.0f", share) + "%";
                    }
                }
            }
            if (shown > 0) {
                builder.append(" | ");
            }
            builder.append(mode.name())
                .append("=")
                .append(localSize)
                .append("/")
                .append(globalSize)
                .append("(need ")
                .append(needed)
                .append(voteHint)
                .append(")");
            shown++;
        }
        player.sendMessage(builder.toString());
        return true;
    }

    private boolean handleQueueGlobal(Player player, String[] args) {
        if (args.length >= 3 && "detail".equalsIgnoreCase(args[2])) {
            int limit = 10;
            if (args.length >= 4) {
                Integer parsed = parsePositiveInt(args[3]);
                if (parsed == null) {
                    player.sendMessage("Invalid detail limit. Use a positive integer.");
                    return true;
                }
                limit = Math.min(50, parsed);
            }
            return handleQueueGlobalDetail(player, limit);
        }
        GameMode mode = null;
        if (args.length >= 3) {
            try {
                mode = GameMode.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                player.sendMessage("Invalid mode.");
                return true;
            }
        }
        var local = queue.queueSizes();
        var remote = queue.queueSizesRemote();
        var global = queue.queueSizesGlobal();
        int sources = queue.activeRemoteSources();
        if (mode != null) {
            int localCount = local.getOrDefault(mode, 0);
            int remoteCount = remote.getOrDefault(mode, 0);
            int globalCount = global.getOrDefault(mode, localCount + remoteCount);
            player.sendMessage(
                "Queue global " + mode
                    + " | local " + localCount
                    + " | remote " + remoteCount
                    + " | global " + globalCount
                    + " | remoteSources " + sources
            );
            return true;
        }
        StringBuilder builder = new StringBuilder("Queue global (local/remote/global, sources ")
            .append(sources)
            .append("): ");
        int shown = 0;
        for (GameMode each : GameMode.values()) {
            if (shown > 0) {
                builder.append(" | ");
            }
            builder.append(each.name())
                .append("=")
                .append(local.getOrDefault(each, 0))
                .append("/")
                .append(remote.getOrDefault(each, 0))
                .append("/")
                .append(global.getOrDefault(each, 0));
            shown++;
        }
        player.sendMessage(builder.toString());
        return true;
    }

    private boolean handleQueueGlobalDetail(Player player, int limit) {
        List<RemoteQueueSourceStatus> sources = queue.remoteSourceStatuses();
        if (sources.isEmpty()) {
            player.sendMessage("Queue global detail: no remote queue sources.");
            return true;
        }
        int remoteTotal = 0;
        for (RemoteQueueSourceStatus source : sources) {
            remoteTotal += source.totalQueued();
        }
        int shown = Math.max(1, Math.min(limit, sources.size()));
        player.sendMessage(
            "Queue global detail (remote sources " + sources.size()
                + ", showing " + shown
                + ", total remote queued " + remoteTotal + "):"
        );
        for (int i = 0; i < shown; i++) {
            RemoteQueueSourceStatus source = sources.get(i);
            StringBuilder line = new StringBuilder(" - ")
                .append(source.serverId())
                .append(" | age ")
                .append(source.ageSeconds())
                .append("s | total ")
                .append(source.totalQueued())
                .append(" | ");
            int index = 0;
            for (GameMode mode : GameMode.values()) {
                if (index > 0) {
                    line.append(" ");
                }
                line.append(mode.name())
                    .append("=")
                    .append(source.queueSizes().getOrDefault(mode, 0));
                index++;
            }
            player.sendMessage(line.toString());
        }
        return true;
    }

    private boolean handleQueueVotes(Player player, String[] args) {
        GameMode mode = GameMode.COMPETITIVE;
        if (args.length >= 3) {
            try {
                mode = GameMode.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                player.sendMessage("Invalid mode.");
                return true;
            }
        }
        var votes = queue.mapVotes(mode);
        int total = queue.queueSize(mode);
        if (votes.isEmpty() || total <= 0) {
            player.sendMessage("No queue votes for " + mode + ".");
            return true;
        }
        player.sendMessage("Queue map votes for " + mode + " (total " + total + "):");
        votes.entrySet().stream()
            .sorted((left, right) -> {
                int byCount = Integer.compare(right.getValue(), left.getValue());
                if (byCount != 0) {
                    return byCount;
                }
                return left.getKey().compareToIgnoreCase(right.getKey());
            })
            .forEach(entry -> {
                double ratio = total == 0 ? 0.0 : (entry.getValue() * 100.0) / total;
                player.sendMessage(" - " + entry.getKey() + ": " + entry.getValue() + " (" + String.format(Locale.ROOT, "%.1f", ratio) + "%)");
            });
        return true;
    }

    private boolean handleAntiCheat(Player sender, String[] args) {
        if (antiCheat == null) {
            sender.sendMessage("Anti-cheat is unavailable.");
            return true;
        }
        if (!sender.isOp() && !sender.hasPermission("csmc.anticheat.manage")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /csmc ac <status|reset|top> [player|limit]");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if ("top".equals(action)) {
            int limit = 10;
            if (args.length >= 3) {
                Integer parsed = parsePositiveInt(args[2]);
                if (parsed == null) {
                    sender.sendMessage("Invalid limit. Use a positive integer.");
                    return true;
                }
                limit = Math.min(50, parsed);
            }
            var snapshot = antiCheat.violationSnapshot();
            if (snapshot.isEmpty()) {
                sender.sendMessage("No anti-cheat violations recorded.");
                return true;
            }
            sender.sendMessage("Top anti-cheat VL (top " + limit + "):");
            snapshot.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(limit)
                .forEach(entry -> {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null || name.isBlank()) {
                        name = entry.getKey().toString();
                    }
                    sender.sendMessage(" - " + name + ": " + entry.getValue());
                });
            return true;
        }
        Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2]) : sender;
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }
        return switch (action) {
            case "status" -> {
                int vl = antiCheat.violationLevel(target.getUniqueId());
                sender.sendMessage("Anti-cheat VL for " + target.getName() + ": " + vl);
                yield true;
            }
            case "reset" -> {
                antiCheat.reset(target.getUniqueId());
                sender.sendMessage("Anti-cheat VL reset for " + target.getName() + ".");
                yield true;
            }
            default -> {
                sender.sendMessage("Usage: /csmc ac <status|reset|top> [player|limit]");
                yield true;
            }
        };
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
