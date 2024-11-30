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

import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

/**
 * @author Luana Coppio
 */
public class AcsMoraleCheckActionHandler extends AbstractAcsActionHandler {

    public AcsMoraleCheckActionHandler(AcsMoraleCheckAction action, AcsGameManager gameManager) {
        super(action, gameManager);
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

        if (!moraleCheck.isTargetRollSuccess(toHit)) {
            var currentMorale = demoralizedFormation.moraleStatus();
            if (SBFFormation.MoraleStatus.values().length == currentMorale.ordinal() + 1) {
                demoralizedFormation.setMoraleStatus(SBFFormation.MoraleStatus.ROUTED);
            } else {
                demoralizedFormation.setMoraleStatus(SBFFormation.MoraleStatus.values()[currentMorale.ordinal() + 1]);
            }
        }
    }
}
