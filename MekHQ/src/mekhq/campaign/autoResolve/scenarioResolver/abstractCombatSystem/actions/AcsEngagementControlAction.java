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

import megamek.common.IGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.EngagementControl;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.handler.AcsActionHandler;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.handler.AcsEngagementControlActionHandler;

import java.io.Serializable;

/**
 * @author Luana Coppio
 */
public class AcsEngagementControlAction implements AcsAction, Serializable {

    private final int formationId;
    private final int targetFormationId;
    private final EngagementControl engagementControl;

    public AcsEngagementControlAction(int formationId, int targetFormationId, EngagementControl engagementControl) {
        this.formationId = formationId;
        this.targetFormationId = targetFormationId;
        this.engagementControl = engagementControl;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    @Override
    public AcsActionHandler getHandler(AcsGameManager gameManager) {
        return new AcsEngagementControlActionHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(IGame game) {
        return false;
    }

    public EngagementControl getEngagementControl() {
        return engagementControl;
    }

    public int getTargetFormationId() {
        return targetFormationId;
    }

    public boolean isIllegal() {
        return false;
    }

    @Override
    public String toString() {
        return "[AcsEngagementControl]: ID: " + formationId + "; engagementControl: " + engagementControl;
    }
}

