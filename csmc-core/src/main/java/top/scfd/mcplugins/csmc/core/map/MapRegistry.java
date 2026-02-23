package top.scfd.mcplugins.csmc.core.map;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MapRegistry {
    private final Map<String, MapDefinition> maps = new ConcurrentHashMap<>();

    public void register(MapDefinition definition) {
        if (definition == null || definition.id() == null) {
            return;
        }
        maps.put(definition.id().toLowerCase(), definition);
    }

    public Optional<MapDefinition> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(maps.get(id.toLowerCase()));
    }

    public Collection<MapDefinition> all() {
        return Collections.unmodifiableCollection(maps.values());
    }

    public void clear() {
        maps.clear();
    }
}
