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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component;

import megamek.common.InGameObject;

/**
 * @author Luana Coppio
 */
public record AcPhasePreparationManager(AcGameManager gameManager) implements AcGameManagerHelper {

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
            if (unit instanceof AcFormation formation) {
                formation.setDone(false);
            }
        }
    }

    private void clearActions() {
        game().clearActions();
    }

    private void clearReports() {
        gameManager.flushPendingReports();
    }
}
