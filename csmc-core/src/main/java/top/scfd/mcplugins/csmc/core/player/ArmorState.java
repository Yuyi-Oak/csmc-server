package top.scfd.mcplugins.csmc.core.player;

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

    public void grantDefuseKit() {
        defuseKit = true;
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
