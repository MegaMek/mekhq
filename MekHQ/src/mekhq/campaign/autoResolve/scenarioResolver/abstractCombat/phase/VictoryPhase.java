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
import megamek.server.victory.VictoryResult;
import mekhq.campaign.autoResolve.damageHandler.DamageHandlerChooser;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter.VictoryPhaseReporter;

public class VictoryPhase extends PhaseHandler {

    private final VictoryPhaseReporter victoryPhaseReporter;

    public VictoryPhase(AcGameManager gameManager) {
        super(gameManager, GamePhase.VICTORY);
        victoryPhaseReporter = new VictoryPhaseReporter(gameManager.getGame(), gameManager::addReport);
    }

    @Override
    protected void executePhase() {
        checkVictory();
    }

    private void checkVictory() {
        // Nobody is left unscathed
        applyDamageToRemainingUnits(getGameManager());

        victoryPhaseReporter.victoryHeader();
        var game = getGameManager().getGame();
        VictoryResult vr = game.getVictoryResult();
        vr.setVictory(true);
        game.setVictoryTeam(vr.getWinningTeam());
        victoryPhaseReporter.victoryResult(getGameManager());
    }

    private static void applyDamageToRemainingUnits(AcGameManager gameManager) {
        for (AcFormation formation : gameManager.getGame().getActiveFormations()) {
            for ( var unit : formation.getUnits()) {
                if (unit.getCurrentArmor() < unit.getArmor()) {
                    for (var element : unit.getElements()) {
                        var entityOpt = gameManager.getGame().getEntity(element.getId());
                        if (entityOpt.isPresent()) {
                            var entity = entityOpt.get();
                            var percent = (double) unit.getCurrentArmor() / unit.getArmor();
                            var crits = Math.min(9, unit.getTargetingCrits() + unit.getMpCrits() + unit.getDamageCrits());
                            percent -= percent * (crits / 11.0);
                            percent = Math.min(0.95, percent);
                            var totalDamage = (int) ((entity.getTotalArmor() + entity.getTotalInternal()) * (1 - percent));
                            DamageHandlerChooser.chooseHandler(entity, DamageHandlerChooser.EntityFinalState.CREW_AND_ENTITY_MUST_SURVIVE)
                                .applyDamageInClusters(totalDamage, 5);
                        }
                    }
                }
            }
        }
    }
}
