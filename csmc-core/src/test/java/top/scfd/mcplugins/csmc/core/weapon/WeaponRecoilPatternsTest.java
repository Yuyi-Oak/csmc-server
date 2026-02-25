package top.scfd.mcplugins.csmc.core.weapon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class WeaponRecoilPatternsTest {
    @Test
    void returnsDedicatedPatternForKnownWeapon() {
        WeaponSpec ak = new WeaponSpec(
            "ak47",
            "AK-47",
            WeaponType.RIFLE,
            2700,
            36.0,
            100.0,
            0.77,
            10.0,
            30,
            2.5
        );
        RecoilPattern pattern = WeaponRecoilPatterns.forWeapon(ak);
        assertTrue(pattern.length() >= 10, "expected a non-trivial AK pattern");
    }

    @Test
    void fallsBackByWeaponTypeForUnknownKey() {
        WeaponSpec unknownSmg = new WeaponSpec(
            "custom_smg",
            "Custom SMG",
            WeaponType.SMG,
            1200,
            24.0,
            70.0,
            0.55,
            12.0,
            30,
            2.1
        );
        RecoilPattern pattern = WeaponRecoilPatterns.forWeapon(unknownSmg);
        assertTrue(pattern.length() > 0, "smg fallback pattern should not be empty");
    }

    @Test
    void knifeFallbackHasNoRecoilPattern() {
        WeaponSpec knife = new WeaponSpec(
            "knife",
            "Knife",
            WeaponType.KNIFE,
            0,
            50.0,
            2.0,
            1.0,
            1.0,
            1,
            0.0
        );
        RecoilPattern pattern = WeaponRecoilPatterns.forWeapon(knife);
        assertTrue(pattern.length() == 0, "knife pattern should stay empty");
    }

    @Test
    void grenadeFallbackHasNoRecoilPattern() {
        WeaponSpec grenade = new WeaponSpec(
            "flashbang",
            "Flashbang",
            WeaponType.GRENADE,
            200,
            0.0,
            0.0,
            0.0,
            1.0,
            1,
            0.0
        );
        RecoilPattern pattern = WeaponRecoilPatterns.forWeapon(grenade);
        assertTrue(pattern.length() == 0, "grenade pattern should stay empty");
    }
}
