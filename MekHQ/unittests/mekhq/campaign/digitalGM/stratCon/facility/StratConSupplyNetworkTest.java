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
package mekhq.campaign.digitalGM.stratCon.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the supply network model: following a route back to its root, and keeping, replacing and dropping routes and
 * roots.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConSupplyNetworkTest {
    // A route of four hexes, FACILITY -> MIDDLE -> NEAR_ROOT -> ROOT, and a branch BRANCH -> MIDDLE.
    private static final StratConCoords ROOT = new StratConCoords(0, 0);
    private static final StratConCoords NEAR_ROOT = new StratConCoords(1, 0);
    private static final StratConCoords MIDDLE = new StratConCoords(2, 0);
    private static final StratConCoords FACILITY = new StratConCoords(3, 0);
    private static final StratConCoords BRANCH = new StratConCoords(2, 1);
    private static final StratConCoords ELSEWHERE = new StratConCoords(7, 7);

    private StratConSupplyNetwork network;

    @BeforeEach
    void setUp() {
        network = new StratConSupplyNetwork();
        network.replaceRoots(List.of(ROOT));
        network.addLink(FACILITY, MIDDLE);
        network.addLink(MIDDLE, NEAR_ROOT);
        network.addLink(NEAR_ROOT, ROOT);
        network.addLink(BRANCH, MIDDLE);
    }

    @Test
    void aRouteRunsFromItsHexToItsRoot() {
        assertEquals(List.of(FACILITY, MIDDLE, NEAR_ROOT, ROOT), network.routeFrom(FACILITY));
        assertTrue(network.isRouted(FACILITY));
        assertTrue(network.isRouted(ROOT), "a root is routed to itself");
    }

    @Test
    void aHexNoLinkLeavesIsItsOwnRoute() {
        assertEquals(List.of(ELSEWHERE), network.routeFrom(ELSEWHERE));
        assertFalse(network.isRouted(ELSEWHERE));
        assertNull(network.nextHex(ELSEWHERE));
    }

    @Test
    void theNetworkHoldsItsRootsAndEveryHexItsLinksTouch() {
        assertEquals(Set.of(ROOT, NEAR_ROOT, MIDDLE, FACILITY, BRANCH), network.hexes());
        assertTrue(network.contains(ROOT));
        assertTrue(network.contains(BRANCH));
        assertFalse(network.contains(ELSEWHERE));
    }

    @Test
    void aNewLinkFromAHexReplacesTheOldOne() {
        network.addLink(FACILITY, BRANCH);

        assertEquals(BRANCH, network.nextHex(FACILITY));
        assertEquals(4, network.getLinks().size());
    }

    @Test
    void aLoopNeverReachesARootAndEndsWhereItStarted() {
        StratConSupplyNetwork looped = new StratConSupplyNetwork();
        looped.addLink(FACILITY, MIDDLE);
        looped.addLink(MIDDLE, FACILITY);

        assertEquals(List.of(FACILITY, MIDDLE), looped.routeFrom(FACILITY));
        assertFalse(looped.isRouted(FACILITY));
    }

    @Test
    void aNewRootLeadsNowhereSoItsOutgoingLinkIsDropped() {
        network.replaceRoots(List.of(ROOT, MIDDLE));

        assertNull(network.nextHex(MIDDLE));
        assertEquals(List.of(FACILITY, MIDDLE), network.routeFrom(FACILITY));
        assertTrue(network.isRouted(FACILITY));
    }

    @Test
    void removingAHexBreaksEveryRouteThroughIt() {
        network.removeHexes(NEAR_ROOT::equals);

        assertFalse(network.isRouted(FACILITY));
        assertFalse(network.isRouted(BRANCH));
        assertFalse(network.contains(NEAR_ROOT));
        assertTrue(network.isRoot(ROOT));
    }

    @Test
    void removingARootTakesItOffTheNetwork() {
        network.removeHexes(ROOT::equals);

        assertFalse(network.isRoot(ROOT));
        assertFalse(network.isRouted(FACILITY));
    }

    @Test
    void retainingKeepsOnlyTheRoutesFromTheGivenHexesThatStillReachARoot() {
        network.addLink(ELSEWHERE, new StratConCoords(8, 8));

        network.retainRoutesFrom(List.of(FACILITY, ELSEWHERE));

        assertEquals(List.of(FACILITY, MIDDLE, NEAR_ROOT, ROOT), network.routeFrom(FACILITY));
        assertNull(network.nextHex(BRANCH), "the branch leads from no kept hex");
        assertNull(network.nextHex(ELSEWHERE), "a route that reaches no root is dropped");
        assertTrue(network.isRoot(ROOT));
    }

    @Test
    void clearingDropsEverything() {
        network.clear();

        assertTrue(network.getLinks().isEmpty());
        assertTrue(network.getRoots().isEmpty());
        assertFalse(network.contains(FACILITY));
    }
}
