package top.scfd.mcplugins.csmc.core.weapon;

import java.util.random.RandomGenerator;
import java.util.Locale;

public final class WeaponSimulator {
    private final DamageModel damageModel = new DamageModel();
    private final RecoilModel recoilModel = new RecoilModel();
    private final SpreadModel spreadModel = new SpreadModel();

    public DamageResult simulateHit(WeaponSpec spec, double distance, boolean armored, Hitbox hitbox) {
        return damageModel.compute(spec, distance, armored, hitbox);
    }

    public double[] nextRecoil(WeaponSpec spec, RecoilPattern pattern, WeaponState state, long tick) {
        if (tick - state.lastShotTick() > 10) {
            state.resetBurst();
        }
        state.recordShot(tick);
        return recoilModel.nextRecoil(pattern, state.shotsFired() - 1);
    }

    public double[] sampleSpread(WeaponSpec spec, double movementFactor, double jumpFactor) {
        return sampleSpread(spec, movementFactor, jumpFactor, null);
    }

    public double[] sampleSpread(WeaponSpec spec, double movementFactor, double jumpFactor, RandomGenerator random) {
        return spreadModel.sampleSpread(baseSpread(spec), movementFactor, jumpFactor, random);
    }

    public double spreadRadius(WeaponSpec spec, double movementFactor, double jumpFactor) {
        return Math.max(0.0, baseSpread(spec) + Math.max(0.0, movementFactor) + Math.max(0.0, jumpFactor));
    }

    public double baseSpread(WeaponSpec spec) {
        if (spec == null) {
            return 0.1;
        }
        String key = spec.key() == null ? "" : spec.key().toLowerCase(Locale.ROOT);
        if ("awp".equals(key)) {
            return 0.008;
        }
        if ("ssg08".equals(key)) {
            return 0.012;
        }
        return switch (spec.type()) {
            case KNIFE, GRENADE -> 0.0;
            case PISTOL -> 0.045;
            case SMG -> 0.075;
            case RIFLE -> 0.060;
            case SNIPER -> 0.020;
            case SHOTGUN -> 0.110;
            case LMG -> 0.090;
        };
    }
}
