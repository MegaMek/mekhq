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

import megamek.common.bays.Bay;
import megamek.common.bays.CargoBay;
import megamek.common.units.Entity;
import mekhq.campaign.parts.missing.MissingBayDoor;
import mekhq.campaign.parts.missing.MissingCubicle;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Refit work on cargo and transport bays follows CO p. 206: cubicles and doors carry their own time and a changed cargo
 * bay takes a month. The work is graded by refit class as CO p. 211 grades any other component, and a refit that changes
 * no bay takes no bay work at all (issue #10204). Refit kits are not available for DropShips, so these are custom jobs
 * and the full class multiplier applies.
 */
class RefitBayTest {
    private static final int SEVEN_DAYS = 7 * 480;
    private static final int TEN_HOURS = 10 * Refit.WORK_HOUR;
    private static final int CLASS_B_MULTIPLIER = 3;
    private static final int CLASS_C_MULTIPLIER = 5;

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

    private int bayParts() {
        return PartsScenario.unitParts(leopard, TransportBayPart.class).size();
    }

    /**
     * @return the Leopard design with its 34-ton cargo bay replaced by one of the given size and doors
     */
    private static Entity leopardWithCargoBay(double tons, int doors) throws Exception {
        Entity leopardDesign = UnitFixture.LEOPARD_DROPSHIP.loadEntity();
        Bay cargoBay = leopardDesign.getTransportBays()
                             .stream()
                             .filter(CargoBay.class::isInstance)
                             .findFirst()
                             .orElseThrow();
        leopardDesign.removeTransporter(cargoBay);
        leopardDesign.addTransporter(new CargoBay(tons, doors, cargoBay.getBayNumber()));
        return leopardDesign;
    }

    @Test
    void anUnchangedShipNeedsNoRefitAndKeepsEveryBay() throws Exception {
        int baysBefore = bayParts();
        Refit refit = new Refit(leopard, UnitFixture.LEOPARD_DROPSHIP.loadEntity(), true, false, false);

        assertEquals(Refit.NO_CHANGE, refit.getRefitClass(), "Changing nothing is not a Class A refit");
        assertEquals(0, refit.getTime());

        complete(refit);

        assertEquals(baysBefore, bayParts(), "The crew quarters keep their bay parts");
        assertEquals(leopard.getEntity().getTransportBays().size(), bayParts());
    }

    @Test
    void aCarrierConversionIsChargedForItsCubiclesAndMovedDoorsOnly() throws Exception {
        // The BattleMek bay (4 cubicles, 4 doors) becomes two more fighter bays (4 cubicles, 4 doors)
        Refit refit = new Refit(leopard, UnitFixture.LEOPARD_CV_DROPSHIP.loadEntity(), true, false, false);

        long cubiclesToBuy = refit.getShoppingList().stream().filter(MissingCubicle.class::isInstance).count();
        assertEquals(4, cubiclesToBuy);
        assertEquals(Refit.CLASS_B, refit.getRefitClass(), "Cubicles are fitted where others were taken out");
        assertEquals(((8 * SEVEN_DAYS) + (4 * TEN_HOURS)) * CLASS_B_MULTIPLIER, refit.getTime(),
              "4 cubicles out and 4 in at 7 days each, and 4 doors moved at 10 hours each; no month per bay");

        complete(refit);

        assertEquals(leopard.getEntity().getTransportBays().size(), bayParts(), "Every bay has its part");
        assertEquals(Map.of("Mek Cubicle", 4), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
    }

    @Test
    void anAddedDoorIsChargedOnce() throws Exception {
        Refit refit = new Refit(leopard, leopardWithCargoBay(34, 1), true, false, false);

        long doorsToBuy = refit.getShoppingList().stream().filter(MissingBayDoor.class::isInstance).count();
        assertEquals(1, doorsToBuy);
        assertEquals(Refit.CLASS_C, refit.getRefitClass(), "A door is added where nothing was taken out");
        assertEquals(TEN_HOURS * CLASS_C_MULTIPLIER, refit.getTime(), "The door takes 10 hours, not 20");
    }

    @Test
    void resizingACargoBayTakesAMonth() throws Exception {
        Refit refit = new Refit(leopard, leopardWithCargoBay(50, 0), true, false, false);

        assertEquals(Refit.CLASS_B, refit.getRefitClass(), "The new cargo bay replaces the old one");
        assertEquals(Refit.WORK_MONTH * CLASS_B_MULTIPLIER, refit.getTime());
    }
}
