package top.scfd.mcplugins.csmc.core.i18n;

import java.util.Map;

public interface MessageService {
    String prefix();

    String message(String key);

    String message(String key, Map<String, String> placeholders);
}
