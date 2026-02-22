package top.scfd.mcplugins.csmc.storage;

public record PlayerStats(
    long kills,
    long deaths,
    long assists,
    long headshots,
    long roundsPlayed,
    long roundsWon
) {
    public static PlayerStats empty() {
        return new PlayerStats(0, 0, 0, 0, 0, 0);
    }
}
