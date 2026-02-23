package top.scfd.mcplugins.csmc.core.weapon;

import java.util.concurrent.ThreadLocalRandom;

public final class SpreadModel {
    public double[] sampleSpread(double baseSpread, double movementFactor, double jumpFactor) {
        double spread = Math.max(0.0, baseSpread + movementFactor + jumpFactor);
        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2.0);
        double radius = ThreadLocalRandom.current().nextDouble(0, spread);
        return new double[] {Math.cos(angle) * radius, Math.sin(angle) * radius};
    }
}
