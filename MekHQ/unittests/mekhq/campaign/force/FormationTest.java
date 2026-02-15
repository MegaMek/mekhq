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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.Vector;
import java.util.stream.Stream;

import megamek.codeUtilities.MathUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FormationTest {
    @Test
    void testGetAllUnits_ParentForceStandard_NoChildForces() {
        // Arrange
        Formation formation = new Formation("Test Force");
        UUID unit1 = UUID.randomUUID();
        UUID unit2 = UUID.randomUUID();
        formation.addUnit(unit1);
        formation.addUnit(unit2);

        // Act
        Vector<UUID> allUnits = formation.getAllUnits(false);

        // Assert
        assertEquals(2, allUnits.size());
    }

    @Test
    void testGetAllUnits_ParentForceStandard_ChildForcesAlsoStandard() {
        // Arrange
        Formation formation = new Formation("Parent Force");
        formation.setFormationType(FormationType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        formation.addUnit(unit);

        Formation childFormation = new Formation("Child Force");
        unit = UUID.randomUUID();
        childFormation.addUnit(unit);
        formation.addSubFormation(childFormation, true);

        Formation childFormation2 = new Formation("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childFormation2.addUnit(unit);
        childFormation.addSubFormation(childFormation2, true);

        // Act
        Vector<UUID> allUnits = formation.getAllUnits(false);

        // Assert
        assertEquals(3, allUnits.size());
    }

    @Test
    void testGetAllUnits_ParentForceStandard_ChildForcesNotStandard() {
        // Arrange
        Formation formation = new Formation("Parent Force");
        formation.setFormationType(FormationType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        formation.addUnit(unit);

        Formation childFormation = new Formation("Child Force");
        unit = UUID.randomUUID();
        childFormation.addUnit(unit);
        formation.addSubFormation(childFormation, true);

        Formation childFormation2 = new Formation("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childFormation2.addUnit(unit);
        childFormation.addSubFormation(childFormation2, true);

        childFormation.setFormationType(FormationType.CONVOY, true);

        // Act
        Vector<UUID> allUnits = formation.getAllUnits(true);

        // Assert
        assertEquals(1, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesNotStandard() {
        // Arrange
        Formation formation = new Formation("Parent Force");
        UUID unit = UUID.randomUUID();
        formation.addUnit(unit);

        Formation childFormation = new Formation("Child Force");
        unit = UUID.randomUUID();
        childFormation.addUnit(unit);
        formation.addSubFormation(childFormation, true);

        Formation childFormation2 = new Formation("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childFormation2.addUnit(unit);
        childFormation.addSubFormation(childFormation2, true);

        formation.setFormationType(FormationType.SECURITY, true);

        // Act
        Vector<UUID> allUnits = formation.getAllUnits(true);

        // Assert
        assertEquals(0, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesNotStandard_NoStandardFilter() {
        // Arrange
        Formation formation = new Formation("Parent Force");
        UUID unit = UUID.randomUUID();
        formation.addUnit(unit);

        Formation childFormation = new Formation("Child Force");
        unit = UUID.randomUUID();
        childFormation.addUnit(unit);
        formation.addSubFormation(childFormation, true);

        Formation childFormation2 = new Formation("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childFormation2.addUnit(unit);
        childFormation.addSubFormation(childFormation2, true);

        formation.setFormationType(FormationType.SECURITY, true);

        // Act
        Vector<UUID> allUnits = formation.getAllUnits(false);

        // Assert
        assertEquals(3, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesStandard_SecondLayerEmpty() {
        // Arrange
        Formation formation = new Formation("Parent Force");
        formation.setFormationType(FormationType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        formation.addUnit(unit);

        Formation childFormation = new Formation("Child Force");
        unit = UUID.randomUUID();
        childFormation.addUnit(unit);
        formation.addSubFormation(childFormation, true);

        Formation childFormation2 = new Formation("Child Force (layer 2)");
        childFormation.addSubFormation(childFormation2, true);

        Formation childFormation3 = new Formation("Child Force (layer 3)");
        unit = UUID.randomUUID();
        childFormation3.addUnit(unit);
        childFormation.addSubFormation(childFormation3, true);

        // Act
        Vector<UUID> allUnits = formation.getAllUnits(false);

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
        void testGetDefaultFormationDepth0(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation = newTeam();
            formation.defaultFormationLevelForFormation(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 0), formation.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth1(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation = newLance();
            formation.defaultFormationLevelForFormation(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 1), formation.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth2(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation = newCompany();
            formation.defaultFormationLevelForFormation(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 2), formation.getFormationLevel());
        }


        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth3(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation = newBattalion();
            formation.defaultFormationLevelForFormation(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 3), formation.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth4(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation = newRegiment();
            formation.defaultFormationLevelForFormation(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 4), formation.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth5(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation = newBrigade();
            formation.defaultFormationLevelForFormation(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 5), formation.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationDepth6(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation = newDivision();
            formation.defaultFormationLevelForFormation(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 6), formation.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationSupportCompanyAttachedToRegiment(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation1 = newRegiment();
            Formation formation2 = newCompany();
            formation2.defaultFormationLevelForFormation(mockCampaign); //Shouldn't change anything
            formation1.addSubFormation(formation2, true);
            formation1.defaultFormationLevelForFormation(mockCampaign);


            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 4), formation1.getFormationLevel());
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 2), formation2.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationBaseCompanies(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation = new Formation("Test Company");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                for (int j = 0; j < mockFaction.getFormationBaseSize(); j++) {
                    UUID unit = UUID.randomUUID();
                    formation.addUnit(unit);
                }
            }
            formation.defaultFormationLevelForFormation(mockCampaign);
            formation.setOverrideFormationLevel(FormationLevel.parseFromDepth(mockCampaign, 2));

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 2), formation.getFormationLevel());
        }

        @ParameterizedTest
        @MethodSource(value = "factions")
        void testGetDefaultFormationTeamAttachedToLance(Faction faction) {
            // Arrange
            setFaction(faction);

            // Act
            Formation formation1 = new Formation("Test Lance");
            Formation formation2 = newTeam();
            formation2.defaultFormationLevelForFormation(mockCampaign); //Shouldn't change anything
            formation1.addSubFormation(formation2, true);
            formation1.defaultFormationLevelForFormation(mockCampaign);

            // Assert
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 1), formation1.getFormationLevel());
            assertEquals(FormationLevel.parseFromDepth(mockCampaign, 0), formation2.getFormationLevel());
        }

        private void setFaction(Faction faction) {
            mockFaction = faction;
            when(mockCampaign.getFaction()).thenReturn(mockFaction);
        }

        private Formation newTeam() {
            Formation formation = new Formation("Test Team");
            for (int i = 0; i < MathUtility.roundTowardsZero(mockFaction.getFormationBaseSize() / 2.0); i++) {
                UUID unit = UUID.randomUUID();
                formation.addUnit(unit);
            }
            return formation;
        }

        private Formation newLance() {
            Formation formation = new Formation("Test Lance");
            for (int i = 0; i < mockFaction.getFormationBaseSize(); i++) {
                UUID unit = UUID.randomUUID();
                formation.addUnit(unit);
            }
            return formation;
        }

        private Formation newCompany() {
            Formation formation = new Formation("Test Company");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Formation newLance = newLance();
                newLance.defaultFormationLevelForFormation(mockCampaign);
                formation.addSubFormation(newLance, true);
            }
            return formation;
        }

        private Formation newBattalion() {
            Formation formation = new Formation("Test Battalion");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Formation newCompany = newCompany();
                newCompany.defaultFormationLevelForFormation(mockCampaign);
                formation.addSubFormation(newCompany, true);
            }
            return formation;
        }

        private Formation newRegiment() {
            Formation formation = new Formation("Test Regiment");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Formation newBattalion = newBattalion();
                newBattalion.defaultFormationLevelForFormation(mockCampaign);
                formation.addSubFormation(newBattalion, true);
            }
            return formation;
        }

        private Formation newBrigade() {
            Formation formation = new Formation("Test Brigade");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Formation newRegiment = newRegiment();
                newRegiment.defaultFormationLevelForFormation(mockCampaign);
                formation.addSubFormation(newRegiment, true);
            }
            return formation;
        }

        private Formation newDivision() {
            Formation formation = new Formation("Test Division");
            for (int i = 0; i < mockFaction.getFormationGrouping(); i++) {
                Formation newBrigade = newBrigade();
                newBrigade.defaultFormationLevelForFormation(mockCampaign);
                formation.addSubFormation(newBrigade, true);
            }
            return formation;
        }

    }
}
