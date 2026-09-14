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

import static mekhq.campaign.force.Formation.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.client.ui.enums.DialogResult;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignNewDayManager;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.contract.utilities.ContractCharacteristics;
import mekhq.campaign.mission.contract.utilities.ContractEmergencyExtension;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.prisoners.PrisonerMissionEndEvent;
import mekhq.campaign.reputation.chaosReputation.ChaosReputation;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.dialog.CompleteMissionDialog;
import mekhq.gui.dialog.RetirementDefectionDialog;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link MissionCompletionManager}.
 *
 * <p>The package-private static helpers are exercised directly, one behaviour per test. The dialog-driven
 * orchestration in {@link MissionCompletionManager#completeMission()} is exercised through {@code mockConstruction}
 * (for the dialogs it builds) and {@code mockStatic} (for the utility calls it makes), covering the happy path and
 * each early-abort branch.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class MissionCompletionManagerTest {
    private static final String NON_PIRATE_FACTION_CODE = "LA";
    private static final LocalDate TODAY = LocalDate.of(3067, 1, 1);

    // region getMissionExperienceAward
    @Nested
    public class GetMissionExperienceAward {
        @Test
        void failedReturnsFailureExperience() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);
            when(campaignOptions.get(CampaignOption.MISSION_XP_FAIL)).thenReturn(3);

            int award = MissionCompletionManager.getMissionExperienceAward(campaignOptions, MissionStatus.FAILED,
                  mission);

            assertEquals(3, award);
        }

        @Test
        void breachReturnsFailureExperience() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);
            when(campaignOptions.get(CampaignOption.MISSION_XP_FAIL)).thenReturn(3);

            int award = MissionCompletionManager.getMissionExperienceAward(campaignOptions, MissionStatus.BREACH,
                  mission);

            assertEquals(3, award);
        }

        @Test
        void activeReturnsZero() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);

            int award = MissionCompletionManager.getMissionExperienceAward(campaignOptions, MissionStatus.ACTIVE,
                  mission);

            assertEquals(0, award);
        }

        @Test
        void successWithoutStratConReturnsSuccessExperience() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getStratConCampaignState()).thenReturn(null);
            when(campaignOptions.get(CampaignOption.MISSION_XP_SUCCESS)).thenReturn(5);

            int award = MissionCompletionManager.getMissionExperienceAward(campaignOptions, MissionStatus.SUCCESS,
                  mission);

            assertEquals(5, award);
        }

        @Test
        void partialWithoutStratConReturnsSuccessExperience() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getStratConCampaignState()).thenReturn(null);
            when(campaignOptions.get(CampaignOption.MISSION_XP_SUCCESS)).thenReturn(5);

            int award = MissionCompletionManager.getMissionExperienceAward(campaignOptions, MissionStatus.PARTIAL,
                  mission);

            assertEquals(5, award);
        }

        @Test
        void successWithLowVictoryPointsReturnsSuccessExperience() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);
            StratConCampaignState stratConCampaignState = mock(StratConCampaignState.class);
            when(mission.getStratConCampaignState()).thenReturn(stratConCampaignState);
            when(stratConCampaignState.getVictoryPoints()).thenReturn(2);
            when(campaignOptions.get(CampaignOption.MISSION_XP_SUCCESS)).thenReturn(5);

            int award = MissionCompletionManager.getMissionExperienceAward(campaignOptions, MissionStatus.SUCCESS,
                  mission);

            assertEquals(5, award);
        }

        @Test
        void successWithHighVictoryPointsReturnsOutstandingExperience() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);
            StratConCampaignState stratConCampaignState = mock(StratConCampaignState.class);
            when(mission.getStratConCampaignState()).thenReturn(stratConCampaignState);
            when(stratConCampaignState.getVictoryPoints()).thenReturn(3);
            when(campaignOptions.get(CampaignOption.MISSION_XP_OUTSTANDING_SUCCESS)).thenReturn(9);

            int award = MissionCompletionManager.getMissionExperienceAward(campaignOptions, MissionStatus.SUCCESS,
                  mission);

            assertEquals(9, award);
        }
    }
    // endregion getMissionExperienceAward

    // region resolveOutstandingScenarios
    @Nested
    public class ResolveOutstandingScenarios {
        @Test
        void setsEveryCurrentScenarioToDraw() {
            AbstractContract mission = mock(AbstractContract.class);
            Scenario firstScenario = mock(Scenario.class);
            Scenario secondScenario = mock(Scenario.class);
            when(mission.getCurrentScenarios()).thenReturn(List.of(firstScenario, secondScenario));

            MissionCompletionManager.resolveOutstandingScenarios(mission);

            verify(firstScenario).setStatus(ScenarioStatus.DRAW);
            verify(secondScenario).setStatus(ScenarioStatus.DRAW);
        }

        @Test
        void noCurrentScenariosIsHarmless() {
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getCurrentScenarios()).thenReturn(new ArrayList<>());

            MissionCompletionManager.resolveOutstandingScenarios(mission);
        }
    }
    // endregion resolveOutstandingScenarios

    // region undeployUnits
    @Nested
    public class UndeployUnits {
        @Test
        void clearsUnitAssignedToThisMission() {
            UUID missionId = UUID.randomUUID();
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getId()).thenReturn(missionId);

            Campaign campaign = mock(Campaign.class);
            Unit unit = mock(Unit.class);
            Scenario scenario = mock(Scenario.class);
            when(unit.getScenarioId()).thenReturn(7);
            when(campaign.getScenario(7)).thenReturn(scenario);
            when(scenario.getMissionId()).thenReturn(missionId);
            when(campaign.getUnits()).thenReturn(List.of(unit));

            MissionCompletionManager.undeployUnits(campaign, mission);

            verify(unit).setScenarioId(NO_ASSIGNED_SCENARIO);
        }

        @Test
        void ignoresUnassignedUnit() {
            AbstractContract mission = mock(AbstractContract.class);
            Campaign campaign = mock(Campaign.class);
            Unit unit = mock(Unit.class);
            when(unit.getScenarioId()).thenReturn(NO_ASSIGNED_SCENARIO);
            when(campaign.getUnits()).thenReturn(List.of(unit));

            MissionCompletionManager.undeployUnits(campaign, mission);

            verify(unit, never()).setScenarioId(NO_ASSIGNED_SCENARIO);
        }

        @Test
        void clearsUnitWithMissingScenario() {
            AbstractContract mission = mock(AbstractContract.class);
            Campaign campaign = mock(Campaign.class);
            Unit unit = mock(Unit.class);
            when(unit.getScenarioId()).thenReturn(7);
            when(campaign.getScenario(7)).thenReturn(null);
            when(campaign.getUnits()).thenReturn(List.of(unit));

            MissionCompletionManager.undeployUnits(campaign, mission);

            verify(unit).setScenarioId(NO_ASSIGNED_SCENARIO);
        }

        @Test
        void ignoresUnitAssignedToOtherMission() {
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getId()).thenReturn(UUID.randomUUID());

            Campaign campaign = mock(Campaign.class);
            Unit unit = mock(Unit.class);
            Scenario scenario = mock(Scenario.class);
            when(unit.getScenarioId()).thenReturn(7);
            when(campaign.getScenario(7)).thenReturn(scenario);
            when(scenario.getMissionId()).thenReturn(UUID.randomUUID());
            when(campaign.getUnits()).thenReturn(List.of(unit));

            MissionCompletionManager.undeployUnits(campaign, mission);

            verify(unit, never()).setScenarioId(NO_ASSIGNED_SCENARIO);
        }
    }
    // endregion undeployUnits

    // region undeployFormations
    @Nested
    public class UndeployFormations {
        @Test
        void cadreDutyRevertsCadreRoleAndReportsCadreForces() {
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getObjectiveType()).thenReturn(ContractObjectiveType.CADRE_DUTY);

            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            Formation formation = mock(Formation.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getAllFormations()).thenReturn(List.of(formation));
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.CADRE);
            when(formation.getScenarioId()).thenReturn(NO_ASSIGNED_SCENARIO);

            boolean hadCadreForces = MissionCompletionManager.undeployFormations(campaign, mission);

            assertTrue(hadCadreForces);
            verify(formation).setCombatRoleInMemory(CombatRole.FRONTLINE);
        }

        @Test
        void nonCadreDutyLeavesRolesAndReportsNoCadreForces() {
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getObjectiveType()).thenReturn(ContractObjectiveType.GARRISON_DUTY);

            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            Formation formation = mock(Formation.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getAllFormations()).thenReturn(List.of(formation));
            when(formation.getScenarioId()).thenReturn(NO_ASSIGNED_SCENARIO);

            boolean hadCadreForces = MissionCompletionManager.undeployFormations(campaign, mission);

            assertFalse(hadCadreForces);
            verify(formation, never()).setCombatRoleInMemory(any());
        }

        @Test
        void clearsFormationAssignedToThisMission() {
            UUID missionId = UUID.randomUUID();
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getObjectiveType()).thenReturn(ContractObjectiveType.GARRISON_DUTY);
            when(mission.getId()).thenReturn(missionId);

            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            Formation formation = mock(Formation.class);
            Scenario scenario = mock(Scenario.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getAllFormations()).thenReturn(List.of(formation));
            when(formation.getScenarioId()).thenReturn(7);
            when(campaign.getScenario(7)).thenReturn(scenario);
            when(scenario.getMissionId()).thenReturn(missionId);

            MissionCompletionManager.undeployFormations(campaign, mission);

            verify(formation).setScenarioId(NO_ASSIGNED_SCENARIO, campaign);
        }
    }
    // endregion undeployFormations

    // region applyPirateCrimeModifier
    @Nested
    public class ApplyPirateCrimeModifier {
        @Test
        void pirateEmployerAppliesModifier() {
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getEmployerFactionCode()).thenReturn(Faction.PIRATE_FACTION_CODE);

            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);

            MissionCompletionManager.applyPirateCrimeModifier(campaign, mission);

            verify(playerForce).changeCrimePirateModifier(10);
        }

        @Test
        void nonPirateEmployerDoesNothing() {
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getEmployerFactionCode()).thenReturn(NON_PIRATE_FACTION_CODE);

            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);

            MissionCompletionManager.applyPirateCrimeModifier(campaign, mission);

            verify(playerForce, never()).changeCrimePirateModifier(anyInt());
        }
    }
    // endregion applyPirateCrimeModifier

    // region clearStratConState
    @Nested
    public class ClearStratConState {
        @Test
        void clearsState() {
            AbstractContract mission = mock(AbstractContract.class);

            MissionCompletionManager.clearStratConState(mission);

            verify(mission).setStratConCampaignState(null);
        }
    }
    // endregion clearStratConState

    // region resolvePrisoners
    @Nested
    public class ResolvePrisoners {
        @Test
        void withActiveContractsCapturesButDoesNotResolve() {
            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            ForceHumanResources humanResources = mock(ForceHumanResources.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getHumanResources()).thenReturn(humanResources);

            List<Person> friendlyPrisoners = List.of(mock(Person.class));
            when(humanResources.getFriendlyPrisoners()).thenReturn(friendlyPrisoners);
            when(campaign.getActiveContracts()).thenReturn(List.of(mock(AbstractContract.class)));

            PrisonerMissionEndEvent prisonerMissionEndEvent = mock(PrisonerMissionEndEvent.class);

            List<Person> captured = MissionCompletionManager.resolvePrisoners(campaign, prisonerMissionEndEvent,
                  MissionStatus.SUCCESS);

            assertEquals(friendlyPrisoners, captured);
            verify(prisonerMissionEndEvent, never()).handlePrisoners(anyBoolean(), anyBoolean());
            verify(playerForce, never()).setTemporaryPrisonerCapacity(anyInt());
        }

        @Test
        void withoutActiveContractsResolvesBothCohortsAndResetsCapacity() {
            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            ForceHumanResources humanResources = mock(ForceHumanResources.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getHumanResources()).thenReturn(humanResources);

            when(humanResources.getFriendlyPrisoners()).thenReturn(List.of(mock(Person.class)));
            when(humanResources.getCurrentPrisoners()).thenReturn(List.of(mock(Person.class)));
            when(campaign.getActiveContracts()).thenReturn(new ArrayList<>());

            PrisonerMissionEndEvent prisonerMissionEndEvent = mock(PrisonerMissionEndEvent.class);

            MissionCompletionManager.resolvePrisoners(campaign, prisonerMissionEndEvent, MissionStatus.SUCCESS);

            verify(prisonerMissionEndEvent).handlePrisoners(true, true);
            verify(prisonerMissionEndEvent).handlePrisoners(true, false);
            verify(playerForce).setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
        }

        @Test
        void withoutActiveContractsSkipsEmptyFriendlyCohort() {
            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            ForceHumanResources humanResources = mock(ForceHumanResources.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getHumanResources()).thenReturn(humanResources);

            when(humanResources.getFriendlyPrisoners()).thenReturn(new ArrayList<>());
            when(humanResources.getCurrentPrisoners()).thenReturn(new ArrayList<>());
            when(campaign.getActiveContracts()).thenReturn(new ArrayList<>());

            PrisonerMissionEndEvent prisonerMissionEndEvent = mock(PrisonerMissionEndEvent.class);

            MissionCompletionManager.resolvePrisoners(campaign, prisonerMissionEndEvent, MissionStatus.FAILED);

            verify(prisonerMissionEndEvent, never()).handlePrisoners(anyBoolean(), anyBoolean());
            verify(playerForce).setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
        }
    }
    // endregion resolvePrisoners

    // region awardMissionExperience
    @Nested
    public class AwardMissionExperience {
        @Test
        void positiveAwardGoesToEligiblePersonnelOnly() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);
            when(mission.getStratConCampaignState()).thenReturn(null);
            when(campaignOptions.get(CampaignOption.MISSION_XP_SUCCESS)).thenReturn(4);

            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            ForceHumanResources humanResources = mock(ForceHumanResources.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getHumanResources()).thenReturn(humanResources);
            when(campaign.getLocalDate()).thenReturn(TODAY);

            Person eligible = mock(Person.class);
            Person child = mock(Person.class);
            Person dependent = mock(Person.class);
            when(eligible.isChild(TODAY)).thenReturn(false);
            when(eligible.isDependent()).thenReturn(false);
            when(child.isChild(TODAY)).thenReturn(true);
            when(dependent.isChild(TODAY)).thenReturn(false);
            when(dependent.isDependent()).thenReturn(true);
            when(humanResources.getActivePersonnel(false, false)).thenReturn(List.of(eligible, child, dependent));

            MissionCompletionManager.awardMissionExperience(campaign, campaignOptions, mission, MissionStatus.SUCCESS);

            verify(eligible).awardXP(campaign, 4);
            verify(child, never()).awardXP(any(Campaign.class), anyInt());
            verify(dependent, never()).awardXP(any(Campaign.class), anyInt());
        }

        @Test
        void zeroAwardTouchesNobody() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);

            Campaign campaign = mock(Campaign.class);

            MissionCompletionManager.awardMissionExperience(campaign, campaignOptions, mission, MissionStatus.ACTIVE);

            verify(campaign, never()).getPlayerForce();
        }
    }
    // endregion awardMissionExperience

    // region updateFactionStandings
    @Nested
    public class UpdateFactionStandings {
        @Test
        void trackingDisabledDoesNothing() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            when(campaignOptions.get(CampaignOption.TRACK_FACTION_STANDING)).thenReturn(false);

            Campaign campaign = mock(Campaign.class);
            AbstractContract mission = mock(AbstractContract.class);

            MissionCompletionManager.updateFactionStandings(campaign, campaignOptions, mission, MissionStatus.SUCCESS);

            verify(campaign, never()).getPlayerForce();
        }

        @Test
        void trackingEnabledProcessesCompletionWithScaledMultiplier() {
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            when(campaignOptions.get(CampaignOption.TRACK_FACTION_STANDING)).thenReturn(true);
            when(campaignOptions.get(CampaignOption.REGARD_MULTIPLIER)).thenReturn(2.0);

            Campaign campaign = mock(Campaign.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            FactionStandings factionStandings = mock(FactionStandings.class);
            Faction campaignFaction = mock(Faction.class);
            Faction employerFaction = mock(Faction.class);
            AbstractContract mission = mock(AbstractContract.class);

            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getFactionStandings()).thenReturn(factionStandings);
            when(playerForce.getFaction()).thenReturn(campaignFaction);
            when(campaign.getLocalDate()).thenReturn(TODAY);
            when(mission.getStandingEmployerFaction()).thenReturn(employerFaction);
            when(mission.getLengthInMonths()).thenReturn(12);

            try (MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                  ContractCharacteristics.class)) {
                contractCharacteristics.when(() -> ContractCharacteristics.getEmployerRegardMultiplier(mission))
                      .thenReturn(3.0);

                MissionCompletionManager.updateFactionStandings(campaign, campaignOptions, mission,
                      MissionStatus.SUCCESS);
            }

            verify(factionStandings).processContractCompletion(campaignFaction, employerFaction, TODAY,
                  MissionStatus.SUCCESS, 6.0, 12);
        }
    }
    // endregion updateFactionStandings

    // region payCompletionBonusAndReputation
    @Nested
    public class PayCompletionBonusAndReputation {
        @Test
        void chaosDisabledPaysBonusOnly() {
            Campaign campaign = mock(Campaign.class);
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            AbstractContract mission = mock(AbstractContract.class);
            when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
            when(campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION)).thenReturn(false);

            try (MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                  ContractCharacteristics.class);
                  MockedStatic<ChaosReputation> chaosReputation = mockStatic(ChaosReputation.class)) {
                MissionCompletionManager.payCompletionBonusAndReputation(campaign, mission, MissionStatus.SUCCESS);

                contractCharacteristics.verify(
                      () -> ContractCharacteristics.payCompletionBonus(campaign, mission, MissionStatus.SUCCESS));
                chaosReputation.verifyNoInteractions();
            }
        }

        @Test
        void chaosEnabledNonPirateProcessesReputationWithoutPiracy() {
            Campaign campaign = mock(Campaign.class);
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            ForceHumanResources humanResources = mock(ForceHumanResources.class);
            AbstractContract mission = mock(AbstractContract.class);

            when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
            when(campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION)).thenReturn(true);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getHumanResources()).thenReturn(humanResources);

            List<Person> personnel = List.of(mock(Person.class));
            when(humanResources.getPersonnelFilteringOutDepartedAndAbsent()).thenReturn(personnel);
            when(mission.getEmployerFactionCode()).thenReturn(NON_PIRATE_FACTION_CODE);

            try (MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                  ContractCharacteristics.class);
                  MockedStatic<ChaosReputation> chaosReputation = mockStatic(ChaosReputation.class)) {
                contractCharacteristics.when(
                      () -> ContractCharacteristics.getUnitReputationMultiplier(mission, MissionStatus.SUCCESS))
                      .thenReturn(1.5);

                MissionCompletionManager.payCompletionBonusAndReputation(campaign, mission, MissionStatus.SUCCESS);

                chaosReputation.verify(() -> ChaosReputation.processContractCompletion(campaign, MissionStatus.SUCCESS,
                      personnel, 1.5));
                chaosReputation.verify(() -> ChaosReputation.resolveActOfPiracy(any(), any(), anyInt(),
                      any(), anyBoolean(), any()), never());
            }
        }

        @Test
        void chaosEnabledPirateAlsoResolvesActOfPiracy() {
            Campaign campaign = mock(Campaign.class);
            CampaignOptions campaignOptions = mock(CampaignOptions.class);
            PlayerForce playerForce = mock(PlayerForce.class);
            ForceHumanResources humanResources = mock(ForceHumanResources.class);
            AbstractContract mission = mock(AbstractContract.class);

            when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
            when(campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION)).thenReturn(true);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getHumanResources()).thenReturn(humanResources);

            List<Person> personnel = List.of(mock(Person.class));
            List<Scenario> scenarios = List.of(mock(Scenario.class));
            when(humanResources.getPersonnelFilteringOutDepartedAndAbsent()).thenReturn(personnel);
            when(mission.getEmployerFactionCode()).thenReturn(Faction.PIRATE_FACTION_CODE);
            when(mission.getScale()).thenReturn(4);
            when(mission.getScenarios()).thenReturn(scenarios);
            when(mission.getName()).thenReturn("Raid");

            try (MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                  ContractCharacteristics.class);
                  MockedStatic<ChaosReputation> chaosReputation = mockStatic(ChaosReputation.class)) {
                contractCharacteristics.when(
                      () -> ContractCharacteristics.getUnitReputationMultiplier(mission, MissionStatus.SUCCESS))
                      .thenReturn(1.0);

                MissionCompletionManager.payCompletionBonusAndReputation(campaign, mission, MissionStatus.SUCCESS);

                chaosReputation.verify(() -> ChaosReputation.resolveActOfPiracy(campaign, personnel, 4, scenarios,
                      true, "Raid"));
            }
        }
    }
    // endregion payCompletionBonusAndReputation

    // region completeMission
    @Nested
    public class CompleteMission {
        @Test
        void happyPathRunsToCompletion() {
            Fixture fixture = new Fixture();

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                        ContractCharacteristics.class);
                  MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                        MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class)) {
                boolean completed = fixture.manager.completeMission();

                assertTrue(completed);
                verify(fixture.campaign).completeMission(fixture.mission, MissionStatus.SUCCESS);
                mekHQ.verify(() -> MekHQ.triggerEvent(any()));
            }
        }

        @Test
        void cancelledDialogAbortsBeforeCompletion() {
            Fixture fixture = new Fixture();

            try (MockedConstruction<CompleteMissionDialog> completeMissionDialog = mockConstruction(
                  CompleteMissionDialog.class,
                  (dialog, context) -> when(dialog.showDialog()).thenReturn(DialogResult.CANCELLED))) {
                boolean completed = fixture.manager.completeMission();

                assertFalse(completed);
                verify(fixture.campaign, never()).completeMission(any(), any());
            }
        }

        @Test
        void activeStatusAbortsBeforeCompletion() {
            Fixture fixture = new Fixture();

            try (MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                  MissionStatus.ACTIVE)) {
                boolean completed = fixture.manager.completeMission();

                assertFalse(completed);
                verify(fixture.campaign, never()).completeMission(any(), any());
            }
        }

        @Test
        void cancelledPrisonerDefectorsAborts() {
            Fixture fixture = new Fixture();
            when(fixture.humanResources.getPrisonerDefectors()).thenReturn(List.of(mock(Person.class)));

            try (MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                  MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class,
                        (event, context) -> when(event.handlePrisonerDefectors()).thenReturn(0))) {
                boolean completed = fixture.manager.completeMission();

                assertFalse(completed);
                verify(fixture.campaign, never()).completeMission(any(), any());
            }
        }

        @Test
        void contractExtensionAborts() {
            Fixture fixture = new Fixture();
            when(fixture.campaignOptions.isUseStratCon()).thenReturn(true);

            try (MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                  MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class);
                  MockedStatic<ContractEmergencyExtension> contractEmergencyExtension = mockStatic(
                        ContractEmergencyExtension.class)) {
                contractEmergencyExtension.when(
                            () -> ContractEmergencyExtension.contractExtended(fixture.campaign, fixture.mission))
                      .thenReturn(true);

                boolean completed = fixture.manager.completeMission();

                assertFalse(completed);
                verify(fixture.campaign, never()).completeMission(any(), any());
            }
        }

        @Test
        void abortedTurnoverWithNoOutstandingPayoutsStops() {
            Fixture fixture = new Fixture();
            when(fixture.campaignOptions.get(CampaignOption.USE_RANDOM_RETIREMENT)).thenReturn(true);
            when(fixture.campaignOptions.get(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT)).thenReturn(
                  true);

            UUID missionId = UUID.randomUUID();
            when(fixture.mission.getId()).thenReturn(missionId);
            RetirementDefectionTracker tracker = mock(RetirementDefectionTracker.class);
            when(fixture.humanResources.getRetirementDefectionTracker()).thenReturn(tracker);
            when(tracker.isOutstanding(missionId)).thenReturn(false);

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                        ContractCharacteristics.class);
                  MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                        MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class);
                  MockedConstruction<RetirementDefectionDialog> retirementDefectionDialog = mockConstruction(
                        RetirementDefectionDialog.class,
                        (dialog, context) -> when(dialog.wasAborted()).thenReturn(true))) {
                boolean completed = fixture.manager.completeMission();

                assertFalse(completed);
                // Completion itself happens before turnover, so it must already have been applied.
                verify(fixture.campaign).completeMission(fixture.mission, MissionStatus.SUCCESS);
            }
        }

        @Test
        void appliedTurnoverAwardsAdministratorExperienceAndContinues() {
            Fixture fixture = new Fixture();
            when(fixture.campaignOptions.get(CampaignOption.USE_RANDOM_RETIREMENT)).thenReturn(true);
            when(fixture.campaignOptions.get(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT)).thenReturn(
                  true);

            RetirementDefectionTracker tracker = mock(RetirementDefectionTracker.class);
            when(fixture.humanResources.getRetirementDefectionTracker()).thenReturn(tracker);
            when(tracker.getRetirees(fixture.mission)).thenReturn(Set.of(UUID.randomUUID()));

            Finances finances = mock(Finances.class);
            Money balance = mock(Money.class);
            when(fixture.playerForce.getFinances()).thenReturn(finances);
            when(finances.getBalance()).thenReturn(balance);
            when(balance.isGreaterOrEqualThan(any(Money.class))).thenReturn(true);

            Person administrator = mock(Person.class);
            when(fixture.humanResources.findBestInRole(any(), any(), any(), anyBoolean(), any())).thenReturn(
                  administrator);
            when(fixture.campaign.applyRetirement(any(), any())).thenReturn(true);

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                        ContractCharacteristics.class);
                  MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                        MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class);
                  MockedConstruction<RetirementDefectionDialog> retirementDefectionDialog = mockConstruction(
                        RetirementDefectionDialog.class,
                        (dialog, context) -> {
                            when(dialog.wasAborted()).thenReturn(false);
                            when(dialog.totalPayout()).thenReturn(Money.zero());
                        })) {
                boolean completed = fixture.manager.completeMission();

                assertTrue(completed);
                verify(administrator, atLeastOnce()).awardXP(fixture.campaign, 1);
            }
        }

        @Test
        void turnoverApplyRetirementFailureAborts() {
            Fixture fixture = new Fixture();
            when(fixture.campaignOptions.get(CampaignOption.USE_RANDOM_RETIREMENT)).thenReturn(true);
            when(fixture.campaignOptions.get(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT)).thenReturn(
                  true);

            RetirementDefectionTracker tracker = mock(RetirementDefectionTracker.class);
            when(fixture.humanResources.getRetirementDefectionTracker()).thenReturn(tracker);
            // A null retiree set skips the administrator-experience block, isolating the applyRetirement abort.
            when(tracker.getRetirees(fixture.mission)).thenReturn(null);
            when(fixture.campaign.applyRetirement(any(), any())).thenReturn(false);

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                        ContractCharacteristics.class);
                  MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                        MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class);
                  MockedConstruction<RetirementDefectionDialog> retirementDefectionDialog = mockConstruction(
                        RetirementDefectionDialog.class,
                        (dialog, context) -> {
                            when(dialog.wasAborted()).thenReturn(false);
                            when(dialog.totalPayout()).thenReturn(Money.zero());
                        })) {
                boolean completed = fixture.manager.completeMission();

                assertFalse(completed);
                verify(fixture.campaign).completeMission(fixture.mission, MissionStatus.SUCCESS);
            }
        }

        @Test
        void autoAwardsCeremonyRunsWhenEnabled() {
            Fixture fixture = new Fixture();
            when(fixture.campaignOptions.get(CampaignOption.ENABLE_AUTO_AWARDS)).thenReturn(true);

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                        ContractCharacteristics.class);
                  MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                        MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class);
                  MockedConstruction<AutoAwardsController> autoAwardsController = mockConstruction(
                        AutoAwardsController.class)) {
                boolean completed = fixture.manager.completeMission();

                assertTrue(completed);
                AutoAwardsController controller = autoAwardsController.constructed().get(0);
                verify(controller).PostMissionController(eq(fixture.campaign), eq(fixture.mission), eq(true), any());
            }
        }

        @Test
        void personnelMarketRefreshedWhenPreviouslyDisabled() {
            Fixture fixture = new Fixture();
            when(fixture.newPersonnelMarket.getAvailabilityMessage()).thenReturn("Disabled");

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                        ContractCharacteristics.class);
                  MockedStatic<CampaignNewDayManager> campaignNewDayManager = mockStatic(CampaignNewDayManager.class);
                  MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                        MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class)) {
                boolean completed = fixture.manager.completeMission();

                assertTrue(completed);
                verify(fixture.humanResources).refreshApplicants(fixture.campaign, true);
                campaignNewDayManager.verify(
                      () -> CampaignNewDayManager.showRarePersonnelDialog(fixture.campaign, false));
            }
        }

        @Test
        void cadreForcesReassignmentShowsNotification() {
            Fixture fixture = new Fixture();
            when(fixture.mission.getObjectiveType()).thenReturn(ContractObjectiveType.CADRE_DUTY);

            Formation formation = mock(Formation.class);
            when(fixture.playerForce.getAllFormations()).thenReturn(List.of(formation));
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.CADRE);
            when(formation.getScenarioId()).thenReturn(NO_ASSIGNED_SCENARIO);

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                        ContractCharacteristics.class);
                  MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                        MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class);
                  MockedConstruction<ImmersiveDialogNotification> notifications = mockConstruction(
                        ImmersiveDialogNotification.class)) {
                boolean completed = fixture.manager.completeMission();

                assertTrue(completed);
                verify(formation).setCombatRoleInMemory(CombatRole.FRONTLINE);
                assertEquals(1, notifications.constructed().size());
            }
        }

        @Test
        void declinedContractExtensionContinues() {
            Fixture fixture = new Fixture();
            when(fixture.campaignOptions.isUseStratCon()).thenReturn(true);

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractCharacteristics> contractCharacteristics = mockStatic(
                        ContractCharacteristics.class);
                  MockedStatic<ContractEmergencyExtension> contractEmergencyExtension = mockStatic(
                        ContractEmergencyExtension.class);
                  MockedConstruction<CompleteMissionDialog> completeMissionDialog = confirmedDialog(
                        MissionStatus.SUCCESS);
                  MockedConstruction<PrisonerMissionEndEvent> prisonerMissionEndEvent = mockConstruction(
                        PrisonerMissionEndEvent.class)) {
                contractEmergencyExtension.when(
                            () -> ContractEmergencyExtension.contractExtended(fixture.campaign, fixture.mission))
                      .thenReturn(false);

                boolean completed = fixture.manager.completeMission();

                assertTrue(completed);
                verify(fixture.campaign).completeMission(fixture.mission, MissionStatus.SUCCESS);
            }
        }
    }
    // endregion completeMission

    /**
     * Builds a {@link CompleteMissionDialog} construction mock that reports the player confirmed the dialog and
     * selected the supplied status.
     *
     * @param status the status the mocked dialog should report
     *
     * @return the construction mock, to be used in a try-with-resources block
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static MockedConstruction<CompleteMissionDialog> confirmedDialog(MissionStatus status) {
        return mockConstruction(CompleteMissionDialog.class, (dialog, context) -> {
            when(dialog.showDialog()).thenReturn(DialogResult.CONFIRMED);
            when(dialog.getStatus()).thenReturn(status);
        });
    }

    /**
     * Wires a complete set of collaborator mocks so that {@link MissionCompletionManager#completeMission()} runs to
     * completion with every optional feature disabled. Individual tests override single stubs to exercise a branch.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static final class Fixture {
        private final MekHQ app;
        private final CampaignGUI campaignGui;
        private final Campaign campaign;
        private final CampaignOptions campaignOptions;
        private final PlayerForce playerForce;
        private final ForceHumanResources humanResources;
        private final NewPersonnelMarket newPersonnelMarket;
        private final AbstractContract mission;
        private final MissionCompletionManager manager;

        private Fixture() {
            app = mock(MekHQ.class, RETURNS_DEEP_STUBS);
            campaignGui = mock(CampaignGUI.class);
            campaign = mock(Campaign.class);
            campaignOptions = mock(CampaignOptions.class);
            playerForce = mock(PlayerForce.class);
            humanResources = mock(ForceHumanResources.class);
            newPersonnelMarket = mock(NewPersonnelMarket.class);
            mission = mock(AbstractContract.class);

            when(campaignGui.getCampaign()).thenReturn(campaign);
            when(campaignGui.getFrame()).thenReturn(null);
            when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getHumanResources()).thenReturn(humanResources);
            when(humanResources.getNewPersonnelMarket()).thenReturn(newPersonnelMarket);
            when(newPersonnelMarket.getAvailabilityMessage()).thenReturn("");
            when(humanResources.getPrisonerDefectors()).thenReturn(new ArrayList<>());
            when(humanResources.getFriendlyPrisoners()).thenReturn(new ArrayList<>());
            when(campaign.getActiveContracts()).thenReturn(List.of(mission));
            when(campaign.getUnits()).thenReturn(new ArrayList<>());
            when(campaign.getLocalDate()).thenReturn(TODAY);
            when(playerForce.getAllFormations()).thenReturn(new ArrayList<>());
            when(mission.getObjectiveType()).thenReturn(ContractObjectiveType.GARRISON_DUTY);
            when(mission.getCurrentScenarios()).thenReturn(new ArrayList<>());
            when(mission.getEmployerFactionCode()).thenReturn(NON_PIRATE_FACTION_CODE);
            when(mission.getStratConCampaignState()).thenReturn(null);

            when(campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION)).thenReturn(false);
            when(campaignOptions.isUseStratCon()).thenReturn(false);
            when(campaignOptions.get(CampaignOption.USE_RANDOM_RETIREMENT)).thenReturn(false);
            when(campaignOptions.get(CampaignOption.ENABLE_AUTO_AWARDS)).thenReturn(false);
            when(campaignOptions.get(CampaignOption.TRACK_FACTION_STANDING)).thenReturn(false);
            when(campaignOptions.get(CampaignOption.MISSION_XP_SUCCESS)).thenReturn(0);

            manager = new MissionCompletionManager(app, campaignGui, mission);
        }
    }
}
