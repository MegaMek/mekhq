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

    static Stream<Arguments> initializeStartingFameValuesProvider() {
        return Stream.of( // targetFaction, expectedFame, expectedStanding
              // Federated Suns (same faction)
              Arguments.of("FS", STARTING_FAME_SAME_FACTION, STANDING_LEVEL_5),
              // Lyran Commonwealth (allied faction)
              Arguments.of("LA", STARTING_FAME_ALLIED_FACTION, STANDING_LEVEL_4),
              // Capellan Confederation (enemy faction)
              Arguments.of("CC", STARTING_FAME_ENEMY_FACTION_AT_WAR, STANDING_LEVEL_3),
              // ComStar (neutral faction)
              Arguments.of("CS", DEFAULT_FAME, STANDING_LEVEL_4));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "initializeStartingFameValuesProvider")
    void test_initializeStartingFameValues(String targetFaction, double expectedFame,
          FactionStandingLevel expectedStanding) {
        // Setup
        FactionStandings factionStandings = getStartingFactionStandings();

        // Act
        double actualFame = factionStandings.getFameForFaction(targetFaction, false);
        FactionStandingLevel actualStanding = calculateFactionStandingLevel(actualFame);

        // Assert
        assertEquals(expectedFame, actualFame, "Expected fame of " + expectedFame + " but got " + actualFame);
        assertEquals(expectedStanding,
              actualStanding,
              "Expected fame level of " + expectedStanding.name() + " but got " + actualStanding.name());
    }

    private static FactionStandings getStartingFactionStandings() {
        Faction campaignFaction = Factions.getInstance().getFaction("FS"); // Federated Suns
        LocalDate today = LocalDate.of(3028, 8, 20); // Start of the 4th Succession War

        FactionStandings factionStandings = new FactionStandings();
        factionStandings.initializeStartingFameValues(campaignFaction, today);

        return factionStandings;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "initializeStartingFameValuesProvider")
    void test_initializeDynamicFameValues(String targetFaction, double expectedFame,
          FactionStandingLevel expectedStanding) {
        // Setup
        Faction campaignFaction = Factions.getInstance().getFaction("FS"); // Federated Suns
        LocalDate today = LocalDate.of(3028, 8, 20); // Start of the 4th Succession War

        FactionStandings factionStandings = new FactionStandings();
        factionStandings.updateDynamicTemporaryFame(campaignFaction, today);

        // Act
        double actualFame = factionStandings.getFameForFaction(targetFaction, true);
        FactionStandingLevel actualStanding = calculateFactionStandingLevel(actualFame);

        // Assert
        assertEquals(expectedFame, actualFame, "Expected fame of " + expectedFame + " but got " + actualFame);
        assertEquals(expectedStanding,
              actualStanding,
              "Expected fame level of " + expectedStanding.name() + " but got " + actualStanding.name());
    }

    static Stream<Arguments> processFameDegradationProvider() {
        return Stream.of( // initialFame, expectedFame
              Arguments.of("Positive Degraded", 5.0, 5.0 - DEFAULT_FAME_DEGRADATION),
              Arguments.of("Negative Degraded", -5.0, -5.0 + DEFAULT_FAME_DEGRADATION),
              Arguments.of("Low Positive Fame Degraded", 0.1, DEFAULT_FAME),
              Arguments.of("Low Negative Fame Degraded", -0.1, DEFAULT_FAME));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "processFameDegradationProvider")
    void test_processFameDegradation(String testName, double initialFame, double expectedFame) {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setFameForFaction("CS", initialFame);

        // Act
        factionStandings.processFameDegradation(3025);

        // Assert
        double actualFame = factionStandings.getFameForFaction("CS", false);
        assertEquals(expectedFame, actualFame, "Expected fame of " + expectedFame + " but got " + actualFame);
    }

    private static Stream<Arguments> provideContractAcceptCases() {
        return Stream.of(Arguments.of("FS Enemy, LA Enemy's Ally",
                    "FS",
                    10.0,
                    "LA",
                    5.0,
                    FAME_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL,
                    FAME_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL),
              Arguments.of("CCO Enemy, CGB Enemy's Ally",
                    "CCO",
                    10.0,
                    "CGB",
                    5.0,
                    FAME_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN,
                    FAME_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN),
              Arguments.of("CS Enemy, CSJ Enemy's Ally",
                    "CS",
                    10.0,
                    "CSJ",
                    5.0,
                    FAME_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL,
                    FAME_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN),
              Arguments.of("CSJ Enemy, CS Enemy's Ally",
                    "CSJ",
                    10.0,
                    "CS",
                    5.0,
                    FAME_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN,
                    FAME_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "provideContractAcceptCases")
    void test_processAcceptContract_various(String testName, String primaryFaction, double primaryStart,
          String secondaryFaction, double secondaryStart, double expectedPrimaryDelta, double expectedSecondaryDelta) {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setFameForFaction(primaryFaction, primaryStart);
        factionStandings.setFameForFaction(secondaryFaction, secondaryStart);

        Faction enemyFaction = Factions.getInstance().getFaction(primaryFaction);
        LocalDate today = LocalDate.of(3049, 11, 3);

        // Act
        factionStandings.processContractAccept(enemyFaction, today);

        // Assert
        assertEquals(primaryStart + expectedPrimaryDelta, factionStandings.getFameForFaction(primaryFaction, false),
              "Incorrect fame for " + primaryFaction);
        assertEquals(secondaryStart + expectedSecondaryDelta,
              factionStandings.getFameForFaction(secondaryFaction, false),
              "Incorrect fame for " + secondaryFaction + " (ally)");
    }

    private static Stream<Arguments> provideContractStatuses() {
        return Stream.of( // Mission status, startingFSFame, startingLAFame, expectedFSFame, expectedLAFame
              Arguments.of("Mission Success",
                    MissionStatus.SUCCESS,
                    10.0,
                    5.0,
                    FAME_DELTA_CONTRACT_SUCCESS_EMPLOYER,
                    FAME_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY),
              Arguments.of("Mission Partial Success",
                    MissionStatus.PARTIAL,
                    10.0,
                    5.0,
                    FAME_DELTA_CONTRACT_PARTIAL_EMPLOYER,
                    FAME_DELTA_CONTRACT_PARTIAL_EMPLOYER_ALLY),
              Arguments.of("Mission Failed",
                    MissionStatus.FAILED,
                    10.0,
                    5.0,
                    FAME_DELTA_CONTRACT_FAILURE_EMPLOYER,
                    FAME_DELTA_CONTRACT_FAILURE_EMPLOYER_ALLY),
              Arguments.of("Mission Contract Breached",
                    MissionStatus.BREACH,
                    10.0,
                    5.0,
                    FAME_DELTA_CONTRACT_BREACH_EMPLOYER,
                    FAME_DELTA_CONTRACT_BREACH_EMPLOYER_ALLY));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "provideContractStatuses")
    void test_processContractCompletion_variousOutcomes(String testName, MissionStatus status, double startingFsFame,
          double startingLaFame, double expectedFsDelta, double expectedLaDelta) {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setFameForFaction("FS", startingFsFame);
        factionStandings.setFameForFaction("LA", startingLaFame);

        Faction employerFaction = Factions.getInstance().getFaction("FS");
        LocalDate today = LocalDate.of(3028, 8, 20);

        // Act
        factionStandings.processContractCompletion(employerFaction, today, status);

        // Assert
        assertEquals(startingFsFame + expectedFsDelta, factionStandings.getFameForFaction("FS", false),
              "Incorrect fame for FS (" + status + ")");
        assertEquals(startingLaFame + expectedLaDelta, factionStandings.getFameForFaction("LA", false),
              "Incorrect fame for LA (ally) (" + status + ")");
    }

    @Test
    void test_processRefusedBatchall_decreasesFame() {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setFameForFaction("CW", 10.0); // Clan Wolf with fame 10.0

        int gameYear = 3050;

        // Act
        List<String> reports = factionStandings.processRefusedBatchall("CW", gameYear);

        // Assert
        assertEquals(1, reports.size(), "Reports size mismatch");

        double expectedFame = 10.0 + FAME_DELTA_REFUSE_BATCHALL;
        double actualFame = factionStandings.getFameForFaction("CW", false);
        assertEquals(expectedFame, actualFame, "Incorrect fame for clan faction after refused Batchall");
    }

    @Test
    void test_executePrisonersOfWar() {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setFameForFaction("FS", 10.0); // Initial fame for Federated Suns
        factionStandings.setFameForFaction("CC", 20.0); // Initial fame for Capellan Confederation
        factionStandings.setFameForFaction("CS", -5.0); // Initial fame for ComStar

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

        double expected = 10.0 + FAME_DELTA_EXECUTING_PRISONER;
        double actual = factionStandings.getFameForFaction("FS", false);
        assertEquals(expected, actual, "Incorrect fame for FS");

        expected = 20.0 + (FAME_DELTA_EXECUTING_PRISONER * 2);
        actual = factionStandings.getFameForFaction("CC", false);
        assertEquals(expected, actual, "Incorrect fame for CC");

        expected = -5.0 + FAME_DELTA_EXECUTING_PRISONER;
        actual = factionStandings.getFameForFaction("CS", false);
        assertEquals(expected, actual, "Incorrect fame for CS");
    }
}
