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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.phase;

import megamek.common.enums.GamePhase;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;

/**
 * @author Luana Coppio
 */
public abstract class PhaseHandler {

    private final GamePhase phase;
    private final AcGameManager gameManager;

    public PhaseHandler(AcGameManager gameManager, GamePhase phase) {
        this.phase = phase;
        this.gameManager = gameManager;
    }

    private boolean isPhase(GamePhase phase) {
        return this.phase == phase;
    }

    protected AcGameManager getGameManager() {
        return gameManager;
    }

    public void execute() {
        if (isPhase(gameManager.getGame().getPhase())) {
            executePhase();
        }
    }

    protected abstract void executePhase();
}
