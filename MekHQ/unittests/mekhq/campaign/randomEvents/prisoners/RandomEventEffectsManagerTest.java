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
package mekhq.campaign.randomEvents.prisoners;

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
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventType.BARTERING;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventType.BREAKOUT;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventType.MISTAKE;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventType.POISON;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventType.UNDERCOVER;
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

import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.randomEventsSystem.RandomEventData;
import mekhq.campaign.randomEvents.randomEventsSystem.RandomEventEffectsManager;
import mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResponseEntry;
import mekhq.campaign.randomEvents.randomEventsSystem.RandomEventResult;
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
    private Faction campaignFaction;

    /**
     * Builds the minimal campaign mock that every test needs. Individual tests add further stubs on top of this
     * baseline.
     */
    @BeforeEach
    void setUpCampaign() {
        mockCampaign = mock(Campaign.class);
        mockCampaignOptions = mock(CampaignOptions.class);
        campaignFaction = mock(Faction.class);

        when(campaignFaction.isMercenary()).thenReturn(true);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
    }

    /**
     * Helper: builds a {@link RandomEventData} / {@link RandomEventResponseEntry} pair for the BREAKOUT event type with
     * identical success and failure result lists.
     */
    private static RandomEventData buildEventData(RandomEventResult result) {
        return buildEventData(BREAKOUT, result);
    }

    private static RandomEventData buildEventData(
          mekhq.campaign.randomEvents.randomEventsSystem.RandomEventType type,
          RandomEventResult result) {
        RandomEventResponseEntry entry = new RandomEventResponseEntry(
              RESPONSE_NEUTRAL,
              "",
              NO_ATTRIBUTE,
              List.of(result),
              List.of(result));
        return new RandomEventData(type, List.of(entry));
    }

    /**
     * Registers {@code persons} as the return value of {@code campaign.getAllPersonnel()}, which is the method
     * {@code getAllPotentialTargets} actually calls.
     *
     * <p><b>Note:</b> do NOT use {@code getCurrentPrisoners()} here — that method is not
     * consulted by the effects manager's targeting logic.</p>
     */
    private void stubAllPersonnel(Person... persons) {
        when(mockCampaign.getAllPersonnel()).thenReturn(new ArrayList<>(List.of(persons)));
    }

    // -----------------------------------------------------------------------
    // PRISONER_CAPACITY
    // -----------------------------------------------------------------------

    @Test
    void testEffectPrisonerCapacity_positiveChange_reportContainsMagnitude() {
        final int MAGNITUDE = 5;

        RandomEventResult result = new RandomEventResult(PRISONER_CAPACITY, new ArrayList<>(), MAGNITUDE, "");
        RandomEventData eventData = buildEventData(result);

        RandomEventEffectsManager manager = new RandomEventEffectsManager(mockCampaign, eventData, 0, true);

        assertTrue(manager.getMechanicalEffectsReport().contains("5"),
              "Report should mention the capacity change amount");
    }

    @Test
    void testEffectPrisonerCapacity_negativeChange_reportContainsMagnitude() {
        final int MAGNITUDE = -3;

        RandomEventResult result = new RandomEventResult(PRISONER_CAPACITY, new ArrayList<>(), MAGNITUDE, "");
        RandomEventData eventData = buildEventData(result);

        RandomEventEffectsManager manager = new RandomEventEffectsManager(mockCampaign, eventData, 0, true);

        assertTrue(manager.getMechanicalEffectsReport().contains("3"),
              "Report should mention the (absolute) capacity change amount");
    }

    // -----------------------------------------------------------------------
    // INJURY (single target)
    // -----------------------------------------------------------------------

    /**
     * A prisoner with no prior injuries and a magnitude of 3 should end up with exactly 3 hits when advanced medical is
     * disabled (straight assignment).
     */
    @Test
    void testEffectInjury_noAdvancedMedical_appliesExactHits() {
        final int MAGNITUDE = 3;

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false); // mark as current prisoner
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(INJURY, List.of(PRISONERS), MAGNITUDE, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(MAGNITUDE, prisoner.getHits());
    }

    /**
     * A magnitude larger than 5 must be clamped to 5 because the dead-man's threshold is 5 hits in the production
     * code.
     */
    @Test
    void testEffectInjury_magnitudeExceedsMax_clampedToFive() {
        final int MAGNITUDE = 10;

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(INJURY, List.of(PRISONERS), MAGNITUDE, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(5, prisoner.getHits(),
              "Hits must not exceed the maximum of 5 so the target cannot be killed by this effect");
    }

    /**
     * With advanced medical enabled the result is non-deterministic (injuries are rolled), but the injury list must be
     * non-empty.
     */
    @Test
    void testEffectInjury_withAdvancedMedical_injuriesApplied() {
        when(mockCampaignOptions.isUseAdvancedMedical()).thenReturn(true);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(INJURY, List.of(PRISONERS), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertFalse(prisoner.getInjuries().isEmpty(),
              "At least one injury should have been diagnosed under advanced medical");
    }

    /**
     * When there are no valid targets, the report for the INJURY effect should be empty and no exception should be
     * thrown.
     */
    @Test
    void testEffectInjury_noTargets_returnsEmptyReport() {
        stubAllPersonnel(); // nobody

        RandomEventResult result = new RandomEventResult(INJURY, List.of(PRISONERS), 3, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank(),
              "Empty target pool should produce no report output");
    }

    // -----------------------------------------------------------------------
    // INJURY_PERCENT
    // -----------------------------------------------------------------------

    /**
     * With a 50 % magnitude and 4 prisoners, exactly 2 should receive injuries.
     */
    @Test
    void testEffectInjuryPercent_50Percent_injuresHalfOfTargets() {
        final int MAGNITUDE = 50;

        Person p0 = new Person(mockCampaign);
        Person p1 = new Person(mockCampaign);
        Person p2 = new Person(mockCampaign);
        Person p3 = new Person(mockCampaign);
        for (Person p : List.of(p0, p1, p2, p3)) {
            p.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        }
        stubAllPersonnel(p0, p1, p2, p3);

        RandomEventResult result = new RandomEventResult(INJURY_PERCENT, List.of(PRISONERS), MAGNITUDE, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        long injured = List.of(p0, p1, p2, p3).stream()
                             .filter(Person::needsFixing)
                             .count();
        assertEquals(2, injured,
              "Exactly half of 4 prisoners should be injured at 50 % magnitude");
    }

    /**
     * With a very small percentage and only 1 prisoner, the minimum of 1 target should still be injured (floor is 1).
     */
    @Test
    void testEffectInjuryPercent_smallMagnitude_minimumOneTarget() {
        final int MAGNITUDE = 1; // 1 % of 1 person rounds down to 0, but floor is 1

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(INJURY_PERCENT, List.of(PRISONERS), MAGNITUDE, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(prisoner.needsFixing(),
              "At least 1 target should always be injured regardless of rounding");
    }

    // -----------------------------------------------------------------------
    // DEATH (fixed count)
    // -----------------------------------------------------------------------

    /**
     * The report for a prisoner death must contain the death count. Because prisoners are NPCs they are removed via
     * {@code campaign.removePerson} (which is mocked and does nothing) so we cannot check removal; the report count is
     * the only observable side effect available in unit tests.
     */
    @Test
    void testEffectDeath_singlePrisoner_reportContainsCount() {
        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(DEATH, List.of(PRISONERS), 1, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        String report = manager.getMechanicalEffectsReport();
        assertFalse(report.isBlank(), "Death effect should produce a non-empty report");
        // "1 " (with trailing space) is how the count appears at the report's start,
        // which avoids false positives from hex colour codes.
        assertTrue(report.contains("1 "), "Report should state that 1 casualty occurred");
    }

    /**
     * When the magnitude exceeds the available prisoner count, the actual death count is capped to the pool size.
     */
    @Test
    void testEffectDeath_magnitudeExceedsPool_clampedToPoolSize() {
        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        // Request 10 deaths from a pool of 1
        RandomEventResult result = new RandomEventResult(DEATH, List.of(PRISONERS), 10, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        // Only 1 death can occur — the report should state "1 " not "10 "
        String report = manager.getMechanicalEffectsReport();
        assertTrue(report.contains("1 "), "Death count should be clamped to pool size");
    }

    /**
     * With no targets, the DEATH effect should produce an empty report.
     */
    @Test
    void testEffectDeath_noTargets_emptyReport() {
        stubAllPersonnel();

        RandomEventResult result = new RandomEventResult(DEATH, List.of(PRISONERS), 1, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // DEATH_PERCENT
    // -----------------------------------------------------------------------

    /**
     * With 50 % magnitude and 4 prisoners, the report must state "2 " casualties.
     */
    @Test
    void testEffectDeathPercent_50Percent_reportContainsCorrectCount() {
        Person p0 = new Person(mockCampaign);
        Person p1 = new Person(mockCampaign);
        Person p2 = new Person(mockCampaign);
        Person p3 = new Person(mockCampaign);
        for (Person p : List.of(p0, p1, p2, p3)) {
            p.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        }
        stubAllPersonnel(p0, p1, p2, p3);

        RandomEventResult result = new RandomEventResult(DEATH_PERCENT, List.of(PRISONERS), 50, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        String report = manager.getMechanicalEffectsReport();
        assertFalse(report.isBlank(), "Death-percent effect should produce a report");
        assertTrue(report.contains("2 "),
              "50 %% of 4 prisoners = 2 casualties; report should say '2 '");
    }

    /**
     * With no targets, the DEATH_PERCENT effect should produce an empty report.
     */
    @Test
    void testEffectDeathPercent_noTargets_emptyReport() {
        stubAllPersonnel();

        RandomEventResult result = new RandomEventResult(DEATH_PERCENT, List.of(PRISONERS), 50, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // SKILL
    // -----------------------------------------------------------------------

    /**
     * A prisoner who does not yet have the target skill should gain it at the specified level.
     */
    @Test
    void testEffectSkill_prisonerLacksSkill_skillAdded() {
        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(SKILL, List.of(PRISONERS), 5, S_ADMIN);
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(prisoner.hasSkill(S_ADMIN), "Prisoner should now have the Admin skill");
        assertEquals(5, prisoner.getSkill(S_ADMIN).getLevel());
    }

    /**
     * A prisoner who already has the skill at a <em>higher</em> level should have it reduced to the event's magnitude
     * (capped downward).
     */
    @Test
    void testEffectSkill_prisonerHasHigherSkill_levelReduced() {
        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        prisoner.addSkill(S_ADMIN, 8, 0);
        stubAllPersonnel(prisoner);

        // magnitude = 3 < existing level 8 → level should drop to 3
        RandomEventResult result = new RandomEventResult(SKILL, List.of(PRISONERS), 3, S_ADMIN);
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(3, prisoner.getSkill(S_ADMIN).getLevel(),
              "Skill level should be reduced to the event magnitude when it was higher");
    }

    /**
     * A prisoner who already has the skill at an <em>equal or lower</em> level should not be affected — no report
     * should be generated for that target.
     */
    @Test
    void testEffectSkill_prisonerHasEqualOrLowerSkill_noChange() {
        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        prisoner.addSkill(S_ADMIN, 2, 0);
        stubAllPersonnel(prisoner);

        // magnitude = 5 is NOT less than existing level 2 → no change expected
        // (Production condition: if magnitude < currentLevel → set; else no-op)
        RandomEventResult result = new RandomEventResult(SKILL, List.of(PRISONERS), 5, S_ADMIN);
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(2, prisoner.getSkill(S_ADMIN).getLevel(),
              "Skill level should not be raised by this effect");
        assertTrue(manager.getMechanicalEffectsReport().isBlank(),
              "No report should be generated when no change was made");
    }

    /**
     * An unknown skill type string should silently produce an empty report.
     */
    @Test
    void testEffectSkill_unknownSkillType_emptyReport() {
        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(SKILL, List.of(PRISONERS), 5, "NONEXISTENT_SKILL_XYZ");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank(),
              "An unknown skill type should produce an empty report");
    }

    // -----------------------------------------------------------------------
    // LOYALTY_ONE
    // -----------------------------------------------------------------------

    /**
     * Positive magnitude should increase a prisoner's loyalty by that amount.
     */
    @Test
    void testEffectLoyaltyOne_loyaltyEnabled_loyaltyIncreased() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        RandomEventResult result = new RandomEventResult(LOYALTY_ONE, List.of(PRISONERS), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(before + 5, prisoner.getBaseLoyalty());
    }

    /**
     * Negative magnitude should decrease a prisoner's loyalty.
     */
    @Test
    void testEffectLoyaltyOne_negativeMagnitude_loyaltyDecreased() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        RandomEventResult result = new RandomEventResult(LOYALTY_ONE, List.of(PRISONERS), -3, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(before - 3, prisoner.getBaseLoyalty());
    }

    /**
     * When loyalty modifiers are disabled the effect is a no-op and the report is empty.
     */
    @Test
    void testEffectLoyaltyOne_loyaltyDisabled_noChangeAndEmptyReport() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(false);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        RandomEventResult result = new RandomEventResult(LOYALTY_ONE, List.of(PRISONERS), 5, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(before, prisoner.getBaseLoyalty(), "Loyalty must not change when the option is disabled");
        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // LOYALTY_ALL
    // -----------------------------------------------------------------------

    /**
     * All prisoners should receive the loyalty adjustment when loyalty is enabled.
     */
    @Test
    void testEffectLoyaltyAll_loyaltyEnabled_allPrisonersAdjusted() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person p0 = new Person(mockCampaign);
        Person p1 = new Person(mockCampaign);
        Person p2 = new Person(mockCampaign);
        Person p3 = new Person(mockCampaign);
        for (Person p : List.of(p0, p1, p2, p3)) {
            p.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        }
        List<Integer> before = List.of(
              p0.getBaseLoyalty(), p1.getBaseLoyalty(),
              p2.getBaseLoyalty(), p3.getBaseLoyalty());
        stubAllPersonnel(p0, p1, p2, p3);

        RandomEventResult result = new RandomEventResult(LOYALTY_ALL, List.of(PRISONERS), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        List<Person> persons = List.of(p0, p1, p2, p3);
        for (int i = 0; i < persons.size(); i++) {
            assertEquals(before.get(i) + 5, persons.get(i).getBaseLoyalty(),
                  "Every prisoner should have their loyalty increased by 5");
        }
    }

    /**
     * When loyalty modifiers are disabled, LOYALTY_ALL produces an empty report and no changes.
     */
    @Test
    void testEffectLoyaltyAll_loyaltyDisabled_emptyReport() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(false);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        RandomEventResult result = new RandomEventResult(LOYALTY_ALL, List.of(PRISONERS), 5, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(before, prisoner.getBaseLoyalty());
        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // ESCAPE (fixed count)
    // -----------------------------------------------------------------------

    /**
     * After the escape effect runs, the escapee set returned by {@link RandomEventEffectsManager#getPersonHashSet()}
     * must contain exactly the requested number of prisoners.
     */
    @Test
    void testEffectEscape_singleEscape_escapeeTracked() {
        Person p0 = new Person(mockCampaign);
        Person p1 = new Person(mockCampaign);
        Person p2 = new Person(mockCampaign);
        Person p3 = new Person(mockCampaign);
        for (Person p : List.of(p0, p1, p2, p3)) {
            p.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        }
        stubAllPersonnel(p0, p1, p2, p3);

        RandomEventResult result = new RandomEventResult(ESCAPE, List.of(PRISONERS), 1, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(1, manager.getPersonHashSet().size(),
              "Exactly 1 prisoner should be tracked as escaped");
    }

    /**
     * Requesting more escapes than there are prisoners caps the escape count at pool size.
     */
    @Test
    void testEffectEscape_magnitudeExceedsPool_clampedToPoolSize() {
        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(ESCAPE, List.of(PRISONERS), 10, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(1, manager.getPersonHashSet().size(),
              "Escape count should be capped to the number of available prisoners");
    }

    /**
     * When there are no prisoners, the ESCAPE effect is a no-op: empty report, empty set.
     */
    @Test
    void testEffectEscape_noTargets_emptyReportAndNoEscapees() {
        stubAllPersonnel();

        RandomEventResult result = new RandomEventResult(ESCAPE, List.of(PRISONERS), 1, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
        assertTrue(manager.getPersonHashSet().isEmpty());
    }

    // -----------------------------------------------------------------------
    // ESCAPE_PERCENT
    // -----------------------------------------------------------------------

    /**
     * With 50 % and 4 prisoners, 2 should escape.
     */
    @Test
    void testEffectEscapePercent_50Percent_twoEscapees() {
        Person p0 = new Person(mockCampaign);
        Person p1 = new Person(mockCampaign);
        Person p2 = new Person(mockCampaign);
        Person p3 = new Person(mockCampaign);
        for (Person p : List.of(p0, p1, p2, p3)) {
            p.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        }
        stubAllPersonnel(p0, p1, p2, p3);

        RandomEventResult result = new RandomEventResult(ESCAPE_PERCENT, List.of(PRISONERS), 50, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(2, manager.getPersonHashSet().size(),
              "50 %% of 4 prisoners = 2 escapees");
    }

    /**
     * A tiny percentage with 1 prisoner must still result in at least 1 escapee (ceil floor).
     */
    @Test
    void testEffectEscapePercent_smallPercentage_minimumOneEscapee() {
        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);

        // 1 % of 1 person would be 0, but ceiling ensures minimum of 1
        RandomEventResult result = new RandomEventResult(ESCAPE_PERCENT, List.of(PRISONERS), 1, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(1, manager.getPersonHashSet().size(),
              "Ceiling calculation should ensure at least 1 person escapes");
    }

    // -----------------------------------------------------------------------
    // FATIGUE_ONE
    // -----------------------------------------------------------------------

    /**
     * With fatigue enabled and fatigueRate=1, fatigue should increase by exactly the magnitude.
     */
    @Test
    void testEffectFatigueOne_fatigueEnabled_fatigueIncreased() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(true);
        when(mockCampaignOptions.getFatigueRate()).thenReturn(1);
        when(mockCampaign.getHangar()).thenReturn(mock(Hangar.class));
        when(mockCampaign.getWarehouse()).thenReturn(mock(Warehouse.class));

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);
        int before = prisoner.getFatigueDirect();

        RandomEventResult result = new RandomEventResult(FATIGUE_ONE, List.of(PRISONERS), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(before + 5, prisoner.getFatigueDirect());
    }

    /**
     * When fatigue is disabled, the effect should be a no-op with an empty report.
     */
    @Test
    void testEffectFatigueOne_fatigueDisabled_noChangeAndEmptyReport() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(false);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);
        int before = prisoner.getFatigueDirect();

        RandomEventResult result = new RandomEventResult(FATIGUE_ONE, List.of(PRISONERS), 5, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(before, prisoner.getFatigueDirect(), "Fatigue must not change when the option is disabled");
        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // FATIGUE_ALL
    // -----------------------------------------------------------------------

    /**
     * All prisoners should receive the fatigue change.
     */
    @Test
    void testEffectFatigueAll_fatigueEnabled_allPrisonersAffected() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(true);
        when(mockCampaignOptions.getFatigueRate()).thenReturn(1);
        when(mockCampaign.getHangar()).thenReturn(mock(Hangar.class));
        when(mockCampaign.getWarehouse()).thenReturn(mock(Warehouse.class));

        Person p0 = new Person(mockCampaign);
        Person p1 = new Person(mockCampaign);
        Person p2 = new Person(mockCampaign);
        Person p3 = new Person(mockCampaign);
        for (Person p : List.of(p0, p1, p2, p3)) {
            p.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        }
        List<Integer> before = List.of(
              p0.getFatigueDirect(), p1.getFatigueDirect(),
              p2.getFatigueDirect(), p3.getFatigueDirect());
        stubAllPersonnel(p0, p1, p2, p3);

        RandomEventResult result = new RandomEventResult(FATIGUE_ALL, List.of(PRISONERS), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        List<Person> persons = List.of(p0, p1, p2, p3);
        for (int i = 0; i < persons.size(); i++) {
            assertEquals(before.get(i) + 5, persons.get(i).getFatigueDirect(),
                  "Every prisoner should have their fatigue increased by 5");
        }
    }

    /**
     * When fatigue is disabled, FATIGUE_ALL must produce no changes and an empty report.
     */
    @Test
    void testEffectFatigueAll_fatigueDisabled_emptyReport() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(false);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);
        int before = prisoner.getFatigueDirect();

        RandomEventResult result = new RandomEventResult(FATIGUE_ALL, List.of(PRISONERS), 5, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(before, prisoner.getFatigueDirect());
        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // SUPPORT_POINT
    // -----------------------------------------------------------------------

    /**
     * Support points on the active contract should increase by the magnitude.
     */
    @Test
    void testEffectSupportPoint_stratConEnabled_pointsIncreased() {
        when(mockCampaignOptions.isUseStratCon()).thenReturn(true);

        AtBContract contract = new AtBContract("Test");
        StratConCampaignState state = new StratConCampaignState(contract);
        contract.setStratConCampaignState(state);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        RandomEventResult result = new RandomEventResult(SUPPORT_POINT, List.of(PRISONERS), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(5, state.getSupportPoints());
    }

    /**
     * When StratCon is disabled, the effect is a no-op and the report is empty.
     */
    @Test
    void testEffectSupportPoint_stratConDisabled_emptyReport() {
        when(mockCampaignOptions.isUseStratCon()).thenReturn(false);

        RandomEventResult result = new RandomEventResult(SUPPORT_POINT, List.of(PRISONERS), 5, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    /**
     * When StratCon is enabled but there are no active contracts, the report is empty.
     */
    @Test
    void testEffectSupportPoint_noActiveContracts_emptyReport() {
        when(mockCampaignOptions.isUseStratCon()).thenReturn(true);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of());

        RandomEventResult result = new RandomEventResult(SUPPORT_POINT, List.of(PRISONERS), 5, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // UNIQUE — BARTERING (morale bump)
    // -----------------------------------------------------------------------

    /**
     * A contract at STALEMATE morale should be bumped one level upward.
     */
    @Test
    void testEffectUniqueBartering_stalemateMorale_moraleBumped() {
        AtBContract contract = new AtBContract("Test");
        contract.setMoraleLevel(STALEMATE);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(PRISONERS), 1, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(BARTERING, result), 0, true);

        assertEquals(STALEMATE.ordinal() + 1, contract.getMoraleLevel().ordinal(),
              "Morale should advance by one level");
    }

    /**
     * A contract already at OVERWHELMING morale should not be eligible for a further bump, so the report should be
     * empty.
     */
    @Test
    void testEffectUniqueBartering_overwhelmingMorale_noChange() {
        AtBContract contract = new AtBContract("Test");
        contract.setMoraleLevel(OVERWHELMING);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(PRISONERS), 1, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(BARTERING, result), 0, true);

        assertEquals(OVERWHELMING, contract.getMoraleLevel(),
              "Morale must not exceed OVERWHELMING");
        assertTrue(manager.getMechanicalEffectsReport().isBlank(),
              "No report should be generated when no contract qualifies");
    }

    /**
     * When there are no active contracts, the bartering effect should be a no-op.
     */
    @Test
    void testEffectUniqueBartering_noContracts_emptyReport() {
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of());

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(PRISONERS), 1, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(BARTERING, result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // UNIQUE — MISTAKE (skill & role wipe)
    // -----------------------------------------------------------------------

    /**
     * The MISTAKE event should strip all skills and reset roles to DEPENDENT / NONE.
     */
    @Test
    void testEffectUniqueMistake_skillsAndRolesReset() {
        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        prisoner.addSkill(S_ADMIN, 1, 0);
        prisoner.addSkill(S_SMALL_ARMS, 1, 0);
        prisoner.addSkill(S_SURGERY, 1, 0);
        prisoner.setPrimaryRoleDirect(SOLDIER);
        prisoner.setSecondaryRole(ADMINISTRATOR_LOGISTICS);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(PRISONERS), 1, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(MISTAKE, result), 0, true);

        assertNull(prisoner.getSkill(S_ADMIN), "Admin skill should be removed");
        assertNull(prisoner.getSkill(S_SMALL_ARMS), "Small Arms skill should be removed");
        assertNull(prisoner.getSkill(S_SURGERY), "Surgery skill should be removed");
        assertSame(DEPENDENT, prisoner.getPrimaryRole(), "Primary role must be DEPENDENT");
        assertSame(NONE, prisoner.getSecondaryRole(), "Secondary role must be NONE");
    }

    /**
     * When there are no valid targets, the MISTAKE effect should produce an empty report.
     */
    @Test
    void testEffectUniqueMistake_noTargets_emptyReport() {
        SkillType.initializeTypes();
        stubAllPersonnel();

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(PRISONERS), 1, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(MISTAKE, result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // UNIQUE — UNDERCOVER (faction change)
    // -----------------------------------------------------------------------

    /**
     * The UNDERCOVER event should change the prisoner's origin faction to the employer faction.
     */
    @Test
    void testEffectUniqueUndercover_prisonerFactionChanged() {
        AtBContract contract = mock(AtBContract.class);
        Faction employerFaction = new Faction("employerFaction", "employerFaction");
        when(contract.getEmployerFaction()).thenReturn(employerFaction);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        Faction originalFaction = new Faction("prisonerFaction", "prisonerFaction");
        prisoner.setOriginFaction(originalFaction);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(PRISONERS), 1, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(UNDERCOVER, result), 0, true);

        assertNotEquals(originalFaction, prisoner.getOriginFaction(),
              "The prisoner's faction should have changed");
        assertEquals(employerFaction, prisoner.getOriginFaction(),
              "The prisoner's faction should now match the employer's");
    }

    /**
     * When there are no active contracts, the UNDERCOVER effect should be a no-op.
     */
    @Test
    void testEffectUniqueUndercover_noContracts_noChange() {
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of());

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        Faction originalFaction = new Faction();
        prisoner.setOriginFaction(originalFaction);
        stubAllPersonnel(prisoner);

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(PRISONERS), 1, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(UNDERCOVER, result), 0, true);

        assertEquals(originalFaction, prisoner.getOriginFaction(),
              "Faction should be unchanged when no contract is available");
    }

    // -----------------------------------------------------------------------
    // UNIQUE — POISON (fatigue with poison-resistance check)
    // -----------------------------------------------------------------------

    /**
     * Personnel without poison resistance should all receive fatigue increases.
     */
    @Test
    void testEffectUniquePoison_fatigueEnabled_allNonResistantPersonnelFatigued() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(true);
        when(mockCampaignOptions.getFatigueRate()).thenReturn(1);
        when(mockCampaign.getHangar()).thenReturn(mock(Hangar.class));
        when(mockCampaign.getWarehouse()).thenReturn(mock(Warehouse.class));

        // Use combat personnel (not prisoners) since that's what the POISON event targets
        Person soldier0 = new Person(mockCampaign);
        soldier0.setPrimaryRoleDirect(SOLDIER);

        Person soldier1 = new Person(mockCampaign);
        soldier1.setPrimaryRoleDirect(SOLDIER);

        Person soldier2 = new Person(mockCampaign);
        soldier2.setPrimaryRoleDirect(SOLDIER);

        stubAllPersonnel(soldier0, soldier1, soldier2);

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(COMBAT_PERSONNEL), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(POISON, result), 0, true);

        long fatigued = List.of(soldier0, soldier1, soldier2).stream()
                              .filter(p -> p.getFatigueDirect() > 0)
                              .count();
        assertEquals(3, fatigued,
              "All 3 personnel without poison resistance should be fatigued");
    }

    @Test
    void testEffectUniquePoison_resistantPerson_isSkipped() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(true);
        when(mockCampaignOptions.getFatigueRate()).thenReturn(1);
        when(mockCampaign.getHangar()).thenReturn(mock(Hangar.class));
        when(mockCampaign.getWarehouse()).thenReturn(mock(Warehouse.class));

        // soldier0 has poison resistance — must NOT gain fatigue
        Person soldier0 = new Person(mockCampaign);
        soldier0.setPrimaryRoleDirect(SOLDIER);
        soldier0.getOptions().getOption(ATOW_POISON_RESISTANCE).setValue(true);

        // soldier1 has no resistance — MUST gain fatigue
        Person soldier1 = new Person(mockCampaign);
        soldier1.setPrimaryRoleDirect(SOLDIER);

        stubAllPersonnel(soldier0, soldier1);

        int resistantFatigueBefore = soldier0.getFatigueDirect();

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(COMBAT_PERSONNEL), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(POISON, result), 0, true);

        assertEquals(resistantFatigueBefore, soldier0.getFatigueDirect(),
              "A person with ATOW_POISON_RESISTANCE must not receive any fatigue from the poison effect");
        assertTrue(soldier1.getFatigueDirect() > 0,
              "A person without poison resistance must still receive fatigue");
    }

    /**
     * When fatigue is disabled, the POISON effect should produce an empty report.
     */
    @Test
    void testEffectUniquePoison_fatigueDisabled_emptyReport() {
        when(mockCampaignOptions.isUseFatigue()).thenReturn(false);

        Person soldier = new Person(mockCampaign);
        stubAllPersonnel(soldier);

        RandomEventResult result = new RandomEventResult(UNIQUE, List.of(COMBAT_PERSONNEL), 5, "");
        RandomEventEffectsManager manager =
              new RandomEventEffectsManager(mockCampaign, buildEventData(POISON, result), 0, true);

        assertTrue(manager.getMechanicalEffectsReport().isBlank());
    }

    // -----------------------------------------------------------------------
    // Failure branch (wasSuccessful = false)
    // -----------------------------------------------------------------------

    /**
     * When {@code wasSuccessful} is {@code false}, the manager should apply the
     * <em>failure</em> effect list rather than the success list. Here we configure
     * different magnitudes on each branch and confirm the failure one fires.
     */
    @Test
    void testFailureBranch_usesFailureResultsList() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);
        stubAllPersonnel(prisoner);
        int before = prisoner.getBaseLoyalty();

        RandomEventResult successResult = new RandomEventResult(LOYALTY_ONE, List.of(PRISONERS), +10, "");
        RandomEventResult failureResult = new RandomEventResult(LOYALTY_ONE, List.of(PRISONERS), -7, "");

        RandomEventResponseEntry entry = new RandomEventResponseEntry(
              RESPONSE_NEUTRAL,
              "",
              NO_ATTRIBUTE,
              List.of(successResult),
              List.of(failureResult));
        RandomEventData eventData = new RandomEventData(BREAKOUT, List.of(entry));

        // wasSuccessful = false → failure list should be used
        new RandomEventEffectsManager(mockCampaign, eventData, 0, false);

        assertEquals(before - 7, prisoner.getBaseLoyalty(),
              "Failure branch should apply the failure effect (−7 loyalty), not the success one (+10)");
    }

    // -----------------------------------------------------------------------
    // Mixed personnel type filtering
    // -----------------------------------------------------------------------

    /**
     * When the effect targets PRISONERS only, non-prisoner active personnel in the same pool must not be selected.
     */
    @Test
    void testPersonnelFiltering_prisonerEffectDoesNotTargetActiveCombatPersonnel() {
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, false);

        Person combatPersonnel = new Person(mockCampaign);
        // combatPersonnel is NOT a prisoner (default state)

        stubAllPersonnel(prisoner, combatPersonnel);
        int combatLoyaltyBefore = combatPersonnel.getBaseLoyalty();

        // Target PRISONERS only
        RandomEventResult result = new RandomEventResult(LOYALTY_ALL, List.of(PRISONERS), 5, "");
        new RandomEventEffectsManager(mockCampaign, buildEventData(result), 0, true);

        assertEquals(combatLoyaltyBefore, combatPersonnel.getBaseLoyalty(),
              "Non-prisoner personnel must not be affected by a PRISONERS-only effect");
    }
}
