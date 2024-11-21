package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.logging.MMLogger;

public record AcsEngagementControlProcessor(AcsGameManager gameManager) implements AcsGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(AcsEngagementControlProcessor.class);

    void processEngagementControl(AcsEngagementControlAction engagementControl, SBFFormation formation) {
        if (!validatePermitted(engagementControl, formation)) {
            return;
        }
        game().addAction(engagementControl);
        formation.setDone(true);
        gameManager.endCurrentTurn();
    }

    private boolean validatePermitted(AcsEngagementControlAction engagementControl, SBFFormation formation) {
        if (!game().getPhase().isMovement()) {
            logger.error("Server got movement packet in wrong phase!");
            return false;
        } else if (engagementControl.isIllegal()) {
            logger.error("Illegal move path!");
            return false;
        } else if (formation.isDone()) {
            logger.error("Formation already done!");
            return false;
        }
        return true;
    }

}
