/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for C3i, NC3, and Nova CEWS network functionality in Campaign. These tests verify fixes for issue #8648 and
 * Nova CEWS support.
 */
class C3NetworkTest {

    private Campaign campaign;
    private List<Unit> units;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        units = new ArrayList<>();

        // Configure mock to call real methods for the network methods we're testing
        doCallRealMethod().when(campaign).getAvailableC3iNetworks();
        doCallRealMethod().when(campaign).getAvailableNC3Networks();
        doCallRealMethod().when(campaign).getAvailableNovaCEWSNetworks();

        // Return our test units list
        when(campaign.getUnits()).thenReturn(units);
    }

    /**
     * Creates a mock Unit with a mock Entity configured for C3i network testing.
     *
     * @param hasC3i    whether the entity has C3i
     * @param freeNodes number of free C3 nodes (5 = unnetworked, < 5 = networked)
     * @param networkId the C3 network ID
     * @param formationId   the formation ID (-1 = not in TO&E)
     *
     * @return configured mock Unit
     */
    private Unit createMockC3iUnit(boolean hasC3i, int freeNodes, String networkId, int formationId) {
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.hasC3i()).thenReturn(hasC3i);
        when(mockEntity.calculateFreeC3Nodes()).thenReturn(freeNodes);
        when(mockEntity.getC3NetId()).thenReturn(networkId);

        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockUnit.getFormationId()).thenReturn(formationId);

        return mockUnit;
    }

    /**
     * Creates a mock Unit with a mock Entity configured for NC3 network testing.
     */
    private Unit createMockNC3Unit(boolean hasNC3, int freeNodes, String networkId, int formationId) {
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.hasNavalC3()).thenReturn(hasNC3);
        when(mockEntity.calculateFreeC3Nodes()).thenReturn(freeNodes);
        when(mockEntity.getC3NetId()).thenReturn(networkId);

        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockUnit.getFormationId()).thenReturn(formationId);

        return mockUnit;
    }

    /**
     * Creates a mock Unit with a mock Entity configured for Nova CEWS network testing.
     *
     * @param hasNovaCEWS whether the entity has Nova CEWS
     * @param freeNodes   number of free nodes (2 = unnetworked, < 2 = networked for Nova)
     * @param networkId   the network ID
     * @param formationId     the formation ID (-1 = not in TO&E)
     *
     * @return configured mock Unit
     */
    private Unit createMockNovaCEWSUnit(boolean hasNovaCEWS, int freeNodes, String networkId, int formationId) {
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.hasNovaCEWS()).thenReturn(hasNovaCEWS);
        when(mockEntity.calculateFreeC3Nodes()).thenReturn(freeNodes);
        when(mockEntity.getC3NetId()).thenReturn(networkId);

        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockUnit.getFormationId()).thenReturn(formationId);

        return mockUnit;
    }

    @Nested
    @DisplayName("C3i Network Tests - Issue #8648 Fix")
    class C3iNetworkTests {

        @Test
        @DisplayName("getAvailableC3iNetworks includes unnetworked units with 5 free nodes")
        void testGetAvailableC3iNetworksIncludesUnnetworkedUnits() {
            // Arrange - create a unit with 5 free nodes (unnetworked C3i unit)
            Unit unnetworkedUnit = createMockC3iUnit(true, 5, "C3i.TestNetwork", 1);
            units.add(unnetworkedUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableC3iNetworks();

            // Assert - should find the network with 5 free nodes
            assertEquals(1, networks.size(), "Should find one available C3i network");
            assertEquals("C3i.TestNetwork", networks.get(0)[0]);
            assertEquals("5", networks.get(0)[1]);
        }

        @Test
        @DisplayName("getAvailableC3iNetworks includes partially filled networks")
        void testGetAvailableC3iNetworksIncludesPartialNetworks() {
            // Arrange - create a unit with 3 free nodes (partially networked)
            Unit partialUnit = createMockC3iUnit(true, 3, "C3i.PartialNetwork", 1);
            units.add(partialUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableC3iNetworks();

            // Assert
            assertEquals(1, networks.size());
            assertEquals("3", networks.get(0)[1]);
        }

        @Test
        @DisplayName("getAvailableC3iNetworks excludes full networks with 0 free nodes")
        void testGetAvailableC3iNetworksExcludesFullNetworks() {
            // Arrange - create a unit with 0 free nodes (full network)
            Unit fullUnit = createMockC3iUnit(true, 0, "C3i.FullNetwork", 1);
            units.add(fullUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableC3iNetworks();

            // Assert - should not find full networks
            assertEquals(0, networks.size(), "Should not include full networks");
        }

        @Test
        @DisplayName("getAvailableC3iNetworks excludes units not in TO&E")
        void testGetAvailableC3iNetworksExcludesUnitsNotInTOE() {
            // Arrange - create a unit not in TO&E (formationId = -1)
            Unit unassignedUnit = createMockC3iUnit(true, 5, "C3i.TestNetwork", -1);
            units.add(unassignedUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableC3iNetworks();

            // Assert
            assertEquals(0, networks.size(), "Should not include units not in TO&E");
        }

        @Test
        @DisplayName("getAvailableC3iNetworks excludes units without C3i")
        void testGetAvailableC3iNetworksExcludesNonC3iUnits() {
            // Arrange - create a unit without C3i
            Unit nonC3iUnit = createMockC3iUnit(false, 5, "C3i.TestNetwork", 1);
            units.add(nonC3iUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableC3iNetworks();

            // Assert
            assertEquals(0, networks.size(), "Should not include non-C3i units");
        }

        @Test
        @DisplayName("getAvailableC3iNetworks returns unique networks only")
        void testGetAvailableC3iNetworksReturnsUniqueNetworks() {
            // Arrange - create two units on the same network
            Unit unit1 = createMockC3iUnit(true, 4, "C3i.SharedNetwork", 1);
            Unit unit2 = createMockC3iUnit(true, 4, "C3i.SharedNetwork", 2);
            units.add(unit1);
            units.add(unit2);

            // Act
            Vector<String[]> networks = campaign.getAvailableC3iNetworks();

            // Assert - should only return one network entry
            assertEquals(1, networks.size(), "Should return unique networks only");
        }
    }

    @Nested
    @DisplayName("NC3 Network Tests - Issue #8648 Fix")
    class NC3NetworkTests {

        @Test
        @DisplayName("getAvailableNC3Networks includes unnetworked units with 5 free nodes")
        void testGetAvailableNC3NetworksIncludesUnnetworkedUnits() {
            // Arrange - create an unnetworked NC3 unit
            Unit unnetworkedUnit = createMockNC3Unit(true, 5, "NC3.TestNetwork", 1);
            units.add(unnetworkedUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableNC3Networks();

            // Assert
            assertEquals(1, networks.size(), "Should find one available NC3 network");
            assertEquals("5", networks.get(0)[1]);
        }

        @Test
        @DisplayName("getAvailableNC3Networks excludes full networks")
        void testGetAvailableNC3NetworksExcludesFullNetworks() {
            // Arrange
            Unit fullUnit = createMockNC3Unit(true, 0, "NC3.FullNetwork", 1);
            units.add(fullUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableNC3Networks();

            // Assert
            assertEquals(0, networks.size());
        }
    }

    @Nested
    @DisplayName("Nova CEWS Network Tests")
    class NovaCEWSNetworkTests {

        @Test
        @DisplayName("getAvailableNovaCEWSNetworks includes unnetworked units with 2 free nodes")
        void testGetAvailableNovaCEWSNetworksIncludesUnnetworkedUnits() {
            // Arrange - Nova CEWS max is 3 nodes, so unnetworked = 2 free nodes
            Unit unnetworkedUnit = createMockNovaCEWSUnit(true, 2, "C3Nova.TestNetwork", 1);
            units.add(unnetworkedUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableNovaCEWSNetworks();

            // Assert
            assertEquals(1, networks.size(), "Should find one available Nova CEWS network");
            assertEquals("C3Nova.TestNetwork", networks.get(0)[0]);
            assertEquals("2", networks.get(0)[1]);
        }

        @Test
        @DisplayName("getAvailableNovaCEWSNetworks includes partially filled networks")
        void testGetAvailableNovaCEWSNetworksIncludesPartialNetworks() {
            // Arrange - create a unit with 1 free node (2 units in network of 3)
            Unit partialUnit = createMockNovaCEWSUnit(true, 1, "C3Nova.PartialNetwork", 1);
            units.add(partialUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableNovaCEWSNetworks();

            // Assert
            assertEquals(1, networks.size());
            assertEquals("1", networks.get(0)[1]);
        }

        @Test
        @DisplayName("getAvailableNovaCEWSNetworks excludes full networks with 0 free nodes")
        void testGetAvailableNovaCEWSNetworksExcludesFullNetworks() {
            // Arrange - create a unit with 0 free nodes (full 3-unit network)
            Unit fullUnit = createMockNovaCEWSUnit(true, 0, "C3Nova.FullNetwork", 1);
            units.add(fullUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableNovaCEWSNetworks();

            // Assert
            assertEquals(0, networks.size(), "Should not include full Nova CEWS networks");
        }

        @Test
        @DisplayName("getAvailableNovaCEWSNetworks excludes units not in TO&E")
        void testGetAvailableNovaCEWSNetworksExcludesUnitsNotInTOE() {
            // Arrange
            Unit unassignedUnit = createMockNovaCEWSUnit(true, 2, "C3Nova.TestNetwork", -1);
            units.add(unassignedUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableNovaCEWSNetworks();

            // Assert
            assertEquals(0, networks.size(), "Should not include units not in TO&E");
        }

        @Test
        @DisplayName("getAvailableNovaCEWSNetworks excludes units without Nova CEWS")
        void testGetAvailableNovaCEWSNetworksExcludesNonNovaUnits() {
            // Arrange
            Unit nonNovaUnit = createMockNovaCEWSUnit(false, 2, "C3Nova.TestNetwork", 1);
            units.add(nonNovaUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableNovaCEWSNetworks();

            // Assert
            assertEquals(0, networks.size(), "Should not include non-Nova CEWS units");
        }

        @Test
        @DisplayName("getAvailableNovaCEWSNetworks excludes networks with more than 2 free nodes")
        void testGetAvailableNovaCEWSNetworksExcludesInvalidFreeNodes() {
            // Arrange - 3 or more free nodes is invalid for Nova CEWS (max 3 nodes total)
            Unit invalidUnit = createMockNovaCEWSUnit(true, 3, "C3Nova.InvalidNetwork", 1);
            units.add(invalidUnit);

            // Act
            Vector<String[]> networks = campaign.getAvailableNovaCEWSNetworks();

            // Assert - condition is <= 2, so 3 should be excluded
            assertEquals(0, networks.size(), "Should not include invalid free node counts");
        }
    }
}
