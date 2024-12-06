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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions;

import megamek.common.actions.EntityAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManagerHelper;

/**
 * @author Luana Coppio
 */
public record AcsActionsProcessor(AcsGameManager gameManager) implements AcsGameManagerHelper {

    public void handleActions() {
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
            } else if (action instanceof AcsWithdrawAction withdraw && withdraw.getHandler(gameManager) != null) {
                game().addActionHandler(withdraw.getHandler(gameManager));
            } else if (action instanceof AcsRecoveringNerveAction recoveringNerve && recoveringNerve.getHandler(gameManager) != null) {
                game().addActionHandler(recoveringNerve.getHandler(gameManager));
            } else if (action instanceof AcsMoraleCheckAction moraleCheck && moraleCheck.getHandler(gameManager) != null) {
                game().addActionHandler(moraleCheck.getHandler(gameManager));
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
