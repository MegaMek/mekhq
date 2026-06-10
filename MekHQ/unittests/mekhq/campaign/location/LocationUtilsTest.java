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
package mekhq.campaign.location;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.CurrentLocation;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LocationUtilsTest {

    PlayerBase base;
    FixedLocation fixed;

    @BeforeEach
    void setUp() {
        fixed = new FixedLocation(mock(PlanetarySystem.class));
        base = new PlayerBase(fixed);
    }

    // ---- isInTransit -------------------------------------------------------

    @Nested
    class IsInTransit {

        @Test
        void nullLocation_returnsFalse() {
            assertFalse(LocationUtils.isInTransit(null));
        }

        @Test
        void mainForceUnit_noTravelNode_returnsFalse() {
            assertFalse(LocationUtils.isInTransit(fixed));
        }

        @Test
        void locationInTravelNodeWithJumpPath_returnsTrue() {
            PlanetarySystem sys = mock(PlanetarySystem.class);
            when(sys.getTimeToJumpPoint(1.0)).thenReturn(1.0);
            CurrentLocation travelNode = new CurrentLocation(sys, 0.0);

            JumpPath mockJumpPath = mock(JumpPath.class);
            when(mockJumpPath.isEmpty()).thenReturn(false);
            travelNode.setJumpPath(mockJumpPath);

            travelNode.setParent(base);
            assertTrue(LocationUtils.isInTransit(travelNode));
        }

        @Test
        void locationInTravelNodeWithEmptyJumpPath_returnsFalse() {
            PlanetarySystem sys = mock(PlanetarySystem.class);
            when(sys.getTimeToJumpPoint(1.0)).thenReturn(1.0);
            CurrentLocation travelNode = new CurrentLocation(sys, 0.0);

            JumpPath emptyJumpPath = mock(JumpPath.class);
            when(emptyJumpPath.isEmpty()).thenReturn(true);
            travelNode.setJumpPath(emptyJumpPath);

            travelNode.setParent(base);
            assertFalse(LocationUtils.isInTransit(travelNode));
        }
    }

    // ---- findEffectiveBase -------------------------------------------------

    @Nested
    class FindEffectiveBase {

        @Test
        void nullLocation_returnsNull() {
            assertNull(LocationUtils.findEffectiveBase(null));
        }

        @Test
        void locationWithNoBaseAncestor_returnsNull() {
            assertNull(LocationUtils.findEffectiveBase(fixed));
        }

        @Test
        void baseHangar_returnsItsBase() {
            assertSame(base, LocationUtils.findEffectiveBase(base.getBaseHangar()));
        }

        @Test
        void baseWarehouse_returnsItsBase() {
            assertSame(base, LocationUtils.findEffectiveBase(base.getBaseWarehouse()));
        }

        @Test
        void base_returnsItself() {
            assertSame(base, LocationUtils.findEffectiveBase(base));
        }
    }

    // ---- areSameEffectiveLocation ------------------------------------------

    @Nested
    class AreSameEffectiveLocation {

        @Test
        void bothNull_returnsFalse() {
            assertFalse(LocationUtils.areSameEffectiveLocation(null, null));
        }

        @Test
        void firstNull_returnsFalse() {
            assertFalse(LocationUtils.areSameEffectiveLocation(null, fixed));
        }

        @Test
        void secondNull_returnsFalse() {
            assertFalse(LocationUtils.areSameEffectiveLocation(fixed, null));
        }

        @Test
        void bothMainForce_returnsTrue() {
            FixedLocation other = new FixedLocation(mock(PlanetarySystem.class));
            assertTrue(LocationUtils.areSameEffectiveLocation(fixed, other));
        }

        @Test
        void bothSameBase_returnsTrue() {
            assertTrue(LocationUtils.areSameEffectiveLocation(
                  base.getBaseHangar(), base.getBaseWarehouse()));
        }

        @Test
        void differentBases_returnsFalse() {
            PlayerBase base2 = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
            assertFalse(LocationUtils.areSameEffectiveLocation(
                  base.getBaseHangar(), base2.getBaseHangar()));
        }

        @Test
        void oneBaseOneMainForce_returnsFalse() {
            assertFalse(LocationUtils.areSameEffectiveLocation(base, fixed));
        }

        @Test
        void eitherInTransit_returnsFalse() {
            PlanetarySystem sys = mock(PlanetarySystem.class);
            when(sys.getTimeToJumpPoint(1.0)).thenReturn(1.0);
            CurrentLocation travelNode = new CurrentLocation(sys, 0.0);
            JumpPath mockJumpPath = mock(JumpPath.class);
            when(mockJumpPath.isEmpty()).thenReturn(false);
            travelNode.setJumpPath(mockJumpPath);
            travelNode.setParent(base);

            assertFalse(LocationUtils.areSameEffectiveLocation(travelNode, base));
        }
    }
}
