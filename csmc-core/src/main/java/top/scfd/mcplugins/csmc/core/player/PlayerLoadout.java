package top.scfd.mcplugins.csmc.core.player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerLoadout {
    private WeaponInstance primary;
    private WeaponInstance secondary;
    private WeaponInstance melee;
    private WeaponSlot activeSlot = WeaponSlot.NONE;
    private final Map<String, Integer> grenades = new LinkedHashMap<>();
    private final ArmorState armor = new ArmorState();

    public WeaponInstance primary() {
        return primary;
    }

    public WeaponInstance secondary() {
        return secondary;
    }

    public WeaponInstance melee() {
        return melee;
    }

    public WeaponSlot activeSlot() {
        return activeSlot;
    }

    public ArmorState armor() {
        return armor;
    }

    public Map<String, Integer> grenades() {
        return Collections.unmodifiableMap(grenades);
    }

    public WeaponInstance activeWeapon() {
        return switch (activeSlot) {
            case PRIMARY -> primary;
            case SECONDARY -> secondary;
            case MELEE -> melee;
            default -> null;
        };
    }

    public void setPrimary(WeaponInstance weapon) {
        primary = weapon;
        if (activeSlot == WeaponSlot.NONE) {
            activeSlot = WeaponSlot.PRIMARY;
        }
    }

    public void setSecondary(WeaponInstance weapon) {
        secondary = weapon;
        if (activeSlot == WeaponSlot.NONE) {
            activeSlot = WeaponSlot.SECONDARY;
        }
    }

    public void setMelee(WeaponInstance weapon) {
        melee = weapon;
        if (activeSlot == WeaponSlot.NONE) {
            activeSlot = WeaponSlot.MELEE;
        }
    }

    public boolean setActiveSlot(WeaponSlot slot) {
        if (slot == null) {
            return false;
        }
        switch (slot) {
            case PRIMARY -> {
                if (primary == null) {
                    return false;
                }
            }
            case SECONDARY -> {
                if (secondary == null) {
                    return false;
                }
            }
            case MELEE -> {
                if (melee == null) {
                    return false;
                }
            }
            case GRENADE, NONE -> {
            }
        }
        activeSlot = slot;
        return true;
    }

    public int grenadeCount() {
        int total = 0;
        for (int count : grenades.values()) {
            total += count;
        }
        return total;
    }

    public boolean addGrenade(String key, int maxTotal, int maxPerType) {
        if (!canAddGrenade(key, maxTotal, maxPerType)) {
            return false;
        }
        String normalized = key.toLowerCase();
        int current = grenades.getOrDefault(normalized, 0);
        grenades.put(normalized, current + 1);
        return true;
    }

    public boolean canAddGrenade(String key, int maxTotal, int maxPerType) {
        if (key == null || maxTotal <= 0 || maxPerType <= 0) {
            return false;
        }
        int total = grenadeCount();
        if (total >= maxTotal) {
            return false;
        }
        int current = grenades.getOrDefault(key.toLowerCase(), 0);
        return current < maxPerType;
    }

    public void clearGrenades() {
        grenades.clear();
    }
}
