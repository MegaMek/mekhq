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

import megamek.common.enums.GamePhase;
import megamek.server.victory.VictoryResult;
import mekhq.campaign.ai.utility.Memory;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsMoraleCheckAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsRecoveringNerveAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsWithdrawAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsGameManager;

public class EndPhase extends PhaseHandler {

    public EndPhase(AcsGameManager gameManager) {
        super(gameManager, GamePhase.END);
    }

    @Override
    protected void executePhase() {
        checkUnitDestruction();
        checkWithdrawingForces();
        checkMorale();
        checkRecoveringNerves();
        forgetEverything();
        checkVictory();
    }

    private void checkVictory() {
        if (getGameManager().checkForVictory()) {
            var game = getGameManager().getGame();
            var gameManager = getGameManager();
            VictoryResult vr = game.getVictoryResult();
            var reports = vr.processVictory(game);

            if (!reports.isEmpty()) {
                reports.forEach(gameManager::addReport);
                vr.setVictory(true);
                game.setVictoryTeam(vr.getWinningTeam());
            }
        }
    }

    private void checkUnitDestruction() {
        var allFormations = getGameManager().getGame().getActiveFormations();
        for (var formation : allFormations) {
            var destroyedUnits = formation.getUnits().stream()
                .filter(u -> u.getCurrentArmor() <= 0)
                .toList();
            if (!destroyedUnits.isEmpty()) {
                getGameManager().getGame().destroyUnits(formation, destroyedUnits);
            }
        }
    }

    private void checkWithdrawingForces() {
        var forcedWithdrawingUnits = getGameManager().getGame().getActiveFormations().stream()
            .filter(f -> f.moraleStatus() == AcsFormation.MoraleStatus.ROUTED || f.isCrippled())
            .toList();

        for (var formation : forcedWithdrawingUnits) {
            getGameManager().addWithdraw(new AcsWithdrawAction(formation.getId()), formation);
        }
    }

    private void checkMorale() {
        var formationNeedsMoraleCheck = getGameManager().getGame().getActiveFormations().stream()
            .filter(AcsFormation::hadHighStressEpisode)
            .filter(f -> f.moraleStatus() != AcsFormation.MoraleStatus.ROUTED)
            .toList();

        for (var formation : formationNeedsMoraleCheck) {
            getGameManager().addMoraleCheck(new AcsMoraleCheckAction(formation.getId()), formation);
        }
    }

    private void checkRecoveringNerves() {
        var recoveringNerves = getGameManager().getGame().getActiveFormations().stream()
            .filter(f -> f.moraleStatus().ordinal() >= AcsFormation.MoraleStatus.SHAKEN.ordinal())
            .toList();

        for (var formation : recoveringNerves) {
            getGameManager().addNerveRecovery(new AcsRecoveringNerveAction(formation.getId()), formation);
        }
    }

    private void forgetEverything() {
        var formations = getGameManager().getGame().getActiveFormations();
        formations.stream().map(AcsFormation::getMemory).forEach(Memory::clear);
        formations.forEach(AcsFormation::reset);

    }
}
