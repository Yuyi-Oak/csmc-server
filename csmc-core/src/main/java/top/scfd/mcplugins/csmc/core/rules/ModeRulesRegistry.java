package top.scfd.mcplugins.csmc.core.rules;

import java.util.EnumMap;
import java.util.Map;
import top.scfd.mcplugins.csmc.api.GameMode;

public final class ModeRulesRegistry {
    private final Map<GameMode, ModeRules> rules = new EnumMap<>(GameMode.class);

    public ModeRulesRegistry() {
        for (GameMode mode : GameMode.values()) {
            rules.put(mode, defaultsFor(mode));
        }
    }

    public ModeRules rulesFor(GameMode mode) {
        return rules.get(mode);
    }

    public void register(GameMode mode, ModeRules modeRules) {
        rules.put(mode, modeRules);
    }

    private ModeRules defaultsFor(GameMode mode) {
        return switch (mode) {
            case COMPETITIVE -> new ModeRules(
                mode,
                10,
                13,
                24,
                true,
                4,
                6,
                new RoundDurations(15, 20, 115, 40, 10, 3),
                defaultEconomy()
            );
            case CASUAL -> new ModeRules(
                mode,
                20,
                8,
                15,
                false,
                0,
                0,
                new RoundDurations(10, 25, 150, 40, 10, 3),
                defaultEconomy()
            );
            case DEATHMATCH, TEAM_DEATHMATCH -> new ModeRules(
                mode,
                20,
                0,
                0,
                false,
                0,
                0,
                new RoundDurations(0, 0, 600, 0, 0, 0),
                deathmatchEconomy()
            );
            case ARMS_RACE -> new ModeRules(
                mode,
                20,
                0,
                0,
                false,
                0,
                0,
                new RoundDurations(10, 10, 600, 0, 0, 0),
                deathmatchEconomy()
            );
            case DEMOLITION -> new ModeRules(
                mode,
                20,
                8,
                15,
                false,
                0,
                0,
                new RoundDurations(10, 15, 130, 40, 10, 3),
                defaultEconomy()
            );
            case WINGMAN -> new ModeRules(
                mode,
                4,
                9,
                16,
                true,
                5,
                8,
                new RoundDurations(15, 20, 115, 40, 10, 3),
                defaultEconomy()
            );
            case DANGER_ZONE -> new ModeRules(
                mode,
                16,
                0,
                0,
                false,
                0,
                0,
                new RoundDurations(0, 0, 900, 0, 0, 0),
                dangerZoneEconomy()
            );
        };
    }

    private EconomyRules defaultEconomy() {
        return new EconomyRules(
            800,
            16000,
            300,
            150,
            300,
            300,
            3250,
            1400,
            500,
            3400
        );
    }

    private EconomyRules deathmatchEconomy() {
        return new EconomyRules(
            10000,
            16000,
            300,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        );
    }

    private EconomyRules dangerZoneEconomy() {
        return new EconomyRules(
            0,
            6000,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        );
    }
}
