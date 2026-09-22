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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests for how a {@link StratConTrackState} holds points of interest: adding, moving and removing them, hex
 * occupancy, trimming to a smaller sector, and surviving a save and load.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConTrackStatePointOfInterestTest {
    private static final String OCCUPYING_TYPE_ID = "UnitTestOccupyingPointOfInterest";
    private static final String NON_OCCUPYING_TYPE_ID = "UnitTestNonOccupyingPointOfInterest";
    private static final String UNDEFINED_TYPE_ID = "UnitTestUndefinedPointOfInterest";

    private StratConTrackState track;

    @BeforeEach
    void setUp() {
        registerDefinition(OCCUPYING_TYPE_ID, true);
        registerDefinition(NON_OCCUPYING_TYPE_ID, false);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(OCCUPYING_TYPE_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(NON_OCCUPYING_TYPE_ID);
    }

    private static void registerDefinition(String typeId, boolean occupiesHex) {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(typeId);
        definition.setOccupiesHex(occupiesHex);
        StratConPointOfInterestDefinitions.registerDefinition(definition);
    }

    private static StratConPointOfInterest pointOfInterest(String typeId, int x, int y) {
        return new StratConPointOfInterest(typeId, new StratConCoords(x, y));
    }

    @Test
    void addedPointsOfInterestAreFoundByHexAndId() {
        StratConPointOfInterest occupying = pointOfInterest(OCCUPYING_TYPE_ID, 1, 1);
        StratConPointOfInterest nonOccupying = pointOfInterest(NON_OCCUPYING_TYPE_ID, 1, 1);

        assertTrue(track.addPointOfInterest(occupying));
        assertTrue(track.addPointOfInterest(nonOccupying));

        assertEquals(List.of(occupying, nonOccupying), track.getPointsOfInterest(new StratConCoords(1, 1)));
        assertTrue(track.getPointsOfInterest(new StratConCoords(2, 2)).isEmpty());
        assertSame(occupying, track.getPointOfInterest(occupying.getId()));
        assertSame(occupying, track.getOccupyingPointOfInterest(new StratConCoords(1, 1)));
        assertNull(track.getPointOfInterest("no such id"));
    }

    @Test
    void addRefusesMissingOrOutOfBoundsCoordsAndDuplicateIds() {
        StratConPointOfInterest withoutCoords = new StratConPointOfInterest();
        withoutCoords.setTypeId(NON_OCCUPYING_TYPE_ID);
        assertFalse(track.addPointOfInterest(withoutCoords));

        assertFalse(track.addPointOfInterest(pointOfInterest(NON_OCCUPYING_TYPE_ID, 5, 0)));
        assertFalse(track.addPointOfInterest(pointOfInterest(NON_OCCUPYING_TYPE_ID, -1, 0)));

        StratConPointOfInterest original = pointOfInterest(NON_OCCUPYING_TYPE_ID, 0, 0);
        assertTrue(track.addPointOfInterest(original));

        StratConPointOfInterest duplicate = pointOfInterest(NON_OCCUPYING_TYPE_ID, 1, 0);
        duplicate.setId(original.getId());
        assertFalse(track.addPointOfInterest(duplicate));

        assertEquals(1, track.getPointsOfInterest().size());
    }

    @Test
    void occupyingPointsOfInterestOccupyTheirHex() {
        StratConCoords coords = new StratConCoords(2, 2);
        assertFalse(track.isHexOccupied(coords));

        track.addPointOfInterest(pointOfInterest(NON_OCCUPYING_TYPE_ID, 2, 2));
        assertFalse(track.isHexOccupied(coords), "a non-occupying point of interest leaves the hex free");

        track.addPointOfInterest(pointOfInterest(OCCUPYING_TYPE_ID, 2, 2));
        assertTrue(track.isHexOccupied(coords));
    }

    @Test
    void undefinedPointsOfInterestDoNotOccupyTheirHex() {
        track.addPointOfInterest(pointOfInterest(UNDEFINED_TYPE_ID, 2, 2));

        assertFalse(track.isHexOccupied(new StratConCoords(2, 2)));
    }

    @Test
    void facilitiesAndScenariosOccupyTheirHex() {
        StratConCoords facilityCoords = new StratConCoords(0, 0);
        track.addFacility(facilityCoords, new StratConFacility());
        assertTrue(track.isHexOccupied(facilityCoords));

        StratConCoords scenarioCoords = new StratConCoords(1, 0);
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(scenarioCoords);
        track.getScenarios().put(scenarioCoords, scenario);
        assertTrue(track.isHexOccupied(scenarioCoords));
    }

    @Test
    void occupyingPointOfInterestCannotJoinAnOccupiedHex() {
        StratConCoords facilityCoords = new StratConCoords(0, 0);
        track.addFacility(facilityCoords, new StratConFacility());

        assertFalse(track.addPointOfInterest(pointOfInterest(OCCUPYING_TYPE_ID, 0, 0)),
              "an occupying point of interest cannot share a facility's hex");
        assertTrue(track.addPointOfInterest(pointOfInterest(NON_OCCUPYING_TYPE_ID, 0, 0)),
              "a non-occupying point of interest can share a facility's hex");

        assertTrue(track.addPointOfInterest(pointOfInterest(OCCUPYING_TYPE_ID, 1, 1)));
        assertFalse(track.addPointOfInterest(pointOfInterest(OCCUPYING_TYPE_ID, 1, 1)),
              "two occupying points of interest cannot share a hex");
    }

    @Test
    void moveUpdatesHexLookupAndRespectsOccupancy() {
        StratConPointOfInterest occupying = pointOfInterest(OCCUPYING_TYPE_ID, 0, 0);
        track.addPointOfInterest(occupying);
        track.addFacility(new StratConCoords(3, 3), new StratConFacility());

        assertFalse(track.movePointOfInterest(occupying.getId(), new StratConCoords(3, 3)),
              "cannot move onto an occupied hex");
        assertFalse(track.movePointOfInterest(occupying.getId(), new StratConCoords(9, 9)),
              "cannot move off the sector");
        assertFalse(track.movePointOfInterest("no such id", new StratConCoords(1, 1)));
        assertTrue(track.movePointOfInterest(occupying.getId(), new StratConCoords(0, 0)),
              "moving to where it already is succeeds");

        assertTrue(track.movePointOfInterest(occupying.getId(), new StratConCoords(2, 1)));
        assertEquals(new StratConCoords(2, 1), occupying.getCoords());
        assertTrue(track.getPointsOfInterest(new StratConCoords(0, 0)).isEmpty());
        assertEquals(List.of(occupying), track.getPointsOfInterest(new StratConCoords(2, 1)));
        assertFalse(track.isHexOccupied(new StratConCoords(0, 0)));
        assertTrue(track.isHexOccupied(new StratConCoords(2, 1)));
    }

    @Test
    void nonOccupyingPointOfInterestCanMoveOntoAnOccupiedHex() {
        StratConPointOfInterest nonOccupying = pointOfInterest(NON_OCCUPYING_TYPE_ID, 0, 0);
        track.addPointOfInterest(nonOccupying);
        track.addFacility(new StratConCoords(3, 3), new StratConFacility());

        assertTrue(track.movePointOfInterest(nonOccupying.getId(), new StratConCoords(3, 3)));
    }

    @Test
    void removeReturnsThePointOfInterestAndClearsItsHex() {
        StratConPointOfInterest occupying = pointOfInterest(OCCUPYING_TYPE_ID, 4, 4);
        track.addPointOfInterest(occupying);

        assertSame(occupying, track.removePointOfInterest(occupying.getId()));
        assertNull(track.removePointOfInterest(occupying.getId()));
        assertTrue(track.getPointsOfInterest().isEmpty());
        assertTrue(track.getPointsOfInterest(new StratConCoords(4, 4)).isEmpty());
        assertFalse(track.isHexOccupied(new StratConCoords(4, 4)));
    }

    @Test
    void trimToBoundsLeavesEveryPointOfInterestForTheCallerToRelocate() {
        StratConPointOfInterest outsideNonOccupying = pointOfInterest(NON_OCCUPYING_TYPE_ID, 4, 4);
        StratConPointOfInterest outsideOccupying = pointOfInterest(OCCUPYING_TYPE_ID, 4, 3);
        track.addPointOfInterest(outsideNonOccupying);
        track.addPointOfInterest(outsideOccupying);

        track.setWidth(3);
        track.setHeight(3);
        track.trimToBounds();

        assertEquals(List.of(outsideNonOccupying, outsideOccupying), track.getPointsOfInterest(),
              "points of interest may carry objectives, so trimming the ground never destroys them");
    }

    @Test
    void occupiedHexCountCountsSharedHexesOnce() {
        StratConCoords facilityCoords = new StratConCoords(0, 0);
        track.addFacility(facilityCoords, new StratConFacility());
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(facilityCoords);
        track.getScenarios().put(facilityCoords, scenario);

        track.addPointOfInterest(pointOfInterest(OCCUPYING_TYPE_ID, 1, 1));
        track.addPointOfInterest(pointOfInterest(NON_OCCUPYING_TYPE_ID, 2, 2));
        track.addPointOfInterest(pointOfInterest(UNDEFINED_TYPE_ID, 3, 3));

        assertEquals(2, track.getOccupiedHexCount(),
              "the facility scenario's hex and the occupying point of interest's hex; the others leave theirs free");
    }

    @Test
    void clearForRegenerationKeepsPointsOfInterest() {
        track.addPointOfInterest(pointOfInterest(NON_OCCUPYING_TYPE_ID, 1, 1));

        track.clearForRegeneration();

        assertEquals(1, track.getPointsOfInterest().size());
    }

    @Test
    void setPointsOfInterestRebuildsHexLookup() {
        StratConPointOfInterest first = pointOfInterest(NON_OCCUPYING_TYPE_ID, 1, 1);
        track.addPointOfInterest(first);
        assertEquals(List.of(first), track.getPointsOfInterest(new StratConCoords(1, 1)));

        StratConPointOfInterest replacement = pointOfInterest(NON_OCCUPYING_TYPE_ID, 2, 2);
        track.setPointsOfInterest(new ArrayList<>(List.of(replacement)));

        assertTrue(track.getPointsOfInterest(new StratConCoords(1, 1)).isEmpty());
        assertEquals(List.of(replacement), track.getPointsOfInterest(new StratConCoords(2, 2)));

        track.setPointsOfInterest(null);
        assertNotNull(track.getPointsOfInterest());
        assertTrue(track.getPointsOfInterest().isEmpty());
    }

    @Test
    void pointsOfInterestSurviveSaveAndLoad() throws Exception {
        StratConPointOfInterest full = pointOfInterest(OCCUPYING_TYPE_ID, 2, 3);
        full.setOwner(ForceAlignment.Opposing);
        full.setStatus(PointOfInterestStatus.RESOLVED);
        full.setRevealed(true);
        full.setExpiryDate(LocalDate.of(3025, 7, 4));
        full.setDisplayNameOverride("Named Point");
        full.setDescriptionOverride("Described point.");
        full.setStateValue("claimedBy", "7");
        track.addPointOfInterest(full);

        // neutral, never expiring, no overrides or state
        StratConPointOfInterest bare = pointOfInterest(NON_OCCUPYING_TYPE_ID, 2, 3);
        track.addPointOfInterest(bare);

        StratConTrackState reloadedTrack = saveAndLoad(track);

        assertEquals(2, reloadedTrack.getPointsOfInterest().size());

        StratConPointOfInterest reloadedFull = reloadedTrack.getPointOfInterest(full.getId());
        assertNotNull(reloadedFull);
        assertEquals(OCCUPYING_TYPE_ID, reloadedFull.getTypeId());
        assertEquals(new StratConCoords(2, 3), reloadedFull.getCoords());
        assertEquals(ForceAlignment.Opposing, reloadedFull.getOwner());
        assertEquals(PointOfInterestStatus.RESOLVED, reloadedFull.getStatus());
        assertTrue(reloadedFull.isRevealed());
        assertEquals(LocalDate.of(3025, 7, 4), reloadedFull.getExpiryDate());
        assertEquals("Named Point", reloadedFull.getDisplayNameOverride());
        assertEquals("Described point.", reloadedFull.getDescriptionOverride());
        assertEquals("7", reloadedFull.getStateValue("claimedBy"));

        StratConPointOfInterest reloadedBare = reloadedTrack.getPointOfInterest(bare.getId());
        assertNotNull(reloadedBare);
        assertNull(reloadedBare.getOwner());
        assertEquals(PointOfInterestStatus.ACTIVE, reloadedBare.getStatus());
        assertFalse(reloadedBare.isRevealed());
        assertNull(reloadedBare.getExpiryDate());
        assertNull(reloadedBare.getDisplayNameOverride());
        assertTrue(reloadedBare.getState().isEmpty());

        assertEquals(2, reloadedTrack.getPointsOfInterest(new StratConCoords(2, 3)).size(),
              "the hex lookup is rebuilt after loading");
        assertTrue(reloadedTrack.isHexOccupied(new StratConCoords(2, 3)));
    }

    @Test
    void sectorsSavedWithoutPointsOfInterestLoadWithAnEmptyList() throws Exception {
        StratConTrackState reloadedTrack = saveAndLoad(track);

        assertNotNull(reloadedTrack.getPointsOfInterest());
        assertTrue(reloadedTrack.getPointsOfInterest().isEmpty());
    }

    private static StratConTrackState saveAndLoad(StratConTrackState track) throws Exception {
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addTrack(track);

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            campaignState.Serialize(printWriter);
        }

        Document document = DocumentBuilderFactory.newInstance()
                                  .newDocumentBuilder()
                                  .parse(new ByteArrayInputStream(stringWriter.toString()
                                                                        .getBytes(StandardCharsets.UTF_8)));

        StratConCampaignState reloadedState = StratConCampaignState.Deserialize(document.getDocumentElement());
        assertNotNull(reloadedState, "the campaign state should load back");
        assertEquals(1, reloadedState.getTracks().size());
        return reloadedState.getTrack(0);
    }
}
