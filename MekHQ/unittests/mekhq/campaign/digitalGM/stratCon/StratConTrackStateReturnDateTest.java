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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a force already deployed to a hex keeps its return date when a new scenario on that hex deploys it
 * there again, unless the new deployment runs longer.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConTrackStateReturnDateTest {
    private static final int FORCE_ID = 7;
    private static final int DEPLOYMENT_TIME = 3;
    private static final LocalDate FIRST_DEPLOYMENT = LocalDate.of(3025, 1, 5);
    private static final LocalDate FIRST_RETURN = FIRST_DEPLOYMENT.plusDays(DEPLOYMENT_TIME);

    private final StratConCoords coords = new StratConCoords(2, 2);
    private StratConTrackState track;

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setDeploymentTime(DEPLOYMENT_TIME);
        track.assignForce(FORCE_ID, coords, FIRST_DEPLOYMENT, false);
    }

    private void placeScenario(LocalDate returnDate) {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(coords);
        scenario.setReturnDate(returnDate);
        track.getScenarios().put(coords, scenario);
    }

    @Test
    void newScenarioOnHexDoesNotShortenDeployment() {
        placeScenario(FIRST_RETURN.minusDays(1));

        track.assignForce(FORCE_ID, coords, FIRST_DEPLOYMENT.plusDays(1), false);

        assertEquals(FIRST_RETURN, track.getAssignedForceReturnDates().get(FORCE_ID));
        assertEquals(FIRST_RETURN.toString(), track.getAssignedForceReturnDatesForStorage().get(FORCE_ID));
    }

    @Test
    void newScenarioOnHexCanLengthenDeployment() {
        LocalDate laterReturn = FIRST_RETURN.plusDays(2);
        placeScenario(laterReturn);

        track.assignForce(FORCE_ID, coords, FIRST_DEPLOYMENT.plusDays(1), false);

        assertEquals(laterReturn, track.getAssignedForceReturnDates().get(FORCE_ID));
    }

    @Test
    void redeployingAfterUnassignKeepsEarlierDeploymentWhenCarriedOver() {
        // mirrors StratConRulesManager.processForceDeployment, which clears the force out before assigning it again
        placeScenario(FIRST_RETURN.minusDays(1));
        LocalDate currentReturnDate = track.getReturnDateIfDeployedTo(FORCE_ID, coords);

        track.unassignFormation(FORCE_ID);
        track.assignForce(FORCE_ID, coords, FIRST_DEPLOYMENT.plusDays(1), false);
        track.keepLaterReturnDate(FORCE_ID, currentReturnDate);

        assertEquals(FIRST_RETURN, track.getAssignedForceReturnDates().get(FORCE_ID));
    }

    @Test
    void movingToAnotherHexStartsAFreshDeployment() {
        StratConCoords otherCoords = new StratConCoords(4, 4);
        LocalDate moveDate = FIRST_DEPLOYMENT.plusDays(1);

        assertNull(track.getReturnDateIfDeployedTo(FORCE_ID, otherCoords));

        track.unassignFormation(FORCE_ID);
        track.assignForce(FORCE_ID, otherCoords, moveDate, false);

        assertEquals(moveDate.plusDays(DEPLOYMENT_TIME), track.getAssignedForceReturnDates().get(FORCE_ID));
    }

    @Test
    void keepLaterReturnDateIgnoresUndeployedForces() {
        track.unassignFormation(FORCE_ID);

        track.keepLaterReturnDate(FORCE_ID, FIRST_RETURN.plusDays(10));

        assertNull(track.getAssignedForceReturnDates().get(FORCE_ID));
    }
}
