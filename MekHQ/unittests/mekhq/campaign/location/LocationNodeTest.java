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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignLocationManager;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.LocationNode.LocationManager;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

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

    /** Tests for {@link LocationNode#getNearestAbstractLocation()}. */
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
            assertSame(currentLocation, node.getNearestAbstractLocation());
        }

        @Test
        void walksUpToParentCurrentLocation() {
            // child is not a CurrentLocation; its parent is
            ILocation childLocatable = mock(ILocation.class);
            LocationNode child = new LocationNode(childLocatable);
            when(childLocatable.getLocationNode()).thenReturn(child);
            LocationManager.setLocation(childLocatable, currentLocation);

            assertSame(currentLocation, child.getNearestAbstractLocation());
        }

        @Test
        void returnsNullWhenNoCurrentLocationInChain() {
            // plain AbstractLocation — not a CurrentLocation
            ILocation plainLoc = mock(ILocation.class);
            LocationNode node = new LocationNode(plainLoc);
            assertNull(node.getNearestAbstractLocation());
        }

        @Test
        void walksMultipleLevels() {
            ILocation midLocatable = mock(ILocation.class);
            LocationNode mid = new LocationNode(midLocatable);
            ILocation leafLocatable = mock(ILocation.class);
            LocationNode leaf = new LocationNode(leafLocatable);

            when(midLocatable.getLocationNode()).thenReturn(mid);
            when(leafLocatable.getLocationNode()).thenReturn(leaf);
            LocationManager.setLocation(midLocatable, currentLocation);
            LocationManager.setLocation(leafLocatable, midLocatable);

            assertSame(currentLocation, leaf.getNearestAbstractLocation());
        }
    }

    /** Tests for {@link LocationNode.LocationManager#setLocation(ILocation, ILocation)}. */
    @Nested
    class LocationManagerTests {

        ILocation parentLoc;
        ILocation childLoc;
        LocationNode parent;
        LocationNode child;

        @BeforeEach
        void setUp() {
            parentLoc = mock(ILocation.class);
            childLoc = mock(ILocation.class);
            parent = new LocationNode(parentLoc);
            child = new LocationNode(childLoc);
            when(parentLoc.getLocationNode()).thenReturn(parent);
            when(childLoc.getLocationNode()).thenReturn(child);
        }

        @Test
        void setLocation_wiresParentAndChild() {
            LocationManager.setLocation(childLoc, parentLoc);
            assertSame(parent, child.getParent());
            assertTrue(parent.getChildren().contains(child));
        }

        @Test
        void setLocation_nullParent_clearsParentLink() {
            LocationManager.setLocation(childLoc, parentLoc);
            LocationManager.setLocation(childLoc, null);
            assertNull(child.getParent());
        }

        @Test
        void setLocation_nullParent_removesFromOldParentsChildren() {
            LocationManager.setLocation(childLoc, parentLoc);
            LocationManager.setLocation(childLoc, null);
            assertFalse(parent.getChildren().contains(child));
        }

        @Test
        void setLocation_movesChildFromOldParentToNew() {
            ILocation newParentLoc = mock(ILocation.class);
            LocationNode newParent = new LocationNode(newParentLoc);
            when(newParentLoc.getLocationNode()).thenReturn(newParent);
            LocationManager.setLocation(childLoc, parentLoc);
            LocationManager.setLocation(childLoc, newParentLoc);

            assertFalse(parent.getChildren().contains(child));
            assertTrue(newParent.getChildren().contains(child));
            assertSame(newParent, child.getParent());
        }

        @Test
        void setLocation_ILocation_overload_wiresCorrectly() {
            LocationManager.setLocation(childLoc, parentLoc);

            assertSame(parent, child.getParent());
            assertTrue(parent.getChildren().contains(child));
        }
    }

    @Nested
    class ReconnectChildren {

        Campaign mockCampaign;
        CampaignLocationManager mockLocationManager;
        FixedLocation parentFixed;

        @BeforeEach
        void setUp() {
            mockCampaign = mock(Campaign.class);
            mockLocationManager = mock(CampaignLocationManager.class);
            when(mockCampaign.getCampaignLocationManager()).thenReturn(mockLocationManager);
            parentFixed = new FixedLocation(mock(PlanetarySystem.class));
        }

        @Test
        void parsesCampusAsChildOfParent() throws Exception {
            String xml = "<locationNodeChildren>"
                               + "<academyCampus>"
                               + "<academySet>TestSet</academySet>"
                               + "<academyName>TestAcademy</academyName>"
                               + "</academyCampus>"
                               + "</locationNodeChildren>";

            LocationNode.reconnectChildren(parseXml(xml), parentFixed, mockCampaign);

            Set<LocationNode> children = parentFixed.getLocationNode().getChildren();
            assertEquals(1, children.size());
            assertTrue(children.iterator().next().getLocatable() instanceof AcademyCampusLocation);
        }

        @Test
        void campusHasCorrectSetAndName() throws Exception {
            String xml = "<locationNodeChildren>"
                               + "<academyCampus>"
                               + "<academySet>TestSet</academySet>"
                               + "<academyName>TestAcademy</academyName>"
                               + "</academyCampus>"
                               + "</locationNodeChildren>";

            LocationNode.reconnectChildren(parseXml(xml), parentFixed, mockCampaign);

            AcademyCampusLocation campus =
                  (AcademyCampusLocation) parentFixed.getLocationNode().getChildren()
                                                .iterator().next().getLocatable();
            assertEquals("TestSet", campus.getAcademySet());
            assertEquals("TestAcademy", campus.getAcademyName());
        }

        @Test
        void parsesNestedCurrentLocationAsCampusChild() throws Exception {
            when(mockCampaign.getSystemById(any())).thenReturn(mock(PlanetarySystem.class));

            String xml = "<locationNodeChildren>"
                               + "<academyCampus>"
                               + "<academySet>TestSet</academySet>"
                               + "<academyName>TestAcademy</academyName>"
                               + "<location>"
                               + "<system>Outreach</system>"
                               + "<transitTime>5.0</transitTime>"
                               + "</location>"
                               + "</academyCampus>"
                               + "</locationNodeChildren>";

            LocationNode.reconnectChildren(parseXml(xml), parentFixed, mockCampaign);

            AcademyCampusLocation campus =
                  (AcademyCampusLocation) parentFixed.getLocationNode().getChildren()
                                                .iterator().next().getLocatable();
            Set<LocationNode> campusChildren = campus.getLocationNode().getChildren();
            // campus has 2 children: the Personnel sub-container + the deserialized CurrentLocation
            assertEquals(2, campusChildren.size());
            assertTrue(campusChildren.stream().anyMatch(c -> c.getLocatable() instanceof CurrentLocation));
        }

        @Test
        void addLocationCalledForNestedCurrentLocation() throws Exception {
            when(mockCampaign.getSystemById(any())).thenReturn(mock(PlanetarySystem.class));

            String xml = "<locationNodeChildren>"
                               + "<academyCampus>"
                               + "<academySet>TestSet</academySet>"
                               + "<academyName>TestAcademy</academyName>"
                               + "<location><system>Outreach</system><transitTime>5.0</transitTime></location>"
                               + "</academyCampus>"
                               + "</locationNodeChildren>";

            LocationNode.reconnectChildren(parseXml(xml), parentFixed, mockCampaign);

            verify(mockLocationManager).addLocation(any(CurrentLocation.class));
        }

        @Test
        void skipsUnrecognizedElements() throws Exception {
            String xml = "<locationNodeChildren>"
                               + "<unknownTag>someValue</unknownTag>"
                               + "</locationNodeChildren>";

            assertDoesNotThrow(() ->
                                     LocationNode.reconnectChildren(parseXml(xml), parentFixed, mockCampaign));
            assertTrue(parentFixed.getLocationNode().getChildren().isEmpty());
        }

        private Node parseXml(String xml) throws Exception {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return db.parse(new ByteArrayInputStream(xml.getBytes())).getDocumentElement();
        }
    }

    /** Tests for {@link ILocation#getPlace()}. */
    @Nested
    class GetPlace {

        @Test
        void returnsNullWithNoAncestors() {
            ILocation leaf = mock(ILocation.class);
            LocationNode node = new LocationNode(leaf);
            when(leaf.getLocationNode()).thenReturn(node);
            when(leaf.hasLocationNode()).thenReturn(true);

            assertNull(leaf.getPlace());
        }

        @Test
        void returnsNullWhenNoIPlaceInChain() {
            ILocation leaf = mock(ILocation.class);
            ILocation middle = mock(ILocation.class);
            LocationNode leafNode = new LocationNode(leaf);
            LocationNode middleNode = new LocationNode(middle);
            when(leaf.getLocationNode()).thenReturn(leafNode);
            when(middle.getLocationNode()).thenReturn(middleNode);
            when(leaf.hasLocationNode()).thenReturn(true);
            LocationManager.setLocation(leaf, middle);

            assertNull(leaf.getPlace());
        }

        @Test
        void returnsNearestIPlaceAncestor() {
            PlayerBase base = new PlayerBase(new FixedLocation(mock(mekhq.campaign.universe.PlanetarySystem.class)));

            // Hangar's parent is the base, so walking from the hangar should find the base.
            assertSame(base, base.getBaseHangar().getPlace());
        }

        @Test
        void sparePart_inBaseWarehouse_returnsBase() {
            PlayerBase base = new PlayerBase(new FixedLocation(mock(mekhq.campaign.universe.PlanetarySystem.class)));

            // Warehouse's parent is the base.
            assertSame(base, base.getBaseWarehouse().getPlace());
        }

        @Test
        void locationInBaseHangar_chainReachesBase() {
            PlayerBase base = new PlayerBase(new FixedLocation(mock(mekhq.campaign.universe.PlanetarySystem.class)));

            // The hangar is below the base: hangar.parent = base.
            // The hangar itself is a concrete ILocation — getPlace() should find the base.
            assertSame(base, base.getBaseHangar().getPlace());
        }
    }
}
