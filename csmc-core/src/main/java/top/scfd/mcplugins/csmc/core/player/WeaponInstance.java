package top.scfd.mcplugins.csmc.core.player;

import top.scfd.mcplugins.csmc.core.weapon.WeaponSpec;
import top.scfd.mcplugins.csmc.core.weapon.WeaponState;

public final class WeaponInstance {
    private final WeaponSpec spec;
    private final WeaponState state;
    private int magazine;
    private int reserve;

    public WeaponInstance(WeaponSpec spec, int magazine, int reserve) {
        this.spec = spec;
        this.state = new WeaponState();
        this.magazine = Math.max(0, magazine);
        this.reserve = Math.max(0, reserve);
    }

    public static WeaponInstance full(WeaponSpec spec) {
        int mag = Math.max(1, spec.magazineSize());
        int reserve = mag * 3;
        return new WeaponInstance(spec, mag, reserve);
    }

    public WeaponSpec spec() {
        return spec;
    }

    public WeaponState state() {
        return state;
    }

    public int magazine() {
        return magazine;
    }

    public int reserve() {
        return reserve;
    }

    public boolean canShoot() {
        return magazine > 0;
    }

    public boolean consumeRound() {
        if (magazine <= 0) {
            return false;
        }
        magazine -= 1;
        return true;
    }

    public int reload() {
        int target = Math.max(1, spec.magazineSize());
        int needed = Math.max(0, target - magazine);
        int toLoad = Math.min(needed, reserve);
        magazine += toLoad;
        reserve -= toLoad;
        return toLoad;
    }

    public void addReserve(int amount) {
        if (amount > 0) {
            reserve += amount;
        }
    }
}
