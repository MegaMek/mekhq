package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.Faction.Tag.CLAN;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.getMaximumStandingLevel;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.getMinimumStandingLevel;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class FactionStandingLevelTest {
    private static Faction innerSphereFaction;
    private static Faction clanFaction;
    private static Faction comStarFaction;

    @BeforeAll
    static void setUp() {
        // Setup
        innerSphereFaction = new Faction("ROD", "Republic of Dave");
        // We validate the factions here to make sure the tests are valid
        assertFalse(innerSphereFaction.isClan());
        assertFalse(innerSphereFaction.isComStarOrWoB());

        clanFaction = new Faction("CSB", "Clan Stink Badger");
        clanFaction.setTags(Collections.singleton(CLAN));
        assertTrue(clanFaction.isClan());

        comStarFaction = new Faction("CS", "ComStar");
        assertTrue(comStarFaction.isComStarOrWoB());
    }

    @ParameterizedTest
    @MethodSource(value = "allStandings")
    void test_getLabel_innerSphere(FactionStandingLevel standing) {
        // Act
        String label = standing.getLabel(innerSphereFaction);

        // Assert
        assertTrue(isResourceKeyValid(label), "Missing Inner Sphere label for " + standing.name());
    }

    @ParameterizedTest
    @MethodSource(value = "allStandings")
    void test_getLabel_clan(FactionStandingLevel standing) {
        // Act
        String label = standing.getLabel(clanFaction);

        // Assert
        assertTrue(isResourceKeyValid(label), "Missing Clan label for " + standing.name());
    }

    @ParameterizedTest
    @MethodSource(value = "allStandings")
    void test_getLabel_comStar(FactionStandingLevel standing) {
        // Act
        String label = standing.getLabel(comStarFaction);

        // Assert
        assertTrue(isResourceKeyValid(label), "Missing ComStar label for " + standing.name());
    }

    @ParameterizedTest
    @MethodSource(value = "allStandings")
    void test_getDescription_innerSphere(FactionStandingLevel standing) {
        // Act
        String description = standing.getDescription(innerSphereFaction);

        // Assert
        assertTrue(isResourceKeyValid(description), "Missing Inner Sphere description for " + standing.name());
    }

    @ParameterizedTest
    @MethodSource(value = "allStandings")
    void test_getDescription_clan(FactionStandingLevel standing) {
        // Act
        String description = standing.getDescription(clanFaction);

        // Assert
        assertTrue(isResourceKeyValid(description), "Missing Clan description for " + standing.name());
    }

    @ParameterizedTest
    @MethodSource(value = "allStandings")
    void test_getDescription_comStar(FactionStandingLevel standing) {
        // Act
        String description = standing.getDescription(comStarFaction);

        // Assert
        assertTrue(isResourceKeyValid(description), "Missing ComStar description for " + standing.name());
    }

    static Stream<FactionStandingLevel> allStandings() {
        return Arrays.stream(FactionStandingLevel.values());
    }

    @Test
    void test_uniqueStandingLevels() {
        // Setup
        List<Integer> usedStandingLevels = new ArrayList<>();

        for (FactionStandingLevel standing : FactionStandingLevel.values()) {
            // Act
            int standingLevel = standing.getStandingLevel();

            // Assert
            assertFalse(usedStandingLevels.contains(standingLevel),
                  "Duplicate standing level: " + standingLevel + " for " + standing.name());
            usedStandingLevels.add(standingLevel);
        }
    }

    @Test
    void test_allPossibleStandingLevelsAccountedFor() {
        // Setup
        List<Integer> standingLevels = new ArrayList<>();
        for (int level = getMinimumStandingLevel(); level <= getMaximumStandingLevel(); level++) {
            standingLevels.add(level);
        }

        // Act
        int expected = getMaximumStandingLevel() + 1;
        int actual = standingLevels.size();

        // Assert
        assertEquals(expected, actual, "Some standing levels were not accounted for: " + standingLevels);
    }

    @Test
    void test_standingLevelMinimumAndMaximumAreValid() {
        // Act
        // Your IDE might tell you this test is always true, and if the class is set up correctly, it will be. This
        // test is to ensure it always stays that way.
        boolean areValid = getMinimumStandingLevel() < getMaximumStandingLevel();

        // Assert
        assertTrue(areValid,
              "Minimum standing level of " + getMinimumStandingLevel() +
                    " is greater than maximum standing level of " + getMaximumStandingLevel());
    }

    @ParameterizedTest
    @MethodSource(value = "allStandings")
    void test_allStandingsAreValid_minimum(FactionStandingLevel standing) {
        // Act
        int standingLevel = standing.getStandingLevel();

        // Assert
        assertTrue(standingLevel >= getMinimumStandingLevel(),
              "Invalid standing level for " +
                    standing.name() +
                    ": " +
                    standingLevel +
                    " < " +
                    getMinimumStandingLevel());
    }

    @ParameterizedTest
    @MethodSource(value = "allStandings")
    void test_allStandingsAreValid_maximum(FactionStandingLevel standing) {
        // Act
        int standingLevel = standing.getStandingLevel();

        // Assert
        assertTrue(standingLevel <= getMaximumStandingLevel(),
              "Invalid standing level for " +
                    standing.name() +
                    ": " +
                    standingLevel +
                    " > " +
                    getMaximumStandingLevel());
    }

    @ParameterizedTest
    @CsvSource(value = { "STANDING_LEVEL_0,STANDING_LEVEL_0", "'STANDING level_0',STANDING_LEVEL_0",
                         "adfqedfq,STANDING_LEVEL_4", "0,STANDING_LEVEL_0", "-6,STANDING_LEVEL_4" })
    void test_fromString_variousInputs(String input, FactionStandingLevel expected) {
        // Act
        FactionStandingLevel standing = FactionStandingLevel.fromString(input);

        // Assert
        assertEquals(expected, standing);
    }
}
