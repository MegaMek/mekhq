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

import megamek.common.actions.EntityAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

/**
 * @author Luana Coppio
 */
public abstract class AbstractAcsActionHandler implements AcsActionHandler {

    private final EntityAction action;
    private final AcsGameManager gameManager;
    private boolean isFinished = false;

    public AbstractAcsActionHandler(EntityAction action, AcsGameManager gameManager) {
        this.action = action;
        this.gameManager = gameManager;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void setFinished() {
        isFinished = true;
    }

    @Override
    public EntityAction getAction() {
        return action;
    }

    @Override
    public AcsGameManager gameManager() {
        return gameManager;
    }
}
