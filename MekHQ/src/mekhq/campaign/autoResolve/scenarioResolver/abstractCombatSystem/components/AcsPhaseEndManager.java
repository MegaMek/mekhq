package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.server.sbf.SBFGameManagerHelper;

import static megamek.common.enums.GamePhase.*;

public record AcsPhaseEndManager(AcsGameManager gameManager) implements AcsGameManagerHelper {

    void managePhase() {
        switch (gameManager.getGame().getPhase()) {
            case STARTING_SCENARIO:
                gameManager.addPendingReportsToGame();
                gameManager.changePhase(GamePhase.INITIATIVE);
                break;
            case INITIATIVE:
                gameManager.addPendingReportsToGame();
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
                gameManager.changePhase(GamePhase.MOVEMENT);
                break;
            case MOVEMENT:
                gameManager.detectHiddenUnits();
                gameManager.updateSpacecraftDetection();
                gameManager.detectSpacecraft();
                gameManager.applyBuildingDamage();
                gameManager.resolveCallSupport();
                gameManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
                gameManager.actionsProcessor.handleActions();
                gameManager.changePhase(GamePhase.END);
                break;
            case END:
                // // remove any entities that died in the heat/end phase before
                // // checking for victory
                // resetEntityPhase(GamePhase.END);
                // boolean victory = victory(); // note this may add reports
                // // check phase report
                // // HACK: hardcoded message ID check
                // if ((vPhaseReport.size() > 3) || ((vPhaseReport.size() > 1)
                // && (vPhaseReport.elementAt(1).messageId != 1205))) {
                // gameManager.getGame().addReports(vPhaseReport);
                // gameManager.changePhase(GamePhase.END_REPORT);
                // } else {
                // // just the heat and end headers, so we'll add
                // // the <nothing> label
                // addReport(new Report(1205, Report.PUBLIC));
                // gameManager.getGame().addReports(vPhaseReport);
                // sendReport();
                // if (victory) {
                // gameManager.changePhase(GamePhase.VICTORY);
                // } else {
                // TODO: remove this and test that after firing, no more selection in
                // firingdisplay, no more firing
                gameManager.changePhase(GamePhase.INITIATIVE);
                break;
            case VICTORY:
                // GameVictoryEvent gve = new GameVictoryEvent(this, game);
                // gameManager.getGame().processGameEvent(gve);
                // transmitGameVictoryEventToAll();
                // resetGame();
                break;
            default:
                break;
        }
    }
}
