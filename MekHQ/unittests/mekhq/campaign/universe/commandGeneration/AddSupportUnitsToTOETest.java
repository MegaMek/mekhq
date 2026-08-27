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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Verifies that support-vehicle placement reuses an existing sub-formation of the same type instead of
 * creating a duplicate, so a top-up generation lands in the existing formation.
 */
class AddSupportUnitsToTOETest {

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
    }

    @Test
    void reusesExistingSubFormationOfSameType() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit first = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        Unit second = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1E());

        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, List.of(first),
              SupportTOEFormationTypes.COMMISSARY_FORMATION);
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, List.of(second),
              SupportTOEFormationTypes.COMMISSARY_FORMATION);

        String label = SupportTOEFormationTypes.COMMISSARY_FORMATION.getLabel();
        long matching = campaign.getPlayerForce().getAllFormations().stream()
              .filter(formation -> formation.getName().equalsIgnoreCase(label))
              .count();
        assertEquals(1, matching, "a second support batch of the same type must reuse the existing formation");
    }
}
