package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.enums.GamePhase;

public abstract class PhaseHandler {

    private final GamePhase phase;
    private final AcsGameManager gameManager;

    public PhaseHandler(AcsGameManager gameManager, GamePhase phase) {
        this.phase = phase;
        this.gameManager = gameManager;
    }

    private boolean isPhase(GamePhase phase) {
        return this.phase == phase;
    }

    protected AcsGameManager getGameManager() {
        return gameManager;
    }

    public void execute() {
        if (isPhase(gameManager.getGame().getPhase())) {
            executePhase();
            gameManager.endCurrentPhase();
        }
    }

    protected abstract void executePhase();
}
