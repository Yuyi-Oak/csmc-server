package top.scfd.mcplugins.csmc.core.weapon;

public record RecoilPattern(
    double[] pitch,
    double[] yaw
) {
    public static RecoilPattern empty() {
        return new RecoilPattern(new double[0], new double[0]);
    }

    public int length() {
        return Math.min(pitch.length, yaw.length);
    }
}
