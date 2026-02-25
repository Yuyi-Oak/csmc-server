package top.scfd.mcplugins.csmc.core.weapon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

final class SpreadModelTest {
    private final SpreadModel spreadModel = new SpreadModel();

    @Test
    void seededSamplingIsDeterministic() {
        double[] first = spreadModel.sampleSpread(0.15, 0.05, 0.0, new SplittableRandom(42L));
        double[] second = spreadModel.sampleSpread(0.15, 0.05, 0.0, new SplittableRandom(42L));
        assertArrayEquals(first, second, 1e-12);
    }

    @Test
    void offsetNeverExceedsComputedSpreadRadius() {
        double spread = 0.3;
        SplittableRandom random = new SplittableRandom(7L);
        for (int i = 0; i < 500; i++) {
            double[] sample = spreadModel.sampleSpread(0.1, 0.1, 0.1, random);
            double distance = Math.hypot(sample[0], sample[1]);
            assertTrue(distance <= spread + 1e-12, "sample exceeded spread radius");
        }
    }

    @Test
    void nonPositiveSpreadCollapsesToZeroOffset() {
        double[] sample = spreadModel.sampleSpread(-0.2, 0.1, 0.0, new SplittableRandom(1L));
        assertArrayEquals(new double[] {0.0, 0.0}, sample, 1e-12);
    }
}
