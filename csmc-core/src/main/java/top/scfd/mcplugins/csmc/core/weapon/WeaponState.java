package top.scfd.mcplugins.csmc.core.weapon;

public final class WeaponState {
    private int shotsFired;
    private long lastShotTick;

    public int shotsFired() {
        return shotsFired;
    }

    public long lastShotTick() {
        return lastShotTick;
    }

    public void recordShot(long tick) {
        shotsFired += 1;
        lastShotTick = tick;
    }

    public void resetBurst() {
        shotsFired = 0;
    }
}
