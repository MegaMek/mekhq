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
package mekhq.campaign.unit.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.Entity;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.InfantryWeaponPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A conventional infantry platoon's equipment can be matched to its mounts (issue #4545). Each trooper's weapon is a
 * part of its own, while MegaMek gives the platoon one mount for the primary weapon, so the rifles of every trooper but
 * the first matched nothing and "Could not unscramble equipment" was logged after every refit of a platoon, such as a
 * field gun platoon trading its AC/5s for Ultra AC/5s. The Ultra AC/5 platoon is a pinned copy of
 * {@code infantry/AFFS/Field Gun Platoon (AFFS) (Motorized UAC5 3067+).blk} from mm-data.
 */
class InfantryEquipmentUnscramblerTest {
    private final PartsScenario scenario = PartsScenario.create();

    @Test
    void aFootPlatoonsEquipmentUnscrambles() {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_MG);

        assertTrue(EquipmentUnscrambler.create(platoon).unscramble().succeeded());
    }

    @Test
    void aFieldGunPlatoonsEquipmentUnscramblesAfterARefit() throws Exception {
        Unit platoon = scenario.withUnit(UnitFixture.FIELD_GUN_PLATOON_AC5);
        Entity ultraPlatoon = MHQTestUtilities.getEntityForUnitTesting(
              "Field Gun Platoon (AFFS) (Motorized UAC5 3067+)", true);
        assertNotNull(ultraPlatoon);
        Refit refit = new Refit(platoon, ultraPlatoon, false, false, false);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());

        refit.succeed();

        assertTrue(EquipmentUnscrambler.create(platoon).unscramble().succeeded());
        int ultraAutocannons = 0;
        for (Part part : platoon.getParts()) {
            if ((part instanceof EquipmentPart equipmentPart) && !(part instanceof InfantryWeaponPart)
                      && "ISUltraAC5".equals(equipmentPart.getType().getInternalName())) {
                ultraAutocannons++;
            }
        }
        assertEquals(3, ultraAutocannons, "Each field gun is now an Ultra AC/5");
    }
}
