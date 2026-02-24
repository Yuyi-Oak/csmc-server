package top.scfd.mcplugins.csmc.paper;

public final class SessionCombatStats {
    private int kills;
    private int deaths;
    private int assists;
    private int headshots;

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public int assists() {
        return assists;
    }

    public int headshots() {
        return headshots;
    }

    public void recordKill() {
        kills++;
    }

    public void recordDeath() {
        deaths++;
    }

    public void recordAssist() {
        assists++;
    }

    public void recordHeadshot() {
        headshots++;
    }
}
