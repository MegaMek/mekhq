/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component;

import megamek.common.enums.GamePhase;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter.AcsPublicReportEntry;

/**
 * @author Luana Coppio
 */
public record AcsPhaseEndManager(AcsGameManager gameManager) implements AcsGameManagerHelper {

    void managePhase() {
        switch (gameManager.getGame().getPhase()) {
            case INITIATIVE:
                gameManager.addReport(new AcsPublicReportEntry(999));
                gameManager.getGame().setupDeployment();
                gameManager.resetFormationsDone();
                gameManager.flushPendingReports();
                if (gameManager.getGame().shouldDeployThisRound()) {
                    gameManager.changePhase(GamePhase.DEPLOYMENT);
                } else {
                    gameManager.changePhase(GamePhase.SBF_DETECTION);
                }
                break;
            case DEPLOYMENT:
                gameManager.addReport(new AcsPublicReportEntry(999));
                gameManager.getGame().clearDeploymentThisRound();
                phaseCleanup();
                gameManager.changePhase(GamePhase.SBF_DETECTION);
                break;
            case SBF_DETECTION:
                gameManager.actionsProcessor.handleActions();
                phaseCleanup();
                gameManager.changePhase(GamePhase.MOVEMENT);
                break;
            case MOVEMENT:
                gameManager.addReport(new AcsPublicReportEntry(999));
                gameManager.addReport(new AcsPublicReportEntry(2201));
                gameManager.actionsProcessor.handleActions();
                phaseCleanup();
                gameManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
                gameManager.addReport(new AcsPublicReportEntry(999));
                gameManager.addReport(new AcsPublicReportEntry(2002));
                gameManager.actionsProcessor.handleActions();
                phaseCleanup();
                gameManager.changePhase(GamePhase.END);
                break;
            case END:
                gameManager.addReport(new AcsPublicReportEntry(999));
                gameManager.addReport(new AcsPublicReportEntry(3335));
                gameManager.actionsProcessor.handleActions();
                phaseCleanup();
                if (gameManager.checkForVictory()) {
                    gameManager.changePhase(GamePhase.VICTORY);
                }
                break;
            case VICTORY:
            case STARTING_SCENARIO:
            default:
                break;
        }
    }

    private void phaseCleanup() {
        gameManager.resetPlayersDone();
        gameManager.resetFormations();
        gameManager.flushPendingReports();
    }
}
