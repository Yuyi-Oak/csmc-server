package top.scfd.mcplugins.csmc.core.shop;

import java.util.EnumSet;
import java.util.Set;
import top.scfd.mcplugins.csmc.api.TeamSide;

public record ShopItem(
    String key,
    String displayName,
    int price,
    ShopCategory category,
    Set<TeamSide> allowedSides
) {
    public ShopItem(String key, String displayName, int price) {
        this(key, displayName, price, ShopCategory.UNKNOWN, defaultAllowedSides());
    }

    public ShopItem(String key, String displayName, int price, ShopCategory category) {
        this(key, displayName, price, category, defaultAllowedSides());
    }

    public ShopItem {
        allowedSides = normalizeAllowedSides(allowedSides);
    }

    public boolean isAllowedFor(TeamSide side) {
        return side == TeamSide.TERRORIST || side == TeamSide.COUNTER_TERRORIST
            ? allowedSides.contains(side)
            : false;
    }

    private static Set<TeamSide> defaultAllowedSides() {
        return Set.of(TeamSide.TERRORIST, TeamSide.COUNTER_TERRORIST);
    }

    private static Set<TeamSide> normalizeAllowedSides(Set<TeamSide> sides) {
        EnumSet<TeamSide> normalized = EnumSet.noneOf(TeamSide.class);
        if (sides != null) {
            for (TeamSide side : sides) {
                if (side == TeamSide.TERRORIST || side == TeamSide.COUNTER_TERRORIST) {
                    normalized.add(side);
                }
            }
        }
        if (normalized.isEmpty()) {
            return defaultAllowedSides();
        }
        return Set.copyOf(normalized);
    }
}
