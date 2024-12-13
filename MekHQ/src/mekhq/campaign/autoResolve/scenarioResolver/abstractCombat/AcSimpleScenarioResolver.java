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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat;

import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.helper.SetupForces;
import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.phase.*;
import mekhq.campaign.autoResolve.scenarioResolver.component.AutoResolveConcludedEvent;
import mekhq.campaign.mission.AtBScenario;

/**
 * @author Luana Coppio
 */
public class AcSimpleScenarioResolver extends ScenarioResolver {

    private final AcGameManager gameManager = new AcGameManager();

    public AcSimpleScenarioResolver(AtBScenario scenario) {
        super(scenario);
    }

    private void initializeGameManager(AutoResolveGame game) {
        gameManager.setGame(game);
        gameManager.addPhaseHandler(new StartingScenarioPhase(gameManager));
        gameManager.addPhaseHandler(new InitiativePhase(gameManager));
        gameManager.addPhaseHandler(new DeploymentPhase(gameManager));
        gameManager.addPhaseHandler(new MovementPhase(gameManager));
        gameManager.addPhaseHandler(new FiringPhase(gameManager));
        gameManager.addPhaseHandler(new EndPhase(gameManager));
        gameManager.addPhaseHandler(new VictoryPhase(gameManager));
    }

    @Override
    public AutoResolveConcludedEvent resolveScenario(AutoResolveGame game, SetupForces setupForces) {
        setupForces.createForcesOnGame(game);
        initializeGameManager(game);
        gameManager.runGame();
        var playerTeamWon = gameManager.getGame().getVictoryTeam() == gameManager.getGame().getLocalPlayer().getTeam();

        return new AutoResolveConcludedEvent(
            playerTeamWon,
            game.getGraveyardEntities(),
            game.inGameTWEntities(),
            game);
    }
}
