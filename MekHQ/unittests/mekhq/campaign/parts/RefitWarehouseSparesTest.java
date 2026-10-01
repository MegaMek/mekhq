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

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Refits use the spares in the warehouse whatever their quality (issue #2714), and a refit kit added by the GM puts
 * its parts in the warehouse so the refit can go ahead (issue #3411). Both were fixed along the way; these tests keep
 * them fixed.
 */
class RefitWarehouseSparesTest {
    private PartsScenario scenario;

    /**
     * Stocks two spares of a part taken from a donor unit, one at quality B and one at quality C, then removes the
     * donor so only the spares remain.
     */
    private void stockTwoSparesOfMixedQuality(UnitFixture donorFixture, String partName) {
        Unit donor = scenario.withUnit(donorFixture);
        Part sourcePart = null;
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(donor, EquipmentPart.class)) {
            if (partName.equals(equipmentPart.getName())) {
                sourcePart = equipmentPart;
            }
        }
        assertNotNull(sourcePart, donorFixture + " carries a " + partName);
        Part qualityBSpare = sourcePart.clone();
        qualityBSpare.setQuality(PartQuality.QUALITY_B);
        Part qualityCSpare = sourcePart.clone();
        qualityCSpare.setQuality(PartQuality.QUALITY_C);
        scenario.withSpare(qualityBSpare, 1);
        scenario.withSpare(qualityCSpare, 1);
        scenario.getCampaign().getPlayerForce().getHangar().removeUnit(donor.getId());
    }

    private static int countOnShoppingList(Refit refit, String partName) {
        int count = 0;
        for (Part part : refit.getShoppingList()) {
            if (partName.equals(part.getName())) {
                count += part.getQuantity();
            }
        }
        return count;
    }

    @Test
    void aRefitUsesTwoSparesOfDifferentQuality() throws Exception {
        scenario = PartsScenario.create();
        stockTwoSparesOfMixedQuality(UnitFixture.LOCUST_LCT_1E, "Small Laser");
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);

        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), false, false, false);

        assertEquals(0, countOnShoppingList(refit, "Small Laser"),
              "The LCT-1E's two Small Lasers come from the quality B and quality C spares");
    }

    @Test
    void anOmniReconfigurationUsesTwoPodSparesOfDifferentQuality() throws Exception {
        scenario = PartsScenario.create();
        stockTwoSparesOfMixedQuality(UnitFixture.EPONA_PURSUIT_TANK_A, "ER Medium Laser");
        Unit epona = scenario.withUnit(UnitFixture.EPONA_PURSUIT_TANK_PRIME);

        Refit refit = new Refit(epona, UnitFixture.EPONA_PURSUIT_TANK_A.loadEntity(), false, false, false);

        assertEquals(0, countOnShoppingList(refit, "ER Medium Laser"),
              "The Epona A's two pod-mounted ER Medium Lasers come from the two spares");
    }

    @Test
    void aRefitKitAddedByTheGmLetsTheRefitGoAhead() throws Exception {
        scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6M);
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6R.loadEntity(), false, false, false);
        refit.begin();

        // What the procurement list's GM "add one item" does with the refit kit
        campaign.getQuartermaster().addPart((Part) refit.getNewEquipment(), 0, true);
        refit.decrementQuantity();

        assertTrue(refit.kitFound());
        assertTrue(refit.getShoppingList().isEmpty(), "Nothing is left to buy");
        assertTrue(refit.acquireParts(), "The refit has every part it needs");
    }
}
