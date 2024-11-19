package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.logging.MMLogger;

public record AcsMovementProcessor(AcsGameManager gameManager) implements AcsGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(AcsMovementProcessor.class);

    void processMovement(AcsMovePath movePath, SBFFormation formation) {
        if (!validatePermitted(movePath, formation)) {
            return;
        }

        formation.setDone(true);
        gameManager.endCurrentTurn();
    }

    private boolean validatePermitted(AcsMovePath movePath, SBFFormation formation) {
        if (!game().getPhase().isMovement()) {
            logger.error("Server got movement packet in wrong phase!");
            return false;
        } else if (movePath.isIllegal()) {
            logger.error("Illegal move path!");
            return false;
        } else if (formation.isDone()) {
            logger.error("Formation already done!");
            return false;
        }
        return true;
    }

}
