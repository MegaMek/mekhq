/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Stream;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.mission.AtBContract.AtBContractRef;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.TestSystems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AtBContractTest {
    private AtBContract contract;
    private Campaign campaign;
    private CampaignOptions options;

    @BeforeAll
    public static void initSingletons() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
        // TODO: fix this in the production code
        RandomCallsignGenerator.getInstance(true); // Required in this code path to generate a random merc company name
        RandomCompanyNameGenerator.getInstance(); // Required in this code path to generate a random merc company name
        try {
            Factions.setInstance(Factions.loadDefault(true));
            Systems.setInstance(TestSystems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    @BeforeEach
    void setup() {
        campaign = mock(Campaign.class);
        options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        contract = new AtBContract();
    }

    @Test
    public void atbContractRestoreDoesNothingWithoutParent() {
        Campaign mockCampaign = mock(Campaign.class);

        int childId = 2;
        AtBContract child = new AtBContract();
        child.setId(childId);
        child.setParentContract(null);

        // Restore the AtBContract
        child.restore(mockCampaign);

        verify(mockCampaign, times(0)).getMission(anyInt());

        // Ensure the parent is not set
        assertNull(child.getParentContract());
    }

    @Test
    public void atbContractRestoresRefs() {
        Campaign mockCampaign = mock(Campaign.class);

        int parentId = 1;
        AtBContract parent = mock(AtBContract.class);
        when(parent.getId()).thenReturn(parentId);
        doReturn(parent).when(mockCampaign).getMission(eq(parentId));

        int childId = 2;
        AtBContract child = new AtBContract();
        child.setId(childId);
        child.setParentContract(new AtBContractRef(parentId));
        doReturn(child).when(mockCampaign).getMission(eq(childId));

        int otherId = 3;
        AtBContract other = mock(AtBContract.class);
        when(other.getId()).thenReturn(otherId);
        doReturn(other).when(mockCampaign).getMission(eq(otherId));

        // Restore the AtBContract
        child.restore(mockCampaign);

        // Ensure the parent is set properly
        assertEquals(parent, child.getParentContract());
    }

    @Test
    public void atbContractRestoreClearsParentIfMissing() {
        Campaign mockCampaign = mock(Campaign.class);

        int parentId = 1;
        doReturn(null).when(mockCampaign).getMission(eq(parentId));

        int childId = 2;
        AtBContract child = new AtBContract();
        child.setId(childId);
        child.setParentContract(new AtBContractRef(parentId));
        doReturn(child).when(mockCampaign).getMission(eq(childId));

        int otherId = 3;
        AtBContract other = mock(AtBContract.class);
        when(other.getId()).thenReturn(otherId);
        doReturn(other).when(mockCampaign).getMission(eq(otherId));

        // Restore the AtBContract
        child.restore(mockCampaign);

        // Ensure the parent is null because it is missing
        assertNull(child.getParentContract());
    }

    @Test
    public void atbContractRestoreClearsParentIfWrongType() {
        Campaign mockCampaign = mock(Campaign.class);

        int parentId = 1;
        Contract parent = mock(Contract.class);
        when(parent.getId()).thenReturn(parentId);
        doReturn(parent).when(mockCampaign).getMission(eq(parentId));

        int childId = 2;
        AtBContract child = new AtBContract();
        child.setId(childId);
        child.setParentContract(new AtBContractRef(parentId));
        doReturn(child).when(mockCampaign).getMission(eq(childId));

        int otherId = 3;
        AtBContract other = mock(AtBContract.class);
        when(other.getId()).thenReturn(otherId);
        doReturn(other).when(mockCampaign).getMission(eq(otherId));

        // Restore the AtBContract
        child.restore(mockCampaign);

        // Ensure the parent is null because it is not the correct type of contract
        assertNull(child.getParentContract());
    }

    @Test
    public void atbContractSharesPercentMatchesPreviousSetting() {
        AtBContract contract = new AtBContract("Test");
        contract.setAtBSharesPercent(50);
        assertEquals(50, contract.getSharesPercent());
    }

    /*
     *  TODO: The following tests prefixed with old_* should be removed along with the deprecated methods they're
     *   testing when deemed safe to do so (roughly 0.50.4 or 0.50.5).
     */

    private static Stream<Arguments> provideContractDifficultyParameters() {
        return Stream.of(Arguments.of(500.0, 0.0, true, 10),
              Arguments.of(500.0, 0.0, false, 10),
              Arguments.of(500.0, 500.0, true, 5),
              Arguments.of(500.0, 500.0, false, 5),
              Arguments.of(500.0, 2000.0, true, 1),
              Arguments.of(500.0, 2000.0, false, 1),
              Arguments.of(500.0, 525.0, true, 4),
              Arguments.of(500.0, 525.0, false, 4),
              Arguments.of(500.0, 350.0, true, 7),
              Arguments.of(500.0, 350.0, false, 7),
              Arguments.of(0.0, 0.0, true, -99),
              Arguments.of(0.0, 0.0, false, -99));
    }

    @ParameterizedTest
    @MethodSource("provideContractDifficultyParameters")
    public void old_calculateContractDifficultySameSkillMatchesExpectedRating(double enemyBV, double playerBV,
          boolean useGenericBattleValue, int expectedResult) {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(enemyBV).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(playerBV).when(contract).estimatePlayerPower(any(Campaign.class));
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(useGenericBattleValue);

        int difficulty = contract.calculateContractDifficulty(campaign);
        assertEquals(expectedResult, difficulty);
    }

    @ParameterizedTest
    @MethodSource("provideContractDifficultyParameters")
    public void new_calculateContractDifficultySameSkillMatchesExpectedRating(double enemyBV, double playerBV,
          boolean useGenericBattleValue, int expectedResult) {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(enemyBV).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(playerBV).when(contract).estimatePlayerPower(anyList(), anyBoolean());
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(useGenericBattleValue);

        int difficulty = contract.calculateContractDifficulty(3025, true, new ArrayList<>());
        assertEquals(expectedResult, difficulty);
    }

    @Test
    public void setContractTypeUpdatesParentMissionType() {
        contract.setContractType(AtBContractType.CADRE_DUTY);
        assertEquals(AtBContractType.CADRE_DUTY, contract.getContractType());
        assertEquals("Cadre Duty", contract.getType());
    }

    private static Stream<Arguments> provideEnemyFactionAndYear() {
        return Stream.of(Arguments.of(3025, "LA", "Lyran Commonwealth"),
              Arguments.of(3059, "LA", "Lyran Alliance"),
              Arguments.of(-1, "LA", "Lyran Commonwealth"),
              Arguments.of(3025, "??", "Unknown"));
    }

    @ParameterizedTest
    @MethodSource("provideEnemyFactionAndYear")
    public void getEnemyNameReturnsCorrectValueInYear(int year, String enemyCode, String fullName) {
        contract.setEnemyCode(enemyCode);
        assertEquals(fullName, contract.getEnemyName(year));
    }

    @Test
    public void getEnemyNameReturnsCorrectValueWhenMerc() {
        String name = "Testing Merc";
        contract.setEnemyCode(MERCENARY_FACTION_CODE);
        contract.setEnemyBotName(name);
        assertEquals(name, contract.getEnemyName(3025));
    }

    @Test
    public void getEnemyNameReturnsNonNullWhenMercAndBotNameNotSet() {
        contract.setEnemyCode("MERC");
        assertNotEquals("", contract.getEnemyName(3025));
    }

    private static Stream<Arguments> provideEmployerNamesAndMercStatus() {
        return Stream.of(Arguments.of(3025, false, "LA", "Lyran Commonwealth"),
              Arguments.of(3059, false, "LA", "Lyran Alliance"),
              Arguments.of(3025, true, "LA", "Mercenary (Lyran Commonwealth)"),
              Arguments.of(3059, true, "LA", "Mercenary (Lyran Alliance)"),
              Arguments.of(-1, true, "LA", "Mercenary (Lyran Commonwealth)"),
              Arguments.of(3025, true, "??", "Mercenary (Unknown)"));
    }

    @ParameterizedTest
    @MethodSource("provideEmployerNamesAndMercStatus")
    public void getEmployerNameReturnsCorrectName(int year, boolean isMercSubcontract, String employerCode,
          String fullName) {
        contract.setEmployerCode(employerCode, year);
        contract.setMercSubcontract(isMercSubcontract);
        assertEquals(fullName, contract.getEmployerName(year));
    }

    @Nested
    class AtBContractCalculateRequiredLancesTests {
        int nextForceId;

        Faction mockFaction;
        Campaign mockCampaign;

        Hangar hangar;

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
            hangar = new Hangar();

            mockFaction = mock(Faction.class);
            mockCampaign = mock(Campaign.class);

            when(mockCampaign.getFaction()).thenReturn(mockFaction);
            when(mockCampaign.getHangar()).thenReturn(hangar);
        }

        @ParameterizedTest
        @MethodSource(value = "getFormationSizesForTests")
        void testNoForces(int formationSize) {
            // Arrange
            ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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

            Force mockForce = mock(Force.class);
            when(mockForce.getId()).thenReturn(forceId);
            when(mockForce.isForceType(ForceType.STANDARD)).thenReturn(true);
            when(mockForce.getFormationLevel()).thenReturn(FormationLevel.INVALID);
            when(mockForce.getAllChildren(mockCampaign)).thenReturn(mockUnits);
            when(mockForce.getAllUnits(anyBoolean())).thenReturn(mockUUIDs);

            forceId = getNextForceId();

            Vector<Object> mockUnits2 = new Vector<>();
            Vector<UUID> mockUUIDs2 = new Vector<>();
            for (int i = 0; i < 2; i++) {
                Unit mockUnit = getMockUnit(UnitType.MEK);
                mockUnits2.add(mockUnit);
                mockUUIDs2.add(mockUnit.getId());
            }

            Force mockForce2 = mock(Force.class);
            when(mockForce2.getId()).thenReturn(forceId);
            when(mockForce2.isForceType(ForceType.STANDARD)).thenReturn(true);
            when(mockForce2.getFormationLevel()).thenReturn(FormationLevel.INVALID);
            when(mockForce2.getAllChildren(mockCampaign)).thenReturn(mockUnits2);
            when(mockForce2.getAllUnits(anyBoolean())).thenReturn(mockUUIDs2);

            forceId = getNextForceId();

            Vector<UUID> allMockUUIDs = new Vector<>();
            allMockUUIDs.addAll(mockUUIDs);
            allMockUUIDs.addAll(mockUUIDs2);

            Vector<Object> allForces = new Vector<>();
            allForces.add(mockForce);
            allForces.add(mockForce2);

            Force finalForce = mock(Force.class);
            when(finalForce.getId()).thenReturn(forceId);
            when(finalForce.isForceType(ForceType.STANDARD)).thenReturn(true);
            when(finalForce.getFormationLevel()).thenReturn(FormationLevel.LANCE);
            when(finalForce.getAllChildren(mockCampaign)).thenReturn(allForces);
            when(finalForce.getAllUnits(anyBoolean())).thenReturn(allMockUUIDs);

            forceId = getNextForceId();

            CombatTeam mockLanceCombatTeam = mock(CombatTeam.class);
            when(mockLanceCombatTeam.getSize(mockCampaign)).thenReturn(4);
            when(mockLanceCombatTeam.getForce(mockCampaign)).thenReturn(finalForce);
            when(mockLanceCombatTeam.getForceId()).thenReturn(forceId);

            ArrayList<CombatTeam> mockedCombatTeams = new ArrayList<>();
            mockedCombatTeams.add(mockLanceCombatTeam);

            when(mockCampaign.getAllCombatTeams()).thenReturn(mockedCombatTeams);

            // Act
            int teams = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false);
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
            Force mockForce = getMockLanceForce(formationSize);
            int forceId = mockForce.getId();

            CombatTeam mockLance = mock(CombatTeam.class);
            when(mockLance.getSize(mockCampaign)).thenReturn(formationSize);
            when(mockLance.getForce(mockCampaign)).thenReturn(mockForce);
            when(mockLance.getForceId()).thenReturn(forceId);
            return mockLance;
        }

        /**
         * Lance-level formation, not necessarily a lance
         *
         * @param formationSize number of units in the formation
         *
         * @return A mocked Force of the desired size
         */
        private Force getMockLanceForce(int formationSize) {
            int forceId = getNextForceId();

            Vector<Object> mockUnits = new Vector<>();
            Vector<UUID> mockUUIDs = new Vector<>();
            for (int i = 0; i < formationSize; i++) {
                Unit mockUnit = getMockUnit(UnitType.MEK);
                mockUnits.add(mockUnit);
                mockUUIDs.add(mockUnit.getId());
            }

            Force mockForce = mock(Force.class);
            when(mockForce.getId()).thenReturn(forceId);
            when(mockForce.isForceType(ForceType.STANDARD)).thenReturn(true);
            when(mockForce.getFormationLevel()).thenReturn(FormationLevel.LANCE);
            when(mockForce.getAllChildren(mockCampaign)).thenReturn(mockUnits);
            when(mockForce.getAllUnits(anyBoolean())).thenReturn(mockUUIDs);
            return mockForce;
        }

        private CombatTeam getMockCompanyCombatTeam(int formationSize) {
            Force mockForce = getMockCompanyForce(formationSize);
            int forceId = mockForce.getId();
            CombatTeam mockCompany = mock(CombatTeam.class);

            when(mockCompany.getSize(mockCampaign)).thenReturn(formationSize * 3);
            when(mockCompany.getForce(mockCampaign)).thenReturn(mockForce);
            when(mockCompany.getForceId()).thenReturn(forceId);

            return mockCompany;
        }

        private Force getMockCompanyForce(int formationSize) {
            int forceId = getNextForceId();
            Force mockCompany = mock(Force.class);

            Vector<Object> subForces = new Vector<>();
            subForces.add(getMockLanceForce(formationSize));
            subForces.add(getMockLanceForce(formationSize));
            subForces.add(getMockLanceForce(formationSize));

            Vector<UUID> mockUUIDs = new Vector<>();
            for (Object subForce : subForces) {
                if (subForce instanceof Force force) {
                    mockUUIDs.addAll(force.getAllUnits(true));
                }
            }

            when(mockCompany.getId()).thenReturn(forceId);
            when(mockCompany.isForceType(ForceType.STANDARD)).thenReturn(true);
            when(mockCompany.getFormationLevel()).thenReturn(FormationLevel.COMPANY);
            when(mockCompany.getAllChildren(mockCampaign)).thenReturn(subForces);
            when(mockCompany.getAllUnits(anyBoolean())).thenReturn(mockUUIDs);

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
}
