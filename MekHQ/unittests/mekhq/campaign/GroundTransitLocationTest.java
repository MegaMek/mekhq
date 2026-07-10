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

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.MekHQ;
import mekhq.campaign.events.TransitCompleteEvent;
import mekhq.campaign.events.TransitStatusChangedEvent;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.w3c.dom.Node;

public class GroundTransitLocationTest {

    PlanetarySystem system;

    @BeforeEach
    void setUp() {
        system = mock(PlanetarySystem.class);
    }

    /** A ground convoy is always considered on-planet, even mid-journey. */
    @Nested
    class IsOnPlanet {
        @Test
        void trueWhenStationary() {
            assertTrue(new GroundTransitLocation(system, 0.0).isOnPlanet());
        }

        @Test
        void trueWhileTravelling() {
            assertTrue(new GroundTransitLocation(system, 5.0).isOnPlanet());
        }
    }

    /** Tests for {@link GroundTransitLocation#isInTransit()}. */
    @Nested
    class IsInTransit {
        @Test
        void trueWhileTransitTimeRemains() {
            assertTrue(new GroundTransitLocation(system, 5.0).isInTransit());
        }

        @Test
        void falseWhenArrived() {
            assertFalse(new GroundTransitLocation(system, 0.0).isInTransit());
        }
    }

    /** Tests for {@link GroundTransitLocation#getPercentageTransit()}. */
    @Nested
    class GetPercentageTransit {
        @Test
        void zeroAtStartOfJourney() {
            // total == remaining → no progress yet
            assertEquals(0.0, new GroundTransitLocation(system, 4.0).getPercentageTransit(), 1e-9);
        }

        @Test
        void oneWhenTotalIsZero() {
            assertEquals(1.0, new GroundTransitLocation(system, 0.0).getPercentageTransit(), 1e-9);
        }

        @Test
        void halfwayWhenHalfElapsed() {
            GroundTransitLocation loc = new GroundTransitLocation(system, 4.0);
            loc.setTransitTime(2.0);
            assertEquals(0.5, loc.getPercentageTransit(), 1e-9);
        }
    }

    /** Tests for {@link GroundTransitLocation#newDay(Campaign, boolean)}. */
    @Nested
    class NewDay {
        Campaign campaign;

        @BeforeEach
        void setUp() {
            campaign = mock(Campaign.class);
        }

        @Test
        void countsDownOneDayAndReports() {
            GroundTransitLocation loc = new GroundTransitLocation(system, 3.0);
            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                loc.newDay(campaign, false);

                assertEquals(2.0, loc.getTransitTime(), 1e-9);
                assertTrue(loc.isInTransit());
                verify(campaign).addReport(eq(GENERAL), anyString());
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitStatusChangedEvent.class)), times(1));
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitCompleteEvent.class)), never());
            }
        }

        @Test
        void firesTransitCompleteOnArrival() {
            GroundTransitLocation loc = new GroundTransitLocation(system, 1.0);
            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                loc.newDay(campaign, false);

                assertEquals(0.0, loc.getTransitTime(), 1e-9);
                assertFalse(loc.isInTransit());
                assertTrue(loc.isOnPlanet());
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitCompleteEvent.class)), times(1));
            }
        }

        @Test
        void noOpWhenAlreadyArrived() {
            GroundTransitLocation loc = new GroundTransitLocation(system, 0.0);
            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                loc.newDay(campaign, false);

                verify(campaign, never()).addReport(any(), any());
                mekHQ.verify(() -> MekHQ.triggerEvent(any()), never());
            }
        }
    }

    /** Round-trips a {@link GroundTransitLocation} through XML. */
    @Nested
    class XmlRoundTrip {
        @Test
        void writeToXmlContainsExpectedFields() {
            when(system.getId()).thenReturn("Outreach");
            GroundTransitLocation loc = new GroundTransitLocation(system, 4.0);
            loc.setTransitTime(2.5);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            loc.writeToXML(new PrintWriter(baos, true), 0);
            String xml = baos.toString();

            assertTrue(xml.contains("<groundTransitLocation>"));
            assertTrue(xml.contains("Outreach"));
            assertTrue(xml.contains("<transitTime>2.5</transitTime>"));
            assertTrue(xml.contains("<totalTransitTime>4.0</totalTransitTime>"));
            assertTrue(xml.contains("</groundTransitLocation>"));
        }

        @Test
        void generateInstanceFromXmlRestoresState() throws Exception {
            String xml = "<groundTransitLocation><currentSystemId>Outreach</currentSystemId>"
                               + "<transitTime>2.5</transitTime>"
                               + "<totalTransitTime>4.0</totalTransitTime></groundTransitLocation>";
            Node node = parseXml(xml);

            Campaign mockCampaign = mock(Campaign.class);
            when(mockCampaign.getSystemById("Outreach")).thenReturn(system);

            GroundTransitLocation loc = GroundTransitLocation.generateInstanceFromXML(node, mockCampaign);

            assertNotNull(loc);
            assertEquals(2.5, loc.getTransitTime(), 1e-9);
            assertEquals(0.375, loc.getPercentageTransit(), 1e-9); // 1 - 2.5 / 4.0
            assertTrue(loc.isInTransit());
            assertTrue(loc.isOnPlanet());
        }

        @Test
        void generateInstanceFromXmlPopulatesPendingPersonIds() throws Exception {
            UUID personId = UUID.randomUUID();
            String xml = "<groundTransitLocation><currentSystemId>Outreach</currentSystemId>"
                               + "<transitTime>1.0</transitTime>"
                               + "<personId>" + personId + "</personId></groundTransitLocation>";
            Node node = parseXml(xml);

            Campaign mockCampaign = mock(Campaign.class);
            when(mockCampaign.getSystemById("Outreach")).thenReturn(system);

            GroundTransitLocation loc = GroundTransitLocation.generateInstanceFromXML(node, mockCampaign);
            assertNotNull(loc);

            List<UUID> ids = loc.drainPendingPersonIds();
            assertEquals(1, ids.size());
            assertEquals(personId, ids.get(0));
        }

        private Node parseXml(String xml) throws Exception {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return db.parse(new ByteArrayInputStream(xml.getBytes())).getDocumentElement();
        }
    }
}
