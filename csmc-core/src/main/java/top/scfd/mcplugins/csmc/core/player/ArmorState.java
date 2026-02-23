package top.scfd.mcplugins.csmc.core.player;

public final class ArmorState {
    private int armor;
    private boolean helmet;

    public int armor() {
        return armor;
    }

    public boolean helmet() {
        return helmet;
    }

    public void grantKevlar() {
        armor = Math.max(armor, 100);
    }

    public void grantHelmet() {
        helmet = true;
    }

    public void reset() {
        armor = 0;
        helmet = false;
    }
}
