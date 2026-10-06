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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.PointOfInterestParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Recon Raid reconnaissance mechanic: which contracts use it, its per-sector objective to scout at least
 * half of the land hexes, what counts as scouted, and the objective only ever rising.
 *
 * <p>The test sector is 4x4, with its leftmost column ocean: 12 land hexes, 6 of which must be scouted.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConReconnaissanceTest {
    private static final String LAND = "Plains";
    private static final String OCEAN = "Sea";
    private static final int LAND_HEXES = 12;
    private static final int REQUIRED_HEXES = 6;

    private StratConCampaignState campaignState;
    private StratConTrackState track;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setWidth(4);
        track.setHeight(4);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                track.setTerrainTile(new StratConCoords(x, y), (x == 0) ? OCEAN : LAND);
            }
        }

        campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
    }

    private static AbstractContract contract(ContractObjectiveType objectiveType) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        when(contract.getStartDate()).thenReturn(LocalDate.of(3025, 1, 15));
        when(contract.getLengthInMonths()).thenReturn(3);
        when(contract.getTrackCount()).thenReturn(1);
        when(contract.getScale()).thenReturn(1);
        return contract;
    }

    /** Reveals the given number of land hexes, column by column from the first land column. */
    private void revealLandHexes(int count) {
        int revealed = 0;
        for (int x = 1; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                if (revealed == count) {
                    return;
                }
                track.getRevealedCoords().add(new StratConCoords(x, y));
                revealed++;
            }
        }
    }

    private StratConStrategicObjective addObjective() {
        StratConReconnaissance.addReconnaissanceObjectives(campaignState);
        StratConStrategicObjective objective = StratConReconnaissance.getObjective(track);
        assertNotNull(objective, "test setup: the sector should have a reconnaissance objective");
        return objective;
    }

    // Which contracts use it

    @Test
    void onlyReconRaidsWithSpecialMechanicsUseReconnaissance() {
        assertTrue(StratConReconnaissance.usesReconnaissance(contract(ContractObjectiveType.RECON_RAID), true));
        assertFalse(StratConReconnaissance.usesReconnaissance(contract(ContractObjectiveType.RECON_RAID), false));
        assertFalse(StratConReconnaissance.usesReconnaissance(contract(ContractObjectiveType.OBJECTIVE_RAID), true));
        assertFalse(StratConReconnaissance.usesReconnaissance(contract(null), true));
    }

    @Test
    void aReconRaidSchedulesNoPointsOfInterestAndKeepsItsEssentialScenarios() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);
        ObjectiveParameters objective = new ObjectiveParameters();
        objective.setObjectiveType(StrategicObjectiveType.PointOfInterest);
        objective.setObjectiveCount(2);
        objective.getObjectivePointsOfInterest().add("UnitTestReconType");
        PointOfInterestParameters ordinary = new PointOfInterestParameters();
        ordinary.setTypeId("UnitTestReconType");
        ordinary.setCount(1);
        StratConContractDefinition definition = new StratConContractDefinition();
        definition.setObjectiveParameters(List.of(objective));
        definition.setPointsOfInterest(List.of(ordinary));
        StratConCampaignState scheduleState = new StratConCampaignState();

        StratConContractInitializer.schedulePointsOfInterest(contract, definition, scheduleState, true, true);

        assertTrue(scheduleState.getScheduledPointsOfInterest().isEmpty(), "not even the definition's own");
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(contract, true));
    }

    // The objective

    @Test
    void everySectorGetsAnObjectiveToScoutHalfItsLand() {
        StratConTrackState secondTrack = new StratConTrackState();
        secondTrack.setWidth(2);
        secondTrack.setHeight(2);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                secondTrack.setTerrainTile(new StratConCoords(x, y), LAND);
            }
        }
        campaignState.addTrack(secondTrack);

        StratConReconnaissance.addReconnaissanceObjectives(campaignState);

        StratConStrategicObjective objective = StratConReconnaissance.getObjective(track);
        assertNotNull(objective);
        assertEquals(REQUIRED_HEXES, objective.getDesiredObjectiveCount());
        assertNotNull(StratConReconnaissance.getObjective(secondTrack));
    }

    @Test
    void oceanHexesDoNotCount() {
        assertEquals(LAND_HEXES, StratConReconnaissance.getLandHexCount(track));
        assertEquals(REQUIRED_HEXES, StratConReconnaissance.getRequiredHexCount(track));

        track.getRevealedCoords().add(new StratConCoords(0, 0));
        assertEquals(0, StratConReconnaissance.getScoutedHexCount(track), "a revealed ocean hex is not land");
    }

    @Test
    void anOddNumberOfLandHexesRoundsTheRequirementUp() {
        track.setTerrainTile(new StratConCoords(1, 0), OCEAN);

        assertEquals(11, StratConReconnaissance.getLandHexCount(track));
        assertEquals(6, StratConReconnaissance.getRequiredHexCount(track), "at least half of 11");
    }

    @Test
    void theObjectiveIsMetOnceHalfTheLandIsScouted() {
        StratConStrategicObjective objective = addObjective();

        revealLandHexes(REQUIRED_HEXES - 1);
        StratConReconnaissance.updateObjectives(track);
        assertFalse(objective.isObjectiveCompleted(track));
        assertEquals(REQUIRED_HEXES - 1, objective.getCurrentObjectiveCount());
        assertFalse(objective.isObjectiveFailed(track), "short of it is not a failure");

        revealLandHexes(REQUIRED_HEXES);
        StratConReconnaissance.updateObjectives(track);
        assertTrue(objective.isObjectiveCompleted(track));
    }

    @Test
    void anyRevealCounts() {
        StratConStrategicObjective objective = addObjective();

        track.setGmRevealed(true);

        assertEquals(LAND_HEXES, StratConReconnaissance.getScoutedHexCount(track));
        StratConReconnaissance.updateObjectives(track);
        assertTrue(objective.isObjectiveCompleted(track));
    }

    @Test
    void aMetObjectiveStaysMetWhenTheFogReturns() {
        StratConStrategicObjective objective = addObjective();
        revealLandHexes(REQUIRED_HEXES);
        StratConReconnaissance.updateObjectives(track);
        assertTrue(objective.isObjectiveCompleted(track));

        track.getRevealedCoords().clear();
        StratConReconnaissance.updateObjectives(track);

        assertTrue(objective.isObjectiveCompleted(track));
    }

    @Test
    void progressNeverFallsBack() {
        StratConStrategicObjective objective = addObjective();
        revealLandHexes(4);
        StratConReconnaissance.updateObjectives(track);

        track.getRevealedCoords().clear();
        StratConReconnaissance.updateObjectives(track);

        assertEquals(4, objective.getCurrentObjectiveCount());
    }

    @Test
    void checkingTheObjectiveChangesNothing() {
        StratConStrategicObjective objective = addObjective();
        revealLandHexes(REQUIRED_HEXES);

        assertFalse(objective.isObjectiveCompleted(track), "not met until the counts are brought up to date");
        assertEquals(0, objective.getCurrentObjectiveCount());

        StratConReconnaissance.updateObjectives(track);
        assertTrue(objective.isObjectiveCompleted(track));
    }

    @Test
    void updatingASectorWithoutTheObjectiveDoesNothing() {
        StratConReconnaissance.updateObjectives(track);

        assertNull(StratConReconnaissance.getObjective(track));
    }

    @Test
    void aSectorWithoutTheObjectiveHasNone() {
        assertNull(StratConReconnaissance.getObjective(track));
    }
}
