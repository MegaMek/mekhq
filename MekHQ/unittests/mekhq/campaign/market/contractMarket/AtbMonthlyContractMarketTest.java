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
package mekhq.campaign.market.contractMarket;

import static megamek.common.enums.SkillLevel.REGULAR;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionHints.FactionHints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AtbMonthlyContractMarketTest {
    private static final int GAME_YEAR = 3067;
    private static final LocalDate TODAY = LocalDate.ofYearDay(GAME_YEAR, 1);

    @AfterEach
    void tearDown() {
        Factions.setInstance(null);
        Systems.setInstance(null);
        RandomFactionGenerator.setInstance(null);
    }

    @Test
    void addAtBContractRetriesWhenJumpPathDoesNotReachContractSystem() {
        TestContext context = new TestContext();

        try (MockedStatic<ContractTypePicker> contractTypePicker = mockStatic(ContractTypePicker.class);
              MockedStatic<ContractUtilities> contractUtilities = mockStatic(ContractUtilities.class);
              MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            contractTypePicker.when(() -> ContractTypePicker.findMissionType(context.employerFaction, 0, 0))
                  .thenReturn(AtBContractType.GARRISON_DUTY);
            contractUtilities.when(ContractUtilities::calculateVarianceFactor).thenReturn(1.0);
            contractUtilities.when(() -> ContractUtilities.calculateBaseNumberOfRequiredLances(context.campaign,
                        false,
                        false,
                        1.0))
                  .thenReturn(1);
            contractUtilities.when(() -> ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(
                  context.campaign)).thenReturn(1);
            contractUtilities.when(() -> ContractUtilities.getEffectiveNumUnits(context.campaign)).thenReturn(1);
            combatTeam.when(() -> CombatTeam.getStandardFormationSize(any(), anyInt())).thenReturn(4);

            AtBContract contract = new AtbMonthlyContractMarket().addAtBContract(context.campaign);

            assertNotNull(contract);
            assertSame(context.reachableTargetSystem, contract.getSystem());
            assertSame(context.validJumpPath, contract.getCachedJumpPath());
            verify(context.campaign).calculateJumpPath(context.currentSystem, context.unreachableTargetSystem);
            verify(context.campaign).calculateJumpPath(context.currentSystem, context.reachableTargetSystem);
        }
    }

    private static class TestContext {
        private static final String EMPLOYER_CODE = "EMPLOYER";
        private static final String ENEMY_CODE = "ENEMY";
        private static final String CURRENT_SYSTEM_ID = "CURRENT";
        private static final String UNREACHABLE_TARGET_ID = "UNREACHABLE_TARGET";
        private static final String REACHABLE_TARGET_ID = "REACHABLE_TARGET";
        private static final String INTERMEDIATE_SYSTEM_ID = "INTERMEDIATE";

        private final Campaign campaign = mock(Campaign.class);
        private final Faction employerFaction = mockFaction(EMPLOYER_CODE, "Contract Employer");
        private final Faction enemyFaction = mockFaction(ENEMY_CODE, "Contract Enemy");
        private final PlanetarySystem currentSystem = mockSystem(CURRENT_SYSTEM_ID);
        private final PlanetarySystem unreachableTargetSystem = mockSystem(UNREACHABLE_TARGET_ID);
        private final PlanetarySystem reachableTargetSystem = mockSystem(REACHABLE_TARGET_ID);
        private final PlanetarySystem intermediateSystem = mockSystem(INTERMEDIATE_SYSTEM_ID);
        private final JumpPath partialJumpPath = mockJumpPath(currentSystem, intermediateSystem);
        private final JumpPath validJumpPath = mockJumpPath(currentSystem, reachableTargetSystem);

        private TestContext() {
            when(enemyFaction.isAggregate()).thenReturn(true);
            setupCampaign();
            setupFactions();
            setupSystems();
            setupRandomFactionGenerator();
            setupJumpPaths();
        }

        private void setupCampaign() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            when(campaignOptions.getContractMaxSalvagePercentage()).thenReturn(100);

            ReputationController reputation = mock(ReputationController.class);
            when(reputation.getReputationFactor()).thenReturn(1.0);
            when(reputation.getAverageSkillLevel()).thenReturn(REGULAR);

            Accountant accountant = mock(Accountant.class);
            when(accountant.getContractBase()).thenReturn(Money.of(1));
            when(accountant.getOverheadExpenses()).thenReturn(Money.of(1));
            when(accountant.getPeacetimeCost()).thenReturn(Money.of(1));

            Hangar hangar = mock(Hangar.class);
            doReturn(Money.of(1)).when(hangar).getUnitCosts(any(), any());

            when(campaign.getFaction()).thenReturn(employerFaction);
            when(campaign.getAtBUnitRatingMod()).thenReturn(DragoonRating.DRAGOON_C.getRating());
            when(campaign.getLocalDate()).thenReturn(TODAY);
            when(campaign.getGameYear()).thenReturn(GAME_YEAR);
            when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
            when(campaign.getReputation()).thenReturn(reputation);
            when(campaign.getAccountant()).thenReturn(accountant);
            when(campaign.getHangar()).thenReturn(hangar);
            when(campaign.getCurrentSystem()).thenReturn(currentSystem);
            when(campaign.getFutureAtBContracts()).thenReturn(List.of());
        }

        private void setupFactions() {
            Faction invalidComStar = mockFaction("CS", "ComStar");
            Faction invalidWordOfBlake = mockFaction("WOB", "Word of Blake");
            when(invalidComStar.validIn(any(LocalDate.class))).thenReturn(false);
            when(invalidWordOfBlake.validIn(any(LocalDate.class))).thenReturn(false);

            Map<String, Faction> factionsByCode = new HashMap<>();
            factionsByCode.put(EMPLOYER_CODE, employerFaction);
            factionsByCode.put(ENEMY_CODE, enemyFaction);
            factionsByCode.put("CS", invalidComStar);
            factionsByCode.put("WOB", invalidWordOfBlake);
            factionsByCode.put("IND", mockFaction("IND", "Independent"));
            factionsByCode.put("MERC", mockFaction("MERC", "Mercenary"));
            factionsByCode.put("PIR", mockFaction("PIR", "Pirates"));
            factionsByCode.put("REB", mockFaction("REB", "Rebels"));

            Factions factions = mock(Factions.class);
            doAnswer(invocation -> factionsByCode.get(invocation.getArgument(0))).when(factions).getFaction(anyString());
            Factions.setInstance(factions);
        }

        private void setupSystems() {
            Map<String, PlanetarySystem> systemsById = new HashMap<>();
            systemsById.put(CURRENT_SYSTEM_ID, currentSystem);
            systemsById.put(UNREACHABLE_TARGET_ID, unreachableTargetSystem);
            systemsById.put(REACHABLE_TARGET_ID, reachableTargetSystem);
            systemsById.put(INTERMEDIATE_SYSTEM_ID, intermediateSystem);

            Systems systems = mock(Systems.class);
            doAnswer(invocation -> systemsById.get(invocation.getArgument(0))).when(systems).getSystemById(anyString());
            Systems.setInstance(systems);
        }

        private void setupRandomFactionGenerator() {
            FactionHints factionHints = mock(FactionHints.class);
            when(factionHints.isNeutral(any(Faction.class))).thenReturn(false);

            RandomFactionGenerator randomFactionGenerator = mock(RandomFactionGenerator.class);
            when(randomFactionGenerator.getFactionHints()).thenReturn(factionHints);
            when(randomFactionGenerator.getEnemy(EMPLOYER_CODE, true)).thenReturn(ENEMY_CODE);
            when(randomFactionGenerator.getMissionTarget(anyString(), anyString()))
                  .thenReturn(UNREACHABLE_TARGET_ID)
                  .thenReturn(REACHABLE_TARGET_ID);
            RandomFactionGenerator.setInstance(randomFactionGenerator);
        }

        private void setupJumpPaths() {
            doReturn(partialJumpPath).when(campaign).calculateJumpPath(currentSystem, unreachableTargetSystem);
            doReturn(validJumpPath).when(campaign).calculateJumpPath(currentSystem, reachableTargetSystem);
        }

        private static Faction mockFaction(String code, String fullName) {
            Faction faction = mock(Faction.class);
            when(faction.getShortName()).thenReturn(code);
            when(faction.getFullName(anyInt())).thenReturn(fullName);
            when(faction.getCamosFolder(anyInt())).thenReturn(Optional.empty());
            when(faction.validIn(any(LocalDate.class))).thenReturn(true);
            return faction;
        }

        private static PlanetarySystem mockSystem(String id) {
            PlanetarySystem system = mock(PlanetarySystem.class);
            when(system.getId()).thenReturn(id);
            when(system.getName(any(LocalDate.class))).thenReturn(id);
            return system;
        }

        private static JumpPath mockJumpPath(PlanetarySystem firstSystem, PlanetarySystem lastSystem) {
            JumpPath jumpPath = mock(JumpPath.class);
            when(jumpPath.isEmpty()).thenReturn(false);
            when(jumpPath.getFirstSystem()).thenReturn(firstSystem);
            when(jumpPath.getLastSystem()).thenReturn(lastSystem);
            when(jumpPath.getJumps()).thenReturn(1);
            when(jumpPath.getTotalTime(any(LocalDate.class), anyInt(), anyBoolean())).thenReturn(10.0);
            return jumpPath;
        }
    }
}