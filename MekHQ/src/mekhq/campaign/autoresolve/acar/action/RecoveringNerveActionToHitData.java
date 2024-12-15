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

package mekhq.campaign.autoresolve.acar.action;

import megamek.common.TargetRoll;
import mekhq.campaign.autoresolve.acar.SimulationContext;
import mekhq.campaign.autoresolve.component.Formation;

public class RecoveringNerveActionToHitData extends TargetRoll {

    public RecoveringNerveActionToHitData(int value, String desc) {
        super(value, desc);
    }

    public static RecoveringNerveActionToHitData compileToHit(SimulationContext game, RecoveringNerveAction recoveringNerveAction) {
        if (recoveringNerveAction.isInvalid(game)) {
            return new RecoveringNerveActionToHitData(TargetRoll.IMPOSSIBLE, "Invalid nerve recovering");
        }
        //noinspection OptionalGetWithoutIsPresent
        var formation = game.getFormation(recoveringNerveAction.getEntityId()).get();
        RecoveringNerveActionToHitData toHit = new RecoveringNerveActionToHitData(formation.getMorale(), "Morale");
        processSkill(toHit, formation);
        return toHit;
    }

    private static void processSkill(RecoveringNerveActionToHitData toHit, Formation formation) {
        switch (formation.getSkill()) {
            case 7 -> toHit.addModifier(+2, "Wet behind the ears");
            case 6 -> toHit.addModifier(+1, "Really Green");
            case 5 -> toHit.addModifier(0, "Green");
            case 4 -> toHit.addModifier(-1, "Regular");
            case 3 -> toHit.addModifier(-2, "Veteran");
            case 2 -> toHit.addModifier(-3, "Elite");
            case 1 -> toHit.addModifier(-4, "Heroic");
            case 0 -> toHit.addModifier(-5, "Legendary");
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, "Invalid skill");
        }
    }
}
