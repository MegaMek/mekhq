package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.logging.MMLogger;

public record AcsRecoveringNerveProcessor(AcsGameManager gameManager) implements AcsGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(AcsRecoveringNerveProcessor.class);

    void processRecoveringNerve(AcsRecoveringNerveAction recoveringNerveAction, SBFFormation formation) {
        if (!validatePermitted(recoveringNerveAction, formation)) {
            return;
        }
        game().addAction(recoveringNerveAction);
        formation.setDone(true);
        gameManager.endCurrentTurn();
    }

    private boolean validatePermitted(AcsRecoveringNerveAction recoveringNerveAction, SBFFormation formation) {
        if (!game().getPhase().isEnd()) {
            logger.error("Server got movement packet in wrong phase!");
            return false;
        } else if (recoveringNerveAction.isIllegal()) {
            logger.error("Illegal move path!");
            return false;
        } else if (formation.isDone()) {
            logger.error("Formation already done!");
            return false;
        }
        return true;
    }

}
