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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem;

import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.damageHandler.DamageHandlerChooser;
import mekhq.campaign.autoResolve.helper.SetupForces;
import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase.*;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
import mekhq.campaign.mission.AtBScenario;

/**
 * @author Luana Coppio
 */
public class AcsSimpleScenarioResolver extends ScenarioResolver {

    private final AcsGameManager gameManager = new AcsGameManager();
    //    private final List<String> forceMustBePreserved = new ArrayList<>();

    public AcsSimpleScenarioResolver(AtBScenario scenario) {
        super(scenario);
    }

    private void initializeState(AutoResolveGame game) {
        gameManager.setGame(game);
        new SetupForces(game.getCampaign(), game.getUnits(), game.getScenario(), game).createForcesOnGame();
        gameManager.addPhaseHandler(new StartingScenarioPhase(gameManager));
        gameManager.addPhaseHandler(new InitiativePhase(gameManager));
        gameManager.addPhaseHandler(new DeploymentPhase(gameManager));
        gameManager.addPhaseHandler(new MovementPhase(gameManager));
        gameManager.addPhaseHandler(new FiringPhase(gameManager));
        gameManager.addPhaseHandler(new EndPhase(gameManager));
//        forceMustBePreserved.clear();
//        scenario.getScenarioObjectives().forEach(objective -> {
//            if (objective.getObjectiveCriterion().equals(ScenarioObjective.ObjectiveCriterion.Preserve)) {
//                forceMustBePreserved.addAll(objective.getAssociatedForceNames());
//            }
//        });
    }

    @Override
    public AutoResolveConcludedEvent resolveScenario(AutoResolveGame game) {
        initializeState(game);
        gameManager.runGame();
        checkDamageToEntities();

        var playerTeamWon = gameManager.getGame().getVictoryTeam() == gameManager.getGame().getCampaign().getPlayer().getTeam();

        return new AutoResolveConcludedEvent(
            playerTeamWon,
            game.getGraveyardEntities(),
            game.inGameTWEntities(),
            game);
    }

    private void checkDamageToEntities() {
        for (AcsFormation formation : gameManager.getGame().getActiveFormations()) {
            for ( var unit : formation.getUnits()) {
                if (unit.getCurrentArmor() < unit.getArmor()) {
                    for (var element : unit.getElements()) {
                        var entityOpt = gameManager.getGame().getEntity(element.getId());
                        if (entityOpt.isPresent()) {
                            var entity = entityOpt.get();
                            var percent = (double) unit.getCurrentArmor() / unit.getArmor();
                            var totalDamage = (int) (entity.getTotalArmor() * (1 - percent));
                            DamageHandlerChooser.chooseHandler(entity).applyDamageInClusters(totalDamage, 5);
                        }
                    }
                }
            }
        }
    }

}
