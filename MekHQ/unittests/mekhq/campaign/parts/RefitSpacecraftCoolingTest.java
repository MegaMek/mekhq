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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testUtilities.parts.RefitKitPricing.expectedKitPrice;

import java.util.List;
import java.util.Map;

import megamek.common.units.Aero;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.parts.missing.MissingAeroHeatSink;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A spacecraft keeps its cooling system through a refit; the refit buys and returns only the heat sinks that change,
 * and removed ammunition bins, which are bay capacity, never linger as spares (issue #10204).
 */
class RefitSpacecraftCoolingTest {
    private PartsScenario scenario;
    private Unit leopard;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
    }

    private void complete(Refit refit) throws Exception {
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());
        refit.succeed();
    }

    private List<SpacecraftCoolingSystem> coolingSystems() {
        return PartsScenario.unitParts(leopard, SpacecraftCoolingSystem.class);
    }

    private int shotsAboard() {
        int shots = 0;
        for (LargeCraftAmmoBin ammoBin : PartsScenario.unitParts(leopard, LargeCraftAmmoBin.class)) {
            shots += ammoBin.getFullShots() - ammoBin.getShotsNeeded();
        }
        return shots;
    }

    @Test
    void aShipKeepsItsCoolingSystemAndItsAmmunition() throws Exception {
        int shotsBefore = shotsAboard();
        Refit refit = new Refit(leopard, UnitFixture.LEOPARD_DROPSHIP.loadEntity(), false, false, false);

        complete(refit);

        assertEquals(1, coolingSystems().size(), "The ship still has its cooling system");
        assertEquals(80, coolingSystems().getFirst().getTotalSinks());
        assertEquals(shotsBefore, shotsAboard(), "The bins the ship keeps hold on to their ammunition");
        assertEquals(Map.of(), PartsCensus.ofWarehouseStock(scenario.getWarehouse()),
              "No cooling system or ammunition is left behind as a spare");
    }

    @Test
    void addedHeatSinksAreBoughtAndPricedOnly() throws Exception {
        Aero moreHeatSinks = (Aero) UnitFixture.LEOPARD_DROPSHIP.loadEntity();
        moreHeatSinks.setOHeatSinks(moreHeatSinks.getOHeatSinks() + 20);
        moreHeatSinks.setHeatSinks(moreHeatSinks.getOHeatSinks());
        Refit refit = new Refit(leopard, moreHeatSinks, false, false, false);

        long heatSinksToBuy = refit.getShoppingList().stream().filter(MissingAeroHeatSink.class::isInstance).count();
        assertEquals(20, heatSinksToBuy);
        assertEquals(expectedKitPrice(refit), refit.getCost().round(), "The kit charges for the heat sinks");

        complete(refit);

        assertEquals(100, coolingSystems().getFirst().getTotalSinks());
        assertEquals(Map.of(), PartsCensus.ofWarehouseStock(scenario.getWarehouse()),
              "The bought heat sinks went into the cooling system");
    }

    @Test
    void changingHeatSinkTypeReplacesAllButTheOnesInTheEngine() throws Exception {
        // The 3056 carries 70 double heat sinks instead of 80 single ones; 24 are built into the engine either way
        Refit refit = new Refit(leopard, UnitFixture.LEOPARD_DROPSHIP_3056.loadEntity(), false, false, false);

        complete(refit);

        SpacecraftCoolingSystem coolingSystem = coolingSystems().getFirst();
        assertEquals(70, coolingSystem.getTotalSinks());
        assertEquals(Aero.HEAT_DOUBLE, coolingSystem.getSinkType());
        Map<String, Integer> stock = PartsCensus.ofWarehouseStock(scenario.getWarehouse());
        assertEquals(56, stock.get("Aero Heat Sink"), "The 56 removable single heat sinks come out as spares");
        boolean hasSpareAmmoBin = stock.keySet().stream().anyMatch(name -> name.endsWith("Ammo Bin"));
        assertFalse(hasSpareAmmoBin, "A removed spacecraft ammunition bin is not a spare");
        assertNotNull(stock.get("LRM 20 Ammo"), "The removed bins' ammunition is returned");
    }
}
