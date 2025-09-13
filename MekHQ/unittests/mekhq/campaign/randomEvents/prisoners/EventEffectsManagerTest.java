/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR_LOGISTICS;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_SMALL_ARMS;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.campaign.randomEvents.prisoners.enums.EventResultEffect.*;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent.BARTERING;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent.BREAKOUT;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent.MISTAKE;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent.POISON;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent.UNDERCOVER;
import static mekhq.campaign.randomEvents.prisoners.enums.ResponseQuality.RESPONSE_NEUTRAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.records.EventResult;
import mekhq.campaign.randomEvents.prisoners.records.PrisonerEventData;
import mekhq.campaign.randomEvents.prisoners.records.PrisonerResponseEntry;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

/**
 * Unit test class for the {@link EventEffectsManager}.
 *
 * <p>This class contains test cases to verify the behavior and effects of different events
 * handled by the {@link EventEffectsManager}. Each test method corresponds to a specific event effect and evaluates its
 * outcomes under various conditions.</p>
 */
class EventEffectsManagerTest {
    @Test
    void testEventEffectPrisonerCapacity() {
        final int MAGNITUDE = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        EventResult eventResult = new EventResult(PRISONER_CAPACITY, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        // Act
        EventEffectsManager effectsManager = new EventEffectsManager(mockCampaign, eventData, 0, true);
        String report = effectsManager.getEventReport();

        // Assert
        // Because we're mocking campaign we can't check whether Prisoner Capacity was actually
        // changed. So we check to see if the change is reflected in the report, instead.
        assertTrue(report.contains("5"));
    }

    @Test
    void testEventEffectInjury_NoAdvancedMedical() {
        final int MAGNITUDE = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        EventResult eventResult = new EventResult(INJURY, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        assertEquals(MAGNITUDE, prisoner.getHits());
    }

    @Test
    void testEventEffectInjury_WithAdvancedMedical() {
        final int MAGNITUDE = 10;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseAdvancedMedical()).thenReturn(true);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        EventResult eventResult = new EventResult(INJURY, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        // Advanced Medical applies a degree of randomization to the number of injuries caused by a
        // Hit. So we can't check for the exact number of injuries sustained, just that the number
        // of injuries is no longer 0.
        assertFalse(prisoner.getInjuries().isEmpty());
    }

    @Test
    void testEventEffectInjuryPercent_NoAdvancedMedical() {
        final int MAGNITUDE = 50;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        EventResult eventResult = new EventResult(INJURY_PERCENT, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner0 = new Person(mockCampaign);
        Person prisoner1 = new Person(mockCampaign);
        Person prisoner2 = new Person(mockCampaign);
        Person prisoner3 = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner0, prisoner1, prisoner2, prisoner3));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        int injuredCharacters = 0;
        for (Person person : mockCampaign.getCurrentPrisoners()) {
            if (person.needsFixing()) {
                injuredCharacters++;
            }
        }

        assertEquals(2, injuredCharacters);
    }

    @Test
    void testEventEffectInjuryPercent_WithAdvancedMedical() {
        final int MAGNITUDE = 50;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseAdvancedMedical()).thenReturn(true);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        EventResult eventResult = new EventResult(INJURY_PERCENT, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner0 = new Person(mockCampaign);
        Person prisoner1 = new Person(mockCampaign);
        Person prisoner2 = new Person(mockCampaign);
        Person prisoner3 = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner0, prisoner1, prisoner2, prisoner3));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        int injuredCharacters = 0;
        for (Person person : mockCampaign.getCurrentPrisoners()) {
            if (person.needsFixing()) {
                injuredCharacters++;
            }
        }

        assertEquals(2, injuredCharacters);
    }

    @Test
    void testEventEffectDeath() {
        final int MAGNITUDE = 1;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        EventResult eventResult = new EventResult(DEATH, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));

        // Act
        EventEffectsManager effectsManager = new EventEffectsManager(mockCampaign, eventData, 0, true);
        String report = effectsManager.getEventReport();

        // Assert
        // Because we remove NPC Prisoners, on death, we need to instead check whether the report
        // shows they have been killed. The whitespace is deliberate, as the death count appears at
        // the beginning of the string, and we don't want the test to get confused by other numbers
        // (such as those in the color hex).
        assertTrue(report.contains("1 "));
    }

    @Test
    void testEventEffectDeathPercent() {
        final int MAGNITUDE = 50;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseAdvancedMedical()).thenReturn(true);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        EventResult eventResult = new EventResult(DEATH_PERCENT, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner0 = new Person(mockCampaign);
        Person prisoner1 = new Person(mockCampaign);
        Person prisoner2 = new Person(mockCampaign);
        Person prisoner3 = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner0, prisoner1, prisoner2, prisoner3));

        // Act
        EventEffectsManager effectsManager = new EventEffectsManager(mockCampaign, eventData, 0, true);
        String report = effectsManager.getEventReport();

        // Assert
        // Because we remove NPC Prisoners, on death, we need to instead check whether the report
        // shows they have been killed. The whitespace is deliberate, as the death count appears at
        // the beginning of the string, and we don't want the test to get confused by other numbers
        // (such as those in the color hex).
        assertTrue(report.contains("2 "));
    }

    @Test
    void testEventEffectSkill() {
        final int MAGNITUDE = 5;
        final String skill = S_ADMIN;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        EventResult eventResult = new EventResult(SKILL, false, MAGNITUDE, S_ADMIN);
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));

        SkillType.initializeTypes();

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        assertTrue(prisoner.hasSkill(skill));
        assertEquals(MAGNITUDE, prisoner.getSkill(skill).getLevel());
    }

    @Test
    void testEventEffectLoyaltyOne() {
        final int MAGNITUDE = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        EventResult eventResult = new EventResult(LOYALTY_ONE, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));
        int oldLoyalty = prisoner.getBaseLoyalty();

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        int expectedLoyalty = oldLoyalty + MAGNITUDE;
        int actualLoyalty = prisoner.getBaseLoyalty();

        assertEquals(expectedLoyalty, actualLoyalty);
    }

    @Test
    void testEventEffectLoyaltyAll() {
        final int MAGNITUDE = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        EventResult eventResult = new EventResult(LOYALTY_ALL, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner0 = new Person(mockCampaign);
        Person prisoner1 = new Person(mockCampaign);
        Person prisoner2 = new Person(mockCampaign);
        Person prisoner3 = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner0, prisoner1, prisoner2, prisoner3));
        List<Integer> oldLoyalties = List.of(prisoner0.getBaseLoyalty(),
              prisoner1.getBaseLoyalty(),
              prisoner2.getBaseLoyalty(),
              prisoner3.getBaseLoyalty());

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        List<Person> currentPrisoners = mockCampaign.getCurrentPrisoners();
        for (int i = 0; i < currentPrisoners.size(); i++) {
            int expectedLoyalty = oldLoyalties.get(i) + MAGNITUDE;
            int actualLoyalty = currentPrisoners.get(i).getBaseLoyalty();

            assertEquals(expectedLoyalty, actualLoyalty);
        }
    }

    @Test
    void testEventEffectEscape() {
        final int MAGNITUDE = 1;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        EventResult eventResult = new EventResult(ESCAPE, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner0 = new Person(mockCampaign);
        Person prisoner1 = new Person(mockCampaign);
        Person prisoner2 = new Person(mockCampaign);
        Person prisoner3 = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner0, prisoner1, prisoner2, prisoner3));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        EventEffectsManager effectsManager = new EventEffectsManager(mockCampaign, eventData, 0, true);
        String report = effectsManager.getEventReport();

        // Assert
        // Because we remove NPC Prisoners, on escape, we need to instead check whether the report
        // shows they have escaped.
        assertTrue(report.contains("1"));
    }

    @Test
    void testEventEffectEscapePercent() {
        final int MAGNITUDE = 50;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        EventResult eventResult = new EventResult(ESCAPE_PERCENT, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner0 = new Person(mockCampaign);
        Person prisoner1 = new Person(mockCampaign);
        Person prisoner2 = new Person(mockCampaign);
        Person prisoner3 = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner0, prisoner1, prisoner2, prisoner3));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        EventEffectsManager effectsManager = new EventEffectsManager(mockCampaign, eventData, 0, true);
        String report = effectsManager.getEventReport();

        // Assert
        // Because we remove NPC Prisoners, on escape, we need to instead check whether the report
        // shows they have escaped.
        assertTrue(report.contains("2"));
    }

    @Test
    void testEventEffectFatigueOne() {
        final int MAGNITUDE = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseFatigue()).thenReturn(true);

        EventResult eventResult = new EventResult(FATIGUE_ONE, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));
        int oldFatigue = prisoner.getFatigue();

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        int expectedFatigue = oldFatigue + MAGNITUDE;
        int actualFatigue = prisoner.getFatigue();

        assertEquals(expectedFatigue, actualFatigue);
    }

    @Test
    void testEventEffectFatigueAll() {
        final int MAGNITUDE = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseFatigue()).thenReturn(true);

        EventResult eventResult = new EventResult(FATIGUE_ALL, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        Person prisoner0 = new Person(mockCampaign);
        Person prisoner1 = new Person(mockCampaign);
        Person prisoner2 = new Person(mockCampaign);
        Person prisoner3 = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner0, prisoner1, prisoner2, prisoner3));
        List<Integer> oldFatigues = List.of(prisoner0.getFatigue(),
              prisoner1.getFatigue(),
              prisoner2.getFatigue(),
              prisoner3.getFatigue());

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        List<Person> currentPrisoners = mockCampaign.getCurrentPrisoners();
        for (int i = 0; i < currentPrisoners.size(); i++) {
            int expectedFatigue = oldFatigues.get(i) + MAGNITUDE;
            int actualFatigue = currentPrisoners.get(i).getFatigue();

            assertEquals(expectedFatigue, actualFatigue);
        }
    }

    @Test
    void testEventEffectSupportPoint() {
        final int MAGNITUDE = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseStratCon()).thenReturn(true);

        AtBContract contract = new AtBContract("Test");
        StratConCampaignState campaignState = new StratConCampaignState(contract);
        contract.setStratConCampaignState(campaignState);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        EventResult eventResult = new EventResult(SUPPORT_POINT, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BREAKOUT, List.of(responseEntry));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        int expectedSupportPoints = MAGNITUDE;
        int actualSupportPoints = contract.getStratconCampaignState().getSupportPoints();

        assertEquals(expectedSupportPoints, actualSupportPoints);
    }

    @Test
    void testEventEffectUniqueBartering() {
        final int MAGNITUDE = 1;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseStratCon()).thenReturn(true);

        AtBContract contract = new AtBContract("Test");
        contract.setMoraleLevel(STALEMATE);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        EventResult eventResult = new EventResult(UNIQUE, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(BARTERING, List.of(responseEntry));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        int expectedMorale = STALEMATE.ordinal() + MAGNITUDE;
        int actualMorale = contract.getMoraleLevel().ordinal();

        assertEquals(expectedMorale, actualMorale);
    }

    @Test
    void testEventEffectUniqueMistake() {
        final int MAGNITUDE = 1;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseLoyaltyModifiers()).thenReturn(true);

        EventResult eventResult = new EventResult(UNIQUE, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(MISTAKE, List.of(responseEntry));

        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));

        // Just some random skills, so we can get whether they were removed
        prisoner.addSkill(S_ADMIN, 1, 0);
        prisoner.addSkill(S_SMALL_ARMS, 1, 0);
        prisoner.addSkill(S_SURGERY, 1, 0);

        prisoner.setPrimaryRole(mockCampaign, SOLDIER);
        prisoner.setSecondaryRole(ADMINISTRATOR_LOGISTICS);

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        assertNull(prisoner.getSkill(S_ADMIN));
        assertNull(prisoner.getSkill(S_SMALL_ARMS));
        assertNull(prisoner.getSkill(S_SURGERY));

        assertSame(DEPENDENT, prisoner.getPrimaryRole());
        assertSame(NONE, prisoner.getSecondaryRole());
    }

    @Test
    void testEventEffectUniqueUndercover() {
        final int MAGNITUDE = 1;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseStratCon()).thenReturn(true);

        AtBContract contract = mock(AtBContract.class);
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        Faction employerFaction = new Faction();
        when(contract.getEmployerFaction()).thenReturn(employerFaction);

        EventResult eventResult = new EventResult(UNIQUE, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(UNDERCOVER, List.of(responseEntry));

        Person prisoner = new Person(mockCampaign);
        Faction prisonerFaction = new Faction();
        prisoner.setOriginFaction(prisonerFaction);
        when(mockCampaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        Faction expectedFaction = employerFaction;
        Faction actualFaction = prisoner.getOriginFaction();

        assertEquals(expectedFaction, actualFaction);
    }

    @Test
    void testEventEffectUniquePoison() {
        final int MAGNITUDE = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isUseFatigue()).thenReturn(true);

        EventResult eventResult = new EventResult(UNIQUE, false, MAGNITUDE, "");
        PrisonerResponseEntry responseEntry = new PrisonerResponseEntry(RESPONSE_NEUTRAL,
              List.of(eventResult),
              List.of(eventResult));
        PrisonerEventData eventData = new PrisonerEventData(POISON, List.of(responseEntry));

        Person soldier0 = new Person(mockCampaign);
        Person soldier1 = new Person(mockCampaign);
        Person soldier2 = new Person(mockCampaign);
        List<Person> potentialTargets = List.of(soldier0, soldier1, soldier2);
        when(mockCampaign.getActivePersonnel(false)).thenReturn(new ArrayList<>(potentialTargets));

        // Act
        new EventEffectsManager(mockCampaign, eventData, 0, true);

        // Assert
        int fatiguedCharacters = 0;
        for (Person character : potentialTargets) {
            if (character.getFatigue() > 0) {
                fatiguedCharacters++;
            }
        }

        assertEquals(3, fatiguedCharacters);
    }
}
