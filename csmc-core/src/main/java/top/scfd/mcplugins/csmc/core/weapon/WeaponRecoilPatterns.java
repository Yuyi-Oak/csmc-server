package top.scfd.mcplugins.csmc.core.weapon;

import java.util.Locale;
import java.util.Map;

public final class WeaponRecoilPatterns {
    private static final RecoilPattern PISTOL_FALLBACK = new RecoilPattern(
        new double[] {0.18, 0.22, 0.26, 0.28, 0.30},
        new double[] {0.03, -0.04, 0.05, -0.06, 0.07}
    );
    private static final RecoilPattern RIFLE_FALLBACK = new RecoilPattern(
        new double[] {0.28, 0.33, 0.38, 0.44, 0.51, 0.57, 0.62, 0.67, 0.71, 0.75},
        new double[] {0.04, -0.06, 0.08, -0.10, 0.11, -0.12, 0.14, -0.15, 0.16, -0.17}
    );
    private static final RecoilPattern SMG_FALLBACK = new RecoilPattern(
        new double[] {0.22, 0.26, 0.30, 0.34, 0.37, 0.40, 0.42},
        new double[] {0.05, -0.06, 0.07, -0.08, 0.09, -0.10, 0.11}
    );
    private static final RecoilPattern SNIPER_FALLBACK = new RecoilPattern(
        new double[] {0.12, 0.18},
        new double[] {0.0, 0.0}
    );
    private static final RecoilPattern SHOTGUN_FALLBACK = new RecoilPattern(
        new double[] {0.36, 0.45, 0.54},
        new double[] {0.02, -0.03, 0.04}
    );
    private static final RecoilPattern KNIFE_FALLBACK = RecoilPattern.empty();

    private static final Map<String, RecoilPattern> BY_KEY = Map.of(
        "glock", new RecoilPattern(
            new double[] {0.16, 0.20, 0.24, 0.27, 0.30, 0.33},
            new double[] {0.02, -0.03, 0.04, -0.05, 0.06, -0.07}
        ),
        "usp", new RecoilPattern(
            new double[] {0.15, 0.19, 0.23, 0.26, 0.29},
            new double[] {0.01, -0.02, 0.03, -0.04, 0.05}
        ),
        "ak47", new RecoilPattern(
            new double[] {0.30, 0.36, 0.42, 0.50, 0.58, 0.66, 0.73, 0.79, 0.84, 0.88, 0.92, 0.95},
            new double[] {0.04, -0.05, 0.07, -0.09, 0.11, -0.14, 0.16, -0.18, 0.20, -0.21, 0.22, -0.24}
        ),
        "m4a1s", new RecoilPattern(
            new double[] {0.24, 0.29, 0.34, 0.40, 0.46, 0.52, 0.57, 0.61, 0.65, 0.69},
            new double[] {0.03, -0.04, 0.05, -0.07, 0.08, -0.09, 0.10, -0.11, 0.12, -0.13}
        ),
        "awp", new RecoilPattern(
            new double[] {0.14, 0.20},
            new double[] {0.0, 0.0}
        )
    );

    private WeaponRecoilPatterns() {
    }

    public static RecoilPattern forWeapon(WeaponSpec spec) {
        if (spec == null) {
            return RecoilPattern.empty();
        }
        String key = spec.key() == null ? "" : spec.key().toLowerCase(Locale.ROOT);
        RecoilPattern byKey = BY_KEY.get(key);
        if (byKey != null) {
            return byKey;
        }
        return switch (spec.type()) {
            case PISTOL -> PISTOL_FALLBACK;
            case RIFLE -> RIFLE_FALLBACK;
            case SMG -> SMG_FALLBACK;
            case SNIPER -> SNIPER_FALLBACK;
            case SHOTGUN -> SHOTGUN_FALLBACK;
            case KNIFE -> KNIFE_FALLBACK;
        };
    }
}
