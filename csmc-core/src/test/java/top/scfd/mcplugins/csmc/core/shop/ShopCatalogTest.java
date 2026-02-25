package top.scfd.mcplugins.csmc.core.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import top.scfd.mcplugins.csmc.api.TeamSide;

final class ShopCatalogTest {
    @Test
    void defaultCatalogIncludesExtendedWeaponsAndSideRules() {
        ShopCatalog catalog = new ShopCatalog();

        ShopItem deagle = catalog.find("deagle").orElseThrow();
        ShopItem mp9 = catalog.find("mp9").orElseThrow();
        ShopItem molotov = catalog.find("molotov").orElseThrow();
        ShopItem incgrenade = catalog.find("incgrenade").orElseThrow();
        ShopItem defuseKit = catalog.find("defuse_kit").orElseThrow();
        ShopItem kevlarHelmet = catalog.find("kevlar_helmet").orElseThrow();

        assertTrue(deagle.isAllowedFor(TeamSide.TERRORIST));
        assertTrue(deagle.isAllowedFor(TeamSide.COUNTER_TERRORIST));
        assertTrue(mp9.isAllowedFor(TeamSide.COUNTER_TERRORIST));
        assertFalse(mp9.isAllowedFor(TeamSide.TERRORIST));
        assertTrue(molotov.isAllowedFor(TeamSide.TERRORIST));
        assertFalse(molotov.isAllowedFor(TeamSide.COUNTER_TERRORIST));
        assertTrue(incgrenade.isAllowedFor(TeamSide.COUNTER_TERRORIST));
        assertFalse(incgrenade.isAllowedFor(TeamSide.TERRORIST));
        assertTrue(defuseKit.isAllowedFor(TeamSide.COUNTER_TERRORIST));
        assertFalse(defuseKit.isAllowedFor(TeamSide.TERRORIST));
        assertTrue(kevlarHelmet.isAllowedFor(TeamSide.TERRORIST));
        assertTrue(kevlarHelmet.isAllowedFor(TeamSide.COUNTER_TERRORIST));
    }

    @Test
    void supportsCommonAliasKeys() {
        ShopCatalog catalog = new ShopCatalog();

        assertEquals("m4a1s", catalog.find("m4a1").orElseThrow().key());
        assertEquals("flashbang", catalog.find("flash").orElseThrow().key());
        assertEquals("hegrenade", catalog.find("he").orElseThrow().key());
        assertEquals("incgrenade", catalog.find("incendiary").orElseThrow().key());
        assertEquals("defuse_kit", catalog.find("kit").orElseThrow().key());
        assertEquals("kevlar_helmet", catalog.find("vesthelm").orElseThrow().key());
    }
}
