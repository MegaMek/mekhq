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
package mekhq.campaign.personnel.turnoverAndRetention;

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import megamek.common.enums.SkillLevel;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FatigueTest {
    private static final LocalDate TODAY = LocalDate.of(3151, 1, 5);
    private static final int LEAVE_THRESHOLD = 13;

    private Campaign campaign;
    private CampaignOptions campaignOptions;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        campaignOptions = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getActiveContracts()).thenReturn(List.of(mock(AbstractContract.class)));
        when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(true);
        when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(0);
    }

    /**
     * Creates a mocked, active, free person whose raw and adjusted fatigue are both {@code fatigue}.
     */
    private static Person mockPerson(int fatigue, int permanentFatigue, boolean isClan, SkillLevel skillLevel) {
        Person person = mock(Person.class);
        when(person.getAdjustedFatigue()).thenReturn(fatigue);
        when(person.getFatigueDirect()).thenReturn(fatigue);
        when(person.getPermanentFatigue()).thenReturn(permanentFatigue);
        when(person.isClanPersonnel()).thenReturn(isClan);
        when(person.getSkillLevel(any(Campaign.class), anyBoolean(), anyBoolean())).thenReturn(skillLevel);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        return person;
    }

    private static Person mockPerson(int fatigue) {
        return mockPerson(fatigue, 0, false, SkillLevel.REGULAR);
    }

    private static MiscMounted mockMiscMounted(boolean isFieldKitchen) {
        MiscType type = mock(MiscType.class);
        when(type.hasFlag(MiscType.F_FIELD_KITCHEN)).thenReturn(isFieldKitchen);
        MiscMounted mounted = mock(MiscMounted.class);
        when(mounted.getType()).thenReturn(type);
        return mounted;
    }

    /**
     * Creates a mocked, available (undeployed, undamaged, fully crewed) unit carrying {@code fieldKitchens} field
     * kitchens and {@code otherEquipment} unrelated pieces of equipment.
     */
    private static Unit mockKitchenUnit(int fieldKitchens, int otherEquipment) {
        List<MiscMounted> misc = new ArrayList<>();
        for (int i = 0; i < fieldKitchens; i++) {
            misc.add(mockMiscMounted(true));
        }
        for (int i = 0; i < otherEquipment; i++) {
            misc.add(mockMiscMounted(false));
        }

        Entity entity = mock(Entity.class);
        when(entity.getMisc()).thenReturn(misc);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.isFullyCrewed()).thenReturn(true);
        return unit;
    }

    @Nested
    class EffectiveFatigue {
        @ParameterizedTest(name = "fatigue {0}, permanent {1}, Clan {2}, {3} -> {4}")
        @CsvSource({
              "10, 0, false, REGULAR, 10",
              "10, 3, false, REGULAR, 13",
              "10, 0, true, REGULAR, 8",
              "10, 0, false, VETERAN, 9",
              "10, 0, false, ELITE, 8",
              "10, 0, false, HEROIC, 8",
              "10, 0, false, LEGENDARY, 8",
              "10, 2, true, VETERAN, 9",
              "0, 0, true, ELITE, -4"
        })
        void appliesAllModifiers(int fatigue, int permanentFatigue, boolean isClan, SkillLevel skillLevel,
              int expected) {
            Person person = mockPerson(fatigue, permanentFatigue, isClan, skillLevel);

            assertEquals(expected, Fatigue.getEffectiveFatigue(person, campaign));
        }
    }

    @Nested
    class TurnoverModifier {
        @ParameterizedTest(name = "effective fatigue {0} -> +{1}")
        @CsvSource({ "-4, 0", "0, 0", "8, 0", "9, 1", "12, 1", "13, 2", "16, 2", "17, 3", "30, 3" })
        void matchesFatigueBands(int effectiveFatigue, int expected) {
            assertEquals(expected, Fatigue.getTurnoverModifier(effectiveFatigue));
        }
    }

    @Nested
    class FatigueActions {
        @ParameterizedTest(name = "effective fatigue {0}")
        @CsvSource({ "5", "8", "9", "11", "12", "16", "17", "30" })
        void reportsAndStartsRecoveryInEveryBand(int fatigue) {
            Person person = mockPerson(fatigue);

            Fatigue.processFatigueActions(campaign, person);

            verify(campaign).addReport(eq(PERSONNEL), anyString());
            verify(person).setIsRecoveringFromFatigue(true);
        }

        @Test
        void doesNothingBelowTired() {
            Person person = mockPerson(4);

            Fatigue.processFatigueActions(campaign, person);

            verify(campaign, never()).addReport(any(), anyString());
            verify(person, never()).setIsRecoveringFromFatigue(anyBoolean());
        }

        @Test
        void doesNothingWhenFatigueDisabled() {
            when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(false);
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(20);

            Fatigue.processFatigueActions(campaign, person);

            verify(campaign, never()).addReport(any(), anyString());
            verify(person, never()).changeStatus(any(), any(), any());
        }
    }

    @Nested
    class AutomaticLeave {
        @BeforeEach
        void enableLeaveThreshold() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
        }

        @Test
        void sendsOnLeaveAtThreshold() {
            Person person = mockPerson(LEAVE_THRESHOLD);

            Fatigue.processFatigueActions(campaign, person);

            verify(person).changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
        }

        @Test
        void usesEffectiveFatigue() {
            // 14 raw fatigue, -2 for Elite, is below the threshold
            Person person = mockPerson(LEAVE_THRESHOLD + 1, 0, false, SkillLevel.ELITE);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentBelowThreshold() {
            Person person = mockPerson(LEAVE_THRESHOLD - 1);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenThresholdDisabled() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(0);
            Person person = mockPerson(30);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenBusy() {
            Person person = mockPerson(LEAVE_THRESHOLD);
            when(person.isBusy()).thenReturn(true);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenInjured() {
            Person person = mockPerson(LEAVE_THRESHOLD);
            when(person.getNonPermanentInjurySeverity()).thenReturn(1);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenPrisoner() {
            Person person = mockPerson(LEAVE_THRESHOLD);
            when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void notSentWhenCampFollower() {
            Person person = mockPerson(LEAVE_THRESHOLD);
            when(person.getStatus()).thenReturn(PersonnelStatus.CAMP_FOLLOWER);

            Fatigue.processFatigueActions(campaign, person);

            verify(person, never()).changeStatus(any(), any(), any());
        }
    }

    @Nested
    class FieldKitchenCapacity {
        private static final int CAPACITY_PER_KITCHEN = 150;

        @Test
        void countsEveryFieldKitchenMount() {
            Unit unit = mockKitchenUnit(2, 1);

            assertEquals(2 * CAPACITY_PER_KITCHEN,
                  Fatigue.checkFieldKitchenCapacity(List.of(unit), CAPACITY_PER_KITCHEN));
        }

        @Test
        void sumsKitchensAcrossUnits() {
            List<Unit> units = List.of(mockKitchenUnit(1, 0), mockKitchenUnit(2, 3));

            assertEquals(3 * CAPACITY_PER_KITCHEN, Fatigue.checkFieldKitchenCapacity(units, CAPACITY_PER_KITCHEN));
        }

        @Test
        void unitsWithoutKitchensProvideNoCapacity() {
            Unit unit = mockKitchenUnit(0, 2);

            assertEquals(0, Fatigue.checkFieldKitchenCapacity(List.of(unit), CAPACITY_PER_KITCHEN));
        }

        @Test
        void noUnitsProvideNoCapacity() {
            assertEquals(0, Fatigue.checkFieldKitchenCapacity(List.of(), CAPACITY_PER_KITCHEN));
        }

        @ParameterizedTest(name = "deployed {0}, damaged {1}, fully crewed {2}")
        @CsvSource({ "true, false, true", "false, true, true", "false, false, false" })
        void unavailableUnitsProvideNoCapacity(boolean isDeployed, boolean isDamaged, boolean isFullyCrewed) {
            Unit unit = mockKitchenUnit(1, 0);
            when(unit.isDeployed()).thenReturn(isDeployed);
            when(unit.isDamaged()).thenReturn(isDamaged);
            when(unit.isFullyCrewed()).thenReturn(isFullyCrewed);

            assertEquals(0, Fatigue.checkFieldKitchenCapacity(List.of(unit), CAPACITY_PER_KITCHEN));
        }
    }

    @Nested
    class FieldKitchenUsage {
        private ForceHumanResources humanResources;

        @BeforeEach
        void setUpHumanResources() {
            PlayerForce playerForce = mock(PlayerForce.class);
            humanResources = mock(ForceHumanResources.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getHumanResources()).thenReturn(humanResources);
            when(humanResources.getTempCrewRoleKeys()).thenReturn(Set.of());
        }

        private static Person mockMember(boolean isCombat, Unit unit) {
            Person person = mock(Person.class);
            when(person.isCombat()).thenReturn(isCombat);
            when(person.getUnit()).thenReturn(unit);
            return person;
        }

        private static Unit mockUnitWithEntity(Entity entity) {
            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(entity);
            return unit;
        }

        @Test
        void personnelWithoutUnitsAreCounted() {
            List<Person> personnel = List.of(mockMember(true, null), mockMember(true, null));

            assertEquals(2, Fatigue.checkFieldKitchenUsage(personnel, false, campaign));
        }

        @Test
        void personnelInUnitWithoutEntityAreCounted() {
            List<Person> personnel = List.of(mockMember(true, mockUnitWithEntity(null)));

            assertEquals(1, Fatigue.checkFieldKitchenUsage(personnel, false, campaign));
        }

        @Test
        void personnelInGroundUnitsAreCounted() {
            List<Person> personnel = List.of(mockMember(true, mockUnitWithEntity(mock(Entity.class))));

            assertEquals(1, Fatigue.checkFieldKitchenUsage(personnel, false, campaign));
        }

        @ParameterizedTest(name = "large craft {0}, small craft {1}")
        @CsvSource({ "true, false", "false, true" })
        void largeAndSmallCraftCrewsFeedThemselves(boolean isLargeCraft, boolean isSmallCraft) {
            Entity entity = mock(Entity.class);
            when(entity.isLargeCraft()).thenReturn(isLargeCraft);
            when(entity.isSmallCraft()).thenReturn(isSmallCraft);
            List<Person> personnel = List.of(mockMember(true, mockUnitWithEntity(entity)));

            assertEquals(0, Fatigue.checkFieldKitchenUsage(personnel, false, campaign));
        }

        @ParameterizedTest(name = "ignore non-combatants {0} -> {1}")
        @CsvSource({ "false, 2", "true, 1" })
        void nonCombatantsAreOnlyCountedWhenNotIgnored(boolean isIgnoreNonCombatants, int expected) {
            List<Person> personnel = List.of(mockMember(true, null), mockMember(false, null));

            assertEquals(expected, Fatigue.checkFieldKitchenUsage(personnel, isIgnoreNonCombatants, campaign));
        }

        @ParameterizedTest(name = "ignore non-combatants {0} -> {1}")
        @CsvSource({ "false, 7", "true, 4" })
        void tempCrewIsCounted(boolean isIgnoreNonCombatants, int expected) {
            when(humanResources.getTempCrewRoleKeys()).thenReturn(Set.of(PersonnelRole.SOLDIER,
                  PersonnelRole.MEK_TECH));
            when(humanResources.getTempCrewPool(PersonnelRole.SOLDIER)).thenReturn(4);
            when(humanResources.getTempCrewPool(PersonnelRole.MEK_TECH)).thenReturn(3);

            assertEquals(expected, Fatigue.checkFieldKitchenUsage(List.of(), isIgnoreNonCombatants, campaign));
        }
    }

    @Nested
    class FieldKitchensWithinCapacity {
        @ParameterizedTest(name = "capacity {0}, usage {1} -> {2}")
        @CsvSource({ "10, 10, true", "10, 9, true", "9, 10, false", "0, 0, true", "0, 1, false" })
        void capacityMustMeetUsage(int capacity, int usage, boolean expected) {
            assertEquals(expected, Fatigue.areFieldKitchensWithinCapacity(capacity, usage));
        }
    }

    @Nested
    class DeploymentFatigueResponses {
        private static final int FORMATION_ID = 7;
        private static final int UNDEPLOYMENT_THRESHOLD = 10;

        private final Vector<UUID> unitIds = new Vector<>();
        private Formation formation;
        private StratConCampaignState stratConState;
        private StratConTrackState track;

        @BeforeEach
        void setUpDeployment() {
            when(campaignOptions.isUseStratCon()).thenReturn(true);
            when(campaignOptions.get(CampaignOption.FATIGUE_UNDEPLOYMENT_THRESHOLD)).thenReturn(UNDEPLOYMENT_THRESHOLD);

            track = mock(StratConTrackState.class);
            stratConState = mock(StratConCampaignState.class);
            when(stratConState.isForceDeployedHere(FORMATION_ID)).thenReturn(true);
            when(stratConState.getTracks()).thenReturn(List.of(track));
            AbstractContract contract = mock(AbstractContract.class);
            when(contract.getStratConCampaignState()).thenReturn(stratConState);
            when(campaign.getActiveContracts()).thenReturn(List.of(contract));

            formation = mock(Formation.class);
            when(formation.getId()).thenReturn(FORMATION_ID);
            when(formation.getName()).thenReturn("Alpha Lance");
            when(formation.getAllUnits(false)).thenReturn(unitIds);
            CombatTeam combatTeam = mock(CombatTeam.class);
            when(combatTeam.getFormation(campaign)).thenReturn(formation);

            PlayerForce playerForce = mock(PlayerForce.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(combatTeam));
        }

        /**
         * Adds a unit to the formation, deployed to the StratCon track alongside it, crewed by {@code crew}.
         */
        private Unit addUnit(Person... crew) {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            when(unit.getCampaign()).thenReturn(campaign);
            when(unit.getFormationId()).thenReturn(FORMATION_ID);
            when(unit.getCrew()).thenReturn(List.of(crew));
            when(campaign.getUnit(unitId)).thenReturn(unit);
            unitIds.add(unitId);
            return unit;
        }

        private void assertPulledBack(boolean isExpected) {
            verify(track, isExpected ? times(1) : never()).unassignFormation(FORMATION_ID);
            verify(campaign, isExpected ? times(1) : never()).addReport(eq(PERSONNEL), anyString());
        }

        @ParameterizedTest(name = "{1} of {0} units fatigued -> pulled back {2}")
        @CsvSource({
              "1, 1, true",
              "2, 1, true",
              "3, 1, false",
              "3, 2, true",
              "4, 1, false",
              "4, 2, true",
              "2, 0, false"
        })
        void pullsBackWhenHalfOrMoreOfTheUnitsAreFatigued(int units, int fatiguedUnits, boolean isExpected) {
            for (int i = 0; i < units; i++) {
                addUnit(mockPerson(i < fatiguedUnits ? UNDEPLOYMENT_THRESHOLD : 0));
            }

            Fatigue.processDeploymentFatigueResponses(campaign);

            assertPulledBack(isExpected);
        }

        @Test
        void unitWithSeveralFatiguedCrewCountsOnce() {
            addUnit(mockPerson(UNDEPLOYMENT_THRESHOLD), mockPerson(UNDEPLOYMENT_THRESHOLD));
            addUnit(mockPerson(0));
            addUnit(mockPerson(0));

            Fatigue.processDeploymentFatigueResponses(campaign);

            assertPulledBack(false);
        }

        @Test
        void usesEffectiveFatigue() {
            // Clan personnel reduce their effective fatigue by 2, taking them below the threshold
            addUnit(mockPerson(UNDEPLOYMENT_THRESHOLD, 0, true, SkillLevel.REGULAR));

            Fatigue.processDeploymentFatigueResponses(campaign);

            assertPulledBack(false);
        }

        @Test
        void unitsNotDeployedToStratConAreNotCounted() {
            Unit undeployedUnit = addUnit(mockPerson(UNDEPLOYMENT_THRESHOLD));
            when(undeployedUnit.getFormationId()).thenReturn(FORMATION_ID + 1);
            addUnit(mockPerson(0));

            Fatigue.processDeploymentFatigueResponses(campaign);

            assertPulledBack(false);
        }

        @Test
        void formationDeployedToScenarioIsLeftInPlace() {
            when(formation.isDeployed()).thenReturn(true);
            addUnit(mockPerson(UNDEPLOYMENT_THRESHOLD));

            Fatigue.processDeploymentFatigueResponses(campaign);

            assertPulledBack(false);
        }

        @Test
        void formationNotDeployedToStratConIsIgnored() {
            when(stratConState.isForceDeployedHere(FORMATION_ID)).thenReturn(false);
            addUnit(mockPerson(UNDEPLOYMENT_THRESHOLD));

            Fatigue.processDeploymentFatigueResponses(campaign);

            assertPulledBack(false);
        }

        @Test
        void unassignsFromEveryTrackOnEveryContract() {
            StratConTrackState secondTrack = mock(StratConTrackState.class);
            StratConTrackState thirdTrack = mock(StratConTrackState.class);
            StratConCampaignState secondState = mock(StratConCampaignState.class);
            when(secondState.getTracks()).thenReturn(List.of(secondTrack, thirdTrack));
            AbstractContract firstContract = campaign.getActiveContracts().get(0);
            AbstractContract contractWithoutStratCon = mock(AbstractContract.class);
            AbstractContract secondContract = mock(AbstractContract.class);
            when(secondContract.getStratConCampaignState()).thenReturn(secondState);
            when(campaign.getActiveContracts()).thenReturn(List.of(firstContract, contractWithoutStratCon,
                  secondContract));
            addUnit(mockPerson(UNDEPLOYMENT_THRESHOLD));

            Fatigue.processDeploymentFatigueResponses(campaign);

            verify(track).unassignFormation(FORMATION_ID);
            verify(secondTrack).unassignFormation(FORMATION_ID);
            verify(thirdTrack).unassignFormation(FORMATION_ID);
        }

        @ParameterizedTest(name = "StratCon {0}, fatigue {1}, threshold {2}")
        @CsvSource({ "false, true, 10", "true, false, 10", "true, true, 0" })
        void doesNothingWhenDisabled(boolean isUseStratCon, boolean isUseFatigue, int threshold) {
            when(campaignOptions.isUseStratCon()).thenReturn(isUseStratCon);
            when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(isUseFatigue);
            when(campaignOptions.get(CampaignOption.FATIGUE_UNDEPLOYMENT_THRESHOLD)).thenReturn(threshold);
            addUnit(mockPerson(30));

            Fatigue.processDeploymentFatigueResponses(campaign);

            assertPulledBack(false);
        }
    }

    @Nested
    class WeeklyRecovery {
        @Test
        void recoversOnePointOnContract() {
            Person person = mockPerson(3);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-1);
        }

        @Test
        void recoversExtraPointOnLeave() {
            Person person = mockPerson(3);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-2);
        }

        @Test
        void recoversExtraPointWithoutContracts() {
            when(campaign.getActiveContracts()).thenReturn(List.of());
            Person person = mockPerson(3);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-2);
        }

        @Test
        void leaveAndNoContractsDoNotStack() {
            when(campaign.getActiveContracts()).thenReturn(List.of());
            Person person = mockPerson(3);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-2);
        }

        @Test
        void recoversExtraPointWithFieldKitchens() {
            Person person = mockPerson(3);

            Fatigue.processFatigueRecovery(campaign, person, true);

            verify(person).changeFatigue(-2);
        }

        @Test
        void allBonusesTogether() {
            Person person = mockPerson(3);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, true);

            verify(person).changeFatigue(-3);
        }

        @Test
        void noRecoveryWithoutFatigue() {
            Person person = mockPerson(0);

            Fatigue.processFatigueRecovery(campaign, person, true);

            verify(person, never()).changeFatigue(anyInt());
        }

        @Test
        void recoveringPersonOverThresholdIsSentOnLeave() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(LEAVE_THRESHOLD + 2);
            when(person.getIsRecoveringFromFatigue()).thenReturn(true);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
            // Already recovering, so no repeat of the fatigue state report
            verify(campaign, never()).addReport(any(), anyString());
        }

        @Test
        void recoveredPersonReturnsFromLeave() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(0);
            when(person.getIsRecoveringFromFatigue()).thenReturn(true);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(campaign, times(1)).addReport(eq(PERSONNEL), anyString());
            verify(person).setIsRecoveringFromFatigue(false);
            verify(person).changeStatus(campaign, TODAY, PersonnelStatus.ACTIVE);
        }

        @Test
        void recoversWithoutTakingActionsWhenFatigueDisabled() {
            when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(false);
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(LEAVE_THRESHOLD + 2);
            when(person.getIsRecoveringFromFatigue()).thenReturn(true);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).changeFatigue(-1);
            verify(person, never()).changeStatus(any(), any(), any());
            verify(person, never()).setIsRecoveringFromFatigue(anyBoolean());
            verify(campaign, never()).addReport(any(), anyString());
        }

        @Test
        void personNotYetRecoveringGetsFullFatigueActions() {
            Person person = mockPerson(10);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(campaign).addReport(eq(PERSONNEL), anyString());
            verify(person).setIsRecoveringFromFatigue(true);
        }

        @Test
        void personOnLeaveIsNotReprocessed() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(LEAVE_THRESHOLD + 2);
            when(person.getIsRecoveringFromFatigue()).thenReturn(true);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person, never()).changeStatus(any(), any(), any());
            verify(campaign, never()).addReport(any(), anyString());
        }

        @Test
        void recoveredPersonStaysOnLeaveWhenThresholdDisabled() {
            Person person = mockPerson(0);
            when(person.getIsRecoveringFromFatigue()).thenReturn(true);
            when(person.getStatus()).thenReturn(PersonnelStatus.ON_LEAVE);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(person).setIsRecoveringFromFatigue(false);
            // Without a leave threshold, leave wasn't granted by fatigue, so fatigue shouldn't end it
            verify(person, never()).changeStatus(any(), any(), any());
        }

        @Test
        void recoveredActivePersonKeepsTheirStatus() {
            when(campaignOptions.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD)).thenReturn(LEAVE_THRESHOLD);
            Person person = mockPerson(0);
            when(person.getIsRecoveringFromFatigue()).thenReturn(true);

            Fatigue.processFatigueRecovery(campaign, person, false);

            verify(campaign).addReport(eq(PERSONNEL), anyString());
            verify(person).setIsRecoveringFromFatigue(false);
            verify(person, never()).changeStatus(any(), any(), any());
        }
    }
}
