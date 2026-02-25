package top.scfd.mcplugins.csmc.core.weapon;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public final class SpreadModel {
    public double[] sampleSpread(double baseSpread, double movementFactor, double jumpFactor) {
        return sampleSpread(baseSpread, movementFactor, jumpFactor, ThreadLocalRandom.current());
    }

    public double[] sampleSpread(double baseSpread, double movementFactor, double jumpFactor, RandomGenerator random) {
        double spread = Math.max(0.0, baseSpread + movementFactor + jumpFactor);
        RandomGenerator source = random == null ? ThreadLocalRandom.current() : random;
        double angle = source.nextDouble() * Math.PI * 2.0;
        // sqrt(u) keeps the 2D radial distribution uniform.
        double radius = Math.sqrt(source.nextDouble()) * spread;
        return new double[] {Math.cos(angle) * radius, Math.sin(angle) * radius};
    }
}
