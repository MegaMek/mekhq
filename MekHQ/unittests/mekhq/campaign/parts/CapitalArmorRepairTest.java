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

import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A WarShip counts its armor in capital points and the warehouse counts armor in standard points, ten to one. Supply,
 * time and consumption must all convert between the two (issue #10212).
 */
class CapitalArmorRepairTest {
    private static final int CAPITAL_POINTS_LOST = 5;
    private static final int STANDARD_POINTS_IN_STOCK = 25;
    /** Twenty-five standard points cover two whole capital points. */
    private static final int CAPITAL_POINTS_THE_STOCK_COVERS = 2;
    private static final int CAPITAL_ARMOR_MINUTES_PER_POINT = 120;

    private Entity warship;
    private Armor armor;
    private int originalArmor;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        Unit essex = scenario.withUnit(UnitFixture.ESSEX_II_WARSHIP);
        warship = essex.getEntity();
        assertTrue(warship.isCapitalScale());
        armor = PartsScenario.unitParts(essex, Armor.class).getFirst();
        originalArmor = warship.getOArmor(armor.getLocation(), armor.isRearMounted());
        warship.setArmor(originalArmor - CAPITAL_POINTS_LOST, armor.getLocation(), armor.isRearMounted());
        armor.updateConditionFromEntity(false);
        assertEquals(CAPITAL_POINTS_LOST, armor.getAmountNeeded(), "Five capital points, fifty standard points");

        Armor spareArmor = armor.clone();
        spareArmor.setAmount(STANDARD_POINTS_IN_STOCK);
        scenario.withSpare(spareArmor, 1);
        assertEquals(STANDARD_POINTS_IN_STOCK, armor.getAmountAvailable());
    }

    @Test
    void twentyFiveStandardPointsAreNotEnoughForFiveCapitalPoints() {
        assertFalse(armor.isInSupply());
        assertFalse(armor.isEnoughSpareArmorAvailable());
    }

    @Test
    void theRepairTakesTimeOnlyForTheCapitalPointsTheStockCovers() {
        assertEquals(CAPITAL_POINTS_THE_STOCK_COVERS * CAPITAL_ARMOR_MINUTES_PER_POINT, armor.getBaseTime());
    }

    @Test
    void theRepairUsesOnlyTheStockForThePointsItPutsOn() {
        armor.fix();

        assertEquals(originalArmor - CAPITAL_POINTS_LOST + CAPITAL_POINTS_THE_STOCK_COVERS,
              warship.getArmorForReal(armor.getLocation(), armor.isRearMounted()));
        assertEquals(STANDARD_POINTS_IN_STOCK - (CAPITAL_POINTS_THE_STOCK_COVERS * 10), armor.getAmountAvailable(),
              "Two capital points use twenty standard points; the other five stay in stock");
    }
}
