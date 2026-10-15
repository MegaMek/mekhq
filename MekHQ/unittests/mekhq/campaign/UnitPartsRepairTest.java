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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.bays.Bay;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.BayDoor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TransportBayPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingBayDoor;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Loading puts right the parts older versions left duplicated or scattered across a base and the main force (issue
 * #10343).
 */
class UnitPartsRepairTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private LocalWarehouse mainWarehouse;
    private LocalWarehouse baseWarehouse;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        mainWarehouse = campaign.getPlayerForce().getWarehouse();
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        baseWarehouse = base.getBaseWarehouse();
    }

    /** Puts a copy of the part on its unit, kept in the given warehouse, as an older version could leave it. */
    private Part copyOnUnit(Part part, LocalWarehouse warehouse) {
        Part copy = part.clone();
        copy.setUnit(part.getUnit());
        warehouse.addPart(copy, false);
        part.getUnit().addPart(copy);
        return copy;
    }

    @Test
    void aSecondCopyOfALocationKeptAtABaseIsRemoved() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        MekLocation centreTorso = PartsScenario.unitParts(locust, MekLocation.class).getFirst();
        int partCount = locust.getParts().size();
        Part copy = copyOnUnit(centreTorso, baseWarehouse);
        Money fundsBefore = campaign.getPlayerForce().getFinances().getBalance();

        assertTrue(UnitPartsRepair.repair(campaign, locust));

        assertEquals(partCount, locust.getParts().size());
        assertTrue(locust.getParts().contains(centreTorso), "The part in the unit's own warehouse is kept");
        assertFalse(locust.getParts().contains(copy));
        assertNull(baseWarehouse.getPart(copy.getUniqueId()));
        assertEquals(fundsBefore, campaign.getPlayerForce().getFinances().getBalance(), "Nothing is paid for it");
    }

    @Test
    void partsAUnitCarriesSeveralOfAreNotTreatedAsDuplicates() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        int heatSinkCount = PartsScenario.unitParts(locust, HeatSink.class).size();

        UnitPartsRepair.repair(campaign, locust);

        assertEquals(heatSinkCount, PartsScenario.unitParts(locust, HeatSink.class).size());
        assertNull(UnitPartsRepair.slotOf(PartsScenario.unitParts(locust, HeatSink.class).getFirst()));
    }

    @Test
    void aPartKeptAwayFromItsUnitIsMovedToTheUnitsWarehouse() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        HeatSink heatSink = PartsScenario.unitParts(locust, HeatSink.class).getFirst();
        mainWarehouse.transferPart(heatSink, baseWarehouse);

        assertTrue(UnitPartsRepair.repair(campaign, locust));

        assertSame(heatSink, mainWarehouse.getPart(heatSink.getUniqueId()));
        assertTrue(locust.getParts().contains(heatSink));
    }

    @Test
    void missingDoorsBeyondWhatTheBaysHaveAreRemoved() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        int partCount = leopard.getParts().size();
        for (int index = 0; index < 3; index++) {
            MissingBayDoor strayDoor = new MissingBayDoor(0, campaign);
            strayDoor.setUnit(leopard);
            mainWarehouse.addPart(strayDoor, false);
            leopard.addPart(strayDoor);
        }

        assertTrue(UnitPartsRepair.repair(campaign, leopard));

        assertEquals(partCount, leopard.getParts().size(), "Every bay already has its doors");
    }

    @Test
    void aSoundUnitIsLeftAlone() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        int partCount = leopard.getParts().size();

        assertFalse(UnitPartsRepair.repair(campaign, leopard));

        assertEquals(partCount, leopard.getParts().size());
    }
    /** Adds a bay part to the unit, as an older version saved it. */
    private TransportBayPart bayPartOnUnit(Unit unit, int bayNumber) {
        TransportBayPart bayPart = new TransportBayPart((int) unit.getEntity().getWeight(), bayNumber, 1, campaign);
        bayPart.setUnit(unit);
        mainWarehouse.addPart(bayPart, false);
        unit.addPart(bayPart);
        return bayPart;
    }

    private static Bay sizeZeroBay(Unit unit) {
        for (Bay bay : unit.getEntity().getTransportBays()) {
            if (!TransportBayPart.isWorthAPart(bay)) {
                return bay;
            }
        }
        return null;
    }

    @Test
    void sizeZeroQuartersGetNoPart() {
        // the Leopard (2537) unit file lists its second class quarters with size 0 (issue #10185)
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        Bay emptyQuarters = sizeZeroBay(leopard);
        assertTrue(emptyQuarters != null, "The Leopard (2537) fixture has size-0 quarters");

        for (TransportBayPart bayPart : PartsScenario.unitParts(leopard, TransportBayPart.class)) {
            assertFalse(bayPart.getBayNumber() == emptyQuarters.getBayNumber(), bayPart.getName());
        }
    }

    @Test
    void quartersPartsAnOlderVersionNumberedZeroAreRemoved() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        List<TransportBayPart> bayParts = PartsScenario.unitParts(leopard, TransportBayPart.class);
        TransportBayPart staleQuarters = bayPartOnUnit(leopard, 0);
        bayPartOnUnit(leopard, 0);
        BayDoor staleDoor = new BayDoor((int) leopard.getEntity().getWeight(), campaign);
        staleDoor.setUnit(leopard);
        mainWarehouse.addPart(staleDoor, false);
        leopard.addPart(staleDoor);
        staleQuarters.addChildPart(staleDoor);

        assertTrue(UnitPartsRepair.repair(campaign, leopard));

        List<TransportBayPart> bayPartsAfter = PartsScenario.unitParts(leopard, TransportBayPart.class);
        assertEquals(bayParts.size(), bayPartsAfter.size(), "One part for each bay, as a new unit has");
        Set<Integer> bayNumbers = new HashSet<>();
        for (TransportBayPart bayPart : bayPartsAfter) {
            assertTrue(bayNumbers.add(bayPart.getBayNumber()), "Each bay has one part");
            assertTrue(bayPart.getBay() != null, "Every part belongs to a bay");
        }
        assertFalse(leopard.getParts().contains(staleDoor), "The removed bay's door goes with it");
        assertNull(mainWarehouse.getPart(staleQuarters.getUniqueId()));
    }

    @Test
    void aPartForSizeZeroQuartersIsRemoved() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        TransportBayPart emptyQuartersPart = bayPartOnUnit(leopard, sizeZeroBay(leopard).getBayNumber());

        assertTrue(UnitPartsRepair.repair(campaign, leopard));

        assertFalse(leopard.getParts().contains(emptyQuartersPart));
    }

    @Test
    void aUnitWhoseBaysAllHaveOnePartIsLeftAlone() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        int bayPartCount = PartsScenario.unitParts(leopard, TransportBayPart.class).size();

        UnitPartsRepair.repair(campaign, leopard);

        assertEquals(bayPartCount, PartsScenario.unitParts(leopard, TransportBayPart.class).size());
    }

    /** A bay on the Leopard with at least one door. */
    private TransportBayPart bayWithADoor(Unit leopard) {
        for (TransportBayPart bayPart : PartsScenario.unitParts(leopard, TransportBayPart.class)) {
            boolean hasADoor = bayPart.getChildParts().stream().anyMatch(BayDoor.class::isInstance);
            if (hasADoor) {
                return bayPart;
            }
        }
        throw new IllegalStateException("The Leopard fixture has no bay with a door");
    }

    /**
     * Leaves a working door on the unit but out of its bay, as versions before 0.49.4 did when a door was replaced,
     * then loads the unit the way a save is loaded: the bay counts a door short and lists it as missing.
     */
    private BayDoor orphanADoorAndReload(Unit leopard, TransportBayPart bayPart) {
        BayDoor door = (BayDoor) bayPart.getChildParts()
                                        .stream()
                                        .filter(BayDoor.class::isInstance)
                                        .findFirst()
                                        .orElseThrow();
        bayPart.removeChildPart(door);
        leopard.initializeParts(true);
        leopard.runDiagnostic(false);
        boolean isDoorListedMissing = bayPart.getChildParts().stream().anyMatch(MissingBayDoor.class::isInstance);
        assertTrue(isDoorListedMissing, "The bay asks for the door to be replaced again");
        return door;
    }

    @Test
    void aReplacedDoorAnOlderVersionLeftOutOfItsBayIsPutBack() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        TransportBayPart bayPart = bayWithADoor(leopard);
        BayDoor door = orphanADoorAndReload(leopard, bayPart);

        assertTrue(UnitPartsRepair.repair(campaign, leopard));

        assertSame(bayPart, door.getParentPart(), "The door is back in its bay");
        assertTrue(PartsScenario.unitParts(leopard, MissingBayDoor.class).isEmpty(), "No door is asked for again");
        Bay bay = bayPart.getBay();
        assertEquals(bay.getDoors(), bay.getCurrentDoors(), "Every door of the bay works");
    }

    @Test
    void aSecondDoorWithNoBayIsLeftOnTheUnitOnceTheBayIsWhole() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        TransportBayPart bayPart = bayWithADoor(leopard);
        BayDoor extraDoor = new BayDoor((int) leopard.getEntity().getWeight(), campaign);
        extraDoor.setUnit(leopard);
        mainWarehouse.addPart(extraDoor, false);
        leopard.addPart(extraDoor);
        orphanADoorAndReload(leopard, bayPart);

        UnitPartsRepair.repair(campaign, leopard);

        assertTrue(PartsScenario.unitParts(leopard, MissingBayDoor.class).isEmpty(), "The missing door was filled");
        long doorsWithNoBay = PartsScenario.unitParts(leopard, BayDoor.class)
                                    .stream()
                                    .filter(bayDoor -> bayDoor.getParentPart() == null)
                                    .count();
        assertEquals(1, doorsWithNoBay, "One door filled the gap and the other is kept as it was");
    }
}
