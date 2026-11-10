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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConRulesManager#isOccupiedForOfficialChallenge}: an Official Challenge is moved off any hex
 * holding a deployed formation, a scenario, or a facility.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConOfficialChallengePlacementTest {

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConTrackState track() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        return track;
    }

    @Test
    void emptyHexIsFree() {
        assertFalse(StratConRulesManager.isOccupiedForOfficialChallenge(track(), new StratConCoords(2, 2)));
    }

    @Test
    void hexWithDeployedFormationIsOccupied() {
        StratConTrackState track = track();
        StratConCoords coords = new StratConCoords(2, 2);
        track.assignForce(7, coords, LocalDate.of(3025, 1, 1), false);

        assertTrue(StratConRulesManager.isOccupiedForOfficialChallenge(track, coords));
        assertFalse(StratConRulesManager.isOccupiedForOfficialChallenge(track, new StratConCoords(1, 1)));
    }

    @Test
    void hexWithFacilityIsOccupied() {
        StratConTrackState track = track();
        StratConCoords coords = new StratConCoords(3, 1);
        track.addFacility(coords, new StratConFacility());

        assertTrue(StratConRulesManager.isOccupiedForOfficialChallenge(track, coords));
    }

    @Test
    void hexWithScenarioIsOccupied() {
        StratConTrackState track = track();
        StratConCoords coords = new StratConCoords(0, 4);
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(coords);
        track.addScenario(scenario);

        assertTrue(StratConRulesManager.isOccupiedForOfficialChallenge(track, coords));
    }

    @Test
    void missingTrackOrCoordsIsNeverOccupied() {
        assertFalse(StratConRulesManager.isOccupiedForOfficialChallenge(null, new StratConCoords(0, 0)));
        assertFalse(StratConRulesManager.isOccupiedForOfficialChallenge(track(), null));
    }
}
