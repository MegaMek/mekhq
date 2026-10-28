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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.Entity;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;

/**
 * Swapping the infantry heavy flamer on a Trike for a support machine gun is a short Class B refit (issue #3595). The
 * heavier gun has to come out of the Trike's tiny cargo bay, and refits used to charge a full month for any cargo bay
 * that changed size, so this refit took about 30000 minutes. Only large craft are charged a month per bay now. The
 * Trike is a pinned copy of {@code vehicles/TRO Irregulars/Smalls/Trike.blk} from mm-data; the Trike MG is the same
 * file with the machine gun and a cargo bay 0.019 tons smaller.
 */
class RefitSmallSupportVehicleTimeTest {
    private final PartsScenario scenario = PartsScenario.create();

    private static Entity loadTrike(String unitName) {
        Entity trike = MHQTestUtilities.getEntityForUnitTesting(unitName, true);
        assertNotNull(trike, unitName);
        return trike;
    }

    private Refit customRefitToMachineGun() {
        Unit trike = scenario.getCampaign().addNewUnit(loadTrike("Trike"), false, 0, PartQuality.QUALITY_D);
        return new Refit(trike, loadTrike("Trike MG"), true, false, false);
    }

    @Test
    void swappingTheTrikesInfantryWeaponIsClassB() {
        assertEquals(Refit.CLASS_B, customRefitToMachineGun().getRefitClass());
    }

    @Test
    void swappingTheTrikesInfantryWeaponTakesDaysNotWeeks() {
        int time = customRefitToMachineGun().getTime();

        // About two working days for a custom job; the cargo bay alone used to add a whole month
        assertTrue(time < Refit.WORK_WEEK, "Refit time " + time + " minutes");
    }
}
