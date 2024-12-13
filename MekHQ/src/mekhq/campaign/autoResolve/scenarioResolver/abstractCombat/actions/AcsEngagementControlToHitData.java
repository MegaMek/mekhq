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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions;

import megamek.common.TargetRoll;
import mekhq.campaign.autoResolve.AutoResolveGame;

/**
 * @author Luana Coppio
 */
public class AcsEngagementControlToHitData extends TargetRoll {

    public AcsEngagementControlToHitData(int value, String desc) {
        super(value, desc);
    }

    public static AcsEngagementControlToHitData compileToHit(AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        if (engagementControl.isIllegal()) {
            return new AcsEngagementControlToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }
        var attackingFormationOpt = game.getFormation(engagementControl.getEntityId());
        if (attackingFormationOpt.isEmpty()) {
            return new AcsEngagementControlToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        var attackingFormation = attackingFormationOpt.get();
        var toHit = new AcsEngagementControlToHitData(attackingFormation.getTactics(), "Tactics");
        processFormationModifiers(toHit, game, engagementControl);
        processMorale(toHit, game, engagementControl);
        processSupply(toHit);
        processEngagementAndControlChosen(toHit, game, engagementControl);
        processSizeDifference(toHit, game, engagementControl);
        return toHit;
    }

    private static void processEngagementAndControlChosen(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        switch (engagementControl.getEngagementControl()) {
            case FORCED_ENGAGEMENT:
                toHit.addModifier(-3, "force engagement");
                break;
            case EVADE:
                toHit.addModifier(-3, "evade");
                break;
            case OVERRUN:
                processSizeDifference(toHit, game, engagementControl);
                break;
            default:
                break;
        }
    }

    private static void processSupply(AcsEngagementControlToHitData toHit) {
        var formationHasSupply = true;
        if (!formationHasSupply) {
            toHit.addModifier(4, "No Supply");
        }
    }

    private static void processFormationModifiers(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        var formationOpt = game.getFormation(engagementControl.getEntityId());
        if (formationOpt.isEmpty()) {
            return;
        }
        var formation = formationOpt.get();

        var formationIsInfantryOnly = formation.isInfantry();
        var formationIsVehicleOnly = formation.isVehicle();

        if (formationIsInfantryOnly) {
            toHit.addModifier(2, "infantry only");
        }
        if (formationIsVehicleOnly) {
            toHit.addModifier(1, "formation is vehicle only");
        }
    }

    private static void processSizeDifference(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        var attackerOpt = game.getFormation(engagementControl.getEntityId());
        var targetOpt = game.getFormation(engagementControl.getTargetFormationId());
        if (attackerOpt.isEmpty() || targetOpt.isEmpty()) {
            return;
        }
        int sizeDifference = attackerOpt.get().getSize() - targetOpt.get().getSize();
        toHit.addModifier(sizeDifference, "Overrun - Size Difference");
    }

    private static void processMorale(AcsEngagementControlToHitData toHit, AutoResolveGame game, AcsEngagementControlAction engagementControl) {
        var targetOpt = game.getFormation(engagementControl.getTargetFormationId());
        if (targetOpt.isEmpty()) {
            return;
        }
        switch (targetOpt.get().moraleStatus()) {
            case SHAKEN -> toHit.addModifier(+1, "shaken");
            case UNSTEADY -> toHit.addModifier(+2, "unsteady");
            case BROKEN -> toHit.addModifier(+3, "broken");
            case ROUTED -> toHit.addModifier(TargetRoll.AUTOMATIC_FAIL, "routed");
        }
    }
}
