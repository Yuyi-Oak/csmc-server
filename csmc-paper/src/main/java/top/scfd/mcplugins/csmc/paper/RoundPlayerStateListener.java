package top.scfd.mcplugins.csmc.paper;

import top.scfd.mcplugins.csmc.core.match.RoundEventListener;
import top.scfd.mcplugins.csmc.core.session.GameSession;

public final class RoundPlayerStateListener implements RoundEventListener {
    private final GameSession session;
    private final PlayerRoundStateService stateService;

    public RoundPlayerStateListener(GameSession session, PlayerRoundStateService stateService) {
        this.session = session;
        this.stateService = stateService;
    }

    @Override
    public void onRoundStart(int roundNumber) {
        stateService.onRoundStart(session);
    }
}
