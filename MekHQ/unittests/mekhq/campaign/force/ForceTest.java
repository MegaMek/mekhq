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
package mekhq.campaign.force;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.Vector;
import java.util.stream.Stream;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ForceTest {
    @Test
    void testGetAllUnits_ParentForceStandard_NoChildForces() {
        // Arrange
        Force force = new Force("Test Force");
        UUID unit1 = UUID.randomUUID();
        UUID unit2 = UUID.randomUUID();
        force.addUnit(unit1);
        force.addUnit(unit2);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(false);

        // Assert
        assertEquals(2, allUnits.size());
    }

    @Test
    void testGetAllUnits_ParentForceStandard_ChildForcesAlsoStandard() {
        // Arrange
        Force force = new Force("Parent Force");
        force.setForceType(ForceType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childForce2.addUnit(unit);
        childForce.addSubForce(childForce2, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(false);

        // Assert
        assertEquals(3, allUnits.size());
    }

    @Test
    void testGetAllUnits_ParentForceStandard_ChildForcesNotStandard() {
        // Arrange
        Force force = new Force("Parent Force");
        force.setForceType(ForceType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childForce2.addUnit(unit);
        childForce.addSubForce(childForce2, true);

        childForce.setForceType(ForceType.CONVOY, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(true);

        // Assert
        assertEquals(1, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesNotStandard() {
        // Arrange
        Force force = new Force("Parent Force");
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childForce2.addUnit(unit);
        childForce.addSubForce(childForce2, true);

        force.setForceType(ForceType.SECURITY, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(true);

        // Assert
        assertEquals(0, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesNotStandard_NoStandardFilter() {
        // Arrange
        Force force = new Force("Parent Force");
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childForce2.addUnit(unit);
        childForce.addSubForce(childForce2, true);

        force.setForceType(ForceType.SECURITY, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(false);

        // Assert
        assertEquals(3, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesStandard_SecondLayerEmpty() {
        // Arrange
        Force force = new Force("Parent Force");
        force.setForceType(ForceType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        childForce.addSubForce(childForce2, true);

        Force childForce3 = new Force("Child Force (layer 3)");
        unit = UUID.randomUUID();
        childForce3.addUnit(unit);
        childForce.addSubForce(childForce3, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(false);

        // Assert
        assertEquals(3, allUnits.size());
    }

    @Nested
    class TestFormationLevels {
        static Faction mockISFaction;
        static Faction mockComstarFaction;
        static Faction mockClanFaction;

        Campaign mockCampaign;
        Faction mockFaction;

        @BeforeAll
        static void beforeAll() {
            mockISFaction = mock(Faction.class);
            when(mockISFaction.isInnerSphere()).thenReturn(true);
            when(mockISFaction.isComStarOrWoB()).thenReturn(false);
            when(mockISFaction.isClan()).thenReturn(false);
            when(mockISFaction.getFormationBaseSize()).thenReturn(4);
            when(mockISFaction.getFormationGrouping()).thenReturn(3);

            mockComstarFaction = mock(Faction.class);
            when(mockComstarFaction.isInnerSphere()).thenReturn(false);
            when(mockComstarFaction.isComStarOrWoB()).thenReturn(true);
            when(mockComstarFaction.isClan()).thenReturn(false);
            when(mockComstarFaction.getFormationBaseSize()).thenReturn(6);
            when(mockComstarFaction.getFormationGrouping()).thenReturn(6);

            mockClanFaction = mock(Faction.class);
            when(mockClanFaction.isInnerSphere()).thenReturn(false);
            when(mockClanFaction.isComStarOrWoB()).thenReturn(false);
            when(mockClanFaction.isClan()).thenReturn(true);
            when(mockClanFaction.getFormationBaseSize()).thenReturn(5);
            when(mockClanFaction.getFormationGrouping()).thenReturn(5);
        }

        @BeforeEach
        void beforeEach() {
            mockCampaign = mock(Campaign.class);
        }

        private static Stream<Arguments> factions() {
            return Stream.of(
                  Arguments.of(mockISFaction),
                  Arguments.of(mockComstarFaction),
                  Arguments.of(mockClanFaction)
            );
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth1(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Force force = newLance();
            force.defaultFormationLevelForForce(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 1), force.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth2(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Force force = newCompany();
            force.defaultFormationLevelForForce(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 2), force.getFormationLevel());
        }



        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth3(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Force force = newBattalion();
            force.defaultFormationLevelForForce(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 3), force.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth4(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Force force = newRegiment();
            force.defaultFormationLevelForForce(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 4), force.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth5(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Force force = newBrigade();
            force.defaultFormationLevelForForce(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 5), force.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth6(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Force force = newDivision();
            force.defaultFormationLevelForForce(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 6), force.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationSupportCompanyAttachedToRegiment(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Force force1 = newRegiment();
            Force force2 = newCompany();
            force2.defaultFormationLevelForForce(mockCampaign); //Shouldn't change anything
            force1.addSubForce(force2, true);
            force1.defaultFormationLevelForForce(mockCampaign);


            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 4), force1.getFormationLevel());
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 2), force2.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationBaseCompanies(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Force force = new Force("Test Company");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                for (int j = 0; j < mockFaction.getFormationBaseSize(); j++) {
                    UUID unit = UUID.randomUUID();
                    force.addUnit(unit);
                }
            }
            force.defaultFormationLevelForForce(mockCampaign);
            force.setOverrideFormationLevel(FormationLevel.parseFromDepth(mockCampaign, 2));

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 2), force.getFormationLevel());
        }

        private void setFaction(Faction faction) {
            mockFaction = faction;
            when(mockCampaign.getFaction()).thenReturn(mockFaction);
        }

        private Force newLance() {
            Force force = new Force("Test Lance");
            for(int i = 0; i < mockFaction.getFormationBaseSize(); i++) {
                UUID unit = UUID.randomUUID();
                force.addUnit(unit);
            }
            return force;
        }

        private Force newCompany() {
            Force force = new Force("Test Company");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Force newLance = newLance();
                newLance.defaultFormationLevelForForce(mockCampaign);
                force.addSubForce(newLance, true);
            }
            return force;
        }

        private Force newBattalion() {
            Force force = new Force("Test Battalion");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Force newCompany = newCompany();
                newCompany.defaultFormationLevelForForce(mockCampaign);
                force.addSubForce(newCompany, true);
            }
            return force;
        }

        private Force newRegiment() {
            Force force = new Force("Test Regiment");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Force newBattalion = newBattalion();
                newBattalion.defaultFormationLevelForForce(mockCampaign);
                force.addSubForce(newBattalion, true);
            }
            return force;
        }

        private Force newBrigade() {
            Force force = new Force("Test Brigade");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Force newRegiment = newRegiment();
                newRegiment.defaultFormationLevelForForce(mockCampaign);
                force.addSubForce(newRegiment, true);
            }
            return force;
        }

        private Force newDivision() {
            Force force = new Force("Test Division");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Force newBrigade = newBrigade();
                newBrigade.defaultFormationLevelForForce(mockCampaign);
                force.addSubForce(newBrigade, true);
            }
            return force;
        }

    }
}
