package top.scfd.mcplugins.csmc.core.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class DamageModelTest {
    private final DamageModel damageModel = new DamageModel();
    private final WeaponSpec rifle = new WeaponSpec(
        "test_rifle",
        "Test Rifle",
        WeaponType.RIFLE,
        0,
        40.0,
        100.0,
        0.75,
        10.0,
        30,
        2.0
    );

    @Test
    void doesNotIncreaseDamageForNegativeDistance() {
        DamageResult result = damageModel.compute(rifle, -10.0, false, Hitbox.CHEST);
        assertEquals(40.0, result.healthDamage(), 1e-6);
        assertEquals(0.0, result.armorDamage(), 1e-6);
    }

    @Test
    void reachesZeroAtAndBeyondRange() {
        DamageResult atRange = damageModel.compute(rifle, 100.0, false, Hitbox.CHEST);
        DamageResult beyondRange = damageModel.compute(rifle, 150.0, false, Hitbox.CHEST);
        assertEquals(0.0, atRange.healthDamage(), 1e-6);
        assertEquals(0.0, beyondRange.healthDamage(), 1e-6);
    }

    @Test
    void splitsDamageBetweenHealthAndArmorWhenArmored() {
        DamageResult result = damageModel.compute(rifle, 0.0, true, Hitbox.HEAD);
        // head multiplier is 4x, base damage 40 => 160 total
        assertEquals(120.0, result.healthDamage(), 1e-6);
        assertEquals(40.0, result.armorDamage(), 1e-6);
    }
}
