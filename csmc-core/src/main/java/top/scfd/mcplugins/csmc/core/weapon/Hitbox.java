package top.scfd.mcplugins.csmc.core.weapon;

public enum Hitbox {
    HEAD(4.0),
    CHEST(1.0),
    STOMACH(1.25),
    ARM(1.0),
    LEG(0.75);

    private final double multiplier;

    Hitbox(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }
}
