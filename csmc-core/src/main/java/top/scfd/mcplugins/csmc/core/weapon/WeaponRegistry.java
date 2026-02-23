package top.scfd.mcplugins.csmc.core.weapon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class WeaponRegistry {
    private final Map<String, WeaponSpec> weapons = new LinkedHashMap<>();

    public WeaponRegistry() {
        register(new WeaponSpec("glock", "Glock-18", WeaponType.PISTOL, 200, 28, 50, 0.47, 6.0, 20, 2.3));
        register(new WeaponSpec("usp", "USP-S", WeaponType.PISTOL, 200, 35, 60, 0.47, 4.0, 12, 2.2));
        register(new WeaponSpec("ak47", "AK-47", WeaponType.RIFLE, 2700, 36, 100, 0.77, 10.0, 30, 2.5));
        register(new WeaponSpec("m4a1s", "M4A1-S", WeaponType.RIFLE, 2900, 33, 90, 0.7, 11.0, 25, 3.1));
        register(new WeaponSpec("awp", "AWP", WeaponType.SNIPER, 4750, 115, 300, 0.97, 1.0, 10, 3.6));
    }

    public Optional<WeaponSpec> find(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(weapons.get(key.toLowerCase()));
    }

    public Map<String, WeaponSpec> all() {
        return Collections.unmodifiableMap(weapons);
    }

    public void register(WeaponSpec spec) {
        weapons.put(spec.key().toLowerCase(), spec);
    }
}
