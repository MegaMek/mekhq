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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase;

import megamek.common.MapSettings;
import megamek.common.enums.GamePhase;
import megamek.server.ServerBoardHelper;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter.StartingScenarioReporter;

import java.util.HashMap;

/**
 * @author Luana Coppio
 */
public class StartingScenarioPhase extends PhaseHandler {

    private final StartingScenarioReporter reporter;

    public StartingScenarioPhase(AcsGameManager gameManager) {
        super(gameManager, GamePhase.STARTING_SCENARIO);
        this.reporter = new StartingScenarioReporter(gameManager.getGame(), gameManager::addReport);
    }

    @Override
    protected void executePhase() {
        getGameManager().resetPlayersDone();
        getGameManager().resetFormations();
        getGameManager().calculatePlayerInitialCounts();
        getGameManager().getGame().setupTeams();
        getGameManager().getGame().getPlanetaryConditions().determineWind();
        getGameManager().getGame().setupDeployment();
        getGameManager().getGame().setVictoryContext(new HashMap<>());
        MapSettings mapSettings = getGameManager().getGame().getMapSettings();
        mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));
        reporter.logHeader();
    }
}
