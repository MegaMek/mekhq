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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.handler;

import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsMoraleCheckAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsRecoveringNerveAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsRecoveringNerveActionToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter.AcMoraleReporter;

/**
 * @author Luana Coppio
 */
public class AcMoraleCheckActionHandler extends AbstractAcActionHandler {

    private final AcMoraleReporter reporter;

    public AcMoraleCheckActionHandler(AcsMoraleCheckAction action, AcGameManager gameManager) {
        super(action, gameManager);
        this.reporter = new AcMoraleReporter(gameManager.getGame(), this::addReport);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isEnd();
    }

    @Override
    public void handle() {
        performMoraleCheck();
        setFinished();
    }

    private void performMoraleCheck() {
        AcsMoraleCheckAction moraleCheckAction = (AcsMoraleCheckAction) getAction();
        if (moraleCheckAction.isIllegal()) {
            return;
        }

        var demoralizedOpt = game().getFormation(moraleCheckAction.getEntityId());

        if (demoralizedOpt.isEmpty()) {
            // Unit is not in the game anymore
            return;
        }

        var demoralizedFormation = demoralizedOpt.get();

        var toHit = AcsRecoveringNerveActionToHitData.compileToHit(game(), new AcsRecoveringNerveAction(demoralizedFormation.getId()));
        Roll moraleCheck = Compute.rollD6(2);

        reporter.reportMoraleCheckStart(demoralizedFormation, toHit.getValue());
        reporter.reportMoraleCheckRoll(demoralizedFormation, moraleCheck);

        if (moraleCheck.isTargetRollSuccess(toHit)) {
            // Success - no morale worsening
            reporter.reportMoraleCheckSuccess(demoralizedFormation);
        } else {
            // Failure - morale worsens
            var oldMorale = demoralizedFormation.moraleStatus();
            if (SBFFormation.MoraleStatus.values().length == oldMorale.ordinal() + 1) {
                demoralizedFormation.setMoraleStatus(SBFFormation.MoraleStatus.ROUTED);
                reporter.reportMoraleCheckFailure(demoralizedFormation, oldMorale, SBFFormation.MoraleStatus.ROUTED);
            } else {
                var newMorale = SBFFormation.MoraleStatus.values()[oldMorale.ordinal() + 1];
                demoralizedFormation.setMoraleStatus(newMorale);
                reporter.reportMoraleCheckFailure(demoralizedFormation, oldMorale, newMorale);
            }
        }
    }
}
