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
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AtBContractTest {
    private AtBContract contract;
    private Campaign campaign;
    private CampaignOptions options;

    @BeforeAll
    public static void setup() {
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
    void setUp() {
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

    @Test
    void old_calculateContractDifficultyEqualSkillShouldBe10() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(0.0).when(contract).estimatePlayerPower(any(Campaign.class));
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(campaign);
        assertEquals(10, difficulty);
    }

    @Test
    void old_calculateContractDifficultyEqualSkillShouldBe5() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(500.0).when(contract).estimatePlayerPower(any(Campaign.class));
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(campaign);
        assertEquals(5, difficulty);
    }

    @Test
    void old_calculateContractDifficultyEqualSkillShouldBe1() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(2000.0).when(contract).estimatePlayerPower(any(Campaign.class));
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(campaign);
        assertEquals(1, difficulty);
    }

    @Test
    void old_calculateContractDifficultyEqualSkillShouldBe4() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(525.0).when(contract).estimatePlayerPower(any(Campaign.class));
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(campaign);
        assertEquals(4, difficulty);
    }

    @Test
    void old_calculateContractDifficultyEqualSkillShouldBe7() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(350.0).when(contract).estimatePlayerPower(any(Campaign.class));
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(campaign);
        assertEquals(7, difficulty);
    }

    @Test
    void old_calculateContractDifficultyNoEnemyRatingShouldBeError() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(0.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(0.0).when(contract).estimatePlayerPower(any(Campaign.class));
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(campaign);
        assertEquals(-99, difficulty);
    }

    @Test
    void new_calculateContractDifficultyEqualSkillShouldBe10() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(0.0).when(contract).estimatePlayerPower(anyList(), anyBoolean());
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(3025, true, new ArrayList<>());
        assertEquals(10, difficulty);
    }

    @Test
    void new_calculateContractDifficultyEqualSkillShouldBe5() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(500.0).when(contract).estimatePlayerPower(anyList(), anyBoolean());
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(3025, true, new ArrayList<>());
        assertEquals(5, difficulty);
    }

    @Test
    void new_calculateContractDifficultyEqualSkillShouldBe1() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(2000.0).when(contract).estimatePlayerPower(anyList(), anyBoolean());
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(3025, true, new ArrayList<>());
        assertEquals(1, difficulty);
    }

    @Test
    void new_calculateContractDifficultyEqualSkillShouldBe4() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(525.0).when(contract).estimatePlayerPower(anyList(), anyBoolean());
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(3025, true, new ArrayList<>());
        assertEquals(4, difficulty);
    }

    @Test
    void new_calculateContractDifficultyEqualSkillShouldBe7() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(500.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(350.0).when(contract).estimatePlayerPower(anyList(), anyBoolean());
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(3025, true, new ArrayList<>());
        assertEquals(7, difficulty);
    }

    @Test
    void new_calculateContractDifficultyNoEnemyRatingShouldBeError() {
        contract = spy(contract);
        doReturn(SkillLevel.REGULAR).when(contract).modifySkillLevelBasedOnFaction(anyString(), any(SkillLevel.class));
        doReturn(0.0).when(contract).estimateMekStrength(anyInt(), anyBoolean(), anyString(), anyInt());
        doReturn(0.0).when(contract).estimatePlayerPower(anyList(), anyBoolean());
        when(campaign.getGameYear()).thenReturn(3025);
        when(options.isUseGenericBattleValue()).thenReturn(true);

        int difficulty = contract.calculateContractDifficulty(3025, true, new ArrayList<>());
        assertEquals(-99, difficulty);
    }
}
