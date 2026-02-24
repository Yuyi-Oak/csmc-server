package top.scfd.mcplugins.csmc.paper.security;

import org.bukkit.scheduler.BukkitRunnable;

public final class AntiCheatTicker extends BukkitRunnable {
    private final AntiCheatService antiCheat;

    public AntiCheatTicker(AntiCheatService antiCheat) {
        this.antiCheat = antiCheat;
    }

    @Override
    public void run() {
        antiCheat.decay();
    }
}
