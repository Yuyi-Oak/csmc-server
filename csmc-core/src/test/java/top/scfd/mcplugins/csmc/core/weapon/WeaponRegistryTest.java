package top.scfd.mcplugins.csmc.core.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class WeaponRegistryTest {
    @Test
    void includesExtendedCoreWeaponSet() {
        WeaponRegistry registry = new WeaponRegistry();

        assertTrue(registry.find("deagle").isPresent());
        assertTrue(registry.find("p250").isPresent());
        assertTrue(registry.find("famas").isPresent());
        assertTrue(registry.find("galil").isPresent());
        assertTrue(registry.find("ssg08").isPresent());
        assertTrue(registry.find("mac10").isPresent());
        assertTrue(registry.find("mp9").isPresent());
        assertTrue(registry.find("ump45").isPresent());
        assertTrue(registry.find("nova").isPresent());
        assertTrue(registry.find("xm1014").isPresent());
    }

    @Test
    void supportsM4AliasLookup() {
        WeaponRegistry registry = new WeaponRegistry();
        WeaponSpec m4Alias = registry.find("m4a1").orElseThrow();
        assertEquals("M4A1-S", m4Alias.displayName());
        assertEquals(25, m4Alias.magazineSize());
    }
}
