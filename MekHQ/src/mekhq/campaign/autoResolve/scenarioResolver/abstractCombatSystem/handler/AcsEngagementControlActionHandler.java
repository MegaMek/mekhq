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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.handler;

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsEngagementControlAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsEngagementControlToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.EngagementControl;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter.AcsEngagementControlReporter;

import java.util.Map;

/**
 * @author Luana Coppio
 */
public class AcsEngagementControlActionHandler extends AbstractAcsActionHandler {

    private final AcsEngagementControlReporter reporter;

    public AcsEngagementControlActionHandler(AcsEngagementControlAction action, AcsGameManager gameManager) {
        super(action, gameManager);
        this.reporter = new AcsEngagementControlReporter(gameManager.getGame(), this::addReport);
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

        // Compute To-Hit
        var toHit = AcsEngagementControlToHitData.compileToHit(game(), engagementControl);
        // Compute defender To-Hit as if roles reversed but same control
        var reverseAction = new AcsEngagementControlAction(target.getId(), attacker.getId(), engagementControl.getEngagementControl());
        var toHitDefender = AcsEngagementControlToHitData.compileToHit(game(), reverseAction);


        // Report the engagement start
        reporter.reportEngagementStart(attacker, target, engagementControl.getEngagementControl());

        // Report attacker to-hit
        reporter.reportAttackerToHitValue(toHit.getValue());

        Roll attackerRoll = Compute.rollD6(2);
        Roll defenderRoll = Compute.rollD6(2);

        // Report rolls
        reporter.reportAttackerRoll(attacker, attackerRoll);
        reporter.reportDefenderRoll(target, defenderRoll);

        var engagements = attacker.getMemory().getMemories("engagementControl");
        var targetEngagements = target.getMemory().getMemories("engagementControl");

        var attackerDelta = attackerRoll.getMarginOfSuccess(toHit);
        var defenderDelta = defenderRoll.getMarginOfSuccess(toHitDefender);

        attacker.setEngagementControl(engagementControl.getEngagementControl());
        attacker.setEngagementControlFailed(true);

        if (attackerDelta > defenderDelta) {
            attacker.setEngagementControlFailed(false);
            reporter.reportAttackerWin(attacker);

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
            // Attacker loses
            reporter.reportAttackerLose(attacker);
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
