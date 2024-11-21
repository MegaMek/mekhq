package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.actions.EntityAction;

public record AcsActionsProcessor(AcsGameManager gameManager) implements AcsGameManagerHelper {

    void handleActions() {
        addNewHandlers();
        processHandlers();
        removeFinishedHandlers();
    }

    private void addNewHandlers() {
        for (EntityAction action : game().getActionsVector()) {
            if (action instanceof AcsAttackAction attack && attack.getHandler(gameManager) != null) {
                game().addActionHandler(attack.getHandler(gameManager));
            } else if (action instanceof AcsEngagementControlAction engagementControl && engagementControl.getHandler(gameManager) != null) {
                game().addActionHandler(engagementControl.getHandler(gameManager));
            }
        }
    }

    private void processHandlers() {
        for (AcsActionHandler handler : game().getActionHandlers()) {
            if (handler.cares()) {
                handler.handle();
            }
        }
    }

    private void removeFinishedHandlers() {
        game().getActionHandlers().removeIf(AcsActionHandler::isFinished);
    }
}
