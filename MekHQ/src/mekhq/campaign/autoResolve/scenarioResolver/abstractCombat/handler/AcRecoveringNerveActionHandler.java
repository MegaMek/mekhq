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
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsRecoveringNerveAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsRecoveringNerveActionToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter.AcRecoveringNerveActionReporter;

/**
 * @author Luana Coppio
 */
public class AcRecoveringNerveActionHandler extends AbstractAcActionHandler {

    private final AcRecoveringNerveActionReporter report;

    public AcRecoveringNerveActionHandler(AcsRecoveringNerveAction action, AcGameManager gameManager) {
        super(action, gameManager);
        this.report = new AcRecoveringNerveActionReporter(game(), this::addReport);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isEnd();
    }

    @Override
    public void handle() {
        AcsRecoveringNerveAction recoveringNerveAction = (AcsRecoveringNerveAction) getAction();
        if (!recoveringNerveAction.isIllegal()) {
            var formationOpt = game().getFormation(recoveringNerveAction.getEntityId());
            if (formationOpt.isEmpty()) {
                setFinished();
                return;
            }

            AcFormation formation = formationOpt.get();
            if (formation.moraleStatus().ordinal() == 0) {
                setFinished();
                return;
            }
            report.reportRecoveringNerveStart(formation);
            AcsRecoveringNerveActionToHitData toHit = AcsRecoveringNerveActionToHitData.compileToHit(game(), recoveringNerveAction);
            report.reportToHitValue(toHit.getValue());
            var roll = Compute.rollD6(2);
            if (!roll.isTargetRollSuccess(toHit)) {
                report.reportSuccessRoll(roll);
                var newMoraleStatus = SBFFormation.MoraleStatus.values()[formation.moraleStatus().ordinal() -1];
                formation.setMoraleStatus(newMoraleStatus);
                report.reportMoraleStatusChange(newMoraleStatus);
            } else {
                report.reportFailureRoll(roll);
            }
        }
        setFinished();
    }
}
