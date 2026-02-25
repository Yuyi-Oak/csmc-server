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

    @Test
    void resolvesBaseSpreadByWeaponClassAndOverrides() {
        WeaponSpec awp = new WeaponSpec("awp", "AWP", WeaponType.SNIPER, 0, 100.0, 300.0, 0.97, 1.0, 10, 3.6);
        WeaponSpec rifle = new WeaponSpec("famas", "FAMAS", WeaponType.RIFLE, 0, 30.0, 90.0, 0.7, 10.0, 25, 2.8);
        WeaponSpec grenade = new WeaponSpec("hegrenade", "HE", WeaponType.GRENADE, 0, 0.0, 0.0, 0.0, 1.0, 1, 0.0);
        WeaponSpec unknown = new WeaponSpec("custom", "Custom", WeaponType.PISTOL, 0, 10.0, 30.0, 0.4, 4.0, 12, 1.6);

        assertEquals(0.008, simulator.baseSpread(awp), 1e-12);
        assertEquals(0.060, simulator.baseSpread(rifle), 1e-12);
        assertEquals(0.0, simulator.baseSpread(grenade), 1e-12);
        assertEquals(0.045, simulator.baseSpread(unknown), 1e-12);
    }
}
