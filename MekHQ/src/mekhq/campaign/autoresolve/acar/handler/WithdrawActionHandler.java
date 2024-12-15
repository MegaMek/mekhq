/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.autoresolve.acar.handler;

import megamek.common.Compute;
import megamek.common.IEntityRemovalConditions;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.action.EngagementControlAction;
import mekhq.campaign.autoresolve.acar.action.EngagementControlToHitData;
import mekhq.campaign.autoresolve.acar.action.WithdrawAction;
import mekhq.campaign.autoresolve.acar.report.WithdrawReporter;
import mekhq.campaign.autoresolve.component.EngagementControl;
import mekhq.campaign.unit.damage.DamageApplierChooser;

public class WithdrawActionHandler extends AbstractActionHandler {

    private final WithdrawReporter reporter;

    public WithdrawActionHandler(WithdrawAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = new WithdrawReporter(gameManager.getGame(), this::addReport);
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

    /**
     * This is not up to rules as written, the intention was to create a play experience that is more challenging and engaging.
     * The rules as written allow for a very simple withdraw mechanic that in this situation is very easy to exploit and would
     * create too many games which result in no losses.
     */
    private void performWithdraw() {
        var withdraw = (WithdrawAction) getAction();
        if (withdraw.isInvalid(game())) {
            return;
        }

        var withdrawOpt = game().getFormation(withdraw.getEntityId());

        if (withdrawOpt.isEmpty()) {
            return;
        }

        var withdrawFormation = withdrawOpt.get();
        var engagementControl = new EngagementControlAction(withdrawFormation.getId(), withdrawFormation.getTargetFormationId(), EngagementControl.NONE);
        var toHit = EngagementControlToHitData.compileToHit(game(), engagementControl);
        if (withdrawFormation.isCrippled()) {
            toHit.addModifier(3, "Crippled");
        }

        var withdrawRoll = Compute.rollD6(2);
        // Reporting the start of the withdrawal attempt
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
                        DamageApplierChooser.damageRemovedEntity(entity, entity.getRemovalCondition());
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
