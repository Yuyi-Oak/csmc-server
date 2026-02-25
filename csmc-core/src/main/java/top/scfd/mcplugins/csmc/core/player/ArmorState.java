package top.scfd.mcplugins.csmc.core.player;

import top.scfd.mcplugins.csmc.core.weapon.Hitbox;

public final class ArmorState {
    private int armor;
    private boolean helmet;
    private boolean defuseKit;

    public int armor() {
        return armor;
    }

    public boolean helmet() {
        return helmet;
    }

    public boolean defuseKit() {
        return defuseKit;
    }

    public void grantKevlar() {
        armor = Math.max(armor, 100);
    }

    public void grantHelmet() {
        helmet = true;
    }

    public void grantKevlarHelmet() {
        grantKevlar();
        grantHelmet();
    }

    public void grantDefuseKit() {
        defuseKit = true;
    }

    public boolean protects(Hitbox hitbox) {
        if (armor <= 0) {
            return false;
        }
        if (hitbox == Hitbox.HEAD) {
            return helmet;
        }
        return true;
    }

    public int applyArmorDamage(double damage) {
        if (damage <= 0) {
            return 0;
        }
        int absorbed = (int) Math.min(armor, Math.ceil(damage));
        armor = Math.max(0, armor - absorbed);
        return absorbed;
    }

    public void reset() {
        armor = 0;
        helmet = false;
        defuseKit = false;
    }
}
