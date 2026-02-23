package top.scfd.mcplugins.csmc.paper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import top.scfd.mcplugins.csmc.api.TeamSide;
import top.scfd.mcplugins.csmc.core.map.MapDefinition;
import top.scfd.mcplugins.csmc.core.map.MapSpawn;
import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class RoundSpawnListener implements RoundEventListener {
    private final GameSession session;
    private final MapDefinition map;

    public RoundSpawnListener(GameSession session, MapDefinition map) {
        this.session = session;
        this.map = map;
    }

    @Override
    public void onRoundStart(int roundNumber) {
        if (map == null) {
            return;
        }
        World world = Bukkit.getWorld(map.world());
        if (world == null) {
            return;
        }
        for (UUID playerId : session.players()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            TeamSide side = session.getSide(playerId);
            Location spawn = pickSpawn(world, map.spawns(side));
            if (spawn != null) {
                player.teleport(spawn);
            }
        }
    }

    private Location pickSpawn(World world, List<MapSpawn> spawns) {
        if (spawns == null || spawns.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(spawns.size());
        MapSpawn spawn = spawns.get(index);
        return new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
    }
}
