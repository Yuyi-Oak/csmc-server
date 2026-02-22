package top.scfd.mcplugins.csmc.platform;

import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.api.CSMCPlatform;

public final class PaperPlatform implements CSMCPlatform {
    private final JavaPlugin plugin;

    public PaperPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String platformName() {
        return "Paper";
    }

    @Override
    public void logInfo(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void logWarn(String message, Throwable error) {
        plugin.getLogger().log(java.util.logging.Level.WARNING, message, error);
    }
}
