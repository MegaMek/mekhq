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

import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Warship;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit counts armor in the right amount and keeps its type: surplus Clan armor stays Clan, capital-scale armor is
 * converted to the standard points the warehouse holds, and taking armor off costs time rather than refunding it (issue
 * #10205). These are custom jobs, so the Class A time multiplier is 2.
 */
class RefitArmorAmountTest {
    private static final int CLASS_A_MULTIPLIER = 2;
    private static final int CAPITAL_TO_STANDARD_POINTS = 10;

    private final PartsScenario scenario = PartsScenario.create();

    private void complete(Refit refit) throws Exception {
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
    }

    private List<Armor> spareArmor() {
        List<Armor> spares = new ArrayList<>();
        for (Part part : scenario.getWarehouse().getParts()) {
            if ((part instanceof Armor armor) && (part.getUnit() == null)) {
                spares.add(armor);
            }
        }
        return spares;
    }

    private static Entity withArmorChange(UnitFixture fixture, int location, int pointsAdded) {
        Entity design = fixture.loadEntity();
        design.initializeArmor(design.getOArmor(location) + pointsAdded, location);
        return design;
    }

    @Test
    void takingArmorOffTakesTime() {
        Unit hoplite = scenario.withUnit(UnitFixture.HOPLITE_C);
        Refit refit = new Refit(hoplite, withArmorChange(UnitFixture.HOPLITE_C, Mek.LOC_CENTER_TORSO, -10), true, false,
              false);

        Armor centerTorsoArmor = PartsScenario.unitParts(hoplite, Armor.class).getFirst();
        int minutesPerPoint = centerTorsoArmor.getBaseTimeFor(hoplite.getEntity());
        assertEquals(10 * minutesPerPoint * CLASS_A_MULTIPLIER, refit.getTime(),
              "Taking 10 points off takes time; it does not refund the time of the points left on");
    }

    @Test
    void surplusClanArmorStaysClan() throws Exception {
        Unit hoplite = scenario.withUnit(UnitFixture.HOPLITE_C);
        Refit refit = new Refit(hoplite, withArmorChange(UnitFixture.HOPLITE_C, Mek.LOC_CENTER_TORSO, -10), true, false,
              false);

        complete(refit);

        List<Armor> spares = spareArmor();
        assertEquals(1, spares.size(), "The 10 points taken off go to the warehouse");
        Armor surplus = spares.getFirst();
        assertEquals(10, surplus.getAmount());
        assertTrue(surplus.isClanTechBase(), "Surplus Clan ferro-fibrous stays Clan: " + surplus.getName());
    }

    @Test
    void addedSupportVehicleArmorBuysOnlyTheNewPoints() {
        Unit ranger = scenario.withUnit(UnitFixture.CELLCO_RANGER_UPU_3000);
        int frontLocation = ranger.getEntity().firstArmorIndex();
        Refit refit = new Refit(ranger, withArmorChange(UnitFixture.CELLCO_RANGER_UPU_3000, frontLocation, 2), true,
              false, false);

        Armor armorSupplies = refit.getNewArmorSupplies();
        assertNotNull(armorSupplies);
        assertEquals(2, armorSupplies.getAmountNeeded(), "BAR 8 armor on BAR 8 armor is the same type");
    }

    @Test
    void addedCapitalArmorIsBoughtInStandardPoints() {
        Unit essex = scenario.withUnit(UnitFixture.ESSEX_II_WARSHIP);
        Refit refit = new Refit(essex, withArmorChange(UnitFixture.ESSEX_II_WARSHIP, Warship.LOC_NOSE, 100), true,
              false, false);

        Armor armorSupplies = refit.getNewArmorSupplies();
        assertNotNull(armorSupplies);
        assertEquals(100 * CAPITAL_TO_STANDARD_POINTS, armorSupplies.getAmountNeeded(),
              "100 capital points are 1,000 standard points");
    }

    @Test
    void removedCapitalArmorReturnsInStandardPoints() throws Exception {
        Unit essex = scenario.withUnit(UnitFixture.ESSEX_II_WARSHIP);
        Refit refit = new Refit(essex, withArmorChange(UnitFixture.ESSEX_II_WARSHIP, Warship.LOC_NOSE, -100), true,
              false, false);

        complete(refit);

        int sparePoints = 0;
        for (Armor spare : spareArmor()) {
            sparePoints += spare.getAmount();
        }
        assertEquals(100 * CAPITAL_TO_STANDARD_POINTS, sparePoints, "100 capital points come back as 1,000 points");
    }
}
