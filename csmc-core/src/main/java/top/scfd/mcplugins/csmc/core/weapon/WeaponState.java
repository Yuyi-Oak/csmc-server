package top.scfd.mcplugins.csmc.core.weapon;

public final class WeaponState {
    private int shotsFired;
    private long lastShotTick;
    private long reloadEndTick;

    public int shotsFired() {
        return shotsFired;
    }

    public long lastShotTick() {
        return lastShotTick;
    }

    public boolean isReloading(long tick) {
        return reloadEndTick > tick;
    }

    public void startReload(long tick, long durationTicks) {
        if (durationTicks <= 0) {
            reloadEndTick = 0;
            return;
        }
        reloadEndTick = Math.max(reloadEndTick, tick + durationTicks);
    }

    public void finishReload() {
        reloadEndTick = 0;
    }

    public void recordShot(long tick) {
        shotsFired += 1;
        lastShotTick = tick;
    }

    public void resetBurst() {
        shotsFired = 0;
    }
}
