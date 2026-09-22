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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the point of interest model: {@link StratConPointOfInterest}, its definitions, and the definition and
 * behavior registries.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPointOfInterestTest {
    private static final String FIXTURE_DIRECTORY = "testresources/data/stratconpointsofinterest";
    private static final String TEST_TYPE_ID = "UnitTestPointOfInterest";
    private static final String TEST_BEHAVIOR_ID = "unitTestBehavior";
    private static final LocalDate TODAY = LocalDate.of(3025, 6, 1);

    @AfterEach
    void resetRegistries() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TEST_TYPE_ID);
        StratConPointOfInterestBehaviors.unregisterBehavior(TEST_BEHAVIOR_ID);
        // back to the default paths, which resolve to nothing under test
        StratConPointOfInterestDefinitions.reloadDefinitions();
    }

    private static StratConPointOfInterestDefinition registerTestDefinition(boolean occupiesHex) {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TEST_TYPE_ID);
        definition.setDisplayableName("Test Point");
        definition.setDescription("A point of interest for tests.");
        definition.setOccupiesHex(occupiesHex);
        StratConPointOfInterestDefinitions.registerDefinition(definition);
        return definition;
    }

    @Test
    void newPointsOfInterestGetDistinctIdsAndStartActive() {
        StratConPointOfInterest first = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(1, 1));
        StratConPointOfInterest second = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(1, 1));

        assertNotNull(first.getId());
        assertNotEquals(first.getId(), second.getId());
        assertEquals(PointOfInterestStatus.ACTIVE, first.getStatus());
        assertTrue(first.isActive());
        assertFalse(first.isRevealed());
        assertTrue(first.isNeutral());
    }

    @Test
    void fromDefinitionTakesOwnerAndLifespan() {
        StratConPointOfInterestDefinition definition = registerTestDefinition(false);
        definition.setDefaultOwner(ForceAlignment.Opposing);
        definition.setLifespanDays(10);

        StratConPointOfInterest pointOfInterest = StratConPointOfInterest.fromDefinition(definition,
              new StratConCoords(2, 3),
              TODAY);

        assertEquals(TEST_TYPE_ID, pointOfInterest.getTypeId());
        assertEquals(new StratConCoords(2, 3), pointOfInterest.getCoords());
        assertEquals(ForceAlignment.Opposing, pointOfInterest.getOwner());
        assertFalse(pointOfInterest.isNeutral());
        assertFalse(pointOfInterest.isOwnerAlliedToPlayer());
        assertEquals(TODAY.plusDays(10), pointOfInterest.getExpiryDate());
    }

    @Test
    void fromDefinitionWithoutLifespanNeverExpires() {
        StratConPointOfInterestDefinition definition = registerTestDefinition(false);

        StratConPointOfInterest pointOfInterest = StratConPointOfInterest.fromDefinition(definition,
              new StratConCoords(0, 0),
              TODAY);

        assertNull(pointOfInterest.getExpiryDate());
        assertTrue(pointOfInterest.isNeutral());
        assertFalse(pointOfInterest.hasReachedExpiryDate(TODAY.plusYears(100)));
    }

    @Test
    void expiryDateIsDueOnTheDayItself() {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(0, 0));
        pointOfInterest.setExpiryDate(TODAY);

        assertFalse(pointOfInterest.hasReachedExpiryDate(TODAY.minusDays(1)));
        assertTrue(pointOfInterest.hasReachedExpiryDate(TODAY));
        assertTrue(pointOfInterest.hasReachedExpiryDate(TODAY.plusDays(1)));
    }

    @Test
    void alliedAndPlayerOwnersAreAlliedToPlayer() {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(0, 0));

        pointOfInterest.setOwner(ForceAlignment.Allied);
        assertTrue(pointOfInterest.isOwnerAlliedToPlayer());

        pointOfInterest.setOwner(ForceAlignment.Player);
        assertTrue(pointOfInterest.isOwnerAlliedToPlayer());

        pointOfInterest.setOwner(null);
        assertFalse(pointOfInterest.isOwnerAlliedToPlayer());
    }

    @Test
    void namesAndDescriptionsFallBackFromOverrideToDefinitionToTypeId() {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(0, 0));

        // no definition registered: the type ID is all there is
        assertEquals(TEST_TYPE_ID, pointOfInterest.getDisplayableName());
        assertNull(pointOfInterest.getDescription());

        registerTestDefinition(false);
        assertEquals("Test Point", pointOfInterest.getDisplayableName());
        assertEquals("A point of interest for tests.", pointOfInterest.getDescription());

        pointOfInterest.setDisplayNameOverride("Renamed Point");
        pointOfInterest.setDescriptionOverride("Rewritten description.");
        assertEquals("Renamed Point", pointOfInterest.getDisplayableName());
        assertEquals("Rewritten description.", pointOfInterest.getDescription());
    }

    @Test
    void occupancyComesFromDefinitionAndIsFalseWhenUndefined() {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(0, 0));
        assertFalse(pointOfInterest.occupiesHex(), "an undefined type must not block its hex");

        registerTestDefinition(true);
        assertTrue(pointOfInterest.occupiesHex());
    }

    @Test
    void stateValuesAreStoredAndRemoved() {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(0, 0));

        pointOfInterest.setStateValue("claimedBy", "12");
        assertEquals("12", pointOfInterest.getStateValue("claimedBy"));

        pointOfInterest.setStateValue("claimedBy", null);
        assertNull(pointOfInterest.getStateValue("claimedBy"));
        assertTrue(pointOfInterest.getState().isEmpty());
    }

    @Test
    void behaviorFallsBackToDefaultWhenUndefinedOrUnregistered() {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(0, 0));
        IStratConPointOfInterestBehavior defaultBehavior = StratConPointOfInterestBehaviors.getBehavior(
              StratConPointOfInterestBehaviors.DEFAULT_BEHAVIOR_ID);

        assertSame(defaultBehavior, pointOfInterest.getBehavior(), "undefined type uses the default behavior");

        StratConPointOfInterestDefinition definition = registerTestDefinition(false);
        definition.setBehaviorId(TEST_BEHAVIOR_ID);
        assertSame(defaultBehavior, pointOfInterest.getBehavior(), "unregistered behavior uses the default");

        IStratConPointOfInterestBehavior testBehavior = new IStratConPointOfInterestBehavior() {
        };
        StratConPointOfInterestBehaviors.registerBehavior(TEST_BEHAVIOR_ID, testBehavior);
        assertSame(testBehavior, pointOfInterest.getBehavior());
    }

    @Test
    void defaultBehaviorCannotBeReplaced() {
        IStratConPointOfInterestBehavior defaultBehavior = StratConPointOfInterestBehaviors.getBehavior(
              StratConPointOfInterestBehaviors.DEFAULT_BEHAVIOR_ID);

        StratConPointOfInterestBehaviors.registerBehavior(StratConPointOfInterestBehaviors.DEFAULT_BEHAVIOR_ID,
              new IStratConPointOfInterestBehavior() {
              });

        assertSame(defaultBehavior,
              StratConPointOfInterestBehaviors.getBehavior(StratConPointOfInterestBehaviors.DEFAULT_BEHAVIOR_ID));
    }

    @Test
    void defaultObjectiveRulesFollowStatus() {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(0, 0));
        StratConTrackState track = new StratConTrackState();
        IStratConPointOfInterestBehavior behavior = pointOfInterest.getBehavior();

        assertFalse(behavior.isObjectiveCompleted(pointOfInterest, track));
        assertFalse(behavior.isObjectiveFailed(pointOfInterest, track));

        pointOfInterest.setStatus(PointOfInterestStatus.RESOLVED);
        assertTrue(behavior.isObjectiveCompleted(pointOfInterest, track));
        assertFalse(behavior.isObjectiveFailed(pointOfInterest, track));

        pointOfInterest.setStatus(PointOfInterestStatus.EXPIRED);
        assertFalse(behavior.isObjectiveCompleted(pointOfInterest, track));
        assertTrue(behavior.isObjectiveFailed(pointOfInterest, track));
    }

    @Test
    void hiddenPointsOfInterestAreInvisibleUntilSomethingRevealsThem() {
        StratConPointOfInterestDefinition definition = registerTestDefinition(false);
        definition.setHiddenUntilScouted(true);
        StratConTrackState track = new StratConTrackState();
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(1, 1));

        assertFalse(pointOfInterest.isVisibleToPlayer(track));

        pointOfInterest.setRevealed(true);
        assertTrue(pointOfInterest.isVisibleToPlayer(track), "revealed");
        pointOfInterest.setRevealed(false);

        track.getRevealedCoords().add(new StratConCoords(1, 1));
        assertTrue(pointOfInterest.isVisibleToPlayer(track), "its hex has been scouted");
        track.getRevealedCoords().clear();

        pointOfInterest.setOwner(ForceAlignment.Allied);
        assertTrue(pointOfInterest.isVisibleToPlayer(track), "allied points of interest are always known");
        pointOfInterest.setOwner(ForceAlignment.Opposing);
        assertFalse(pointOfInterest.isVisibleToPlayer(track));

        track.setGmRevealed(true);
        assertTrue(pointOfInterest.isVisibleToPlayer(track), "the GM has revealed the sector");
    }

    @Test
    void pointsOfInterestNotHiddenUntilScoutedAreAlwaysVisible() {
        registerTestDefinition(false);
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(1, 1));

        assertTrue(pointOfInterest.isVisibleToPlayer(new StratConTrackState()));
    }

    @Test
    void undefinedPointsOfInterestAreTreatedAsHidden() {
        StratConTrackState track = new StratConTrackState();
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(TEST_TYPE_ID, new StratConCoords(1, 1));

        assertFalse(pointOfInterest.isVisibleToPlayer(track));

        track.setGmRevealed(true);
        assertTrue(pointOfInterest.isVisibleToPlayer(track), "the GM can still see it to clean it up");
    }

    @Test
    void manifestLoadsDefinitionsAndSkipsMissingFiles() {
        StratConPointOfInterestDefinitions.loadForTest(
              new File(FIXTURE_DIRECTORY, "pointofinterestmanifest.json").getPath(),
              FIXTURE_DIRECTORY);

        List<StratConPointOfInterestDefinition> allDefinitions = StratConPointOfInterestDefinitions.getAllDefinitions();
        assertEquals(2, allDefinitions.size(), "the missing file in the manifest is skipped");
        assertEquals("TestSupplyCache", allDefinitions.get(0).getTypeId(), "manifest order is kept");

        StratConPointOfInterestDefinition supplyCache = StratConPointOfInterestDefinitions.getDefinition(
              "TestSupplyCache");
        assertNotNull(supplyCache);
        assertEquals("Supply Cache", supplyCache.getDisplayableName());
        assertEquals("An abandoned supply cache.", supplyCache.getDescription());
        assertEquals("data/images/stratcon/SupplyCache.png", supplyCache.getImagePath());
        assertFalse(supplyCache.isOccupiesHex());
        assertTrue(supplyCache.isHiddenUntilScouted());
        assertEquals(14, supplyCache.getLifespanDays());
        assertTrue(supplyCache.isRemoveOnExpiry());
        assertEquals(-5, supplyCache.getScenarioOddsModifier());
        assertEquals(List.of("Plains", "Forest"), supplyCache.getAllowedTerrainCategories());
        assertTrue(supplyCache.isAvoidCities());
        assertTrue(supplyCache.isLandOnly(), "land-only defaults to true when a file leaves it out");
        assertNull(supplyCache.getDefaultOwner(), "no default owner means neutral");
        assertEquals(StratConPointOfInterestBehaviors.DEFAULT_BEHAVIOR_ID, supplyCache.getBehaviorId());

        StratConPointOfInterestDefinition crashSite = StratConPointOfInterestDefinitions.getDefinition(
              "TestCrashSite");
        assertNotNull(crashSite);
        assertTrue(crashSite.isOccupiesHex());
        assertEquals(ForceAlignment.Opposing, crashSite.getDefaultOwner());
        assertEquals(1, crashSite.getScanRangeIncrease());
        assertFalse(crashSite.isLandOnly());
        assertEquals("testCrashSiteBehavior", crashSite.getBehaviorId());
    }

    @Test
    void registeredDefinitionsOverrideFileDefinitionsAndSurviveReload() {
        StratConPointOfInterestDefinitions.loadForTest(
              new File(FIXTURE_DIRECTORY, "pointofinterestmanifest.json").getPath(),
              FIXTURE_DIRECTORY);

        StratConPointOfInterestDefinition override = new StratConPointOfInterestDefinition();
        override.setTypeId("TestSupplyCache");
        override.setDisplayableName("Overridden Cache");
        StratConPointOfInterestDefinitions.registerDefinition(override);

        try {
            assertSame(override, StratConPointOfInterestDefinitions.getDefinition("TestSupplyCache"));
            assertEquals(2, StratConPointOfInterestDefinitions.getAllDefinitions().size(),
                  "an override replaces the file definition rather than adding a second one");

            StratConPointOfInterestDefinitions.reloadDefinitions();
            assertSame(override, StratConPointOfInterestDefinitions.getDefinition("TestSupplyCache"),
                  "a registered definition survives a reload");
        } finally {
            StratConPointOfInterestDefinitions.unregisterDefinition("TestSupplyCache");
        }
    }

    @Test
    void definitionsWithoutTypeIdAreNotRegistered() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        assertFalse(StratConPointOfInterestDefinitions.getAllDefinitions().contains(definition));
        assertNull(StratConPointOfInterestDefinitions.getDefinition(null));
    }
}
