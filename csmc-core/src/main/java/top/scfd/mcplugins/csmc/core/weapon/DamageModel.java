package top.scfd.mcplugins.csmc.core.weapon;

public final class DamageModel {
    public DamageResult compute(WeaponSpec spec, double distance, boolean armored, Hitbox hitbox) {
        double base = spec.damage();
        double scaled = applyRangeFalloff(base, distance, spec.range());
        double hit = scaled * hitbox.multiplier();
        if (!armored) {
            return new DamageResult(hit, 0.0);
        }
        double penetration = Math.max(0.0, Math.min(1.0, spec.armorPenetration()));
        double armorDamage = hit * (1.0 - penetration);
        double healthDamage = hit * penetration;
        return new DamageResult(healthDamage, armorDamage);
    }

    private double applyRangeFalloff(double damage, double distance, double range) {
        double base = Math.max(0.0, damage);
        if (range <= 0) {
            return base;
        }
        double normalizedDistance = Math.max(0.0, distance);
        double ratio = Math.min(1.0, normalizedDistance / range);
        double falloff = 1.0 - ratio;
        return base * falloff;
    }
}
