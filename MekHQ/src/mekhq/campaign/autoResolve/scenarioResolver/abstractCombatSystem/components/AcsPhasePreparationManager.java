package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.InGameObject;
import megamek.common.enums.GamePhase;

public record AcsPhasePreparationManager (AcsGameManager gameManager) implements AcsGameManagerHelper {

    void managePhase() {
        clearActions();
        switch (game().getPhase()) {
            case DEPLOYMENT:
            case SBF_DETECTION:
            case MOVEMENT:
            case FIRING:
                resetEntityPhase();
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
            case INITIATIVE:
                clearActions();
                break;
            case END:
            case VICTORY:
            default:
                clearReports();
                clearActions();
                break;
        }
    }

    public void resetEntityPhase() {
        for (InGameObject unit : game().getInGameObjects()) {
            if (unit instanceof AcsFormation formation) {
                formation.setDone(false);
            }
        }
    }

    private void clearActions() {
        game().clearActions();
    }

    private void clearReports() {
        gameManager.addPendingReportsToGame();
        gameManager.clearPendingReports();
    }
}
