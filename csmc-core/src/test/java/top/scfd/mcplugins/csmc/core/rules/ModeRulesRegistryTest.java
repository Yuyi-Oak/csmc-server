package top.scfd.mcplugins.csmc.core.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import top.scfd.mcplugins.csmc.api.GameMode;

final class ModeRulesRegistryTest {
    @Test
    void defaultFriendlyFireMatchesModeIntent() {
        ModeRulesRegistry registry = new ModeRulesRegistry();

        assertTrue(registry.rulesFor(GameMode.COMPETITIVE).friendlyFireEnabled());
        assertTrue(registry.rulesFor(GameMode.WINGMAN).friendlyFireEnabled());
        assertTrue(registry.rulesFor(GameMode.DEMOLITION).friendlyFireEnabled());

        assertFalse(registry.rulesFor(GameMode.CASUAL).friendlyFireEnabled());
        assertFalse(registry.rulesFor(GameMode.DEATHMATCH).friendlyFireEnabled());
        assertFalse(registry.rulesFor(GameMode.TEAM_DEATHMATCH).friendlyFireEnabled());
        assertFalse(registry.rulesFor(GameMode.ARMS_RACE).friendlyFireEnabled());
        assertFalse(registry.rulesFor(GameMode.DANGER_ZONE).friendlyFireEnabled());
    }
}
