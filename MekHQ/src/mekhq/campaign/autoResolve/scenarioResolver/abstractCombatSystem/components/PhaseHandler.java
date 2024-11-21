package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.enums.GamePhase;

public abstract class PhaseHandler {

    private final GamePhase phase;
    private final GamePhase nextPhase;
    private final AcsGameManager gameManager;

    public PhaseHandler(AcsGameManager gameManager, GamePhase phase, GamePhase nextPhase) {
        this.phase = phase;
        this.nextPhase = nextPhase;
        this.gameManager = gameManager;
    }

    public PhaseHandler(AcsGameManager gameManager, GamePhase phase) {
        this(gameManager, phase, null);
    }

    private boolean isPhase(GamePhase phase) {
        return this.phase == phase;
    }

    protected AcsGameManager getGameManager() {
        return gameManager;
    }

    public void execute() {
        if (this.phase.equals(gameManager.getGame().getPhase())) {
            executePhase();
            if (nextPhase != null){
                gameManager.changePhase(nextPhase);
            }
        }
    }

    protected abstract void executePhase();
}
