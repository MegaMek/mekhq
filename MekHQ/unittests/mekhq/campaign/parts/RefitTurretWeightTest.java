/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Changing the weapons in a vehicle's turret changes the turret's weight, a tenth of the weapons it carries. Changing
 * the weight of an existing turret is Class D; only adding a new turret is Class F (CamOps p.211).
 * Before, every turret weight change was graded as a new turret, because the check for an existing turret looked for
 * it at no location.
 */
class RefitTurretWeightTest {
    private final PartsScenario scenario = PartsScenario.create();

    /** A Vedette Medium Tank with an SRM 6 added beside its AC/5, taking the turret from 1 ton to 1.5 tons. */
    private static Entity vedetteWithAnSrmInTheTurret() throws LocationFullException {
        Entity vedette = UnitFixture.VEDETTE_MEDIUM_TANK.loadEntity();
        vedette.addEquipment(EquipmentType.get("ISSRM6"), Tank.LOC_TURRET);
        return vedette;
    }

    @Test
    void changingTheWeaponsInATurretIsClassD() throws LocationFullException {
        Unit vedette = scenario.withUnit(UnitFixture.VEDETTE_MEDIUM_TANK);

        Refit refit = new Refit(vedette, vedetteWithAnSrmInTheTurret(), true, false, false);

        assertEquals(Refit.CLASS_D, refit.getRefitClass(), String.join("; ", refit.getRefitClassReasons()));
    }

    @Test
    void theBriefingNamesTheTurretWhoseWeightChanges() throws LocationFullException {
        Unit vedette = scenario.withUnit(UnitFixture.VEDETTE_MEDIUM_TANK);

        Refit refit = new Refit(vedette, vedetteWithAnSrmInTheTurret(), true, false, false);

        boolean isTurretWeightReasonGiven = refit.getRefitClassReasons()
                                                  .stream()
                                                  .anyMatch(reason -> reason.endsWith(
                                                        "Changing the weight of the turret in the Turret"));
        assertTrue(isTurretWeightReasonGiven, String.join("; ", refit.getRefitClassReasons()));
    }
}
