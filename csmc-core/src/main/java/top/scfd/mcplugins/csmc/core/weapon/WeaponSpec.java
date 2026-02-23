package top.scfd.mcplugins.csmc.core.weapon;

public record WeaponSpec(
    String key,
    String displayName,
    WeaponType type,
    int price,
    double damage,
    double range,
    double armorPenetration,
    double fireRate,
    int magazineSize,
    double reloadTime
) {
}
