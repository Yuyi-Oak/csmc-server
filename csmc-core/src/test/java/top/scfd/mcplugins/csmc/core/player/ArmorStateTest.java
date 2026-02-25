package top.scfd.mcplugins.csmc.core.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import top.scfd.mcplugins.csmc.core.weapon.Hitbox;

final class ArmorStateTest {
    @Test
    void kevlarDoesNotProtectHeadWithoutHelmet() {
        ArmorState armor = new ArmorState();
        armor.grantKevlar();

        assertTrue(armor.protects(Hitbox.CHEST));
        assertTrue(armor.protects(Hitbox.STOMACH));
        assertFalse(armor.protects(Hitbox.HEAD));
    }

    @Test
    void kevlarHelmetProtectsHead() {
        ArmorState armor = new ArmorState();
        armor.grantKevlarHelmet();

        assertTrue(armor.protects(Hitbox.HEAD));
    }
}
