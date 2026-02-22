package top.scfd.mcplugins.csmc.paper;

import org.bukkit.scheduler.BukkitRunnable;
import top.scfd.mcplugins.csmc.core.session.GameSession;
import top.scfd.mcplugins.csmc.core.session.GameSessionManager;

public final class PaperSessionTicker extends BukkitRunnable {
    private final GameSessionManager sessions;

    public PaperSessionTicker(GameSessionManager sessions) {
        this.sessions = sessions;
    }

    @Override
    public void run() {
        for (GameSession session : sessions.allSessions()) {
            session.tickRound();
        }
    }
}
