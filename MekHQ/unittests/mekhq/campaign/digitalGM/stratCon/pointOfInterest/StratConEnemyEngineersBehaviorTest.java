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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests what enemy engineers leave behind when they are not driven off.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConEnemyEngineersBehaviorTest {
    private static final StratConCoords COORDS = new StratConCoords(2, 2);

    private final StratConEnemyEngineersBehavior behavior = new StratConEnemyEngineersBehavior();
    private StratConTrackState track;
    private Campaign campaign;
    private StratConPointOfInterest engineers;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setWidth(6);
        track.setHeight(6);
        campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getGUI()).thenReturn(null);
        engineers = new StratConPointOfInterest(StratConEnemyEngineersBehavior.TYPE_ID, COORDS);
        track.addPointOfInterest(engineers);
    }

    @Test
    void engineersLeftAloneBuildAFullyGarrisonedEnemyOutpost() {
        behavior.onExpired(engineers, track, campaign);

        StratConFacility outpost = track.getFacility(COORDS);
        assertNotNull(outpost);
        assertEquals(ForceAlignment.Opposing, outpost.getOwner());
        assertEquals(FacilityTier.OUTPOST, outpost.getTier());
        assertEquals(outpost.getGarrisonMaximum(), outpost.getGarrison());
        assertTrue(outpost.getVisible());
        assertNull(track.getPointOfInterest(engineers.getId()));
    }

    @Test
    void engineersWhoseEscortWinsBuildTheirOutpostToo() {
        behavior.onLinkedScenarioEnded(engineers, track, false, campaign);

        assertNotNull(track.getFacility(COORDS));
    }

    @Test
    void engineersDrivenOffLeaveNothing() {
        behavior.onLinkedScenarioEnded(engineers, track, true, campaign);

        assertNull(track.getFacility(COORDS));
        assertNull(track.getPointOfInterest(engineers.getId()));
    }

    @Test
    void engineersWhoseHexIsTakenMeanwhileBuildNothingButStillLeave() {
        StratConFacility existing = StratConTestData.facility(ForceAlignment.Allied,
              FacilityType.MekBase,
              new LocalModifiersEffect(List.of("MekGarrison.json")));
        track.addFacility(COORDS, existing);

        assertNull(behavior.buildOutpost(engineers, track, campaign));

        assertEquals(existing, track.getFacility(COORDS));
        assertNull(track.getPointOfInterest(engineers.getId()));
    }

    @Test
    void engineersInASectorAtItsFacilityCapBuildNothing() {
        // Fill the sector to its cap: half of its 36 dry hexes.
        int placed = 0;
        for (int x = 0; (x < track.getWidth()) && (placed < 18); x++) {
            for (int y = 0; (y < track.getHeight()) && (placed < 18); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (!coords.equals(COORDS)) {
                    track.addFacility(coords, StratConTestData.facility(ForceAlignment.Opposing,
                          FacilityType.MekBase,
                          new LocalModifiersEffect(List.of("MekGarrison.json"))));
                    placed++;
                }
            }
        }

        assertNull(behavior.buildOutpost(engineers, track, campaign));
        assertNull(track.getFacility(COORDS));
    }
}
