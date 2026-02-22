package top.scfd.mcplugins.csmc.api;

public interface CSMCPlatform {
    String platformName();

    void logInfo(String message);

    void logWarn(String message, Throwable error);
}
