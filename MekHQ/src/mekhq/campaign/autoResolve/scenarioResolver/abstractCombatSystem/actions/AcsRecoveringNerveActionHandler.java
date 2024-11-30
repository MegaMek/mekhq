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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions;

import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

/**
 * @author Luana Coppio
 */
public class AcsRecoveringNerveActionHandler extends AbstractAcsActionHandler {

    public AcsRecoveringNerveActionHandler(AcsRecoveringNerveAction action, AcsGameManager gameManager) {
        super(action, gameManager);
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
            SBFFormation formation = formationOpt.get();

            // Process Engagement Controll roll in here
            AcsRecoveringNerveActionToHitData toHit = AcsRecoveringNerveActionToHitData.compileToHit(game(), recoveringNerveAction);

//            AcsEngagementControlToHitData toHit = AcsEngagementControlToHitData.compileToHit(game(), engagementControl);
//            SBFReportEntry report = new SBFReportEntry(2001).noNL();
//            report.add(new SBFUnitReportEntry(attacker, -1, ownerColor(attacker, game())).text());
//            report.add(new SBFFormationReportEntry(
//                target.generalName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(target, game()))).text());
//            addReport(report);
//            // TODO : Change everything from here and down!
//            if (toHit.cannotSucceed()) {
//                addReport(new SBFReportEntry(2010).add(toHit.getDesc()));
//            } else {
//                addReport(new SBFReportEntry(2003).add(toHit.getValue()).noNL());
//                Roll roll = Compute.rollD6(2);
//                report = new SBFReportEntry(2020).noNL();
//                report.add(new SBFPlayerNameReportEntry(game().getPlayer(attacker.getOwnerId())).text());
//                report.add(new SBFRollReportEntry(roll).noNL().text());
//                addReport(report);
//            }
        }
        setFinished();
    }
}
