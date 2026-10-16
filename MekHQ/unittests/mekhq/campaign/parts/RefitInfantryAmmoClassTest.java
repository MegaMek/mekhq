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

import megamek.common.units.Entity;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.InfantryAmmoBin;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;

/**
 * The ammunition clip that comes with an infantry weapon on a support vehicle is carried with the weapon and takes no
 * slot, so it does not count as a separate piece of equipment for the refit class (issue #9900). Swapping the
 * Hoverbike's two support machine guns for two support lasers is a weapon-for-weapon swap. The Hoverbikes are pinned
 * copies of {@code vehicles/TRO Irregulars/Smalls/Hoverbike (MG).blk} and {@code Hoverbike (Laser).blk} from mm-data.
 */
class RefitInfantryAmmoClassTest {
    private static final String MACHINE_GUN_HOVERBIKE = "Hoverbike (MG)";
    private static final String LASER_HOVERBIKE = "Hoverbike (Laser)";

    private final PartsScenario scenario = PartsScenario.create();

    private static Entity loadHoverbike(String unitName) {
        Entity hoverbike = MHQTestUtilities.getEntityForUnitTesting(unitName, true);
        assertNotNull(hoverbike, unitName);
        return hoverbike;
    }

    private Unit machineGunHoverbike() {
        return scenario.getCampaign().addNewUnit(loadHoverbike(MACHINE_GUN_HOVERBIKE), false, 0, PartQuality.QUALITY_D);
    }

    @Test
    void swappingInfantryWeaponsIsClassB() {
        Unit hoverbike = machineGunHoverbike();

        Refit refit = new Refit(hoverbike, loadHoverbike(LASER_HOVERBIKE), false, false, false);

        assertEquals(Refit.CLASS_B, refit.getRefitClass(),
              "A laser in place of a machine gun is new equipment where old equipment came out");
    }

    @Test
    void theNewWeaponsClipsAreStillBought() {
        Unit hoverbike = machineGunHoverbike();

        Refit refit = new Refit(hoverbike, loadHoverbike(LASER_HOVERBIKE), false, false, false);

        long newClips = refit.getShoppingList().stream().filter(part -> part instanceof InfantryAmmoBin).count();
        assertEquals(2, newClips, "Each new laser still needs its ammunition");
    }
}
