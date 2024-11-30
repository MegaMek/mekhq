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
import megamek.common.actions.EntityAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

/**
 * @author Luana Coppio
 */
public interface AcsAction extends EntityAction {

    /**
     * @return A handler that will process this action as an extension of the SBFGameManager
     */
    AcsActionHandler getHandler(AcsGameManager gameManager);

    /**
     * Validates the data of this action. Validation should not check game rule details, only if the action
     * can be handled without running into missing or false data (NullPointerExceptions). Errors should
     * be logged. The action will typically be ignored and removed if validation fails.
     *
     * @param game The game
     * @return true when this action is valid, false otherwise
     */
    boolean isDataValid(IGame game);
}

