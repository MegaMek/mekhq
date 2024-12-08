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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions;

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.EngagementControl;

import java.util.Map;

/**
 * @author Luana Coppio
 */
public class AcsEngagementControlActionHandler extends AbstractAcsActionHandler {

    public AcsEngagementControlActionHandler(AcsEngagementControlAction action, AcsGameManager gameManager) {
        super(action, gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isMovement();
    }

    @Override
    public void handle() {
        performEngagementControl();
        setFinished();
    }

    /**
     * Perform the engagement control action
     * This changes a little bit the "status quo" during the round, making units do or take less damage or evade specifically one unit
     */
    private void performEngagementControl() {
        // WARNING: THIS IS NOT UP TO RULES AS WRITTEN
        AcsEngagementControlAction engagementControl = (AcsEngagementControlAction) getAction();
        if (engagementControl.isIllegal()) {
            return;
        }
        var attackerOpt = game().getFormation(engagementControl.getEntityId());
        var targetOpt = game().getFormation(engagementControl.getTargetFormationId());
        if (attackerOpt.isEmpty() || targetOpt.isEmpty()) {
            return;
        }
        var attacker = attackerOpt.get();
        if (attacker.getEngagementControl() != null) {
            // the attacker is already tied down in an engagement and control
            return;
        }

        if (engagementControl.getEngagementControl().equals(EngagementControl.NONE)) {
            attacker.setEngagementControl(EngagementControl.NONE);
            return;
        }

        var target = targetOpt.get();
        var toHit = AcsEngagementControlToHitData.compileToHit(game(), engagementControl);
        var toHitDefender = AcsEngagementControlToHitData.compileToHit(game(), new AcsEngagementControlAction(target.getId(), attacker.getId(), engagementControl.getEngagementControl()));

        SBFReportEntry report = new SBFReportEntry(2200).noNL()
            .add(
                new SBFFormationReportEntry(attacker.generalName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(attacker, game()))).text()
            )
            .add(
                new SBFFormationReportEntry(target.generalName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(target, game()))).text()
            )
            .add(engagementControl.getEngagementControl().name());
        addReport(report);

        addReport(new SBFReportEntry(2203).add(toHit.getValue()).noNL());
        Roll attackerRoll = Compute.rollD6(2);
        Roll defenderRoll = Compute.rollD6(2);

        report = new SBFReportEntry(2202).noNL();
        report.add(new SBFPlayerNameReportEntry(game().getPlayer(attacker.getOwnerId())).text());
        report.add(new SBFRollReportEntry(attackerRoll).noNL().text());
        addReport(report);

        report = new SBFReportEntry(2202).noNL();
        report.add(new SBFPlayerNameReportEntry(game().getPlayer(target.getOwnerId())).text());
        report.add(new SBFRollReportEntry(defenderRoll).noNL().text());
        addReport(report);

        var engagements = attacker.getMemory().getMemories("engagementControl");
        var targetEngagements = target.getMemory().getMemories("engagementControl");

        var attackerDelta = attackerRoll.getMarginOfSuccess(toHit);
        var defenderDelta = defenderRoll.getMarginOfSuccess(toHitDefender);

        attacker.setEngagementControl(engagementControl.getEngagementControl());
        attacker.setEngagementControlFailed(true);

        if (attackerDelta > defenderDelta) {
            attacker.setEngagementControlFailed(false);
            addReport(
                new SBFReportEntry(2204).noNL()
                    .add(new SBFPlayerNameReportEntry(game().getPlayer(attacker.getOwnerId())).text())
            );
            switch (engagementControl.getEngagementControl()) {
                case NONE:
                    attacker.setEngagementControl(EngagementControl.NONE);
                    break;
                case FORCED_ENGAGEMENT:
                case EVADE:
                case OVERRUN:
                case STANDARD:
                    attacker.setTargetFormationId(target.getId());
                    target.setEngagementControl(engagementControl.getEngagementControl());
                    // Adding memory, so the unit can remember that it is engaged with the target
                    engagements.add(Map.of(
                        "targetFormationId", attacker.getId(),
                        "wonEngagementControl", false,
                        "attacker", true,
                        "engagementControl", engagementControl.getEngagementControl()
                    ));
                    // Adding memory, so the unit can remember that it is engaged with the attacker
                    targetEngagements.add(Map.of(
                        "targetFormationId", attacker.getId(),
                        "wonEngagementControl", false,
                        "attacker", false,
                        "engagementControl", engagementControl.getEngagementControl()
                    ));
            }
        } else {
            addReport(new SBFPublicReportEntry(2205));
            // Adding memory, so the unit can remember that it is engaged with the target
            engagements.add(Map.of(
                "targetFormationId", attacker.getId(),
                "wonEngagementControl", false,
                "attacker", true,
                "engagementControl", engagementControl.getEngagementControl()
            ));
            // Adding memory, so the unit can remember that it is engaged with the attacker
            targetEngagements.add(Map.of(
                "targetFormationId", attacker.getId(),
                "wonEngagementControl", false,
                "attacker", false,
                "engagementControl", engagementControl.getEngagementControl()
            ));
        }


    }

}
