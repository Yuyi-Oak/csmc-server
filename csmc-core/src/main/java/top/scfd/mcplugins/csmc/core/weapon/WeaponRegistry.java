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
        register(new WeaponSpec("deagle", "Desert Eagle", WeaponType.PISTOL, 700, 53, 70, 0.93, 2.2, 7, 2.2));
        register(new WeaponSpec("p250", "P250", WeaponType.PISTOL, 300, 38, 55, 0.64, 4.0, 13, 2.2));
        register(new WeaponSpec("fiveseven", "Five-SeveN", WeaponType.PISTOL, 500, 32, 55, 0.91, 5.0, 20, 2.2));
        register(new WeaponSpec("tec9", "Tec-9", WeaponType.PISTOL, 500, 33, 50, 0.90, 6.7, 18, 2.5));
        register(new WeaponSpec("ak47", "AK-47", WeaponType.RIFLE, 2700, 36, 100, 0.77, 10.0, 30, 2.5));
        register(new WeaponSpec("m4a1s", "M4A1-S", WeaponType.RIFLE, 2900, 33, 90, 0.7, 11.0, 25, 3.1));
        register(new WeaponSpec("m4a1", "M4A1-S", WeaponType.RIFLE, 2900, 33, 90, 0.7, 11.0, 25, 3.1));
        register(new WeaponSpec("famas", "FAMAS", WeaponType.RIFLE, 2050, 30, 85, 0.70, 11.0, 25, 3.0));
        register(new WeaponSpec("galil", "Galil AR", WeaponType.RIFLE, 1800, 30, 85, 0.77, 11.0, 35, 3.3));
        register(new WeaponSpec("awp", "AWP", WeaponType.SNIPER, 4750, 115, 300, 0.97, 1.0, 10, 3.6));
        register(new WeaponSpec("ssg08", "SSG 08", WeaponType.SNIPER, 1700, 88, 230, 0.85, 1.2, 10, 3.7));
        register(new WeaponSpec("mac10", "MAC-10", WeaponType.SMG, 1050, 29, 75, 0.57, 13.3, 30, 3.2));
        register(new WeaponSpec("mp9", "MP9", WeaponType.SMG, 1250, 26, 75, 0.60, 14.0, 30, 2.1));
        register(new WeaponSpec("ump45", "UMP-45", WeaponType.SMG, 1200, 35, 80, 0.65, 11.0, 25, 3.5));
        register(new WeaponSpec("nova", "Nova", WeaponType.SHOTGUN, 1050, 26, 28, 0.50, 1.0, 8, 3.6));
        register(new WeaponSpec("xm1014", "XM1014", WeaponType.SHOTGUN, 2000, 20, 28, 0.50, 3.0, 7, 3.2));
        register(new WeaponSpec("knife", "Knife", WeaponType.KNIFE, 0, 55, 2, 1.0, 1.0, 1, 0.0));
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
