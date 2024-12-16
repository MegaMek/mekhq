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

public class WithdrawToHitData extends TargetRoll {

    public WithdrawToHitData(int value, String desc) {
        super(value, desc);
    }

    public static WithdrawToHitData compileToHit(SimulationContext game, Formation formation) {
        var toHit = new WithdrawToHitData(formation.getTactics(), "Tactics");
        processFormationModifiers(toHit, formation);
        processMorale(toHit, formation);
        return toHit;
    }

    private static void processFormationModifiers(WithdrawToHitData toHit, Formation formation) {
        var formationIsInfantryOnly = formation.isInfantry();
        var formationIsVehicleOnly = formation.isVehicle();

        if (formationIsInfantryOnly) {
            toHit.addModifier(2, "Formation is infantry only");
        }
        if (formationIsVehicleOnly) {
            toHit.addModifier(1, "Formation is vehicle only");
        }
    }

    private static void processMorale(WithdrawToHitData toHit, Formation formation) {
        switch (formation.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(+1, "Shaken morale");
            case UNSTEADY -> toHit.addModifier(+2, "Unsteady morale");
            case BROKEN -> toHit.addModifier(+3, "Broken morale");
            case ROUTED -> toHit.addModifier(TargetRoll.AUTOMATIC_FAIL, "Routed morale");
        }
    }
}
