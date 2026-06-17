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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FixedLocationTest {

    PlanetarySystem system;
    FixedLocation location;

    @BeforeEach
    void setUp() {
        system = mock(PlanetarySystem.class);
        location = new FixedLocation(system);
    }

    @Test
    void getCurrentSystem_returnsConstructorArg() {
        assertSame(system, location.getCurrentSystem());
    }

    @Test
    void getLocationNode_isNotNull() {
        assertNotNull(location.getLocationNode());
    }

    @Test
    void getLocationNode_locatableIsThis() {
        assertSame(location, location.getLocationNode().getLocatable());
    }

    @Test
    void isOnPlanet_alwaysTrue() {
        assertTrue(location.isOnPlanet());
    }

    @Test
    void isAtJumpPoint_alwaysFalse() {
        assertFalse(location.isAtJumpPoint());
    }

    @Test
    void isInTransit_alwaysFalse() {
        assertFalse(location.isInTransit());
    }

    @Test
    void getTransitTime_alwaysZero() {
        org.junit.jupiter.api.Assertions.assertEquals(0.0, location.getTransitTime());
    }

    @Test
    void getJumpPath_alwaysNull() {
        assertNull(location.getJumpPath());
    }

    @Test
    void getLocationNode_noParentByDefault() {
        assertNull(location.getLocationNode().getParent());
    }

    @Test
    void getLocationNode_noChildrenByDefault() {
        assertTrue(location.getLocationNode().getChildren().isEmpty());
    }
}
