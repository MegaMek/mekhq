package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.actions.EntityAction;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.logging.MMLogger;

import java.util.List;

public record AcsAttackProcessor(AcsGameManager gameManager) implements AcsGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(AcsAttackProcessor.class);

    void processAttacks(List<EntityAction> actions, SBFFormation formation) {
        if (!validatePermitted(actions, formation)) {
            return;
        }

        actions.forEach(game()::addAction);
        formation.setDone(true);
        gameManager.endCurrentTurn();
    }

    private boolean validatePermitted(List<EntityAction> actions, SBFFormation formation) {
        if (!game().getPhase().isFiring()) {
            logger.error("Server got attacks packet in wrong phase!");
            return false;
        } else if (formation.isDone()) {
            logger.error("Formation already done!");
            return false;
        } else if (AcsToHitData.targetsOfFormation(formation, game()).size() > 2) {
            logger.error("Formation targeting too many targets!");
            return false;
        }

        return true;
    }
}
