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

import megamek.common.Compute;
import megamek.common.IEntityRemovalConditions;
import megamek.common.Roll;
import mekhq.campaign.autoResolve.damageHandler.DamageHandlerChooser;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsEngagementControlAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsEngagementControlToHitData;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsWithdrawAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.EngagementControl;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase.EndPhase;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter.AcsWithdrawReporter;

/**
 * @author Luana Coppio
 */
public class AcsWithdrawActionHandler extends AbstractAcsActionHandler {

    private final AcsWithdrawReporter reporter;

    public AcsWithdrawActionHandler(AcsWithdrawAction action, AcsGameManager gameManager) {
        super(action, gameManager);
        this.reporter = new AcsWithdrawReporter(gameManager.getGame(), this::addReport);
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
        // Reporting the start of the withdraw attempt
        reporter.reportStartWithdraw(withdrawFormation, toHit);
        // Reporting the roll
        reporter.reportWithdrawRoll(withdrawFormation, withdrawRoll);

        if (withdrawRoll.isTargetRollSuccess(11)) {
            // successful withdraw
            withdrawFormation.setDeployed(false);
            for (var unit : withdrawFormation.getUnits()) {
                for (var element : unit.getElements()) {
                    game().getEntity(element.getId()).ifPresent(entity -> {
                        entity.setDeployed(false);
                        entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
                        DamageHandlerChooser.damageRemovedEntity(entity, entity.getRemovalCondition());
                    });
                }
            }
            reporter.reportSuccessfulWithdraw();
            game().removeFormation(withdrawFormation);
        } else {
            reporter.reportFailedWithdraw();
        }
    }
}
