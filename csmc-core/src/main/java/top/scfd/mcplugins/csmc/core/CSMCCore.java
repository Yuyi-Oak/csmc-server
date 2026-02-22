package top.scfd.mcplugins.csmc.core;

import top.scfd.mcplugins.csmc.api.CSMCPlatform;

public final class CSMCCore {
    private final CSMCPlatform platform;

    public CSMCCore(CSMCPlatform platform) {
        this.platform = platform;
    }

    public void start() {
        platform.logInfo("CSMC core starting on " + platform.platformName());
    }

    public void stop() {
        platform.logInfo("CSMC core stopping");
    }
}
