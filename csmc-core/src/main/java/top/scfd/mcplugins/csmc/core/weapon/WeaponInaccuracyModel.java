package top.scfd.mcplugins.csmc.core.weapon;

public final class WeaponInaccuracyModel {
    public double movementFactor(double horizontalSpeed, boolean sprinting, boolean sneaking) {
        double factor = Math.max(0.0, horizontalSpeed) * 0.07;
        if (sprinting) {
            factor += 0.035;
        }
        if (sneaking) {
            factor *= 0.65;
        }
        return Math.max(0.0, factor);
    }

    public double jumpFactor(boolean grounded, double verticalVelocity, float fallDistance) {
        if (grounded) {
            return 0.0;
        }
        double upwardPenalty = Math.max(0.0, verticalVelocity) * 0.4;
        double fallPenalty = fallDistance > 0.0f ? 0.04 : 0.0;
        return 0.16 + Math.min(0.12, upwardPenalty) + fallPenalty;
    }
}
