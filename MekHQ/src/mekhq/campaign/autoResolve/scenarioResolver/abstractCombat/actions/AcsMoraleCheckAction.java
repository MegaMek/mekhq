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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions;

import megamek.common.IGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.handler.AcActionHandler;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.handler.AcMoraleCheckActionHandler;

import java.io.Serializable;

/**
 * @author Luana Coppio
 */
public class AcsMoraleCheckAction implements AcsAction, Serializable {

    private final int formationId;

    public AcsMoraleCheckAction(int formationId) {
        this.formationId = formationId;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    @Override
    public AcActionHandler getHandler(AcGameManager gameManager) {
        return new AcMoraleCheckActionHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(IGame game) {
        return false;
    }

    public boolean isIllegal() {
        return false;
    }

    @Override
    public String toString() {
        return "[AcsMoraleCheckAction]: ID: " + formationId;
    }
}

