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
import static mekhq.campaign.universe.factionStanding.FactionStandings.DEFAULT_FAME;
import static mekhq.campaign.universe.factionStanding.FactionStandings.DEFAULT_FAME_DEGRADATION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_FAME_ALLIED_FACTION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_FAME_ENEMY_FACTION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_FAME_SAME_FACTION;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.STANDING_LEVEL_3;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.STANDING_LEVEL_4;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.STANDING_LEVEL_5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.stream.Stream;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FactionStandingsTest {
    static Stream<Arguments> initializeStartingFameValuesProvider() {
        return Stream.of( // targetFaction, expectedFame, expectedStanding
              // Federated Suns (same faction)
              Arguments.of("FS", STARTING_FAME_SAME_FACTION, STANDING_LEVEL_5),
              // Lyran Commonwealth (allied faction)
              Arguments.of("LA", STARTING_FAME_ALLIED_FACTION, STANDING_LEVEL_4),
              // Capellan Confederation (enemy faction)
              Arguments.of("CC", STARTING_FAME_ENEMY_FACTION, STANDING_LEVEL_3),
              // ComStar (neutral faction)
              Arguments.of("CS", DEFAULT_FAME, STANDING_LEVEL_4));
    }

    @ParameterizedTest
    @MethodSource(value = "initializeStartingFameValuesProvider")
    void test_initializeStartingFameValues(String targetFaction, double expectedFame,
          FactionStandingLevel expectedStanding) {
        // Setup
        FactionStandings factionStandings = getStartingFactionStandings();

        // Act
        double actualFame = factionStandings.getFameForFaction(targetFaction);
        FactionStandingLevel actualStanding = calculateFactionStandingLevel(actualFame);

        // Assert
        assertEquals(expectedFame, actualFame, "Expected fame of " + expectedFame + " but got " + actualFame);
        assertEquals(expectedStanding,
              actualStanding,
              "Expected fame level of " + expectedStanding.name() + " but got " + actualStanding.name());
    }

    private static FactionStandings getStartingFactionStandings() {
        try {
            Factions.setInstance(Factions.loadDefault());
        } catch (Exception ignored) {
        }

        // Validate our Faction data, an error here will throw everything off
        assertFalse(Factions.getInstance().getFactions().isEmpty(), "Factions list is empty");

        Faction campaignFaction = Factions.getInstance().getFaction("FS"); // Federated Suns
        LocalDate today = LocalDate.of(3028, 8, 20); // Start of the 4th Succession War

        FactionStandings factionStandings = new FactionStandings();
        factionStandings.initializeStartingFameValues(campaignFaction, today);

        return factionStandings;
    }

    static Stream<Arguments> processFameDegradationProvider() {
        return Stream.of( // initialFame, expectedFame, description
              Arguments.of(5.0, 5.0 - DEFAULT_FAME_DEGRADATION),
              Arguments.of(-5.0, -5.0 + DEFAULT_FAME_DEGRADATION),
              Arguments.of(0.1, DEFAULT_FAME),
              Arguments.of(-0.1, DEFAULT_FAME));
    }

    @ParameterizedTest()
    @MethodSource(value = "processFameDegradationProvider")
    void test_processFameDegradation(double initialFame, double expectedFame) {
        // Setup
        FactionStandings factionStandings = new FactionStandings();
        factionStandings.setFameForFaction("CS", initialFame);

        // Act
        factionStandings.processFameDegradation(3025);

        // Assert
        double actualFame = factionStandings.getFameForFaction("CS");
        assertEquals(expectedFame, actualFame, "Expected fame of " + expectedFame + " but got " + actualFame);
    }
}
