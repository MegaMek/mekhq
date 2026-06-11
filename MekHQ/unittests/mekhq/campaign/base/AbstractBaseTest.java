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
package mekhq.campaign.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.FixedLocation;
import mekhq.campaign.Hangar;
import mekhq.campaign.Personnel;
import mekhq.campaign.Warehouse;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AbstractBaseTest {

    FixedLocation parent;
    PlayerBase base;

    @BeforeEach
    void setUp() {
        parent = new FixedLocation(mock(PlanetarySystem.class));
        base = new PlayerBase(parent);
    }

    @Test
    void getId_isNotNull() {
        assertNotNull(base.getId());
    }

    @Test
    void getLocationNode_isNotNull() {
        assertNotNull(base.getLocationNode());
    }

    @Test
    void getLocationNode_locatableIsBase() {
        assertSame(base, base.getLocationNode().getLocatable());
    }

    @Nested
    class ResourceOwnership {
        @Test
        void getWarehouse_returnsBaseWarehouse() {
            Warehouse wh = base.getWarehouse();
            assertNotNull(wh);
            assertSame(wh, base.getBaseWarehouse());
        }

        @Test
        void getHangar_returnsBaseHangar() {
            Hangar h = base.getHangar();
            assertNotNull(h);
            assertSame(h, base.getBaseHangar());
        }

        @Test
        void getPersonnel_returnsBasePersonnel() {
            Personnel p = base.getPersonnel();
            assertNotNull(p);
            assertSame(p, base.getBasePersonnel());
        }
    }

    @Nested
    class LocationTreeWiring {
        @Test
        void baseHangar_parentIsBase() {
            LocationNode hangarNode = base.getBaseHangar().getLocationNode();
            assertNotNull(hangarNode.getParent());
            assertSame(base.getLocationNode(), hangarNode.getParent());
        }

        @Test
        void baseWarehouse_parentIsBase() {
            LocationNode warehouseNode = base.getBaseWarehouse().getLocationNode();
            assertNotNull(warehouseNode.getParent());
            assertSame(base.getLocationNode(), warehouseNode.getParent());
        }

        @Test
        void basePersonnel_parentIsBase() {
            LocationNode personnelNode = base.getBasePersonnel().getLocationNode();
            assertNotNull(personnelNode.getParent());
            assertSame(base.getLocationNode(), personnelNode.getParent());
        }

        @Test
        void base_parentIsSuppliedFixedLocation() {
            assertSame(parent, base.getParent());
        }
    }

    @Nested
    class DisplayFields {
        @Test
        void displayName_roundTrip() {
            base.setDisplayName("Alpha Base");
            assertEquals("Alpha Base", base.getDisplayName());
        }

        @Test
        void displayType_roundTrip() {
            base.setDisplayType("Repair Depot");
            assertEquals("Repair Depot", base.getDisplayType());
        }

        @Test
        void displayType_nullByDefault() {
            assertNull(base.getDisplayType());
        }

        @Test
        void planetId_roundTrip() {
            base.setPlanetId("Galatea");
            assertEquals("Galatea", base.getPlanetId());
        }
    }

    @Nested
    class PartInventoryDefault {
        @Test
        void emptyWarehouse_returnsZeroCounts() {
            PartInventory inv = base.getPartInventory(new MekSensor());
            assertEquals(0, inv.getSupply());
            assertEquals(0, inv.getTransit());
        }

        @Test
        void supplyCountMatchesPresentSpareParts() {
            MekSensor template = new MekSensor();
            MekSensor spare1 = new MekSensor();
            MekSensor spare2 = new MekSensor();
            base.getBaseWarehouse().addPart(spare1);
            base.getBaseWarehouse().addPart(spare2);

            PartInventory inv = base.getPartInventory(template);
            // Each spare has quantity 1 by default.
            assertEquals(2, inv.getSupply());
            assertEquals(0, inv.getTransit());
        }

        @Test
        void transitCountMatchesNonPresentSpareParts() {
            MekSensor template = new MekSensor();
            MekSensor inTransit = new MekSensor();
            inTransit.setDaysToArrival(3);
            base.getBaseWarehouse().addPart(inTransit);

            PartInventory inv = base.getPartInventory(template);
            assertEquals(0, inv.getSupply());
            assertEquals(1, inv.getTransit());
        }

        @Test
        void armorPart_setsCountModifierToPoints() {
            Armor armorTemplate = new Armor();
            PartInventory inv = base.getPartInventory(armorTemplate);
            assertEquals(" points", inv.getCountModifier());
        }
    }

    @Nested
    class PendingIdDrain {
        @Test
        void drainPendingPersonIds_returnsEmptyByDefault() {
            List<UUID> ids = base.drainPendingPersonIds();
            assertTrue(ids.isEmpty());
        }

        @Test
        void drainPendingPersonIds_clearsOnSecondCall() {
            // First drain may not be empty if populated during XML load, but second must be.
            base.drainPendingPersonIds();
            assertTrue(base.drainPendingPersonIds().isEmpty());
        }

        @Test
        void drainPendingBaseWarehouseParts_returnsEmptyByDefault() {
            assertTrue(base.drainPendingBaseWarehouseParts().isEmpty());
        }

        @Test
        void drainPendingBaseWarehouseParts_clearsOnSecondCall() {
            base.drainPendingBaseWarehouseParts();
            assertTrue(base.drainPendingBaseWarehouseParts().isEmpty());
        }

        @Test
        void drainPendingBaseHangarUnits_returnsEmptyByDefault() {
            assertTrue(base.drainPendingBaseHangarUnits().isEmpty());
        }

        @Test
        void drainPendingBaseHangarUnits_clearsOnSecondCall() {
            base.drainPendingBaseHangarUnits();
            assertTrue(base.drainPendingBaseHangarUnits().isEmpty());
        }
    }
}
