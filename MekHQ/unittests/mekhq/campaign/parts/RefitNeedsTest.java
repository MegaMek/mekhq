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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * What a planned refit needs and whether the warehouse covers it, as the refit list and its Parts Needed box show it.
 * Ammunition is counted in rounds, since a refit fills its new bins from the warehouse's ammunition.
 */
class RefitNeedsTest {
    private final PartsScenario scenario = PartsScenario.create();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static List<String> describe(List<Refit.RefitNeed> needs, boolean isInStock) {
        return needs.stream().filter(need -> need.isInStock() == isInStock).map(Refit.RefitNeed::description).toList();
    }

    @Test
    void aSpareInTheWarehouseLeavesTheRestToOrder() {
        // The LCT-1E needs a Medium Laser and two Small Lasers; a spare Medium Laser is on the shelf
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        for (EquipmentPart part : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if ("Medium Laser".equals(part.getName())) {
                scenario.withSpare(part.clone(), 1);
                break;
            }
        }

        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), true, false, false);
        RefitPartsStatus partsStatus = RefitPartsStatus.of(scenario.getCampaign(), refit);

        assertEquals(List.of("Medium Laser"), describe(refit.describeNeeds(), true));
        assertEquals(List.of("2 \u00d7 Small Laser"), describe(refit.describeNeeds(), false));
        assertEquals(RefitPartsStatus.State.PARTLY_IN_STOCK, partsStatus.state());
        assertTrue(partsStatus.describe().startsWith("1 of 2 items are in the warehouse"), partsStatus.describe());
    }

    @Test
    void ammunitionIsCountedInRoundsNotBins() {
        Unit javelin = scenario.getCampaign().addNewUnit(UnitTestUtilities.getJavelinJVN10N(), false, 0,
              PartQuality.QUALITY_D);

        Refit refit = new Refit(javelin, UnitTestUtilities.getJavelinJVN10A(), true, false, false);

        List<String> toOrder = describe(refit.describeNeeds(), false);
        boolean isOrderingABin = toOrder.stream().anyMatch(item -> item.contains("Bin"));
        assertFalse(isOrderingABin, "A bin is filled from the warehouse, not ordered: " + toOrder);
        boolean isOrderingRounds = toOrder.stream().anyMatch(item -> item.contains("LRM 15") && item.contains("rounds"));
        assertTrue(isOrderingRounds, "With no LRM 15 ammunition in stock, its rounds are ordered: " + toOrder);
    }

    @Test
    void ammunitionOnTheShelfIsInStock() {
        Unit javelin = scenario.getCampaign().addNewUnit(UnitTestUtilities.getJavelinJVN10N(), false, 0,
              PartQuality.QUALITY_D);
        AmmoType lrmAmmunition = (AmmoType) EquipmentType.get("IS Ammo LRM-15");
        scenario.withSpare(new AmmoStorage(1, lrmAmmunition, 40, scenario.getCampaign()), 1);

        Refit refit = new Refit(javelin, UnitTestUtilities.getJavelinJVN10A(), true, false, false);

        boolean isAmmunitionInStock = describe(refit.describeNeeds(), true).stream()
                                            .anyMatch(item -> item.contains("LRM 15") && item.contains("rounds"));
        assertTrue(isAmmunitionInStock, "The LRM 15 rounds come from the shelf: " + refit.describeNeeds());
        boolean isOrderingAmmunition = describe(refit.describeNeeds(), false).stream()
                                             .anyMatch(item -> item.contains("rounds"));
        assertFalse(isOrderingAmmunition, "Nothing more to buy: " + refit.describeNeeds());
    }
}
