package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.MegaMek;
import megamek.common.InGameObject;
import megamek.common.MapSettings;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.logging.MMLogger;
import megamek.server.ServerBoardHelper;

public record AcsPhasePreparationManager (AcsGameManager gameManager) implements AcsGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(AcsPhasePreparationManager.class);

    void managePhase() {
        clearActions();
        switch (game().getPhase()) {
            case STARTING_SCENARIO:
                gameManager.clearPendingReports();
                MapSettings mapSettings = game().getMapSettings();
                mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));
                break;
            case INITIATIVE:
                game().clearActions();
                gameManager.clearPendingReports();
                resetEntityPhase(game().getPhase());
                gameManager.resetPlayersDone();
                gameManager.rollInitiative();
                gameManager.incrementAndSendGameRound();
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                gameManager.initiativeHelper.writeInitiativeReport();
                logger.info("Round {} memory usage: {}",
                    game().getCurrentRound(), MegaMek.getMemoryUsed());
                break;
            case SBF_DETECTION:
            case MOVEMENT:
            case FIRING:
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                break;
            case END:
            case VICTORY:
                gameManager.addPendingReportsToGame();
                gameManager.clearPendingReports();
                break;
            default:
                break;
        }
    }

    private void resetEntityPhase(GamePhase phase) {
        for (InGameObject unit : game().getInGameObjects()) {
            if (unit instanceof SBFFormation formation) {
                formation.setDone(false);
            }
        }
    }

    private void clearActions() {
        game().clearActions();
    }
}
