package top.scfd.mcplugins.csmc.paper;

import org.bukkit.scheduler.BukkitRunnable;

public final class PaperSessionTicker extends BukkitRunnable {
    private final SessionRegistry sessions;

    public PaperSessionTicker(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    @Override
    public void run() {
        sessions.tickSessions();
    }
}
