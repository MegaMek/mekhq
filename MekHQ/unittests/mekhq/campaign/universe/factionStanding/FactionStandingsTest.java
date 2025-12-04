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

import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_3;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_4;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_5;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.calculateFactionStandingLevel;
import static mekhq.campaign.universe.factionStanding.FactionStandings.CLIMATE_REGARD_ALLIED_FACTION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.CLIMATE_REGARD_ENEMY_FACTION_AT_WAR;
import static mekhq.campaign.universe.factionStanding.FactionStandings.CLIMATE_REGARD_SAME_FACTION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.DEFAULT_REGARD;
import static mekhq.campaign.universe.factionStanding.FactionStandings.DEFAULT_REGARD_DEGRADATION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.REGARD_DELTA_EXECUTING_PRISONER;
import static mekhq.campaign.universe.factionStanding.FactionStandings.REGARD_DELTA_REFUSE_BATCHALL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FactionStandingsTest {
    private static Factions factions;

    @BeforeEach
    void setUp() {
        try {
            Factions.setInstance(Factions.loadDefault(true));
            factions = Factions.getInstance();
        } catch (Exception ignored) {
        }

        // Validate our Faction data, an error here will throw everything off
        assertFalse(factions.getFactions().isEmpty(), "Factions list is empty");
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
        Faction campaignFaction = factions.getFaction("FS"); // Federated Suns
        LocalDate today = LocalDate.of(3028, 8, 20); // Start of the 4th Succession War

        FactionStandings factionStandings = new FactionStandings();
        factionStandings.updateClimateRegard(campaignFaction, today, 1.0, true, true);

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
        factionStandings.setRegardForFaction(null, "FS", initialRegard, 3025, false);

        // Act
        factionStandings.processRegardDegradation("FS", 3025, 1.0);

        // Assert
        double actualRegard = factionStandings.getRegardForFaction("FS", false);
        assertEquals(expectedRegard, actualRegard, "Expected regard of " + expectedRegard + " but got " + actualRegard);
    }

    @Test
    void test_processRefusedBatchall_decreasesRegard() {
        // Setup
        int gameYear = 3050;

        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setRegardForFaction(null, "CW", 10.0, gameYear, false); // Clan Wolf with regard 10.0

        // Act
        List<String> reports = factionStandings.processRefusedBatchall("FS", "CW", gameYear, 1.0);

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
        factionStandings.setRegardForFaction(null, "FS", 10.0, 3025, false); // Initial regard for Federated Suns
        factionStandings.setRegardForFaction(null,
              "CC",
              20.0,
              3025,
              false); // Initial regard for Capellan Confederation
        factionStandings.setRegardForFaction(null, "CS", -5.0, 3025, false); // Initial regard for ComStar

        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getFaction()).thenReturn(factions.getDefaultFaction());

        Faction federatedSuns = factions.getFaction("FS");
        Faction capellanConfederation = factions.getFaction("CC");
        Faction comStar = factions.getFaction("CS");

        Person federatedSunsPrisoner = new Person(mockCampaign);
        federatedSunsPrisoner.setOriginFaction(federatedSuns);

        Person capellanPrisoner = new Person(mockCampaign);
        capellanPrisoner.setOriginFaction(capellanConfederation);

        Person comStarPrisoner = new Person(mockCampaign);
        comStarPrisoner.setOriginFaction(comStar);

        // List of prisoners
        List<Person> prisoners = List.of(federatedSunsPrisoner, capellanPrisoner, capellanPrisoner, comStarPrisoner);

        // Act
        List<String> reports = factionStandings.executePrisonersOfWar("FS", prisoners, 3025, 1.0);

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
