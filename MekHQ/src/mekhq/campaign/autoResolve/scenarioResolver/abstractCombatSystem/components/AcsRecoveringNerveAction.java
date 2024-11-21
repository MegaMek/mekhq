package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.IGame;

import java.io.Serializable;

public class AcsRecoveringNerveAction implements AcsAction, Serializable {

    private final int formationId;

    public AcsRecoveringNerveAction(int formationId) {
        this.formationId = formationId;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    @Override
    public AcsActionHandler getHandler(AcsGameManager gameManager) {
        return new AcsRecoveringNerveActionHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(IGame game) {
        return false;
    }

    public boolean isIllegal() {
        return false;
    }

    @Override
    public String toString() {
        return "[AcsEngagementControl]: ID: " + formationId;
    }
}

