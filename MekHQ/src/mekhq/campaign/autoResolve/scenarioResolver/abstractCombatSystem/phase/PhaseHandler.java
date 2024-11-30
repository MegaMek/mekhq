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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase;

import megamek.common.enums.GamePhase;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

/**
 * @author Luana Coppio
 */
public abstract class PhaseHandler {

    private final GamePhase phase;
    private final AcsGameManager gameManager;

    public PhaseHandler(AcsGameManager gameManager, GamePhase phase) {
        this.phase = phase;
        this.gameManager = gameManager;
    }

    private boolean isPhase(GamePhase phase) {
        return this.phase == phase;
    }

    protected AcsGameManager getGameManager() {
        return gameManager;
    }

    public void execute() {
        if (isPhase(gameManager.getGame().getPhase())) {
            executePhase();
        }
    }

    protected abstract void executePhase();
}
