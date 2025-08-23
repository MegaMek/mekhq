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

import static java.lang.Math.round;
import static megamek.common.equipment.MiscType.createBeagleActiveProbe;
import static megamek.common.equipment.MiscType.createISImprovedSensors;
import static mekhq.campaign.randomEvents.prisoners.CapturePrisoners.*;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.BECOMING_BONDSMAN;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER;
import static mekhq.campaign.rating.IUnitRating.DRAGOON_C;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import megamek.common.enums.AvailabilityValue;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.MapSettings;
import megamek.common.universe.FactionTag;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The {@link CapturePrisonersTest} class is a test suite for validating the functionality of the prisoner capture and
 * processing mechanisms. This class contains a variety of unit test cases to ensure all intended scenarios for
 * capturing and handling prisoners are resolved correctly.
 *
 * <p>The test cases examine different settings such as ground and space environments, the use of
 * sensors and probes, the capture of NPCs, and the processing of prisoners under specific factions or capture methods
 * such as Campaign Operations and MekHQ. Additionally, scenarios regarding prisoner defection are also tested for
 * various factions and conditions.</p>
 */
class CapturePrisonersTest {
    private static Factions factions;

    @BeforeAll
    public static void setup() {
        Factions.setInstance(Factions.loadDefault());
        factions = Factions.getInstance();
    }

    @Test
    void testCapturePrisoners_Ground() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Scenario scenario = new Scenario();
        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        AvailabilityValue activeProbeAvailability = getPartAvailability(today, true);
        AvailabilityValue improvedSensorsAvailability = getPartAvailability(today, false);

        // Act
        int quality = -1;
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign, mockFaction, scenario, quality);

        // Assert
        int expectedTargetNumber = BASE_TARGET_NUMBER
                                         + HAS_BATTLEFIELD_CONTROL
                                         + GOING_TO_GROUND
                                         + SAR_CONTAINS_VTOL_OR_WIGE;

        int actualTargetNumber = capturePrisoners.getSarTargetNumber().getValue();
        assertEquals(expectedTargetNumber, actualTargetNumber);
        // TODO: sarQuality is evaluated against the index of a TechRating. doesn't seems very nice. See constructor of CapturePrisoners.
        assertTrue(quality < activeProbeAvailability.getIndex());
        assertTrue(quality < improvedSensorsAvailability.getIndex());
    }

    @Test
    void testCapturePrisoners_Ground_ActiveProbe() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Scenario scenario = new Scenario();
        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        AvailabilityValue activeProbeAvailability = getPartAvailability(today, true);
        AvailabilityValue improvedSensorsAvailability = getPartAvailability(today, false);

        // Act
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign,
              mockFaction,
              scenario,
              activeProbeAvailability.getIndex());

        // Assert
        int expectedTargetNumber = BASE_TARGET_NUMBER
                                         + HAS_BATTLEFIELD_CONTROL
                                         + GOING_TO_GROUND
                                         + SAR_CONTAINS_VTOL_OR_WIGE
                                         + SAR_HAS_ACTIVE_PROBE;

        int actualTargetNumber = capturePrisoners.getSarTargetNumber().getValue();
        assertEquals(expectedTargetNumber, actualTargetNumber);
        assertTrue(improvedSensorsAvailability.isBetterThan(activeProbeAvailability));
    }

    @Test
    void testCapturePrisoners_Ground_ImprovedSensors() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        Scenario scenario = new Scenario();
        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        AvailabilityValue activeProbeAvailability = getPartAvailability(today, true);
        AvailabilityValue improvedSensorsAvailability = getPartAvailability(today, false);

        // Act
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign,
              mockFaction,
              scenario,
              improvedSensorsAvailability.getIndex());

        // Assert
        int expectedTargetNumber = BASE_TARGET_NUMBER
                                         + HAS_BATTLEFIELD_CONTROL
                                         + GOING_TO_GROUND
                                         + SAR_CONTAINS_VTOL_OR_WIGE
                                         + SAR_HAS_IMPROVED_SENSORS;

        int actualTargetNumber = capturePrisoners.getSarTargetNumber().getValue();
        assertEquals(expectedTargetNumber, actualTargetNumber);
        assertTrue(improvedSensorsAvailability.isBetterThan(activeProbeAvailability));
    }

    @Test
    void testCapturePrisoners_Space() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);

        Scenario scenario = new Scenario();
        scenario.setBoardType(MapSettings.MEDIUM_SPACE);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        // Act
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign, mockFaction, scenario, DRAGOON_C);

        // Assert
        int expectedTargetNumber = BASE_TARGET_NUMBER
                                         + HAS_BATTLEFIELD_CONTROL
                                         + NOT_IN_PLANET_ORBIT
                                         + SAR_INCLUDES_DROPSHIP;

        int actualTargetNumber = capturePrisoners.getSarTargetNumber().getValue();
        assertEquals(expectedTargetNumber, actualTargetNumber);
    }

    @Test
    void testAttemptCaptureOfNPC_PickedUp() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);

        Scenario scenario = new Scenario();

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        // Act
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign, mockFaction, scenario, DRAGOON_C);

        // Assert
        assertTrue(capturePrisoners.attemptCaptureOfNPC(true));
    }

    @Test
    void testAttemptCaptureOfNPC_NotPickedUp_Captured() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);

        Scenario scenario = new Scenario();

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        CapturePrisoners realCapturePrisoners = new CapturePrisoners(mockCampaign, mockFaction, scenario, DRAGOON_C) {
            @Override
            protected int d6(int dice) {
                return this.getSarTargetNumber().getValue(); // Whatever value goes here will be the value rolled
            }
        };

        // Act
        CapturePrisoners capturePrisoners = spy(realCapturePrisoners);

        // Assert
        assertTrue(capturePrisoners.attemptCaptureOfNPC(false));
    }

    @Test
    void testAttemptCaptureOfNPC_NotPickedUp_Escaped() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);

        Scenario scenario = new Scenario();

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        CapturePrisoners realCapturePrisoners = new CapturePrisoners(mockCampaign, mockFaction, scenario, DRAGOON_C) {
            @Override
            protected int d6(int dice) {
                return this.getSarTargetNumber().getValue() - 1; // Whatever value goes here will be the value rolled
            }
        };

        // Act
        CapturePrisoners capturePrisoners = spy(realCapturePrisoners);

        // Assert
        assertFalse(capturePrisoners.attemptCaptureOfNPC(false));
    }

    @Test
    void testProcessPrisoner_CampaignOperations_InnerSphereFaction() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);

        CapturePrisoners realCapturePrisoners = new CapturePrisoners(mockCampaign, mockFaction, scenario, DRAGOON_C) {
            @Override
            protected int d6(int dice) {
                return 5; // Whatever value goes here will be the value rolled
            }
        };

        // Act
        CapturePrisoners capturePrisoners = spy(realCapturePrisoners);
        capturePrisoners.processPrisoner(prisoner, mockFaction, false, true);

        // Assert
        PrisonerStatus expectedStatus = PRISONER;
        PrisonerStatus actualStatus = prisoner.getPrisonerStatus();

        assertSame(expectedStatus, actualStatus);
    }

    @Test
    void testProcessPrisoner_CampaignOperations_ClanFaction_TakenAsPrisoner() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction campaignFaction = factions.getFaction("CJF");
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);

        CapturePrisoners realCapturePrisoners = new CapturePrisoners(mockCampaign,
              campaignFaction,
              scenario,
              DRAGOON_C) {
            @Override
            protected int d6(int dice) {
                return Integer.MIN_VALUE;
            }
        };

        // Act
        CapturePrisoners capturePrisoners = spy(realCapturePrisoners);
        capturePrisoners.processPrisoner(prisoner, campaignFaction, false, true);

        // Assert
        PrisonerStatus expectedStatus = PRISONER;
        PrisonerStatus actualStatus = prisoner.getPrisonerStatus();

        assertSame(expectedStatus, actualStatus);
    }

    @Test
    void testProcessPrisoner_CampaignOperations_ClanFaction_TakenAsBondsman() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction campaignFaction = factions.getFaction("CJF");
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);
        Faction prisonerFaction = factions.getFaction("CJF");
        prisoner.setOriginFaction(prisonerFaction);

        CapturePrisoners realCapturePrisoners = new CapturePrisoners(mockCampaign,
              campaignFaction,
              scenario,
              DRAGOON_C) {
            @Override
            protected int d6(int dice) {
                return Integer.MAX_VALUE;
            }
        };

        // Act
        CapturePrisoners capturePrisoners = spy(realCapturePrisoners);
        capturePrisoners.processPrisoner(prisoner, campaignFaction, false, true);

        // Assert
        PrisonerStatus expectedStatus = BECOMING_BONDSMAN;
        PrisonerStatus actualStatus = prisoner.getPrisonerStatus();

        assertSame(expectedStatus, actualStatus);
    }

    @Test
    void testProcessPrisoner_MekHQ_InnerSphereFaction() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);

        CapturePrisoners realCapturePrisoners = new CapturePrisoners(mockCampaign, mockFaction, scenario, DRAGOON_C) {
            @Override
            protected int d6(int dice) {
                return 5; // Whatever value goes here will be the value rolled
            }
        };

        // Act
        CapturePrisoners capturePrisoners = spy(realCapturePrisoners);
        capturePrisoners.processPrisoner(prisoner, mockFaction, true, true);

        // Assert
        PrisonerStatus expectedStatus = PRISONER;
        PrisonerStatus actualStatus = prisoner.getPrisonerStatus();

        assertSame(expectedStatus, actualStatus);
    }

    @Test
    void testProcessPrisoner_MekHQ_ClanFaction_TakenAsPrisoner() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction campaignFaction = factions.getFaction("CJF");
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);

        CapturePrisoners realCapturePrisoners = new CapturePrisoners(mockCampaign,
              campaignFaction,
              scenario,
              DRAGOON_C) {
            @Override
            protected int d6(int dice) {
                return Integer.MIN_VALUE;
            }
        };

        // Act
        CapturePrisoners capturePrisoners = spy(realCapturePrisoners);
        capturePrisoners.processPrisoner(prisoner, campaignFaction, true, true);

        // Assert
        PrisonerStatus expectedStatus = PRISONER;
        PrisonerStatus actualStatus = prisoner.getPrisonerStatus();

        assertSame(expectedStatus, actualStatus);
    }

    @Test
    void testProcessPrisoner_MekHQ_ClanFaction_TakenAsBondsman() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction campaignFaction = factions.getFaction("CJF");
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);
        Faction prisonerFaction = factions.getFaction("LA");
        prisoner.setOriginFaction(prisonerFaction);

        CapturePrisoners realCapturePrisoners = new CapturePrisoners(mockCampaign,
              campaignFaction,
              scenario,
              DRAGOON_C) {
            @Override
            protected int d6(int dice) {
                return Integer.MAX_VALUE;
            }
        };

        // Act
        CapturePrisoners capturePrisoners = spy(realCapturePrisoners);
        capturePrisoners.processPrisoner(prisoner, campaignFaction, true, true);

        // Assert
        PrisonerStatus expectedStatus = BECOMING_BONDSMAN;
        PrisonerStatus actualStatus = prisoner.getPrisonerStatus();

        assertSame(expectedStatus, actualStatus);
    }

    @Test
    void testDetermineDefectionChance() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction campaignFaction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);

        // Act
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign, campaignFaction, scenario, DRAGOON_C);
        capturePrisoners.determineDefectionChance(prisoner, true);
        int defectionChance = capturePrisoners.determineDefectionChance(prisoner, true);

        // Assert
        int expectedTargetNumber = DEFECTION_CHANCE;
        int actualTargetNumber = defectionChance;

        assertEquals(expectedTargetNumber, actualTargetNumber);
    }

    @Test
    void testDetermineDefection_Chance_MercenaryPrisoner() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction campaignFaction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);
        Faction prisonerFaction = new Faction();
        prisonerFaction.setTags(Set.of(FactionTag.MERC));
        prisoner.setOriginFaction(prisonerFaction);

        // Act
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign, campaignFaction, scenario, DRAGOON_C);
        capturePrisoners.determineDefectionChance(prisoner, true);
        int defectionChance = capturePrisoners.determineDefectionChance(prisoner, true);

        // Assert
        int expectedTargetNumber = (int) round(DEFECTION_CHANCE * MERCENARY_MULTIPLIER);
        int actualTargetNumber = defectionChance;

        assertEquals(expectedTargetNumber, actualTargetNumber);
    }

    @Test
    void testDetermineDefection_Chance_ClanPrisoner_NotDezgraFaction() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction campaignFaction = new Faction();
        campaignFaction.setTags(Set.of(FactionTag.CLAN));
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);
        prisoner.setClanPersonnel(true);

        // Act
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign, campaignFaction, scenario, DRAGOON_C);
        capturePrisoners.determineDefectionChance(prisoner, true);
        int defectionChance = capturePrisoners.determineDefectionChance(prisoner, true);

        // Assert
        int expectedTargetNumber = DEFECTION_CHANCE;
        int actualTargetNumber = defectionChance;

        assertEquals(expectedTargetNumber, actualTargetNumber);
    }

    @Test
    void testDetermineDefection_Chance_ClanPrisoner_DezgraFaction() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Faction campaignFaction = new Faction();
        campaignFaction.setTags(Set.of(FactionTag.MERC));
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);

        Scenario scenario = new Scenario();

        Person prisoner = new Person(mockCampaign);
        prisoner.setClanPersonnel(true);

        // Act
        CapturePrisoners capturePrisoners = new CapturePrisoners(mockCampaign, campaignFaction, scenario, DRAGOON_C);
        int defectionChance = capturePrisoners.determineDefectionChance(prisoner, true);

        // Assert
        int expectedTargetNumber = DEFECTION_CHANCE * CLAN_DEZGRA_MULTIPLIER;
        int actualTargetNumber = defectionChance;

        assertEquals(expectedTargetNumber, actualTargetNumber);
    }


    // Utility Methods

    /**
     * Determines the availability of a particular part based on the current date and whether an active probe is being
     * used.
     *
     * @param today         The current date represented as a LocalDate object.
     * @param isActiveProbe A boolean indicating if an active probe is being utilized.
     *
     * @return An integer representing the availability of the part for the given year and technology type.
     */
    private AvailabilityValue getPartAvailability(LocalDate today, boolean isActiveProbe) {
        int year = today.getYear();
        megamek.common.enums.Faction techFaction = ITechnology.getFactionFromMMAbbr("IS");

        if (isActiveProbe) {
            return createBeagleActiveProbe().calcYearAvailability(year, false, techFaction);
        } else {
            return createISImprovedSensors().calcYearAvailability(year, false, techFaction);
        }
    }
}
