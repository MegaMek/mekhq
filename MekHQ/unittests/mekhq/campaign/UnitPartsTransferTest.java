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
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TransportBayPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A unit that moves to or from a base takes its parts with it, intact (issue #10343).
 */
class UnitPartsTransferTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private LocalWarehouse mainWarehouse;
    private PlayerBase base;
    private LocalWarehouse baseWarehouse;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        mainWarehouse = campaign.getPlayerForce().getWarehouse();
        base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        baseWarehouse = base.getBaseWarehouse();
    }

    private static boolean isKeptBy(LocalWarehouse warehouse, Part part) {
        return warehouse.getPart(part.getUniqueId()) == part;
    }

    @Test
    void aDropShipMovesWithEveryPartAndItsBaysKeepTheirDoors() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        List<Part> partsBefore = new ArrayList<>(leopard.getParts());
        TransportBayPart bay = PartsScenario.unitParts(leopard, TransportBayPart.class).getFirst();
        List<Part> doorsBefore = new ArrayList<>(bay.getChildParts());
        List<UUID> identitiesBefore = partsBefore.stream().map(Part::getUniqueId).toList();

        int movedCount = UnitPartsTransfer.moveUnitParts(campaign, leopard, baseWarehouse);

        assertEquals(partsBefore.size(), movedCount);
        for (Part part : partsBefore) {
            assertSame(part, baseWarehouse.getPart(part.getUniqueId()), part.getName() + " is at the base");
            assertNull(mainWarehouse.getPart(part.getUniqueId()));
        }
        assertEquals(doorsBefore, bay.getChildParts(), "The bay still has its doors");
        assertEquals(identitiesBefore, leopard.getParts().stream().map(Part::getUniqueId).toList());
    }

    @Test
    void aUnitsRefitKitAndReservedSparesGoWithIt() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        EquipmentPart laser = PartsScenario.unitParts(locust, EquipmentPart.class).getFirst();
        Part refitKitPart = laser.clone();
        refitKitPart.setRefitUnit(locust);
        mainWarehouse.addPart(refitKitPart, false);
        Part reservedSpare = laser.clone();
        mainWarehouse.addPart(reservedSpare, false);
        laser.setReplacementPart(reservedSpare);

        UnitPartsTransfer.moveUnitParts(campaign, locust, baseWarehouse);

        assertSame(refitKitPart, baseWarehouse.getPart(refitKitPart.getUniqueId()), "The refit kit went along");
        assertSame(reservedSpare, baseWarehouse.getPart(reservedSpare.getUniqueId()), "The reserved spare went along");
    }

    @Test
    void aSpareMovesWithThePartsThatBelongToIt() {
        EquipmentPart laser = PartsScenario.unitParts(scenario.withUnit(UnitFixture.LOCUST_LCT_1V),
              EquipmentPart.class).getFirst();
        Part spare = laser.clone();
        Part belongingPart = laser.clone();
        spare.addChildPart(belongingPart);
        mainWarehouse.addPart(spare, false);
        mainWarehouse.addPart(belongingPart, false);

        UnitPartsTransfer.moveSpareParts(campaign, List.of(spare), baseWarehouse);

        assertSame(spare, baseWarehouse.getPart(spare.getUniqueId()));
        assertSame(belongingPart, baseWarehouse.getPart(belongingPart.getUniqueId()));
        assertEquals(List.of(belongingPart), spare.getChildParts());
    }

    @Test
    void aRefitKitBoughtForAUnitAtABaseArrivesAtTheBase() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        campaign.getPlayerForce().getHangar().removeUnit(locust.getId());
        base.getBaseHangar().addUnit(locust);
        Part refitKitPart = PartsScenario.unitParts(locust, EquipmentPart.class).getFirst().clone();
        refitKitPart.setRefitUnit(locust);

        campaign.getQuartermaster().addPart(refitKitPart, 0, false);

        assertSame(refitKitPart, baseWarehouse.getPart(refitKitPart.getUniqueId()));
        assertFalse(isKeptBy(mainWarehouse, refitKitPart));
    }

    @Test
    void aWarehouseDoesNotMoveAPartItDoesNotKeep() {
        Part spare = PartsScenario.unitParts(scenario.withUnit(UnitFixture.LOCUST_LCT_1V), EquipmentPart.class)
                           .getFirst()
                           .clone();
        baseWarehouse.addPart(spare, false);

        assertFalse(mainWarehouse.transferPart(spare, baseWarehouse));
        assertSame(spare, baseWarehouse.getPart(spare.getUniqueId()));
    }
}
