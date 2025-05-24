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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.calculateFactionStandingLevel;
import static mekhq.campaign.universe.factionStanding.FactionStandings.*;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.STANDING_LEVEL_3;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.STANDING_LEVEL_4;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.STANDING_LEVEL_5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FactionStandingsTest {
    @BeforeEach
    void setUp() {
        try {
            Factions.setInstance(Factions.loadDefault());
        } catch (Exception ignored) {
        }

        // Validate our Faction data, an error here will throw everything off
        assertFalse(Factions.getInstance().getFactions().isEmpty(), "Factions list is empty");
    }

    static Stream<Arguments> initializeStartingRegardValuesProvider() {
        return Stream.of( // targetFaction, expectedRegard, expectedStanding
              // Federated Suns (same faction)
              Arguments.of("FS", STARTING_REGARD_SAME_FACTION, STANDING_LEVEL_5),
              // Lyran Commonwealth (allied faction)
              Arguments.of("LA", STARTING_REGARD_ALLIED_FACTION, STANDING_LEVEL_4),
              // Capellan Confederation (enemy faction)
              Arguments.of("CC", STARTING_REGARD_ENEMY_FACTION_AT_WAR, STANDING_LEVEL_3),
              // ComStar (neutral faction)
              Arguments.of("CS", DEFAULT_REGARD, STANDING_LEVEL_4));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "initializeStartingRegardValuesProvider")
    void test_initializeStartingRegardValues(String targetFaction, double expectedRegard,
          FactionStandingLevel expectedStanding) {
        // Setup
        FactionStandings factionStandings = getStartingFactionStandings();

        // Act
        double actualRegard = factionStandings.getRegardForFaction(targetFaction, false);
        FactionStandingLevel actualStanding = calculateFactionStandingLevel(actualRegard);

        // Assert
        assertEquals(expectedRegard, actualRegard, "Expected regard of " + expectedRegard + " but got " + actualRegard);
        assertEquals(expectedStanding,
              actualStanding,
              "Expected regard level of " + expectedStanding.name() + " but got " + actualStanding.name());
    }

    private static FactionStandings getStartingFactionStandings() {
        Faction campaignFaction = Factions.getInstance().getFaction("FS"); // Federated Suns
        LocalDate today = LocalDate.of(3028, 8, 20); // Start of the 4th Succession War

        FactionStandings factionStandings = new FactionStandings();
        factionStandings.initializeStartingRegardValues(campaignFaction, today);

        return factionStandings;
    }

    static Stream<Arguments> initializeDynamicRegardValuesProvider() {
        return Stream.of( // targetFaction, expectedRegard, expectedStanding
              // Federated Suns (same faction)
              Arguments.of("FS", CLIMATE_REGARD_SAME_FACTION, STANDING_LEVEL_5),
              // Lyran Commonwealth (allied faction)
              Arguments.of("LA", CLIMATE_REGARD_ALLIED_FACTION, STANDING_LEVEL_4),
              // Capellan Confederation (enemy faction)
              Arguments.of("CC", CLIMATE_REGARD_ENEMY_FACTION_AT_WAR, STANDING_LEVEL_3),
              // ComStar (neutral faction)
              Arguments.of("CS", DEFAULT_REGARD, STANDING_LEVEL_4));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "initializeDynamicRegardValuesProvider")
    void test_initializeDynamicRegardValues(String targetFaction, double expectedRegard,
          FactionStandingLevel expectedStanding) {
        // Setup
        Faction campaignFaction = Factions.getInstance().getFaction("FS"); // Federated Suns
        LocalDate today = LocalDate.of(3028, 8, 20); // Start of the 4th Succession War

        FactionStandings factionStandings = new FactionStandings();
        factionStandings.updateClimateRegard(campaignFaction, today);

        // Act
        double actualRegard = factionStandings.getRegardForFaction(targetFaction, true);
        FactionStandingLevel actualStanding = calculateFactionStandingLevel(actualRegard);

        // Assert
        assertEquals(expectedRegard, actualRegard, "Expected regard of " + expectedRegard + " but got " + actualRegard);
        assertEquals(expectedStanding,
              actualStanding,
              "Expected regard level of " + expectedStanding.name() + " but got " + actualStanding.name());
    }

    static Stream<Arguments> processRegardDegradationProvider() {
        return Stream.of( // initialRegard, expectedRegard
              Arguments.of("Positive Degraded", 5.0, 5.0 - DEFAULT_REGARD_DEGRADATION),
              Arguments.of("Negative Degraded", -5.0, -5.0 + DEFAULT_REGARD_DEGRADATION),
              Arguments.of("Low Positive Regard Degraded", 0.1, DEFAULT_REGARD),
              Arguments.of("Low Negative Regard Degraded", -0.1, DEFAULT_REGARD));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "processRegardDegradationProvider")
    void test_processRegardDegradation(String testName, double initialRegard, double expectedRegard) {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setRegardForFaction("CS", initialRegard);

        // Act
        factionStandings.processRegardDegradation(3025);

        // Assert
        double actualRegard = factionStandings.getRegardForFaction("CS", false);
        assertEquals(expectedRegard, actualRegard, "Expected regard of " + expectedRegard + " but got " + actualRegard);
    }

    private static Stream<Arguments> provideContractAcceptCases() {
        return Stream.of(Arguments.of("FS Enemy, LA Enemy's Ally",
                    "FS",
                    10.0,
                    "LA",
                    5.0, REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL, REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL),
              Arguments.of("CCO Enemy, CGB Enemy's Ally",
                    "CCO",
                    10.0,
                    "CGB",
                    5.0, REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN, REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN),
              Arguments.of("CS Enemy, CSJ Enemy's Ally",
                    "CS",
                    10.0,
                    "CSJ",
                    5.0, REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL, REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN),
              Arguments.of("CSJ Enemy, CS Enemy's Ally",
                    "CSJ",
                    10.0,
                    "CS",
                    5.0, REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN, REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "provideContractAcceptCases")
    void test_processAcceptContract_various(String testName, String primaryFaction, double primaryStart,
          String secondaryFaction, double secondaryStart, double expectedPrimaryDelta, double expectedSecondaryDelta) {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setRegardForFaction(primaryFaction, primaryStart);
        factionStandings.setRegardForFaction(secondaryFaction, secondaryStart);

        Faction enemyFaction = Factions.getInstance().getFaction(primaryFaction);
        LocalDate today = LocalDate.of(3049, 11, 3);

        // Act
        factionStandings.processContractAccept(enemyFaction, today);

        // Assert
        assertEquals(primaryStart + expectedPrimaryDelta,
              factionStandings.getRegardForFaction(primaryFaction, false),
              "Incorrect regard for " + primaryFaction);
        assertEquals(secondaryStart + expectedSecondaryDelta,
              factionStandings.getRegardForFaction(secondaryFaction, false),
              "Incorrect regard for " + secondaryFaction + " (ally)");
    }

    private static Stream<Arguments> provideContractStatuses() {
        return Stream.of( // Mission status, startingFSRegard, startingLARegard, expectedFSRegard, expectedLARegard
              Arguments.of("Mission Success",
                    MissionStatus.SUCCESS,
                    10.0,
                    5.0, REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER, REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY),
              Arguments.of("Mission Partial Success",
                    MissionStatus.PARTIAL,
                    10.0,
                    5.0, REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER, REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER_ALLY),
              Arguments.of("Mission Failed",
                    MissionStatus.FAILED,
                    10.0,
                    5.0, REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER, REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER_ALLY),
              Arguments.of("Mission Contract Breached",
                    MissionStatus.BREACH,
                    10.0,
                    5.0, REGARD_DELTA_CONTRACT_BREACH_EMPLOYER, REGARD_DELTA_CONTRACT_BREACH_EMPLOYER_ALLY));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "provideContractStatuses")
    void test_processContractCompletion_variousOutcomes(String testName, MissionStatus status, double startingFsRegard,
          double startingLaRegard, double expectedFsDelta, double expectedLaDelta) {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setRegardForFaction("FS", startingFsRegard);
        factionStandings.setRegardForFaction("LA", startingLaRegard);

        Faction employerFaction = Factions.getInstance().getFaction("FS");
        LocalDate today = LocalDate.of(3028, 8, 20);

        // Act
        factionStandings.processContractCompletion(employerFaction, today, status);

        // Assert
        assertEquals(startingFsRegard + expectedFsDelta,
              factionStandings.getRegardForFaction("FS", false),
              "Incorrect regard for FS (" + status + ")");
        assertEquals(startingLaRegard + expectedLaDelta,
              factionStandings.getRegardForFaction("LA", false),
              "Incorrect regard for LA (ally) (" + status + ")");
    }

    @Test
    void test_processRefusedBatchall_decreasesRegard() {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setRegardForFaction("CW", 10.0); // Clan Wolf with regard 10.0

        int gameYear = 3050;

        // Act
        List<String> reports = factionStandings.processRefusedBatchall("CW", gameYear);

        // Assert
        assertEquals(1, reports.size(), "Reports size mismatch");

        double expectedRegard = 10.0 + REGARD_DELTA_REFUSE_BATCHALL;
        double actualRegard = factionStandings.getRegardForFaction("CW", false);
        assertEquals(expectedRegard, actualRegard, "Incorrect regard for clan faction after refused Batchall");
    }

    @Test
    void test_executePrisonersOfWar() {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setRegardForFaction("FS", 10.0); // Initial regard for Federated Suns
        factionStandings.setRegardForFaction("CC", 20.0); // Initial regard for Capellan Confederation
        factionStandings.setRegardForFaction("CS", -5.0); // Initial regard for ComStar

        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getFaction()).thenReturn(Factions.getInstance().getDefaultFaction());

        Faction federatedSuns = Factions.getInstance().getFaction("FS");
        Faction capellanConfederation = Factions.getInstance().getFaction("CC");
        Faction comStar = Factions.getInstance().getFaction("CS");

        Person federatedSunsPrisoner = new Person(mockCampaign);
        federatedSunsPrisoner.setOriginFaction(federatedSuns);

        Person capellanPrisoner = new Person(mockCampaign);
        capellanPrisoner.setOriginFaction(capellanConfederation);

        Person comStarPrisoner = new Person(mockCampaign);
        comStarPrisoner.setOriginFaction(comStar);

        // List of prisoners
        List<Person> prisoners = List.of(federatedSunsPrisoner, capellanPrisoner, capellanPrisoner, comStarPrisoner);

        // Act
        List<String> reports = factionStandings.executePrisonersOfWar(prisoners, 3025);

        // Assert
        assertEquals(3, reports.size(), "Reports size mismatch");

        double expected = 10.0 + REGARD_DELTA_EXECUTING_PRISONER;
        double actual = factionStandings.getRegardForFaction("FS", false);
        assertEquals(expected, actual, "Incorrect regard for FS");

        expected = 20.0 + (REGARD_DELTA_EXECUTING_PRISONER * 2);
        actual = factionStandings.getRegardForFaction("CC", false);
        assertEquals(expected, actual, "Incorrect regard for CC");

        expected = -5.0 + REGARD_DELTA_EXECUTING_PRISONER;
        actual = factionStandings.getRegardForFaction("CS", false);
        assertEquals(expected, actual, "Incorrect regard for CS");
    }
}
