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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.CurrentLocation;
import mekhq.campaign.location.LocationNode.LocationManager;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LocationNodeTest {

    /** Tests for {@link LocationNode#LocationNode(ILocation)}. */
    @Nested
    class Constructor {

        @Test
        void constructor_setsLocatable() {
            ILocation loc = mock(ILocation.class);
            LocationNode node = new LocationNode(loc);
            assertSame(loc, node.getLocatable());
        }

        @Test
        void constructor_noParent() {
            LocationNode node = new LocationNode(mock(ILocation.class));
            assertNull(node.getParent());
        }

        @Test
        void constructor_noChildren() {
            LocationNode node = new LocationNode(mock(ILocation.class));
            assertTrue(node.getChildren().isEmpty());
        }

    } // end Constructor

    /**
     * Tests for {@link LocationNode#addChild(LocationNode)}, {@link LocationNode#removeChild(LocationNode)}, and
     * {@link LocationNode#getChildren()}.
     */
    @Nested
    class ChildManagement {

        LocationNode parent;
        LocationNode child;

        @BeforeEach
        void setUp() {
            parent = new LocationNode(mock(ILocation.class));
            child = new LocationNode(mock(ILocation.class));
        }

        @Test
        void addChild_appearsInChildren() {
            parent.addChild(child);
            assertTrue(parent.getChildren().contains(child));
        }

        @Test
        void removeChild_disappearsFromChildren() {
            parent.addChild(child);
            parent.removeChild(child);
            assertFalse(parent.getChildren().contains(child));
        }

        @Test
        void getChildren_returnsImmutableCopy() {
            parent.addChild(child);
            assertThrows(UnsupportedOperationException.class,
                  () -> parent.getChildren().add(new LocationNode(mock(ILocation.class))));
        }

        @Test
        void getChildren_copyDoesNotReflectLaterModifications() {
            var snapshot = parent.getChildren();
            parent.addChild(child);
            assertFalse(snapshot.contains(child));
        }
    }

    /** Tests for {@link LocationNode#getCurrentLocation()}. */
    @Nested
    class GetCurrentLocation {

        PlanetarySystem system;
        CurrentLocation currentLocation;

        @BeforeEach
        void setUp() {
            system = mock(PlanetarySystem.class);
            currentLocation = new CurrentLocation(system, 0.0);
        }

        @Test
        void returnsItselfWhenLocatableIsCurrentLocation() {
            LocationNode node = currentLocation.getLocationNode();
            assertSame(currentLocation, node.getCurrentLocation());
        }

        @Test
        void walksUpToParentCurrentLocation() {
            // child is not a CurrentLocation; its parent is
            ILocation childLocatable = mock(ILocation.class);
            LocationNode child = new LocationNode(childLocatable);
            LocationManager.setLocation(child, currentLocation.getLocationNode());

            assertSame(currentLocation, child.getCurrentLocation());
        }

        @Test
        void returnsNullWhenNoCurrentLocationInChain() {
            // plain AbstractLocation — not a CurrentLocation
            ILocation plainLoc = mock(ILocation.class);
            LocationNode node = new LocationNode(plainLoc);
            assertNull(node.getCurrentLocation());
        }

        @Test
        void walksMultipleLevels() {
            ILocation midLocatable = mock(ILocation.class);
            LocationNode mid = new LocationNode(midLocatable);
            ILocation leafLocatable = mock(ILocation.class);
            LocationNode leaf = new LocationNode(leafLocatable);

            LocationManager.setLocation(mid, currentLocation.getLocationNode());
            LocationManager.setLocation(leaf, mid);

            assertSame(currentLocation, leaf.getCurrentLocation());
        }
    }

    /** Tests for {@link LocationNode.LocationManager#setLocation(LocationNode, LocationNode)}. */
    @Nested
    class LocationManagerTests {

        LocationNode parent;
        LocationNode child;

        @BeforeEach
        void setUp() {
            parent = new LocationNode(mock(ILocation.class));
            child = new LocationNode(mock(ILocation.class));
        }

        @Test
        void setLocation_wiresParentAndChild() {
            LocationManager.setLocation(child, parent);
            assertSame(parent, child.getParent());
            assertTrue(parent.getChildren().contains(child));
        }

        @Test
        void setLocation_nullParent_clearsParentLink() {
            LocationManager.setLocation(child, parent);
            LocationManager.setLocation(child, (LocationNode) null);
            assertNull(child.getParent());
        }

        @Test
        void setLocation_nullParent_removesFromOldParentsChildren() {
            LocationManager.setLocation(child, parent);
            LocationManager.setLocation(child, (LocationNode) null);
            assertFalse(parent.getChildren().contains(child));
        }

        @Test
        void setLocation_movesChildFromOldParentToNew() {
            LocationNode newParent = new LocationNode(mock(ILocation.class));
            LocationManager.setLocation(child, parent);
            LocationManager.setLocation(child, newParent);

            assertFalse(parent.getChildren().contains(child));
            assertTrue(newParent.getChildren().contains(child));
            assertSame(newParent, child.getParent());
        }

        @Test
        void setLocation_ILocation_overload_wiresCorrectly() {
            ILocation childLoc = mock(ILocation.class);
            ILocation parentLoc = mock(ILocation.class);
            LocationNode childNode = new LocationNode(childLoc);
            LocationNode parentNode = new LocationNode(parentLoc);
            when(childLoc.getLocationNode()).thenReturn(childNode);
            when(parentLoc.getLocationNode()).thenReturn(parentNode);

            LocationManager.setLocation(childLoc, parentLoc);

            assertSame(parentNode, childNode.getParent());
            assertTrue(parentNode.getChildren().contains(childNode));
        }
    }
}
