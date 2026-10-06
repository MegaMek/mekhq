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
package mekhq.campaign.mission.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Sizing tests for {@link ContractUtilities#calculateBaseNumberOfRequiredLances} and
 * {@link ContractUtilities#calculateBaseNumberOfUnitsRequiredInCombatTeams} across the three formation sizes the game
 * fields (lance, star, Level II) and across lance-, company-, and mixed-level table of organization shapes.
 *
 * <p>The required-lance count is driven by the number of combat teams, while the required-unit count is driven by the
 * units inside them, so both are asserted on every shape: a table of organization that reports the right team count but
 * the wrong unit count would otherwise pass unnoticed.</p>
 */
class ContractUtilitiesFormationSizingTest {
    int nextForceId;

    Faction mockFaction;
    Campaign mockCampaign;

    LocalHangar hangar;

    public static Stream<Arguments> getFormationSizesForTests() {
        return Stream.of(
              //Arguments.of(3), //Society?
              Arguments.of(CombatTeam.LANCE_SIZE),
              Arguments.of(CombatTeam.STAR_SIZE),
              Arguments.of(CombatTeam.LEVEL_II_SIZE)
        );
    }

    @BeforeEach
    void beforeEach() {
        nextForceId = 0;
        hangar = new LocalHangar();

        mockFaction = mock(Faction.class);
        mockCampaign = mockCampaign();

        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(mockFaction);
        when(mockCampaign.getPlayerForce().getHangar()).thenReturn(hangar);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testNoForces(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(1, teams);
        assertEquals(1, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testOneLance(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        mockedCombatTeams.add(getMockLanceCombatTeam(formationSize));

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(1, teams);
        assertEquals(formationSize, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testThreeLances(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mockedCombatTeams.add(getMockLanceCombatTeam(formationSize));
        }

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(3, teams);
        assertEquals(3 * formationSize, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testNineLances(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            mockedCombatTeams.add(getMockLanceCombatTeam(formationSize));
        }

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(9, teams);
        assertEquals(9 * formationSize, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testOneCompany(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        mockedCombatTeams.add(getMockCompanyCombatTeam(formationSize));

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(1, teams);
        assertEquals(3 * formationSize, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testThreeCompanies(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mockedCombatTeams.add(getMockCompanyCombatTeam(formationSize));
        }

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(3, teams);
        assertEquals(3 * 3 * formationSize, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testNineCompanies(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            mockedCombatTeams.add(getMockCompanyCombatTeam(formationSize));
        }

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(9, teams);
        assertEquals(9 * 3 * formationSize, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testOneLanceAndOneCompany(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        mockedCombatTeams.add(getMockLanceCombatTeam(formationSize));
        mockedCombatTeams.add(getMockCompanyCombatTeam(formationSize));

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(2, teams);
        assertEquals(4 * formationSize, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testThreeLanceAndOneCompany(int formationSize) {
        // Arrange
        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mockedCombatTeams.add(getMockLanceCombatTeam(formationSize));
        }
        mockedCombatTeams.add(getMockCompanyCombatTeam(formationSize));

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(4, teams);
        assertEquals(6 * formationSize, requiredUnits);
    }

    @ParameterizedTest
    @MethodSource(value = "getFormationSizesForTests")
    void testLancesWithTeams(int formationSize) {
        // Arrange
        int forceId = getNextForceId();

        Vector<Object> mockUnits = new Vector<>();
        Vector<UUID> mockUUIDs = new Vector<>();
        for (int i = 0; i < 2; i++) {
            Unit mockUnit = getMockUnit(UnitType.MEK);
            mockUnits.add(mockUnit);
            mockUUIDs.add(mockUnit.getId());
        }

        Formation mockFormation = mock(Formation.class);
        when(mockFormation.getId()).thenReturn(forceId);
        when(mockFormation.isFormationType(FormationType.STANDARD)).thenReturn(true);
        when(mockFormation.getFormationLevel()).thenReturn(FormationLevel.INVALID);
        when(mockFormation.getAllChildren(mockCampaign)).thenReturn(mockUnits);
        when(mockFormation.getAllUnits(anyBoolean())).thenReturn(mockUUIDs);
        when(mockFormation.getCombatRoleInMemory()).thenReturn(CombatRole.FRONTLINE);

        forceId = getNextForceId();

        Vector<Object> mockUnits2 = new Vector<>();
        Vector<UUID> mockUUIDs2 = new Vector<>();
        for (int i = 0; i < 2; i++) {
            Unit mockUnit = getMockUnit(UnitType.MEK);
            mockUnits2.add(mockUnit);
            mockUUIDs2.add(mockUnit.getId());
        }

        Formation mockFormation2 = mock(Formation.class);
        when(mockFormation2.getId()).thenReturn(forceId);
        when(mockFormation2.isFormationType(FormationType.STANDARD)).thenReturn(true);
        when(mockFormation2.getFormationLevel()).thenReturn(FormationLevel.INVALID);
        when(mockFormation2.getAllChildren(mockCampaign)).thenReturn(mockUnits2);
        when(mockFormation2.getAllUnits(anyBoolean())).thenReturn(mockUUIDs2);
        when(mockFormation2.getCombatRoleInMemory()).thenReturn(CombatRole.FRONTLINE);

        forceId = getNextForceId();

        Vector<UUID> allMockUUIDs = new Vector<>();
        allMockUUIDs.addAll(mockUUIDs);
        allMockUUIDs.addAll(mockUUIDs2);

        Vector<Object> allForces = new Vector<>();
        allForces.add(mockFormation);
        allForces.add(mockFormation2);

        Formation finalFormation = mock(Formation.class);
        when(finalFormation.getId()).thenReturn(forceId);
        when(finalFormation.isFormationType(FormationType.STANDARD)).thenReturn(true);
        when(finalFormation.getFormationLevel()).thenReturn(FormationLevel.LANCE);
        when(finalFormation.getAllChildren(mockCampaign)).thenReturn(allForces);
        when(finalFormation.getAllUnits(anyBoolean())).thenReturn(allMockUUIDs);
        when(finalFormation.getCombatRoleInMemory()).thenReturn(CombatRole.FRONTLINE);

        forceId = getNextForceId();

        CombatTeam mockLanceCombatTeam = mock(CombatTeam.class);
        when(mockLanceCombatTeam.getSize(mockCampaign)).thenReturn(4);
        when(mockLanceCombatTeam.getFormation(mockCampaign)).thenReturn(finalFormation);
        when(mockLanceCombatTeam.getFormationId()).thenReturn(forceId);

        ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
        mockedCombatTeams.add(mockLanceCombatTeam);

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockedCombatTeams);

        // Act
        int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);
        int requiredUnits = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(mockCampaign);
        // Assert
        assertEquals(1, teams);
        assertEquals(4, requiredUnits);
    }


    /**
     * Lance-level formation, not necessarily a lance
     *
     * @param formationSize number of units in the formation
     *
     * @return A mocked CombatTeam of the desired size
     */
    private CombatTeam getMockLanceCombatTeam(int formationSize) {
        Formation mockFormation = getMockLanceForce(formationSize);
        int forceId = mockFormation.getId();

        when(mockFormation.getCombatRoleInMemory()).thenReturn(CombatRole.FRONTLINE);

        CombatTeam mockLance = mock(CombatTeam.class);
        when(mockLance.getSize(mockCampaign)).thenReturn(formationSize);
        when(mockLance.getFormation(mockCampaign)).thenReturn(mockFormation);
        when(mockLance.getFormationId()).thenReturn(forceId);
        return mockLance;
    }

    /**
     * Lance-level formation, not necessarily a lance
     *
     * @param formationSize number of units in the formation
     *
     * @return A mocked Force of the desired size
     */
    private Formation getMockLanceForce(int formationSize) {
        int forceId = getNextForceId();

        Vector<Object> mockUnits = new Vector<>();
        Vector<UUID> mockUUIDs = new Vector<>();
        for (int i = 0; i < formationSize; i++) {
            Unit mockUnit = getMockUnit(UnitType.MEK);
            mockUnits.add(mockUnit);
            mockUUIDs.add(mockUnit.getId());
        }

        Formation mockFormation = mock(Formation.class);
        when(mockFormation.getId()).thenReturn(forceId);
        when(mockFormation.isFormationType(FormationType.STANDARD)).thenReturn(true);
        when(mockFormation.getFormationLevel()).thenReturn(FormationLevel.LANCE);
        when(mockFormation.getAllChildren(mockCampaign)).thenReturn(mockUnits);
        when(mockFormation.getAllUnits(anyBoolean())).thenReturn(mockUUIDs);
        return mockFormation;
    }

    private CombatTeam getMockCompanyCombatTeam(int formationSize) {
        Formation mockFormation = getMockCompanyForce(formationSize);
        int forceId = mockFormation.getId();
        CombatTeam mockCompany = mock(CombatTeam.class);

        when(mockCompany.getSize(mockCampaign)).thenReturn(formationSize * 3);
        when(mockCompany.getFormation(mockCampaign)).thenReturn(mockFormation);
        when(mockFormation.getCombatRoleInMemory()).thenReturn(CombatRole.FRONTLINE);
        when(mockCompany.getFormationId()).thenReturn(forceId);

        return mockCompany;
    }

    private Formation getMockCompanyForce(int formationSize) {
        int forceId = getNextForceId();
        Formation mockCompany = mock(Formation.class);

        Vector<Object> subForces = new Vector<>();
        subForces.add(getMockLanceForce(formationSize));
        subForces.add(getMockLanceForce(formationSize));
        subForces.add(getMockLanceForce(formationSize));

        Vector<UUID> mockUUIDs = new Vector<>();
        for (Object subForce : subForces) {
            if (subForce instanceof Formation formation) {
                mockUUIDs.addAll(formation.getAllUnits(true));
            }
        }

        when(mockCompany.getId()).thenReturn(forceId);
        when(mockCompany.isFormationType(FormationType.STANDARD)).thenReturn(true);
        when(mockCompany.getFormationLevel()).thenReturn(FormationLevel.COMPANY);
        when(mockCompany.getAllChildren(mockCampaign)).thenReturn(subForces);
        when(mockCompany.getAllUnits(anyBoolean())).thenReturn(mockUUIDs);
        when(mockCompany.getCombatRoleInMemory()).thenReturn(CombatRole.FRONTLINE);

        return mockCompany;
    }

    private Unit getMockUnit(int unitType) {
        Entity mockEntity = getMockEntity(unitType);

        UUID uuid = UUID.randomUUID();

        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getEntity()).thenReturn(mockEntity);
        when(mockUnit.getId()).thenReturn(uuid);

        hangar.addUnit(mockUnit);

        return mockUnit;
    }

    private Entity getMockEntity(int unitType) {
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.getUnitType()).thenReturn(unitType);

        return mockEntity;
    }

    private int getNextForceId() {
        return ++nextForceId;
    }
}
