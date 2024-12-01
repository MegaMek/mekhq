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

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.IEntityRemovalConditions;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.EngagementControl;

/**
 * @author Luana Coppio
 */
public class AcsWithdrawActionHandler extends AbstractAcsActionHandler {

    public AcsWithdrawActionHandler(AcsWithdrawAction action, AcsGameManager gameManager) {
        super(action, gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isEnd();
    }

    @Override
    public void handle() {
        performWithdraw();
        setFinished();
    }

    private void performWithdraw() {
        // WARNING: THIS IS NOT UP TO RULES AS WRITTEN
        AcsWithdrawAction withdraw = (AcsWithdrawAction) getAction();
        if (withdraw.isIllegal()) {
            return;
        }

        var withdrawOpt = game().getFormation(withdraw.getEntityId());

        if (withdrawOpt.isEmpty()) {
            return;
        }

        var withdrawFormation = withdrawOpt.get();
        var engagementControl = new AcsEngagementControlAction(withdrawFormation.getId(), withdrawFormation.getTargetFormationId(), EngagementControl.NONE);
        var toHit = AcsEngagementControlToHitData.compileToHit(game(), engagementControl);
        if (withdrawFormation.isCrippled()) {
            toHit.addModifier(3, "Crippled");
        }
        Roll withdrawRoll = Compute.rollD6(2);

        SBFReportEntry report = new SBFReportEntry(3330).noNL()
            .add(
                new SBFFormationReportEntry(withdrawFormation.generalName(), UIUtil.hexColor(SBFInGameObjectTooltip.ownerColor(withdrawFormation, game()))).text()
            )
            .add(engagementControl.getEngagementControl().name());
        addReport(report);

        addReport(new SBFReportEntry(3331).add(toHit.getValue()).noNL());

        report = new SBFReportEntry(3332).noNL();
        report.add(new SBFPlayerNameReportEntry(game().getPlayer(withdrawFormation.getOwnerId())).text());
        report.add(new SBFRollReportEntry(withdrawRoll).noNL().text());
        addReport(report);

        if (withdrawRoll.getIntValue() == 12) {
            // successful withdraw
            withdrawFormation.setDeployed(false);
            for (var unit : withdrawFormation.getUnits()) {
                for (var element : unit.getElements()) {
                    game().getEntity(element.getId()).ifPresent(entity -> {
                        entity.setDeployed(false);
                        entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
                        game().damageEntity(entity, IEntityRemovalConditions.REMOVE_IN_RETREAT);
                    });
                }
            }
            addReport(new SBFPublicReportEntry(3333));
            game().removeFormation(withdrawFormation);
        } else {
            addReport(new SBFPublicReportEntry(3334));
        }
    }
}
