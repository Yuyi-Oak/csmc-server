package top.scfd.mcplugins.csmc.core.weapon;

import java.util.random.RandomGenerator;

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
        double baseSpread = spec.type() == WeaponType.SNIPER ? 0.2 : 0.1;
        return spreadModel.sampleSpread(baseSpread, movementFactor, jumpFactor, random);
    }
}
