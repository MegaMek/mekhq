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
import megamek.common.units.Mek;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.missing.MissingMekGyro;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;

/**
 * A Mek built without a gyro, such as one with an Interface Cockpit, has no gyro part, so a refit to one buys no gyro
 * (issue #8618). Before, the Mek carried a "None" gyro part, and refitting to it put that part on the shopping list,
 * where with some campaign options it could never be bought. The Parash and Parash 3 are pinned copies of
 * {@code meks/3085u/Cutting Edge/Parash.mtf} and {@code meks/XTRs/Republic III/Parash 3.mtf} from mm-data.
 */
class RefitNoGyroTest {
    private static final String PARASH = "Parash";
    private static final String PARASH_WITHOUT_GYRO = "Parash 3";

    private final PartsScenario scenario = PartsScenario.create();

    private static Entity loadParash(String unitName) {
        Entity parash = MHQTestUtilities.getEntityForUnitTesting(unitName, false);
        assertNotNull(parash, unitName);
        return parash;
    }

    private Unit addParash(String unitName) {
        return scenario.getCampaign().addNewUnit(loadParash(unitName), false, 0, PartQuality.QUALITY_D);
    }

    private static long gyroParts(Iterable<Part> parts) {
        long gyroParts = 0;
        for (Part part : parts) {
            if ((part instanceof MekGyro) || (part instanceof MissingMekGyro)) {
                gyroParts++;
            }
        }
        return gyroParts;
    }

    @Test
    void theTestDesignHasNoGyro() {
        assertEquals(Mek.GYRO_NONE, ((Mek) loadParash(PARASH_WITHOUT_GYRO)).getGyroType());
    }

    @Test
    void aGyroPartLeftByAnOlderVersionIsRemovedOnLoad() {
        Unit parash = addParash(PARASH_WITHOUT_GYRO);
        MekGyro noneGyro = new MekGyro(35, Mek.GYRO_NONE, 0.0, true, scenario.getCampaign());
        parash.addPart(noneGyro);
        scenario.getCampaign().getQuartermaster().addPart(noneGyro, 0, false);

        parash.initializeParts(true);

        assertEquals(0, gyroParts(parash.getParts()));
        assertEquals(0, gyroParts(scenario.getWarehouse().getParts()), "The part leaves the campaign");
    }

    @Test
    void aRefitToAMekWithoutAGyroBuysNoGyro() {
        Unit parash = addParash(PARASH);

        Refit refit = new Refit(parash, loadParash(PARASH_WITHOUT_GYRO), false, false, false);

        assertEquals(0, gyroParts(refit.getShoppingList()));
    }

    @Test
    void theOldGyroComesOutAsASpare() throws Exception {
        Unit parash = addParash(PARASH);
        Refit refit = new Refit(parash, loadParash(PARASH_WITHOUT_GYRO), false, false, false);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());

        refit.succeed();

        assertEquals(0, gyroParts(parash.getParts()), "The refitted Parash has no gyro part");
        assertEquals(1, scenario.countSpareParts(MekGyro.class), "The old gyro goes to the warehouse");
    }

    @Test
    void aRefitFromAMekWithoutAGyroStillBuysOne() {
        Unit parash = addParash(PARASH_WITHOUT_GYRO);

        Refit refit = new Refit(parash, loadParash(PARASH), false, false, false);

        long gyrosToBuy = refit.getShoppingList().stream().filter(part -> part instanceof MissingMekGyro).count();
        assertEquals(1, gyrosToBuy);
    }
}
