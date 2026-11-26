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

import megamek.common.enums.TechRating;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;

/**
 * Spare support vehicle armor that older versions kept as plain armor is sold when a campaign loads, since no repair
 * or refit can use it and resupply would order its replacement anyway (issue #11171).
 */
class PlainBarArmorSaleTest {
    private static final int SPARE_POINTS = 640;

    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
    }

    private Armor plainArmor(int armorType) {
        Armor armor = new Armor(0, armorType, SPARE_POINTS, -1, false, false, campaign);
        scenario.withSpare(armor, 1);
        return armor;
    }

    private boolean isInWarehouse(Part part) {
        return scenario.getWarehouse().getParts().contains(part);
    }

    @Test
    void plainBarArmorSpareIsSoldAtItsSalePrice() {
        Armor oldBarArmor = plainArmor(EquipmentType.T_ARMOR_SV_BAR_7);
        Money salePrice = oldBarArmor.getSalePrice();
        Money balanceBefore = campaign.getPlayerForce().getFinances().getBalance();

        int soldCount = PlainBarArmorSale.sellPlainBarArmor(campaign);

        assertEquals(1, soldCount);
        assertFalse(isInWarehouse(oldBarArmor), "Nothing can use plain BAR armor, so it leaves the warehouse");
        assertEquals(balanceBefore.plus(salePrice), campaign.getPlayerForce().getFinances().getBalance(),
              "The player is paid what the Sell button would pay");
    }

    @Test
    void otherArmorIsKept() {
        Armor standardArmor = plainArmor(EquipmentType.T_ARMOR_STANDARD);
        SVArmor supportVehicleArmor = new SVArmor(7, TechRating.D, SPARE_POINTS, -1, campaign);
        scenario.withSpare(supportVehicleArmor, 1);

        int soldCount = PlainBarArmorSale.sellPlainBarArmor(campaign);

        assertEquals(0, soldCount);
        assertTrue(isInWarehouse(standardArmor), "Plain standard armor is still used by Meks and vehicles");
        assertTrue(isInWarehouse(supportVehicleArmor), "Support vehicle armor kept by BAR is the current form");
    }

    @Test
    void plainBarArmorStillInTransitIsKept() {
        Armor orderedArmor = new Armor(0, EquipmentType.T_ARMOR_SV_BAR_7, SPARE_POINTS, -1, false, false, campaign);
        campaign.getQuartermaster().addPart(orderedArmor, 5, false);

        int soldCount = PlainBarArmorSale.sellPlainBarArmor(campaign);

        assertEquals(0, soldCount, "Armor not yet delivered is not on hand to sell");
    }
}
