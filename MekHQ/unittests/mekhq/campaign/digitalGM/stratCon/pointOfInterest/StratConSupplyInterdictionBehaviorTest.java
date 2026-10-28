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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests what becomes of an enemy supply interdiction when the player deals with it, or fails to, and which hexes it
 * blocks while it is in play.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConSupplyInterdictionBehaviorTest {
    private static final StratConCoords COORDS = new StratConCoords(2, 2);

    private final StratConSupplyInterdictionBehavior behavior = new StratConSupplyInterdictionBehavior();
    private StratConTrackState track;
    private Campaign campaign;
    private StratConPointOfInterest interdiction;

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
        interdiction = new StratConPointOfInterest(StratConSupplyInterdictionBehavior.TYPE_ID, COORDS);
        track.addPointOfInterest(interdiction);
    }

    @Test
    void itIsRegisteredUnderItsBehaviorId() {
        assertEquals(StratConSupplyInterdictionBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConSupplyInterdictionBehavior.BEHAVIOR_ID).getClass());
    }

    @Test
    void anInterdictionInPlayBlocksItsOwnHexOnly() {
        assertTrue(StratConSupplyInterdictionBehavior.isInterdicted(track, COORDS));
        assertFalse(StratConSupplyInterdictionBehavior.isInterdicted(track, new StratConCoords(3, 3)));
    }

    @Test
    void anotherKindOfNavPointBlocksNothing() {
        StratConCoords otherCoords = new StratConCoords(4, 4);
        track.addPointOfInterest(new StratConPointOfInterest(StratConEnemyEngineersBehavior.TYPE_ID, otherCoords));

        assertFalse(StratConSupplyInterdictionBehavior.isInterdicted(track, otherCoords));
    }

    @Test
    void anInterdictionNoLongerInPlayBlocksNothing() {
        StratConPointOfInterestRules.resolvePointOfInterest(track, interdiction);

        assertFalse(StratConSupplyInterdictionBehavior.isInterdicted(track, COORDS));
    }

    @Test
    void breakingItUpTakesItOffTheMap() {
        behavior.onLinkedScenarioEnded(interdiction, track, true, campaign);

        assertNull(track.getPointOfInterest(interdiction.getId()));
        assertFalse(StratConSupplyInterdictionBehavior.isInterdicted(track, COORDS));
    }

    @Test
    void losingToTheRaidersLeavesThemHoldingTheRoute() {
        behavior.onLinkedScenarioEnded(interdiction, track, false, campaign);

        assertEquals(interdiction, track.getPointOfInterest(interdiction.getId()));
        assertTrue(interdiction.isActive());
        assertTrue(StratConSupplyInterdictionBehavior.isInterdicted(track, COORDS));
    }

    @Test
    void theRaidersAlwaysAmbushAndPayNoCombatBonus() {
        assertTrue(behavior.isScenarioAnAmbush());
        assertFalse(behavior.isCombatBonusPaid(mock(AbstractContract.class)));
    }
}
