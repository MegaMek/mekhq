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

import megamek.common.Compute;
import megamek.common.IEntityRemovalConditions;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFUnit;
import mekhq.campaign.ai.utility.Memory;
import mekhq.campaign.autoResolve.damageHandler.DamageHandlerChooser;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsMoraleCheckAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsRecoveringNerveAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsWithdrawAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter.EndPhaseReporter;

import java.util.List;

public class EndPhase extends PhaseHandler {

    private EndPhaseReporter reporter;

    public EndPhase(AcGameManager gameManager) {
        super(gameManager, GamePhase.END);
        reporter = new EndPhaseReporter(gameManager.getGame(), gameManager::addReport);
    }

    @Override
    protected void executePhase() {
        reporter.endPhaseHeader();
        checkUnitDestruction();
        checkWithdrawingForces();
        checkMorale();
        checkRecoveringNerves();
        forgetEverything();

    }

    private void checkUnitDestruction() {
        var allFormations = getGameManager().getGame().getActiveFormations();
        for (var formation : allFormations) {
            var destroyedUnits = formation.getUnits().stream()
                .filter(u -> u.getCurrentArmor() <= 0)
                .toList();
            if (!destroyedUnits.isEmpty()) {
                destroyUnits(formation, destroyedUnits);
            }
        }
    }

    private void checkWithdrawingForces() {
        if (getGameManager().checkForVictory()) {
            // If the game is over, no need to withdraw
            return;
        }
        var forcedWithdrawingUnits = getGameManager().getGame().getActiveFormations().stream()
            .filter(f -> f.moraleStatus() == AcFormation.MoraleStatus.ROUTED || f.isCrippled())
            .toList();

        for (var formation : forcedWithdrawingUnits) {
            getGameManager().addWithdraw(new AcsWithdrawAction(formation.getId()), formation);
        }
    }

    private void checkMorale() {
        var formationNeedsMoraleCheck = getGameManager().getGame().getActiveFormations().stream()
            .filter(AcFormation::hadHighStressEpisode)
            .toList();

        for (var formation : formationNeedsMoraleCheck) {
            getGameManager().addMoraleCheck(new AcsMoraleCheckAction(formation.getId()), formation);
        }
    }

    private void checkRecoveringNerves() {
        var recoveringNerves = getGameManager().getGame().getActiveFormations().stream()
            .filter(f -> f.moraleStatus().ordinal() > AcFormation.MoraleStatus.NORMAL.ordinal())
            .toList();

        for (var formation : recoveringNerves) {
            getGameManager().addNerveRecovery(new AcsRecoveringNerveAction(formation.getId()), formation);
        }
    }

    private void forgetEverything() {
        var formations = getGameManager().getGame().getActiveFormations();
        formations.stream().map(AcFormation::getMemory).forEach(Memory::clear);
        formations.forEach(AcFormation::reset);
    }

    public void destroyUnits(AcFormation formation, List<SBFUnit> destroyedUnits) {
        for (var unit : destroyedUnits) {
            for (var element : unit.getElements()) {
                var entityOpt = getGameManager().getGame().getEntity(element.getId());
                if (entityOpt.isPresent()) {
                    var entity = entityOpt.get();
                    getGameManager().getGame().addUnitToGraveyard(entity);
                    var roll = Compute.rollD6(2);
                    switch (roll.getIntValue()) {
                        case 3, 4, 10, 11 -> entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_EJECTED);
                        case 2, 12 -> entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
                        default -> entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
                    }
                    DamageHandlerChooser.damageRemovedEntity(entity, entity.getRemovalCondition());
                    reporter.reportUnitDestroyed(entity);
                }
            }

            formation.removeUnit(unit);
            if (formation.getUnits().isEmpty()) {
                getGameManager().getGame().removeFormation(formation);
            }
        }
    }
}
