/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions;

import megamek.common.IGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.EngagementControl;

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

