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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.CurrentLocation;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

public class AcademyCampusLocationTest {

    static final String ACADEMY_SET = "TestSet";
    static final String ACADEMY_NAME = "Test Academy";

    AcademyCampusLocation campus;

    @BeforeEach
    void setUp() {
        campus = new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME);
    }

    @Test
    void getLocationNode_isNotNull() {
        assertNotNull(campus.getLocationNode());
    }

    @Test
    void getLocationNode_locatableIsThis() {
        assertSame(campus, campus.getLocationNode().getLocatable());
    }

    @Test
    void getLocationNode_noParentByDefault() {
        assertNull(campus.getLocationNode().getParent());
    }

    @Test
    void getLocationNode_hasPersonnelChildByDefault() {
        assertEquals(1, campus.getLocationNode().getChildren().size());
        assertSame(campus.getPersonnel().getLocationNode(),
              campus.getLocationNode().getChildren().iterator().next());
    }

    @Test
    void getAcademySet_returnsConstructorArg() {
        assertEquals(ACADEMY_SET, campus.getAcademySet());
    }

    @Test
    void getAcademyName_returnsConstructorArg() {
        assertEquals(ACADEMY_NAME, campus.getAcademyName());
    }

    @Nested
    class ParentLinking {

        FixedLocation fixed;

        @BeforeEach
        void setUp() {
            fixed = new FixedLocation(mock(PlanetarySystem.class));
        }

        @Test
        void setParent_toFixedLocation_succeeds() {
            assertTrue(campus.setParent(fixed));
        }

        @Test
        void setParent_toFixedLocation_wiresParentLink() {
            campus.setParent(fixed);
            assertSame(fixed.getLocationNode(), campus.getLocationNode().getParent());
        }

        @Test
        void setParent_toFixedLocation_addsToFixedChildren() {
            campus.setParent(fixed);
            assertTrue(fixed.getLocationNode().getChildren().contains(campus.getLocationNode()));
        }

        @Test
        void setParent_null_clearsParentLink() {
            campus.setParent(fixed);
            campus.setParent(null);
            assertNull(campus.getLocationNode().getParent());
        }

        @Test
        void setParent_null_removesFromFixedChildren() {
            campus.setParent(fixed);
            campus.setParent(null);
            assertFalse(fixed.getLocationNode().getChildren().contains(campus.getLocationNode()));
        }

        @Test
        void canSetParent_toNakedCampusWithoutRoot_returnsFalse() {
            AcademyCampusLocation orphan = new AcademyCampusLocation("X", "Y");
            assertFalse(campus.canSetParent(orphan));
        }
    }

    @Nested
    class XmlSerialization {

        @Test
        void writeToXML_containsAcademySetAndName() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            campus.writeToXML(new PrintWriter(baos, true), 0);
            String xml = baos.toString();
            assertTrue(xml.contains(ACADEMY_SET));
            assertTrue(xml.contains(ACADEMY_NAME));
            assertTrue(xml.contains("<academyCampus>"));
            assertTrue(xml.contains("</academyCampus>"));
        }

        @Test
        void writeToXML_includesPersonIdForPersonChild() {
            campus.setParent(new FixedLocation(mock(PlanetarySystem.class)));
            Person person = new Person("First", "Last", null, "MERC");
            person.setParent(campus.getPersonnel());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            campus.writeToXML(new PrintWriter(baos, true), 0);

            assertTrue(baos.toString().contains("<personId>" + person.getId() + "</personId>"));
        }

        @Test
        void writeToXML_includesCurrentLocationChild() {
            PlanetarySystem sys = mock(PlanetarySystem.class);
            when(sys.getId()).thenReturn("Outreach");
            when(sys.getTimeToJumpPoint(1.0)).thenReturn(10.0);
            campus.setParent(new FixedLocation(mock(PlanetarySystem.class)));
            CurrentLocation travelLoc = new CurrentLocation(sys, 0.0);
            travelLoc.setParent(campus);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            campus.writeToXML(new PrintWriter(baos, true), 0);
            String xml = baos.toString();

            assertTrue(xml.contains("<location>"));
            assertTrue(xml.contains("Outreach"));
        }

        @Test
        void generateInstanceFromXML_populatesPendingPersonIds() throws Exception {
            UUID personId = UUID.randomUUID();
            String xml = "<academyCampus><academySet>" + ACADEMY_SET + "</academySet>"
                               + "<academyName>" + ACADEMY_NAME + "</academyName>"
                               + "<personId>" + personId + "</personId></academyCampus>";
            Node node = parseXml(xml);

            AcademyCampusLocation result = AcademyCampusLocation.generateInstanceFromXML(node);
            assertNotNull(result);
            assertEquals(ACADEMY_SET, result.getAcademySet());

            List<UUID> ids = result.drainPendingPersonIds();
            assertEquals(1, ids.size());
            assertEquals(personId, ids.get(0));
        }

        @Test
        void drainPendingPersonIds_clearsListOnSecondCall() throws Exception {
            UUID personId = UUID.randomUUID();
            String xml = "<academyCampus><academySet>" + ACADEMY_SET + "</academySet>"
                               + "<academyName>" + ACADEMY_NAME + "</academyName>"
                               + "<personId>" + personId + "</personId></academyCampus>";
            AcademyCampusLocation result =
                  AcademyCampusLocation.generateInstanceFromXML(parseXml(xml));

            result.drainPendingPersonIds();
            assertTrue(result.drainPendingPersonIds().isEmpty());
        }

        @Test
        void generateInstanceFromXML_returnsNullWhenMissingAcademySet() throws Exception {
            String xml = "<academyCampus><academyName>" + ACADEMY_NAME + "</academyName></academyCampus>";
            assertNull(AcademyCampusLocation.generateInstanceFromXML(parseXml(xml)));
        }

        @Test
        void generateInstanceFromXML_returnsNullWhenMissingAcademyName() throws Exception {
            String xml = "<academyCampus><academySet>" + ACADEMY_SET + "</academySet></academyCampus>";
            assertNull(AcademyCampusLocation.generateInstanceFromXML(parseXml(xml)));
        }

        private Node parseXml(String xml) throws Exception {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return db.parse(new ByteArrayInputStream(xml.getBytes())).getDocumentElement();
        }
    }

    @Nested
    class IPlaceBehavior {
        @Test
        void getPersonnel_returnsNonNull() {
            assertNotNull(campus.getPersonnel());
        }

        @Test
        void getWarehouse_returnsNull() {
            assertNull(campus.getWarehouse());
        }

        @Test
        void getHangar_returnsNull() {
            assertNull(campus.getHangar());
        }

        @Test
        void getPartInventory_returnsEmptyWhenNoWarehouse() {
            mekhq.campaign.parts.PartInventory inv =
                  campus.getPartInventory(new mekhq.campaign.parts.meks.MekSensor());
            assertEquals(0, inv.getSupply());
            assertEquals(0, inv.getTransit());
        }
    }
}
