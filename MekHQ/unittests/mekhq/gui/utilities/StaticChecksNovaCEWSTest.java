/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.Vector;

import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Nova CEWS network helper methods in StaticChecks.
 */
class StaticChecksNovaCEWSTest {

    /**
     * Creates a mock Unit with a mock Entity configured for Nova CEWS testing.
     *
     * @param hasNovaCEWS whether the entity has Nova CEWS
     * @param freeNodes   number of free nodes (2 = unnetworked, < 2 = networked)
     * @param networkId   the network ID (null if not networked)
     *
     * @return configured mock Unit
     */
    private Unit createMockNovaCEWSUnit(boolean hasNovaCEWS, int freeNodes, String networkId) {
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.hasNovaCEWS()).thenReturn(hasNovaCEWS);
        when(mockEntity.calculateFreeC3Nodes()).thenReturn(freeNodes);
        when(mockEntity.getC3NetId()).thenReturn(networkId);

        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        return mockUnit;
    }

    @Nested
    @DisplayName("doAllUnitsHaveNovaCEWS Tests")
    class DoAllUnitsHaveNovaCEWSTests {

        @Test
        @DisplayName("returns true when all units have Nova CEWS")
        void testAllUnitsHaveNovaCEWS() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network2"));

            assertTrue(StaticChecks.doAllUnitsHaveNovaCEWS(units));
        }

        @Test
        @DisplayName("returns false when some units lack Nova CEWS")
        void testSomeUnitsLackNovaCEWS() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(false, 2, null));

            assertFalse(StaticChecks.doAllUnitsHaveNovaCEWS(units));
        }

        @Test
        @DisplayName("returns false when no units have Nova CEWS")
        void testNoUnitsHaveNovaCEWS() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(false, 0, null));
            units.add(createMockNovaCEWSUnit(false, 0, null));

            assertFalse(StaticChecks.doAllUnitsHaveNovaCEWS(units));
        }

        @Test
        @DisplayName("returns true for empty vector")
        void testEmptyVector() {
            Vector<Unit> units = new Vector<>();
            // allMatch on empty stream returns true
            assertTrue(StaticChecks.doAllUnitsHaveNovaCEWS(units));
        }

        @Test
        @DisplayName("returns false when unit has null entity")
        void testNullEntity() {
            Unit mockUnit = mock(Unit.class);
            when(mockUnit.getEntity()).thenReturn(null);

            Vector<Unit> units = new Vector<>();
            units.add(mockUnit);

            assertFalse(StaticChecks.doAllUnitsHaveNovaCEWS(units));
        }
    }

    @Nested
    @DisplayName("areAllUnitsNotNovaCEWSNetworked Tests")
    class AreAllUnitsNotNovaCEWSNetworkedTests {

        @Test
        @DisplayName("returns true when all units have 2 free nodes (unnetworked)")
        void testAllUnitsUnnetworked() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network2"));

            assertTrue(StaticChecks.areAllUnitsNotNovaCEWSNetworked(units));
        }

        @Test
        @DisplayName("returns false when some units are networked (< 2 free nodes)")
        void testSomeUnitsNetworked() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network1")); // networked

            assertFalse(StaticChecks.areAllUnitsNotNovaCEWSNetworked(units));
        }

        @Test
        @DisplayName("returns false when all units are networked")
        void testAllUnitsNetworked() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(true, 0, "C3Nova.Network1"));

            assertFalse(StaticChecks.areAllUnitsNotNovaCEWSNetworked(units));
        }

        @Test
        @DisplayName("returns false when unit lacks Nova CEWS")
        void testUnitLacksNovaCEWS() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(false, 2, null));

            assertFalse(StaticChecks.areAllUnitsNotNovaCEWSNetworked(units));
        }
    }

    @Nested
    @DisplayName("areAllUnitsNovaCEWSNetworked Tests")
    class AreAllUnitsNovaCEWSNetworkedTests {

        @Test
        @DisplayName("returns true when all units are networked (< 2 free nodes)")
        void testAllUnitsNetworked() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(true, 0, "C3Nova.Network1"));

            assertTrue(StaticChecks.areAllUnitsNovaCEWSNetworked(units));
        }

        @Test
        @DisplayName("returns false when some units are unnetworked")
        void testSomeUnitsUnnetworked() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network2")); // unnetworked

            assertFalse(StaticChecks.areAllUnitsNovaCEWSNetworked(units));
        }

        @Test
        @DisplayName("returns false when all units are unnetworked")
        void testAllUnitsUnnetworked() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(true, 2, "C3Nova.Network2"));

            assertFalse(StaticChecks.areAllUnitsNovaCEWSNetworked(units));
        }

        @Test
        @DisplayName("returns false when unit lacks Nova CEWS")
        void testUnitLacksNovaCEWS() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(false, 1, null));

            assertFalse(StaticChecks.areAllUnitsNovaCEWSNetworked(units));
        }
    }

    @Nested
    @DisplayName("areAllUnitsOnSameNovaCEWSNetwork Tests")
    class AreAllUnitsOnSameNovaCEWSNetworkTests {

        @Test
        @DisplayName("returns true when all units share the same network ID")
        void testAllUnitsSameNetwork() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.SharedNetwork"));
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.SharedNetwork"));
            units.add(createMockNovaCEWSUnit(true, 0, "C3Nova.SharedNetwork"));

            assertTrue(StaticChecks.areAllUnitsOnSameNovaCEWSNetwork(units));
        }

        @Test
        @DisplayName("returns false when units are on different networks")
        void testUnitsDifferentNetworks() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network2"));

            assertFalse(StaticChecks.areAllUnitsOnSameNovaCEWSNetwork(units));
        }

        @Test
        @DisplayName("returns false when first unit has null network ID")
        void testFirstUnitNullNetworkId() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 2, null));
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network1"));

            assertFalse(StaticChecks.areAllUnitsOnSameNovaCEWSNetwork(units));
        }

        @Test
        @DisplayName("returns false for empty vector")
        void testEmptyVector() {
            Vector<Unit> units = new Vector<>();
            assertFalse(StaticChecks.areAllUnitsOnSameNovaCEWSNetwork(units));
        }

        @Test
        @DisplayName("returns false when first unit has null entity")
        void testFirstUnitNullEntity() {
            Unit mockUnit = mock(Unit.class);
            when(mockUnit.getEntity()).thenReturn(null);

            Vector<Unit> units = new Vector<>();
            units.add(mockUnit);

            assertFalse(StaticChecks.areAllUnitsOnSameNovaCEWSNetwork(units));
        }

        @Test
        @DisplayName("returns false when some units lack Nova CEWS")
        void testSomeUnitsLackNovaCEWS() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network1"));
            units.add(createMockNovaCEWSUnit(false, 1, "C3Nova.Network1"));

            assertFalse(StaticChecks.areAllUnitsOnSameNovaCEWSNetwork(units));
        }

        @Test
        @DisplayName("returns true for single unit with valid network")
        void testSingleUnit() {
            Vector<Unit> units = new Vector<>();
            units.add(createMockNovaCEWSUnit(true, 1, "C3Nova.Network1"));

            assertTrue(StaticChecks.areAllUnitsOnSameNovaCEWSNetwork(units));
        }
    }
}
