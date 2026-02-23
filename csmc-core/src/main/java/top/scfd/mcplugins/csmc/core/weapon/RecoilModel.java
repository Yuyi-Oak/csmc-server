package top.scfd.mcplugins.csmc.core.weapon;

public final class RecoilModel {
    public double[] nextRecoil(RecoilPattern pattern, int shotIndex) {
        if (pattern == null || pattern.length() == 0) {
            return new double[] {0.0, 0.0};
        }
        int idx = Math.min(shotIndex, pattern.length() - 1);
        return new double[] {pattern.pitch()[idx], pattern.yaw()[idx]};
    }
}
