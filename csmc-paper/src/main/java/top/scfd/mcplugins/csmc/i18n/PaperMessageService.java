package top.scfd.mcplugins.csmc.i18n;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.scfd.mcplugins.csmc.core.i18n.MessageService;

public final class PaperMessageService implements MessageService {
    private final String prefix;
    private final Map<String, String> messages;

    public PaperMessageService(JavaPlugin plugin, String languageTag) {
        String langCode = toLangCode(languageTag);
        String resourcePath = "lang/messages_" + langCode + ".yml";
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(target);
        this.prefix = config.getString("prefix", "CSMC");
        this.messages = new HashMap<>();
        ConfigurationSection root = config.getConfigurationSection("");
        if (root != null) {
            flatten(root, "", messages);
        }
    }

    @Override
    public String prefix() {
        return prefix;
    }

    @Override
    public String message(String key) {
        return messages.getOrDefault(key, key);
    }

    @Override
    public String message(String key, Map<String, String> placeholders) {
        String base = message(key);
        if (placeholders == null || placeholders.isEmpty()) {
            return base;
        }
        String resolved = base;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return resolved;
    }

    private void flatten(ConfigurationSection section, String prefix, Map<String, String> output) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                flatten(child, fullKey, output);
            } else if (value != null) {
                output.put(fullKey, value.toString());
            }
        }
    }

    private String toLangCode(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return "en";
        }
        String lower = languageTag.toLowerCase(Locale.ROOT);
        if (lower.startsWith("zh")) {
            return "zh";
        }
        if (lower.startsWith("ru")) {
            return "ru";
        }
        if (lower.startsWith("fr")) {
            return "fr";
        }
        if (lower.startsWith("de")) {
            return "de";
        }
        return "en";
    }
}
