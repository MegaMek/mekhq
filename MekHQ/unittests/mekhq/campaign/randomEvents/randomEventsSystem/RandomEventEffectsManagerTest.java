/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.randomEventsSystem;

import static mekhq.campaign.mission.enums.AtBMoraleLevel.OVERWHELMING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_POISON_RESISTANCE;
import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR_LOGISTICS;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_SMALL_ARMS;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NO_ATTRIBUTE;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventEffectedPersonnelType.COMBAT_PERSONNEL;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventEffectedPersonnelType.PRISONERS;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResponseQuality.RESPONSE_NEUTRAL;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResultEffect.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RandomEventEffectsManagerTest {
    // -----------------------------------------------------------------------
    // Shared test infrastructure
    // -----------------------------------------------------------------------

    private Campaign mockCampaign;
    private CampaignOptions mockCampaignOptions;

    @BeforeEach
    void setUpCampaign() {
        mockCampaign = mock(Campaign.class);
        mockCampaignOptions = mock(CampaignOptions.class);
        Faction campaignFaction = mock(Faction.class);

        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
    }

    private static RandomEventData buildEventData(RandomEventResult result) {
        return buildEventData("BREAKOUT", result);
    }

    private static RandomEventData buildEventData(String type, RandomEventResult result) {
        RandomEventResponseEntry entry = new RandomEventResponseEntry(
              RESPONSE_NEUTRAL, "", 0, "", NO_ATTRIBUTE, List.of(result), List.of(result));
        return new RandomEventData(type, List.of(entry));
    }

    /** Creates a manager from a pre-built {@link RandomEventData}. */
    private RandomEventEffectsManager runEffect(RandomEventData eventData) {
        return new RandomEventEffectsManager(mockCampaign, eventData, 0, true);
    }

    /** Builds a result, wraps it in event data, runs the effect, and returns the manager. */
    private RandomEventEffectsManager runEffect(RandomEventResultEffect effect,
          List<RandomEventEffectedPersonnelType> targets, int magnitude, String extra) {
        RandomEventResult result = new RandomEventResult(effect, targets, magnitude, extra);
        return runEffect(buildEventData(result));
    }

    private RandomEventEffectsManager runEffect(RandomEventResultEffect effect,
          List<RandomEventEffectedPersonnelType> targets, int magnitude) {
        return runEffect(effect, targets, magnitude, "");
    }

    private RandomEventEffectsManager runEffectForType(String type,
          List<RandomEventEffectedPersonnelType> targets, int magnitude) {
        RandomEventResult result = new RandomEventResult(RandomEventResultEffect.UNIQUE, targets, magnitude, "");
        return runEffect(buildEventData(type, result));
    }

    /** Stubs {@code getAllPersonnel()} to return the given persons. */
    private void stubAllPersonnel(Person... persons) {
        when(mockCampaign.getAllPersonnel()).thenReturn(new ArrayList<>(List.of(persons)));
    }

    /** Creates a prisoner (marked with PRISONER status) backed by {@code mockCampaign}. */
    private Person makePrisoner() {
        Person person = new Person(mockCampaign);
        person.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        return person;
    }

    /** Enables fatigue and stubs the hangar/warehouse mocks needed by the fatigue path. */
    private void enableFatigue() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(true);
        when(mockCampaignOptions.getFatigueRate()).thenReturn(1);
        when(mockCampaign.getHangar()).thenReturn(mock(Hangar.class));
        when(mockCampaign.getWarehouse()).thenReturn(mock(Warehouse.class));
    }

    // -----------------------------------------------------------------------
    // PRISONER_CAPACITY
    // -----------------------------------------------------------------------

    @Test
    void testEffectPrisonerCapacity_positiveChange_reportContainsMagnitude() {
        RandomEventEffectsManager manager = runEffect(PRISONER_CAPACITY, new ArrayList<>(), 5);

        assertTrue(manager.getMechanicalEffectsReport().contains("5"),
              "Report should mention the capacity change amount");
    }

    @Test
    void testEffectPrisonerCapacity_negativeChange_reportContainsMagnitude() {
        RandomEventEffectsManager manager = runEffect(PRISONER_CAPACITY, new ArrayList<>(), -3);

        assertTrue(manager.getMechanicalEffectsReport().contains("3"),
              "Report should mention the (absolute) capacity change amount");
    }

    // -----------------------------------------------------------------------
    // INJURY (single target)
    // -----------------------------------------------------------------------

    @Test
    void testEffectInjury_noAdvancedMedical_appliesExactHits() {
        final int MAGNITUDE = 3;

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        runEffect(INJURY, List.of(PRISONERS), MAGNITUDE);

        assertEquals(MAGNITUDE, prisoner.getHits());
    }

    @Test
    void testEffectInjury_magnitudeExceedsMax_clampedToFive() {
        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        runEffect(INJURY, List.of(PRISONERS), 10);

        assertEquals(5, prisoner.getHits(),
              "Hits must not exceed the maximum of 5 so the target cannot be killed by this effect");
    }

    @Test
    void testEffectInjury_withAdvancedMedical_injuriesApplied() {
        when(mockCampaignOptions.isUseAdvancedMedical()).thenReturn(true);

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        runEffect(INJURY, List.of(PRISONERS), 5);

        assertFalse(prisoner.getInjuries().isEmpty(),
              "At least one injury should have been diagnosed under advanced medical");
    }

    @Test
    void testEffectInjury_noTargets_returnsEmptyReport() {
        stubAllPersonnel();

        RandomEventEffectsManager manager = runEffect(INJURY, List.of(PRISONERS), 3);

        assertTrue(manager.getMechanicalEffectsReport().isBlank(),
              "Empty target pool should produce no report output");
    }

    // -----------------------------------------------------------------------
    // INJURY_PERCENT
    // -----------------------------------------------------------------------

    @Test
    void testEffectInjuryPercent_50Percent_injuresHalfOfTargets() {
        Person person0 = makePrisoner();
        Person person1 = makePrisoner();
        Person person2 = makePrisoner();
        Person person3 = makePrisoner();
        stubAllPersonnel(person0, person1, person2, person3);

        runEffect(INJURY_PERCENT, List.of(PRISONERS), 50);

        long injured = Stream.of(person0, person1, person2, person3).filter(Person::needsFixing).count();
        assertEquals(2, injured, "Exactly half of 4 prisoners should be injured at 50 % magnitude");
    }

    @Test
    void testEffectInjuryPercent_smallMagnitude_minimumOneTarget() {
        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        runEffect(INJURY_PERCENT, List.of(PRISONERS), 1);

        assertTrue(prisoner.needsFixing(),
              "At least 1 target should always be injured regardless of rounding");
    }

    // -----------------------------------------------------------------------
    // DEATH (fixed count)
    // -----------------------------------------------------------------------

    @Test
    void testEffectDeath_singlePrisoner_reportContainsCount() {
        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        String report = runEffect(DEATH, List.of(PRISONERS), 1).getMechanicalEffectsReport();

        assertFalse(report.isBlank(), "Death effect should produce a non-empty report");
        assertTrue(report.contains("1 "), "Report should state that 1 casualty occurred");
    }

    @Test
    void testEffectDeath_magnitudeExceedsPool_clampedToPoolSize() {
        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        String report = runEffect(DEATH, List.of(PRISONERS), 10).getMechanicalEffectsReport();

        assertTrue(report.contains("1 "), "Death count should be clamped to pool size");
    }

    @Test
    void testEffectDeath_noTargets_emptyReport() {
        stubAllPersonnel();

        assertTrue(runEffect(DEATH, List.of(PRISONERS), 1).getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // DEATH_PERCENT
    // -----------------------------------------------------------------------

    @Test
    void testEffectDeathPercent_50Percent_reportContainsCorrectCount() {
        Person person0 = makePrisoner();
        Person person1 = makePrisoner();
        Person person2 = makePrisoner();
        Person person3 = makePrisoner();
        stubAllPersonnel(person0, person1, person2, person3);

        String report = runEffect(DEATH_PERCENT, List.of(PRISONERS), 50).getMechanicalEffectsReport();

        assertFalse(report.isBlank(), "Death-percent effect should produce a report");
        assertTrue(report.contains("2 "), "50 %% of 4 prisoners = 2 casualties; report should say '2 '");
    }

    @Test
    void testEffectDeathPercent_noTargets_emptyReport() {
        stubAllPersonnel();

        assertTrue(runEffect(DEATH_PERCENT, List.of(PRISONERS), 50).getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // SKILL
    // -----------------------------------------------------------------------

    @Test
    void testEffectSkill_prisonerLacksSkill_skillAdded() {
        SkillType.initializeTypes();

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        runEffect(SKILL, List.of(PRISONERS), 5, S_ADMIN);

        assertTrue(prisoner.hasSkill(S_ADMIN), "Prisoner should now have the Admin skill");
        assertEquals(5, prisoner.getSkill(S_ADMIN).getLevel());
    }

    @Test
    void testEffectSkill_prisonerHasHigherSkill_levelReduced() {
        SkillType.initializeTypes();

        Person prisoner = makePrisoner();
        prisoner.addSkill(S_ADMIN, 8, 0);
        stubAllPersonnel(prisoner);

        runEffect(SKILL, List.of(PRISONERS), 3, S_ADMIN);

        assertEquals(3, prisoner.getSkill(S_ADMIN).getLevel(),
              "Skill level should be reduced to the event magnitude when it was higher");
    }

    @Test
    void testEffectSkill_prisonerHasEqualOrLowerSkill_noChange() {
        SkillType.initializeTypes();

        Person prisoner = makePrisoner();
        prisoner.addSkill(S_ADMIN, 2, 0);
        stubAllPersonnel(prisoner);

        RandomEventEffectsManager manager = runEffect(SKILL, List.of(PRISONERS), 5, S_ADMIN);

        assertEquals(2, prisoner.getSkill(S_ADMIN).getLevel(), "Skill level should not be raised by this effect");
        assertTrue(manager.getMechanicalEffectsReport().isBlank(),
              "No report should be generated when no change was made");
    }

    @Test
    void testEffectSkill_unknownSkillType_emptyReport() {
        SkillType.initializeTypes();

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        RandomEventEffectsManager manager = runEffect(SKILL, List.of(PRISONERS), 5, "NONEXISTENT_SKILL_XYZ");

        assertTrue(manager.getMechanicalEffectsReport().isBlank(),
              "An unknown skill type should produce an empty report");
    }

    // -----------------------------------------------------------------------
    // LOYALTY_ONE
    // -----------------------------------------------------------------------

    @Test
    void testEffectLoyaltyOne_loyaltyEnabled_loyaltyIncreased() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        runEffect(LOYALTY_ONE, List.of(PRISONERS), 5);

        assertEquals(before + 5, prisoner.getBaseLoyalty());
    }

    @Test
    void testEffectLoyaltyOne_negativeMagnitude_loyaltyDecreased() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        runEffect(LOYALTY_ONE, List.of(PRISONERS), -3);

        assertEquals(before - 3, prisoner.getBaseLoyalty());
    }

    @Test
    void testEffectLoyaltyOne_loyaltyDisabled_noChangeAndEmptyReport() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(false);

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        RandomEventEffectsManager manager = runEffect(LOYALTY_ONE, List.of(PRISONERS), 5);

        assertEquals(before, prisoner.getBaseLoyalty(), "Loyalty must not change when the option is disabled");
        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // LOYALTY_ALL
    // -----------------------------------------------------------------------

    @Test
    void testEffectLoyaltyAll_loyaltyEnabled_allPrisonersAdjusted() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person person0 = makePrisoner();
        Person person1 = makePrisoner();
        Person person2 = makePrisoner();
        Person person3 = makePrisoner();
        List<Integer> before = List.of(
              person0.getBaseLoyalty(), person1.getBaseLoyalty(),
              person2.getBaseLoyalty(), person3.getBaseLoyalty());
        stubAllPersonnel(person0, person1, person2, person3);

        runEffect(LOYALTY_ALL, List.of(PRISONERS), 5);

        List<Person> persons = List.of(person0, person1, person2, person3);
        for (int i = 0; i < persons.size(); i++) {
            assertEquals(before.get(i) + 5, persons.get(i).getBaseLoyalty(),
                  "Every prisoner should have their loyalty increased by 5");
        }
    }

    @Test
    void testEffectLoyaltyAll_loyaltyDisabled_emptyReport() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(false);

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        RandomEventEffectsManager manager = runEffect(LOYALTY_ALL, List.of(PRISONERS), 5);

        assertEquals(before, prisoner.getBaseLoyalty());
        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // ESCAPE (fixed count)
    // -----------------------------------------------------------------------

    @Test
    void testEffectEscape_singleEscape_escapeeTracked() {
        Person person0 = makePrisoner();
        Person person1 = makePrisoner();
        Person person2 = makePrisoner();
        Person person3 = makePrisoner();
        stubAllPersonnel(person0, person1, person2, person3);

        RandomEventEffectsManager manager = runEffect(ESCAPE, List.of(PRISONERS), 1);

        assertEquals(1, manager.getPersonHashSet().size(),
              "Exactly 1 prisoner should be tracked as escaped");
    }

    @Test
    void testEffectEscape_magnitudeExceedsPool_clampedToPoolSize() {
        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        RandomEventEffectsManager manager = runEffect(ESCAPE, List.of(PRISONERS), 10);

        assertEquals(1, manager.getPersonHashSet().size(),
              "Escape count should be capped to the number of available prisoners");
    }

    @Test
    void testEffectEscape_noTargets_emptyReportAndNoEscapees() {
        stubAllPersonnel();

        RandomEventEffectsManager manager = runEffect(ESCAPE, List.of(PRISONERS), 1);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
        assertTrue(manager.getPersonHashSet().isEmpty());
    }

    // -----------------------------------------------------------------------
    // ESCAPE_PERCENT
    // -----------------------------------------------------------------------

    @Test
    void testEffectEscapePercent_50Percent_twoEscapees() {
        Person person0 = makePrisoner();
        Person person1 = makePrisoner();
        Person person2 = makePrisoner();
        Person person3 = makePrisoner();
        stubAllPersonnel(person0, person1, person2, person3);

        RandomEventEffectsManager manager = runEffect(ESCAPE_PERCENT, List.of(PRISONERS), 50);

        assertEquals(2, manager.getPersonHashSet().size(), "50 %% of 4 prisoners = 2 escapees");
    }

    @Test
    void testEffectEscapePercent_smallPercentage_minimumOneEscapee() {
        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);

        RandomEventEffectsManager manager = runEffect(ESCAPE_PERCENT, List.of(PRISONERS), 1);

        assertEquals(1, manager.getPersonHashSet().size(),
              "Ceiling calculation should ensure at least 1 person escapes");
    }

    // -----------------------------------------------------------------------
    // FATIGUE_ONE
    // -----------------------------------------------------------------------

    @Test
    void testEffectFatigueOne_fatigueEnabled_fatigueIncreased() {
        enableFatigue();

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);
        int before = prisoner.getFatigueDirect();

        runEffect(FATIGUE_ONE, List.of(PRISONERS), 5);

        assertEquals(before + 5, prisoner.getFatigueDirect());
    }

    @Test
    void testEffectFatigueOne_fatigueDisabled_noChangeAndEmptyReport() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(false);

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);
        int before = prisoner.getFatigueDirect();

        RandomEventEffectsManager manager = runEffect(FATIGUE_ONE, List.of(PRISONERS), 5);

        assertEquals(before, prisoner.getFatigueDirect(), "Fatigue must not change when the option is disabled");
        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // FATIGUE_ALL
    // -----------------------------------------------------------------------

    @Test
    void testEffectFatigueAll_fatigueEnabled_allPrisonersAffected() {
        enableFatigue();

        Person person0 = makePrisoner();
        Person person1 = makePrisoner();
        Person person2 = makePrisoner();
        Person person3 = makePrisoner();
        List<Integer> before = List.of(
              person0.getFatigueDirect(), person1.getFatigueDirect(),
              person2.getFatigueDirect(), person3.getFatigueDirect());
        stubAllPersonnel(person0, person1, person2, person3);

        runEffect(FATIGUE_ALL, List.of(PRISONERS), 5);

        List<Person> persons = List.of(person0, person1, person2, person3);
        for (int i = 0; i < persons.size(); i++) {
            assertEquals(before.get(i) + 5, persons.get(i).getFatigueDirect(),
                  "Every prisoner should have their fatigue increased by 5");
        }
    }

    @Test
    void testEffectFatigueAll_fatigueDisabled_emptyReport() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(false);

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);
        int before = prisoner.getFatigueDirect();

        RandomEventEffectsManager manager = runEffect(FATIGUE_ALL, List.of(PRISONERS), 5);

        assertEquals(before, prisoner.getFatigueDirect());
        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // SUPPORT_POINT
    // -----------------------------------------------------------------------

    @Test
    void testEffectSupportPoint_stratConEnabled_pointsIncreased() {
        when(mockCampaignOptions.isUseStratCon()).thenReturn(true);

        AtBContract contract = new AtBContract("Test");
        StratConCampaignState state = new StratConCampaignState(contract);
        contract.setStratConCampaignState(state);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        runEffect(SUPPORT_POINT, List.of(PRISONERS), 5);

        assertEquals(5, state.getSupportPoints());
    }

    @Test
    void testEffectSupportPoint_stratConDisabled_emptyReport() {
        when(mockCampaignOptions.isUseStratCon()).thenReturn(false);

        assertTrue(runEffect(SUPPORT_POINT, List.of(PRISONERS), 5).getMechanicalEffectsReport().isBlank());
    }

    @Test
    void testEffectSupportPoint_noActiveContracts_emptyReport() {
        when(mockCampaignOptions.isUseStratCon()).thenReturn(true);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of());

        assertTrue(runEffect(SUPPORT_POINT, List.of(PRISONERS), 5).getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // UNIQUE — BARTERING (morale bump)
    // -----------------------------------------------------------------------

    @Test
    void testEffectUniqueBartering_stalemateMorale_moraleBumped() {
        AtBContract contract = new AtBContract("Test");
        contract.setMoraleLevel(STALEMATE);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        runEffectForType("BARTERING", List.of(PRISONERS), 1);

        assertEquals(STALEMATE.ordinal() + 1, contract.getMoraleLevel().ordinal(),
              "Morale should advance by one level");
    }

    @Test
    void testEffectUniqueBartering_overwhelmingMorale_noChange() {
        AtBContract contract = new AtBContract("Test");
        contract.setMoraleLevel(OVERWHELMING);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        RandomEventEffectsManager manager = runEffectForType("BARTERING", List.of(PRISONERS), 1);

        assertEquals(OVERWHELMING, contract.getMoraleLevel(), "Morale must not exceed OVERWHELMING");
        assertTrue(manager.getMechanicalEffectsReport().isBlank(),
              "No report should be generated when no contract qualifies");
    }

    @Test
    void testEffectUniqueBartering_noContracts_emptyReport() {
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of());

        assertTrue(runEffectForType("BARTERING", List.of(PRISONERS), 1)
                         .getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // UNIQUE — MISTAKE (skill & role wipe)
    // -----------------------------------------------------------------------

    @Test
    void testEffectUniqueMistake_skillsAndRolesReset() {
        SkillType.initializeTypes();

        Person prisoner = makePrisoner();
        prisoner.addSkill(S_ADMIN, 1, 0);
        prisoner.addSkill(S_SMALL_ARMS, 1, 0);
        prisoner.addSkill(S_SURGERY, 1, 0);
        prisoner.setPrimaryRoleDirect(SOLDIER);
        prisoner.setSecondaryRole(ADMINISTRATOR_LOGISTICS);
        stubAllPersonnel(prisoner);

        runEffectForType("MISTAKE", List.of(PRISONERS), 1);

        assertNull(prisoner.getSkill(S_ADMIN), "Admin skill should be removed");
        assertNull(prisoner.getSkill(S_SMALL_ARMS), "Small Arms skill should be removed");
        assertNull(prisoner.getSkill(S_SURGERY), "Surgery skill should be removed");
        assertSame(DEPENDENT, prisoner.getPrimaryRole(), "Primary role must be DEPENDENT");
        assertSame(NONE, prisoner.getSecondaryRole(), "Secondary role must be NONE");
    }

    @Test
    void testEffectUniqueMistake_noTargets_emptyReport() {
        SkillType.initializeTypes();
        stubAllPersonnel();

        assertTrue(runEffectForType("MISTAKE", List.of(PRISONERS), 1)
                         .getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // UNIQUE — UNDERCOVER (faction change)
    // -----------------------------------------------------------------------

    @Test
    void testEffectUniqueUndercover_prisonerFactionChanged() {
        AtBContract contract = mock(AtBContract.class);
        Faction employerFaction = new Faction("employerFaction", "employerFaction");
        when(contract.getEmployerFaction()).thenReturn(employerFaction);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        Person prisoner = makePrisoner();
        Faction originalFaction = new Faction("prisonerFaction", "prisonerFaction");
        prisoner.setOriginFaction(originalFaction);
        stubAllPersonnel(prisoner);

        runEffectForType("UNDERCOVER", List.of(PRISONERS), 1);

        assertNotEquals(originalFaction, prisoner.getOriginFaction(), "The prisoner's faction should have changed");
        assertEquals(employerFaction, prisoner.getOriginFaction(),
              "The prisoner's faction should now match the employer's");
    }

    @Test
    void testEffectUniqueUndercover_noContracts_noChange() {
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of());

        Person prisoner = makePrisoner();
        Faction originalFaction = new Faction();
        prisoner.setOriginFaction(originalFaction);
        stubAllPersonnel(prisoner);

        runEffectForType("UNDERCOVER", List.of(PRISONERS), 1);

        assertEquals(originalFaction, prisoner.getOriginFaction(),
              "Faction should be unchanged when no contract is available");
    }

    // -----------------------------------------------------------------------
    // UNIQUE — POISON (fatigue with poison-resistance check)
    // -----------------------------------------------------------------------

    @Test
    void testEffectUniquePoison_fatigueEnabled_allNonResistantPersonnelFatigued() {
        enableFatigue();

        Person soldier0 = new Person(mockCampaign);
        soldier0.setPrimaryRoleDirect(SOLDIER);
        Person soldier1 = new Person(mockCampaign);
        soldier1.setPrimaryRoleDirect(SOLDIER);
        Person soldier2 = new Person(mockCampaign);
        soldier2.setPrimaryRoleDirect(SOLDIER);
        stubAllPersonnel(soldier0, soldier1, soldier2);

        runEffectForType("POISON", List.of(COMBAT_PERSONNEL), 5);

        long fatigued = Stream.of(soldier0, soldier1, soldier2)
                              .filter(p -> p.getFatigueDirect() > 0)
                              .count();
        assertEquals(3, fatigued, "All 3 personnel without poison resistance should be fatigued");
    }

    @Test
    void testEffectUniquePoison_resistantPerson_isSkipped() {
        enableFatigue();

        Person soldier0 = new Person(mockCampaign);
        soldier0.setPrimaryRoleDirect(SOLDIER);
        soldier0.getOptions().getOption(ATOW_POISON_RESISTANCE).setValue(true);

        Person soldier1 = new Person(mockCampaign);
        soldier1.setPrimaryRoleDirect(SOLDIER);
        stubAllPersonnel(soldier0, soldier1);

        int resistantFatigueBefore = soldier0.getFatigueDirect();

        runEffectForType("POISON", List.of(COMBAT_PERSONNEL), 5);

        assertEquals(resistantFatigueBefore, soldier0.getFatigueDirect(),
              "A person with ATOW_POISON_RESISTANCE must not receive any fatigue from the poison effect");
        assertTrue(soldier1.getFatigueDirect() > 0,
              "A person without poison resistance must still receive fatigue");
    }

    @Test
    void testEffectUniquePoison_fatigueDisabled_emptyReport() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(false);

        Person soldier = new Person(mockCampaign);
        stubAllPersonnel(soldier);

        assertTrue(runEffectForType("POISON", List.of(COMBAT_PERSONNEL), 5)
                         .getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // Failure branch (wasSuccessful = false)
    // -----------------------------------------------------------------------

    @Test
    void testFailureBranch_usesFailureResultsList() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person prisoner = makePrisoner();
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        RandomEventResult successResult = new RandomEventResult(LOYALTY_ONE, List.of(PRISONERS), +10, "");
        RandomEventResult failureResult = new RandomEventResult(LOYALTY_ONE, List.of(PRISONERS), -7, "");

        RandomEventResponseEntry entry = new RandomEventResponseEntry(
              RESPONSE_NEUTRAL, "", 0, "", NO_ATTRIBUTE, List.of(successResult), List.of(failureResult));
        RandomEventData eventData = new RandomEventData("BREAKOUTON", List.of(entry));

        new RandomEventEffectsManager(mockCampaign, eventData, 0, false);

        assertEquals(before - 7, prisoner.getBaseLoyalty(),
              "Failure branch should apply the failure effect (−7 loyalty), not the success one (+10)");
    }

    // -----------------------------------------------------------------------
    // Mixed personnel type filtering
    // -----------------------------------------------------------------------

    @Test
    void testPersonnelFiltering_prisonerEffectDoesNotTargetActiveCombatPersonnel() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person prisoner = makePrisoner();
        Person combatPersonnel = new Person(mockCampaign); // not a prisoner
        stubAllPersonnel(prisoner, combatPersonnel);
        int combatLoyaltyBefore = combatPersonnel.getBaseLoyalty();

        runEffect(LOYALTY_ALL, List.of(PRISONERS), 5);

        assertEquals(combatLoyaltyBefore, combatPersonnel.getBaseLoyalty(),
              "Non-prisoner personnel must not be affected by a PRISONERS-only effect");
    }
}
