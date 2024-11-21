package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.IGame;

import java.io.Serializable;

public class AcsEngagementControlAction implements AcsAction, Serializable {

    private final int formationId;
    private final int targetFormationId;
    private final EngagementControl engagementControl;

    public AcsEngagementControlAction(int formationId, int targetFormationId, EngagementControl engagementControl) {
        this.formationId = formationId;
        this.targetFormationId = targetFormationId;
        this.engagementControl = engagementControl;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    @Override
    public AcsActionHandler getHandler(AcsGameManager gameManager) {
        return new AcsEngagementControlActionHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(IGame game) {
        return false;
    }

    public EngagementControl getEngagementControl() {
        return engagementControl;
    }

    public int getTargetFormationId() {
        return targetFormationId;
    }

    public boolean isIllegal() {
        return false;
    }

    @Override
    public String toString() {
        return "[AcsEngagementControl]: ID: " + formationId + "; engagementControl: " + engagementControl;
    }
}

