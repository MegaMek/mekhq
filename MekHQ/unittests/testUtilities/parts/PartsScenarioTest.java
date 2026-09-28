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
package testUtilities.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Self-tests for {@link UnitFixture} and {@link PartsScenario}: every fixture loads as the unit family it claims,
 * becomes a campaign unit with parts, and the warehouse helpers report real warehouse state.
 */
class PartsScenarioTest {
    @ParameterizedTest
    @EnumSource(UnitFixture.class)
    void fixtureLoadsAsItsUnitFamily(UnitFixture fixture) {
        Entity entity = fixture.loadEntity();

        assertTrue(fixture.getUnitFamily().isInstance(entity));
    }

    @ParameterizedTest
    @EnumSource(UnitFixture.class)
    void fixtureUnitJoinsTheCampaignWithParts(UnitFixture fixture) {
        PartsScenario scenario = PartsScenario.create();

        Unit unit = scenario.withUnit(fixture);

        assertFalse(unit.getParts().isEmpty(), "Fixture " + fixture.name() + " has no parts");
        assertTrue(scenario.getSpareParts().isEmpty(),
              "Adding fixture " + fixture.name() + " put spare parts in the warehouse");
    }

    @Test
    void refitPairsShareAChassis() {
        for (UnitFixture.RefitPair refitPair : UnitFixture.REFIT_PAIRS) {
            Entity original = refitPair.original().loadEntity();
            Entity target = refitPair.target().loadEntity();

            assertEquals(original.getChassis(), target.getChassis(),
                  "Refit pair " + refitPair.original() + " to " + refitPair.target());
            assertNotEquals(original.getModel(), target.getModel(),
                  "Refit pair " + refitPair.original() + " to " + refitPair.target() + " is the same model");
        }
    }

    @Test
    void newCampaignWarehouseIsEmpty() {
        PartsScenario scenario = PartsScenario.create();

        assertTrue(scenario.getWarehouse().getParts().isEmpty());
        assertTrue(scenario.getCampaign().getPlayerForce().getHangar().getUnits().isEmpty());
    }

    @Test
    void withUnitReturnsTheUnitJustAdded() {
        PartsScenario scenario = PartsScenario.create();

        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);

        assertNotSame(locust, wolverine);
        assertEquals("LCT-1V", locust.getEntity().getModel());
        assertEquals("WVR-6R", wolverine.getEntity().getModel());
    }

    @Test
    void installedPartsAreNotCountedAsSpares() {
        PartsScenario scenario = PartsScenario.create();
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);

        List<EnginePart> engines = PartsScenario.unitParts(locust, EnginePart.class);

        assertEquals(1, engines.size());
        assertSame(locust, engines.getFirst().getUnit());
        assertEquals(0, scenario.countSpareParts(EnginePart.class));
    }

    @Test
    void addedSpareShowsInTheWarehouseCount() {
        PartsScenario scenario = PartsScenario.create();
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Part spareEngine = PartsScenario.unitParts(locust, EnginePart.class).getFirst().clone();

        scenario.withSpare(spareEngine, 2);

        assertEquals(2, scenario.countSpareParts(EnginePart.class));
        assertEquals(1, scenario.getSpareParts().size());
    }
}
