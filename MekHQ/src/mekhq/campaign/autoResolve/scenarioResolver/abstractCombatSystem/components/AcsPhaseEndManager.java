package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.server.sbf.SBFGameManagerHelper;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;

import static megamek.common.enums.GamePhase.*;

public record AcsPhaseEndManager(AcsGameManager gameManager) implements AcsGameManagerHelper {

    void managePhase() {
        switch (gameManager.getGame().getPhase()) {
            case STARTING_SCENARIO:
                gameManager.changePhase(GamePhase.INITIATIVE);
                break;
            case INITIATIVE:
                gameManager.getGame().setupDeployment();
                if (gameManager.getGame().shouldDeployThisRound()) {
                    gameManager.changePhase(GamePhase.DEPLOYMENT);
                } else {
                    gameManager.changePhase(GamePhase.SBF_DETECTION);
                }
                break;
            case DEPLOYMENT:
                gameManager.getGame().clearDeploymentThisRound();
                gameManager.changePhase(GamePhase.SBF_DETECTION);
                break;
            case SBF_DETECTION:
                gameManager.actionsProcessor.handleActions();
                gameManager.changePhase(GamePhase.MOVEMENT);
                break;
            case MOVEMENT:
                gameManager.resolveCallSupport();
                gameManager.actionsProcessor.handleActions();
                gameManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
                gameManager.actionsProcessor.handleActions();
                gameManager.changePhase(GamePhase.END);
                break;
            case END:
                if (gameManager.checkForVictory()) {
                    gameManager.changePhase(GamePhase.VICTORY);
                } else {
                    gameManager.changePhase(GamePhase.INITIATIVE);
                }
                break;
            case VICTORY:
            default:
                break;
        }
    }
}
