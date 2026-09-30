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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * An aerospace fighter keeps its heat sink parts through a refit, a change of heat sink type replaces the ones built
 * into the engine too, and pod heat sinks added in an OmniFighter reconfiguration count toward its time (issue
 * #10203).
 */
class RefitFighterHeatSinkTest {
    private PartsScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
    }

    private Unit refit(UnitFixture original, UnitFixture target) throws Exception {
        Unit fighter = scenario.withUnit(original);
        Refit refit = new Refit(fighter, target.loadEntity(), false, false, false);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
        return fighter;
    }

    private static SortedMap<String, Integer> heatSinkParts(Unit unit) {
        SortedMap<String, Integer> heatSinks = new TreeMap<>();
        for (Part part : unit.getParts()) {
            if (part instanceof AeroHeatSink heatSink) {
                heatSinks.merge(heatSink.getName() + (heatSink.isOmniPodded() ? " [pod]" : ""), 1, Integer::sum);
            }
        }
        return heatSinks;
    }

    @Test
    void aFighterKeepsItsHeatSinkPartsAndFreesTheOneItNoLongerNeeds() throws Exception {
        // The SL-17 has 20 heat sinks, 10 of them weight-free in the engine; the SL-17AC has 19
        Unit shilone = refit(UnitFixture.SHILONE_SL_17, UnitFixture.SHILONE_SL_17AC);

        assertEquals(heatSinkParts(scenario.withUnit(UnitFixture.SHILONE_SL_17AC)), heatSinkParts(shilone));
        assertEquals(1, PartsCensus.ofWarehouseStock(scenario.getWarehouse()).get("Aero Heat Sink"),
              "The heat sink taken out is a spare");
    }

    @Test
    void changingHeatSinkTypeReplacesTheHeatSinksInTheEngineToo() throws Exception {
        Unit shilone = refit(UnitFixture.SHILONE_SL_17, UnitFixture.SHILONE_SL_17R);

        assertEquals(heatSinkParts(scenario.withUnit(UnitFixture.SHILONE_SL_17R)), heatSinkParts(shilone));
        assertEquals(20, PartsCensus.ofWarehouseStock(scenario.getWarehouse()).get("Aero Heat Sink"),
              "All 20 single heat sinks come out, the 10 in the engine included");
    }

    @Test
    void podHeatSinksAddedInAReconfigurationCountTowardItsTime() throws Exception {
        Unit batu = scenario.withUnit(UnitFixture.BATU_PRIME);

        Refit refit = new Refit(batu, UnitFixture.BATU_D.loadEntity(), false, false, false);

        // The D changes weapons in three locations and adds two pod heat sinks in the body: four locations at 30
        // minutes each. Without the heat sinks counting, only the three weapon locations were charged (90 minutes).
        assertEquals(Refit.CLASS_OMNI, refit.getRefitClass());
        assertEquals(120, refit.getTime());
    }

    @Test
    void aReconfiguredOmniFighterHasTheNewConfigurationsPodHeatSinks() throws Exception {
        Unit batu = refit(UnitFixture.BATU_PRIME, UnitFixture.BATU_D);

        assertEquals(Map.of("Aero Double Heat Sink (Clan) [pod]", 4), heatSinkParts(batu));
    }
}
