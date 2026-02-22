package top.scfd.mcplugins.csmc;

import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.core.CSMCCore;
import top.scfd.mcplugins.csmc.platform.PaperPlatform;

public final class CSMCPlugin extends JavaPlugin {
    private CSMCCore core;

    @Override
    public void onEnable() {
        core = new CSMCCore(new PaperPlatform(this));
        core.start();
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.stop();
        }
    }
}
