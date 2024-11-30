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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.IGame;
import megamek.common.InGameObject;
import mekhq.campaign.autoResolve.AutoResolveGame;

/**
 * @author Luana Coppio
 */
public class AcsPlayerTurn extends AcsTurn {

    /**
     * Creates a new player action turn for an SBF Game.
     *
     * @param playerId The player who has to take action
     */
    public AcsPlayerTurn(int playerId) {
        super(playerId);
    }

    @Override
    public boolean isValidEntity(InGameObject unit, IGame game) {
        return false;
    }

    @Override
    public boolean isValid(AutoResolveGame game) {
        return game.getPlayer(playerId()) != null;
    }
}
