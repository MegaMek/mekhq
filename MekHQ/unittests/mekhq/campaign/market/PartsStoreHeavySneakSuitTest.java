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
package mekhq.campaign.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.InfantryArmorPart;
import mekhq.campaign.parts.missing.MissingInfantryArmorPart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A heavy sneak suit is custom infantry armor from the unit editor: damage divisor 2 with one or more sneak systems. A
 * refit to a platoon wearing one orders the armor, but the parts store never stocked it, so it could not be bought by
 * hand (MekHQ issue 5032).
 */
class PartsStoreHeavySneakSuitTest {
    private static final double HEAVY_DAMAGE_DIVISOR = 2.0;
    /** Seven mixes of camouflage, infrared baffling and ECM, each encumbering or not. */
    private static final int HEAVY_SNEAK_SUIT_COUNT = 14;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private List<InfantryArmorPart> stockedHeavySneakSuits(Campaign campaign) {
        PartsStore partsStore = new PartsStore();
        partsStore.stockHeavySneakSuits(campaign);
        return partsStore.getInventory()
                     .stream()
                     .filter(InfantryArmorPart.class::isInstance)
                     .map(InfantryArmorPart.class::cast)
                     .toList();
    }

    @Test
    void storeStocksEveryHeavySneakSuit() {
        List<InfantryArmorPart> suits = stockedHeavySneakSuits(mockCampaign());

        assertEquals(HEAVY_SNEAK_SUIT_COUNT, suits.size());
        Set<String> namesAndDetails = new HashSet<>();
        for (InfantryArmorPart suit : suits) {
            assertNull(suit.getArmorKit(), "A heavy sneak suit is custom armor, not an armor kit");
            assertEquals(HEAVY_DAMAGE_DIVISOR, suit.getDamageDivisor());
            assertEquals("Heavy Sneak Suit", suit.getName());
            namesAndDetails.add(suit.getName() + " (" + suit.getDetails() + ")");
        }
        // The store finds a part by its name and details, so each suit must read differently
        assertEquals(HEAVY_SNEAK_SUIT_COUNT, namesAndDetails.size());
    }

    /**
     * The reported case: a platoon refitted to a heavy sneak suit with camouflage needs that armor, and the suit
     * bought from the store must fill the need.
     */
    @Test
    void boughtHeavySneakSuitFillsTheRefitsNeed() {
        Campaign campaign = mockCampaign();
        MissingInfantryArmorPart neededArmor = new MissingInfantryArmorPart(0, campaign, null, HEAVY_DAMAGE_DIVISOR,
              false, false, true, false, false, false);

        boolean isInStock = false;
        for (InfantryArmorPart suit : stockedHeavySneakSuits(campaign)) {
            isInStock |= neededArmor.isAcceptableReplacement(suit, true);
        }

        assertTrue(isInStock, "The store must sell the heavy sneak suit with camouflage the refit needs");
    }
}
