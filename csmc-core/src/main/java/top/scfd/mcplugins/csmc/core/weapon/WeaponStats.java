package top.scfd.mcplugins.csmc.core.weapon;

public record WeaponStats(
    int shotsFired,
    int hits,
    int headshots
) {
    public static WeaponStats empty() {
        return new WeaponStats(0, 0, 0);
    }
}
