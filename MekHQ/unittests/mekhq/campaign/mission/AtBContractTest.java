/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission;

import megamek.common.EquipmentType;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.mission.AtBContract.AtBContractRef;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AtBContractTest {
    private AtBContract contract;
    private Campaign campaign;
    private CampaignOptions options;

    @BeforeAll
    public static void initSingletons() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
        try {
            Factions.setInstance(Factions.loadDefault());
            Systems.setInstance(Systems.loadDefault());
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
     *  TODO: The following tests prefixed with old_* should be removed along with the deprecated methods
     *  they're testing when deemed safe to do so (roughly 0.50.4 or 0.50.5).
     */

    private static Stream<Arguments> provideContractDifficultyParameters() {
        return Stream.of(
            Arguments.of(500.0, 0.0, true, 10),
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
            Arguments.of(0.0, 0.0, false, -99)
        );
    }

    @ParameterizedTest
    @MethodSource("provideContractDifficultyParameters")
    public void old_calculateContractDifficultySameSkillMatchesExpectedRating(double enemyBV,
                                                                              double playerBV,
                                                                              boolean useGenericBattleValue,
                                                                              int expectedResult) {
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
    public void new_calculateContractDifficultySameSkillMatchesExpectedRating(double enemyBV,
                                                                              double playerBV,
                                                                              boolean useGenericBattleValue,
                                                                              int expectedResult) {
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
        return Stream.of(
            Arguments.of(3025, "LA", "Lyran Commonwealth"),
            Arguments.of(3059, "LA", "Lyran Alliance"),
            Arguments.of(-1, "LA", "Lyran Commonwealth"),
            Arguments.of(3025, "??", "Unknown"),
            Arguments.of(3025, "MERC", "")
        );
    }

    @ParameterizedTest
    @MethodSource("provideEnemyFactionAndYear")
    public void getEnemyNameReturnsCorrectValueInYear(int year, String enemyCode, String fullName) {
        contract.setEnemyCode(enemyCode);
        // TODO: fix this in the production code
        RandomCompanyNameGenerator.getInstance(); // Required in this codepath to generate a random merc company name
        assertEquals(fullName, contract.getEnemyName(year));
    }

    @Test
    public void getEnemyNameReturnsCorrectValueWhenMerc() {
        String name = "Testing Merc";
        contract.setEnemyCode("MERC");
        contract.setEnemyBotName(name);
        assertEquals(name, contract.getEnemyName(3025));
    }

    private static Stream<Arguments> provideEmployerNamesAndMercStatus() {
        return Stream.of(
            Arguments.of(3025, false, "LA", "Lyran Commonwealth"),
            Arguments.of(3059, false, "LA", "Lyran Alliance"),
            Arguments.of(3025, true, "LA", "Mercenary (Lyran Commonwealth)"),
            Arguments.of(3059, true, "LA", "Mercenary (Lyran Alliance)"),
            Arguments.of(-1, true, "LA", "Mercenary (Lyran Commonwealth)"),
            Arguments.of(3025, true, "??", "Mercenary (Unknown)")
        );
    }

    @ParameterizedTest
    @MethodSource("provideEmployerNamesAndMercStatus")
    public void getEmployerNameReturnsCorrectName(int year, boolean isMercSubcontract, String employerCode, String fullName) {
        contract.setEmployerCode(employerCode, year);
        contract.setMercSubcontract(isMercSubcontract);
        assertEquals(fullName, contract.getEmployerName(year));
    }
}
