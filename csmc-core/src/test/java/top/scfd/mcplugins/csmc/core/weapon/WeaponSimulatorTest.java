package top.scfd.mcplugins.csmc.core.weapon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

final class WeaponSimulatorTest {
    private final WeaponSimulator simulator = new WeaponSimulator();

    @Test
    void burstResetsAfterLongShotGap() {
        RecoilPattern pattern = new RecoilPattern(
            new double[] {1.0, 2.0, 3.0},
            new double[] {0.5, 1.0, 1.5}
        );
        WeaponState state = new WeaponState();
        WeaponSpec rifle = new WeaponSpec(
            "rifle",
            "Rifle",
            WeaponType.RIFLE,
            0,
            30.0,
            80.0,
            0.7,
            10.0,
            30,
            2.4
        );

        simulator.nextRecoil(rifle, pattern, state, 0L);
        simulator.nextRecoil(rifle, pattern, state, 5L);
        assertEquals(2, state.shotsFired());

        double[] recoilAfterGap = simulator.nextRecoil(rifle, pattern, state, 20L);
        assertEquals(1, state.shotsFired());
        assertArrayEquals(new double[] {1.0, 0.5}, recoilAfterGap, 1e-12);
    }

    @Test
    void spreadSamplingCanBeDeterministicViaSeededGenerator() {
        WeaponSpec sniper = new WeaponSpec(
            "sniper",
            "Sniper",
            WeaponType.SNIPER,
            0,
            80.0,
            300.0,
            0.95,
            1.0,
            10,
            3.0
        );

        double[] first = simulator.sampleSpread(sniper, 0.02, 0.0, new SplittableRandom(88L));
        double[] second = simulator.sampleSpread(sniper, 0.02, 0.0, new SplittableRandom(88L));
        assertArrayEquals(first, second, 1e-12);
    }
}
