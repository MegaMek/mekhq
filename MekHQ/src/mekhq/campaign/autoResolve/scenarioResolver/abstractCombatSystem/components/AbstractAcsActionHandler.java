package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.actions.EntityAction;

public abstract class AbstractAcsActionHandler implements AcsActionHandler {

    private final EntityAction action;
    private final AcsGameManager gameManager;
    private boolean isFinished = false;

    public AbstractAcsActionHandler(EntityAction action, AcsGameManager gameManager) {
        this.action = action;
        this.gameManager = gameManager;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void setFinished() {
        isFinished = true;
    }

    @Override
    public EntityAction getAction() {
        return action;
    }

    @Override
    public AcsGameManager gameManager() {
        return gameManager;
    }
}
