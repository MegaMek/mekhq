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
package mekhq.campaign.mission.contract.contractSpecialRules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Tests {@link PirateLooting#resolveActOfPiracy}: the piracy-avoidance roll (and its commander SPA modifiers), the
 * always-credited booty, the campaign-level vs per-character criminal-record consequences of getting caught, and the
 * StratCon Essential-scenario filter on component loot.
 */
class PirateLootingTest {
    private static final LocalDate DATE = LocalDate.of(3025, 6, 15);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private PlayerForce playerForce;
    private ForceHumanResources humanResources;
    private Finances finances;

    @BeforeEach
    void setUp() {
        campaignOptions = mock(CampaignOptions.class);
        // CampaignOptions.get(...) auto-unboxes to a primitive boolean at the call site, so an unstubbed mock (which
        // would otherwise return null) must be given an explicit default here to avoid a NullPointerException.
        when(campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(false);
        finances = mock(Finances.class);
        humanResources = mock(ForceHumanResources.class);
        // No commander by default: SLIPPERY/CONSPICUOUS/LOOT_GOBLIN tests override this explicitly.
        when(humanResources.getCommander(any(), anyBoolean(), any())).thenReturn(null);
        playerForce = mock(PlayerForce.class);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(playerForce.getFinances()).thenReturn(finances);
        when(playerForce.isClanForce()).thenReturn(false);
        campaign = mock(Campaign.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(DATE);
    }

    private static Person personWithOption(String option, boolean value, boolean employed) {
        Person person = mock(Person.class);
        when(person.isEmployed()).thenReturn(employed);
        PersonnelOptions options = mock(PersonnelOptions.class);
        if (option != null) {
            when(options.booleanOption(option)).thenReturn(value);
        }
        when(person.getOptions()).thenReturn(options);
        return person;
    }

    private static Scenario scenarioWith(int id, ScenarioStatus status) {
        Scenario scenario = mock(Scenario.class);
        when(scenario.getId()).thenReturn(id);
        when(scenario.getStatus()).thenReturn(status);
        return scenario;
    }

    // region avoidance roll and booty

    @Test
    void bootyIsCreditedEvenWhenTheForceIsNotCaught() {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> dialog = mockConstruction(ImmersiveDialogSimple.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(8); // >= TN(7): not caught
            compute.when(() -> Compute.d6(1)).thenReturn(3, 4); // loot roll -> 750, component roll -> 500
            when(campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(true);

            PirateLooting.resolveActOfPiracy(campaign, List.of(), 1, List.of(), null, true, "Raid");

            // (750 + 500) support points * 10,000 C-bills/point = 12,500,000
            verify(finances).credit(eq(TransactionType.THEFT), eq(DATE), eq(Money.of(12_500_000)), any());
            verify(playerForce, never()).changeChaosCampaignReputation(anyInt());
            assertEquals(1, dialog.constructed().size());
        }
    }

    @Test
    void gettingCaughtUnderCampaignLevelReputationAppliesTheDelta() {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> ignored = mockConstruction(ImmersiveDialogSimple.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(6); // < TN(7): caught
            compute.when(() -> Compute.d6(1)).thenReturn(3, 4); // 750 + 500 = 1250 SP
            when(campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(true);

            PirateLooting.resolveActOfPiracy(campaign, List.of(), 1, List.of(), null, true, "Raid");

            // delta = -ceil(1250 / 500) = -3
            verify(playerForce).changeChaosCampaignReputation(-3);
        }
    }

    @Test
    void invalidDiceStateFailsFastInsteadOfSilentlyMiscalculatingLoot() {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(8);
            compute.when(() -> Compute.d6(1)).thenReturn(0); // outside the documented 1-6 range

            assertThrows(IllegalStateException.class,
                  () -> PirateLooting.resolveActOfPiracy(campaign, List.of(), 1, List.of(), null, true, "Raid"));
        }
    }

    // endregion avoidance roll and booty

    // region commander SPA modifiers on the avoidance target number

    @Test
    void aSlipperyCommanderMakesTheForceHarderToCatch() {
        Person commander = personWithOption(PersonnelOptions.SLIPPERY, true, true);
        when(humanResources.getCommander(any(), anyBoolean(), any())).thenReturn(commander);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> ignored = mockConstruction(ImmersiveDialogSimple.class)) {
            // Baseline TN is 7; SLIPPERY lowers it to 6, so a roll of 6 is no longer a catch (6 < 6 is false).
            compute.when(() -> Compute.d6(2)).thenReturn(6);
            compute.when(() -> Compute.d6(1)).thenReturn(1, 1);
            when(campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(true);

            PirateLooting.resolveActOfPiracy(campaign, List.of(), 1, List.of(), null, true, "Raid");

            verify(playerForce, never()).changeChaosCampaignReputation(anyInt());
        }
    }

    @Test
    void aConspicuousCommanderMakesTheForceEasierToCatch() {
        Person commander = personWithOption(PersonnelOptions.CONSPICUOUS, true, true);
        when(humanResources.getCommander(any(), anyBoolean(), any())).thenReturn(commander);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> ignored = mockConstruction(ImmersiveDialogSimple.class)) {
            // Baseline TN is 7; CONSPICUOUS raises it to 8, so a roll of 7 is now a catch (7 < 8 is true).
            compute.when(() -> Compute.d6(2)).thenReturn(7);
            compute.when(() -> Compute.d6(1)).thenReturn(1, 1);
            when(campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(true);

            PirateLooting.resolveActOfPiracy(campaign, List.of(), 1, List.of(), null, true, "Raid");

            verify(playerForce).changeChaosCampaignReputation(anyInt());
        }
    }

    @Test
    void aLootGoblinCommanderRerollsALowLootRoll() {
        Person commander = personWithOption(PersonnelOptions.LOOT_GOBLIN, true, true);
        when(humanResources.getCommander(any(), anyBoolean(), any())).thenReturn(commander);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> ignored = mockConstruction(ImmersiveDialogSimple.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(8);
            // Loot roll comes back as 4 but LOOT_GOBLIN bumps it to 5 (base 1500) before scaling; component roll is 2
            // (base 300).
            compute.when(() -> Compute.d6(1)).thenReturn(4, 2);
            when(campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(true);

            PirateLooting.resolveActOfPiracy(campaign, List.of(), 1, List.of(), null, true, "Raid");

            // (1500 + 300) * 10,000 = 18,000,000 - without the reroll this would be (1250 + 300) * 10,000.
            verify(finances).credit(eq(TransactionType.THEFT), eq(DATE), eq(Money.of(18_000_000)), any());
        }
    }

    // endregion commander SPA modifiers on the avoidance target number

    // region per-character criminal record (campaign-level reputation off)

    @Test
    void notCaughtInPerCharacterModeAppliesNoBaseDeltaButLooseLipsStillRecords() {
        Person clean = personWithOption(null, false, true);
        Person looseLips = personWithOption(PersonnelOptions.LOOSE_LIPS, true, true);
        Person notEmployed = personWithOption(PersonnelOptions.LOOSE_LIPS, true, false);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> ignored = mockConstruction(ImmersiveDialogSimple.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(8); // not caught
            compute.when(() -> Compute.d6(1)).thenReturn(1, 1);
            when(campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(false);

            PirateLooting.resolveActOfPiracy(campaign, List.of(clean, looseLips, notEmployed), 1, List.of(), null,
                  true, "Raid");

            verify(clean, never()).changeCriminalRecord(anyInt());
            // "Leaves Paper Trail" always records one point, even on a clean getaway.
            verify(looseLips).changeCriminalRecord(-1);
            // Not employed: skipped entirely regardless of SPAs.
            verify(notEmployed, never()).changeCriminalRecord(anyInt());
        }
    }

    @Test
    void caughtInPerCharacterModeAppliesTheDeltaAndLeavesNoTrailSoftensIt() {
        Person plain = personWithOption(null, false, true);
        Person leavesNoTrail = personWithOption(PersonnelOptions.LEAVES_NO_TRAIL, true, true);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> ignored = mockConstruction(ImmersiveDialogSimple.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(2); // caught
            compute.when(() -> Compute.d6(1)).thenReturn(3, 4); // 750 + 500 = 1250 SP -> delta -3
            when(campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)).thenReturn(false);

            PirateLooting.resolveActOfPiracy(campaign, List.of(plain, leavesNoTrail), 1, List.of(), null, true,
                  "Raid");

            verify(plain).changeCriminalRecord(-3);
            // Softened by one point, never past zero.
            verify(leavesNoTrail).changeCriminalRecord(-2);
        }
    }

    // endregion per-character criminal record (campaign-level reputation off)

    // region component loot - victory status and StratCon Essential filtering

    @Test
    void onlyAnExactVictoryCountsTowardsComponentLootWithoutStratCon() {
        // isVictory() only matches VICTORY - MARGINAL/PYRRHIC/DECISIVE victories do not count here, unlike the
        // "overall victory" family used elsewhere (e.g. TwoConsecutiveTracks).
        List<Scenario> scenarios = List.of(scenarioWith(1, ScenarioStatus.VICTORY),
              scenarioWith(2, ScenarioStatus.MARGINAL_VICTORY),
              scenarioWith(3, ScenarioStatus.DECISIVE_VICTORY));

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> ignored = mockConstruction(ImmersiveDialogSimple.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(8);
            compute.when(() -> Compute.d6(1)).thenReturn(1);

            PirateLooting.resolveActOfPiracy(campaign, List.of(), 1, scenarios, null, true, "Raid");

            // 1 (loot roll) + 2 (runningTotal 1 -> loop i=0,1) = 3 calls to d6(1).
            compute.verify(() -> Compute.d6(1), times(3));
        }
    }

    @Test
    void onlyEssentialScenariosCountTowardsComponentLootUnderStratCon() {
        Scenario essentialWin = scenarioWith(100, ScenarioStatus.VICTORY);
        Scenario nonEssentialWin = scenarioWith(200, ScenarioStatus.VICTORY);
        Scenario otherEssentialWin = scenarioWith(300, ScenarioStatus.VICTORY);
        List<Scenario> scenarios = List.of(essentialWin, nonEssentialWin, otherEssentialWin);

        StratConScenario essentialTrackEntry = mock(StratConScenario.class);
        when(essentialTrackEntry.isStrategicObjective()).thenReturn(true);
        when(essentialTrackEntry.getBackingScenarioID()).thenReturn(100);

        StratConScenario nonEssentialTrackEntry = mock(StratConScenario.class);
        when(nonEssentialTrackEntry.isStrategicObjective()).thenReturn(false);

        StratConScenario otherEssentialTrackEntry = mock(StratConScenario.class);
        when(otherEssentialTrackEntry.isStrategicObjective()).thenReturn(true);
        when(otherEssentialTrackEntry.getBackingScenarioID()).thenReturn(300);

        Map<StratConCoords, StratConScenario> trackScenarios = Map.of(mock(StratConCoords.class),
              essentialTrackEntry,
              mock(StratConCoords.class),
              nonEssentialTrackEntry,
              mock(StratConCoords.class),
              otherEssentialTrackEntry);

        StratConTrackState track = mock(StratConTrackState.class);
        when(track.getScenarios()).thenReturn(trackScenarios);

        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(campaignState.getTracks()).thenReturn(List.of(track));

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedConstruction<ImmersiveDialogSimple> ignored = mockConstruction(ImmersiveDialogSimple.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(8);
            compute.when(() -> Compute.d6(1)).thenReturn(1);

            PirateLooting.resolveActOfPiracy(campaign, List.of(), 1, scenarios, campaignState, true, "Raid");

            // 1 (loot roll) + 3 (2 Essential wins -> runningTotal 2 -> loop i=0,1,2) = 4 calls to d6(1); the
            // non-essential win at id 200 must not be counted even though it is also a victory.
            compute.verify(() -> Compute.d6(1), times(4));
        }
    }

    // endregion component loot - victory status and StratCon Essential filtering
}
