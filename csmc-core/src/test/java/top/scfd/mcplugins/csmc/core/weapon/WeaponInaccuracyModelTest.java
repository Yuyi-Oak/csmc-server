package top.scfd.mcplugins.csmc.core.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class WeaponInaccuracyModelTest {
    private final WeaponInaccuracyModel model = new WeaponInaccuracyModel();

    @Test
    void movementPenaltyIncreasesWithSpeedAndSprinting() {
        double walking = model.movementFactor(0.2, false, false);
        double sprinting = model.movementFactor(0.2, true, false);
        assertTrue(sprinting > walking);
    }

    @Test
    void sneakingReducesMovementPenalty() {
        double normal = model.movementFactor(0.3, false, false);
        double sneaking = model.movementFactor(0.3, false, true);
        assertTrue(sneaking < normal);
    }

    @Test
    void groundedJumpPenaltyIsZero() {
        assertEquals(0.0, model.jumpFactor(true, 0.42, 0.0f), 1e-12);
    }

    @Test
    void airbornePenaltyAccountsForUpwardVelocityAndFalling() {
        double upward = model.jumpFactor(false, 0.42, 0.0f);
        double falling = model.jumpFactor(false, -0.2, 1.0f);
        assertTrue(upward > 0.16);
        assertTrue(falling > 0.16);
    }
}
