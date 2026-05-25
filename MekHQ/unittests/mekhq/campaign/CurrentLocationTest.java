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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.personnel.Person;
import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.LocationChangedEvent;
import mekhq.campaign.events.TransitStatusChangedEvent;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.w3c.dom.Node;

public class CurrentLocationTest {

    static final double TIME_TO_JP = 10.0;

    PlanetarySystem system;

    @BeforeEach
    void setUp() {
        system = mock(PlanetarySystem.class);
        when(system.getTimeToJumpPoint(1.0)).thenReturn(TIME_TO_JP);
    }

    /** Tests for {@link CurrentLocation#isOnPlanet()}. */
    @Nested
    class IsOnPlanet {
        @Test
        void trueWhenTransitTimeIsZero() {
            assertTrue(new CurrentLocation(system, 0.0).isOnPlanet());
        }

        @Test
        void trueWhenTransitTimeIsNegative() {
            assertTrue(new CurrentLocation(system, -1.0).isOnPlanet());
        }

        @Test
        void falseWhenTransitTimeIsPositive() {
            assertFalse(new CurrentLocation(system, 5.0).isOnPlanet());
        }
    }

    /** Tests for {@link CurrentLocation#isAtJumpPoint()}. */
    @Nested
    class IsAtJumpPoint {
        @Test
        void trueWhenTransitTimeEqualsTimeToJumpPoint() {
            assertTrue(new CurrentLocation(system, TIME_TO_JP).isAtJumpPoint());
        }

        @Test
        void trueWhenTransitTimeExceedsTimeToJumpPoint() {
            assertTrue(new CurrentLocation(system, TIME_TO_JP + 1.0).isAtJumpPoint());
        }

        @Test
        void falseWhenTransitTimeBelowTimeToJumpPoint() {
            assertFalse(new CurrentLocation(system, TIME_TO_JP - 1.0).isAtJumpPoint());
        }
    }

    /** Tests for {@link CurrentLocation#isInTransit()}. */
    @Nested
    class IsInTransit {
        @Test
        void trueWhenBetweenPlanetAndJumpPoint() {
            assertTrue(new CurrentLocation(system, 5.0).isInTransit());
        }

        @Test
        void falseWhenOnPlanet() {
            assertFalse(new CurrentLocation(system, 0.0).isInTransit());
        }

        @Test
        void falseWhenAtJumpPoint() {
            assertFalse(new CurrentLocation(system, TIME_TO_JP).isInTransit());
        }
    }

    /** Tests for {@link CurrentLocation#getPercentageTransit()}. */
    @Nested
    class GetPercentageTransit {
        @Test
        void zeroWhenAtJumpPoint() {
            // 1 - TIME_TO_JP / TIME_TO_JP = 0
            assertEquals(0.0, new CurrentLocation(system, TIME_TO_JP).getPercentageTransit(), 1e-9);
        }

        @Test
        void oneWhenTransitTimeIsZero() {
            assertEquals(1.0, new CurrentLocation(system, 0.0).getPercentageTransit(), 1e-9);
        }

        @Test
        void halfwayWhenHalfTransitElapsed() {
            double half = TIME_TO_JP / 2.0;
            assertEquals(0.5, new CurrentLocation(system, half).getPercentageTransit(), 1e-9);
        }
    }

    /**
     * Tests for {@link CurrentLocation#getTransitTime()}, {@link CurrentLocation#setTransitTime(double)},
     * {@link CurrentLocation#getRechargeTime()}, {@link CurrentLocation#setRechargeTime(double)},
     * {@link CurrentLocation#isJumpZenith()}, {@link CurrentLocation#getJumpPath()}, and
     * {@link CurrentLocation#setJumpPath(JumpPath)}.
     */
    @Nested
    class FieldAccessors {
        @Test
        void getTransitTime_reflectsConstructorArg() {
            assertEquals(3.5, new CurrentLocation(system, 3.5).getTransitTime(), 1e-9);
        }

        @Test
        void setTransitTime_updatesValue() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            assertTrue(loc.isOnPlanet());
            assertFalse(loc.isAtJumpPoint());
            assertFalse(loc.isInTransit());
            assertEquals(1.0, loc.getPercentageTransit());

            loc.setTransitTime(TIME_TO_JP / 2); // 50% of getTimeToJumpPoint
            assertFalse(loc.isOnPlanet());
            assertFalse(loc.isAtJumpPoint());
            assertTrue(loc.isInTransit());
            assertEquals(5.0, loc.getTransitTime(), 1e-9);

            loc.setTransitTime(TIME_TO_JP);// 100% of getTimeToJumpPoint
            assertFalse(loc.isOnPlanet());
            assertTrue(loc.isAtJumpPoint());
            assertFalse(loc.isInTransit());
            assertEquals(0.0, loc.getPercentageTransit(), 1e-9);
        }

        @Test
        void isJumpZenith_defaultsTrueAfterConstruction() {
            assertTrue(new CurrentLocation(system, 0.0).isJumpZenith());
        }

        @Test
        void getRechargeTime_defaultsToZero() {
            assertEquals(0.0, new CurrentLocation(system, 0.0).getRechargeTime(), 1e-9);
        }

        @Test
        void setRechargeTime_updatesValue() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            loc.setRechargeTime(88.0);
            assertEquals(88.0, loc.getRechargeTime(), 1e-9);
        }

        @Test
        void getJumpPath_defaultsToNull() {
            assertNull(new CurrentLocation(system, 0.0).getJumpPath());
        }

        @Test
        void setJumpPath_updatesValue() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            JumpPath path = mock(JumpPath.class);
            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                loc.setJumpPath(path);
                assertSame(path, loc.getJumpPath());
                mekHQ.verify(() -> MekHQ.triggerEvent(any()), times(1));
            }
        }
    }

    /**
     * Tests for {@link AbstractLocation#applyRechargeForHours(Campaign, java.time.LocalDate, boolean, double, boolean)}
     * via virtual dispatch on {@link CurrentLocation}.
     */
    @Nested
    class RechargeAccumulation {
        LocalDate today;
        Campaign campaign;

        @BeforeEach
        void setUp() {
            today = LocalDate.of(3025, 1, 1);
            campaign = mock(Campaign.class);
            when(system.getRechargeTime(today, false)).thenReturn(176.0);
            when(campaign.getLocalDate()).thenReturn(today);
        }

        @Test
        void rechargeAccumulatesAcrossMultipleCalls() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            loc.applyRechargeForHours(campaign, today, false, 24.0, false);
            loc.applyRechargeForHours(campaign, today, false, 24.0, false);
            assertEquals(48.0, loc.getRechargeTime(), 1e-9);
        }

        @Test
        void fullyChargedReportAddedWhenRechargeComplete() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            // neededRechargeTime = 20, give 24 hours → fully charged
            when(system.getRechargeTime(today, false)).thenReturn(20.0);
            loc.applyRechargeForHours(campaign, today, false, 24.0, false);
            // Two reports: spending hours + fully charged
            verify(campaign, times(2)).addReport(eq(GENERAL), anyString());
        }

        @Test
        void containsExpectedFields() {
            when(system.getId()).thenReturn("Outreach");
            CurrentLocation loc = new CurrentLocation(system, 5.5);
            loc.setRechargeTime(12.0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos, true);
            loc.writeToXML(pw, 0);
            String xml = baos.toString();

            assertTrue(xml.contains("<location>"));
            assertTrue(xml.contains("Outreach"));
            assertTrue(xml.contains("5.5"));
            assertTrue(xml.contains("12.0"));
            assertTrue(xml.contains("</location>"));
        }

        @Test
        void jumpPathNodeIncludedWhenPresent() {
            when(system.getId()).thenReturn("Terra");
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            JumpPath path = mock(JumpPath.class);
            loc.setJumpPath(path);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos, true);
            loc.writeToXML(pw, 0);

            verify(path).writeToXML(eq(pw), anyInt());
        }

        @Test
        void jumpPathNodeOmittedWhenNull() {
            when(system.getId()).thenReturn("Terra");
            CurrentLocation loc = new CurrentLocation(system, 0.0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            loc.writeToXML(new PrintWriter(baos, true), 0);

            assertFalse(baos.toString().contains("jumpPath"));
        }

        @Test
        void testChargeFully() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                loc.chargeFully(campaign);
                assertEquals(176.0, loc.getRechargeTime());
                mekHQ.verify(() -> MekHQ.triggerEvent(any()), times(1));
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitStatusChangedEvent.class)), times(1));
            }
        }

        @Test
        void testIsRecharging() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            when(campaign.isUseCommandCircuit()).thenReturn(false);
            assertTrue(loc.isRecharging(campaign));
            when(system.getRechargeTime(today, false)).thenReturn(0.0);
            assertFalse(loc.isRecharging(campaign));
        }
    }


    /**
     * Tests for {@link CurrentLocation#newDay(Campaign)}
     */
    @Nested
    class NewDay {

        final LocalDate today = LocalDate.of(2474, 11, 1);
        CurrentLocation currentLocation;
        Campaign campaign;

        @BeforeEach
        void setUp() {
            when(system.getPrimaryPlanet()).thenReturn(new Planet());
            when(system.getPrintableName(today)).thenReturn("Test");
            when(system.getRechargeTime(today, false)).thenReturn(176.0);

            campaign = mock(Campaign.class);
            when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
            when(campaign.getLocalDate()).thenReturn(today);
            when(campaign.isUseCommandCircuit()).thenReturn(false);
            when(campaign.getAutomatedMothballUnits()).thenReturn(Collections.emptyList());
            when(campaign.getFutureContracts()).thenReturn(Collections.emptyList());

            currentLocation = new CurrentLocation(system, 0.0);
        }

        @Test
        void testNewDayNoTransitRecharge() {
            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                currentLocation.newDay(campaign);
                assertEquals(24.0, currentLocation.getRechargeTime());
                verify(campaign).addReport(eq(DailyReportType.GENERAL), contains("recharging drives"));
                for (int i = 0; i < 9; i++) {
                    currentLocation.newDay(campaign);
                }
                assertEquals(176.0, currentLocation.getRechargeTime()); // do not charge past maximum
                mekHQ.verify(() -> MekHQ.triggerEvent(any()), never());
            }
        }

        @Test
        void testNewDayTransitToJumpPoint() {
            JumpPath jumpPath = mock(JumpPath.class);
            when(jumpPath.size()).thenReturn(2);
            when(jumpPath.isEmpty()).thenReturn(false);
            when(system.getTimeToJumpPoint(1.0)).thenReturn(2.5);

            assertEquals(0.0, currentLocation.getTransitTime());
            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                currentLocation.setJumpPath(jumpPath);
                mekHQ.clearInvocations();

                currentLocation.newDay(campaign);

                assertEquals(1.0, currentLocation.getTransitTime());
                verify(campaign).addReport(eq(DailyReportType.GENERAL), contains("hours in transit"));
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitStatusChangedEvent.class)), times(1));

                currentLocation.newDay(campaign);
                assertEquals(2.0, currentLocation.getTransitTime());
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitStatusChangedEvent.class)), times(2));
                currentLocation.newDay(campaign);
                assertEquals(2.5, currentLocation.getTransitTime()); // limited by getTimeToJumpPoint
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitStatusChangedEvent.class)), times(3));

                mekHQ.verify(() -> MekHQ.triggerEvent(isA(LocationChangedEvent.class)), never());
            }
        }

        @Test
        void testNewDayJumpToNextSystem() {
            when(system.getTimeToJumpPoint(1.0)).thenReturn(2.5);
            PlanetarySystem nextSystem = mock(PlanetarySystem.class);
            when(nextSystem.getTimeToJumpPoint(1.0)).thenReturn(4.0);
            when(nextSystem.getPrintableName(today)).thenReturn("Next-System");

            JumpPath jumpPath = mock(JumpPath.class);
            when(jumpPath.size()).thenReturn(2);
            when(jumpPath.isEmpty()).thenReturn(false);
            when(jumpPath.get(1)).thenReturn(nextSystem);

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                // get ready for the jump
                currentLocation.setJumpPath(jumpPath);
                currentLocation.chargeFully(campaign);
                mekHQ.clearInvocations();
                currentLocation.setTransitTime(2.0); // (2.5 - 2) days of transit remaining

                currentLocation.newDay(campaign);

                // verify the jump
                verify(jumpPath).removeFirstSystem();
                assertEquals(nextSystem, currentLocation.getCurrentSystem());
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(LocationChangedEvent.class)), times(1));
                verify(campaign).addReport(eq(DailyReportType.GENERAL), contains("Jumping to Next-System"));

                // verify transit status change
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitStatusChangedEvent.class)), times(1));
                assertEquals(4.0, currentLocation.getTransitTime());
                // only 12 hours because we were 0.5 days from the jump point
                assertEquals(12.0, currentLocation.getRechargeTime());
            }
        }

        @Test
        void testNewDayTransitToPlanet() {
            JumpPath jumpPath = mock(JumpPath.class);
            when(jumpPath.size()).thenReturn(1);
            when(jumpPath.isEmpty()).thenReturn(false);
            when(jumpPath.getLastSystem()).thenReturn(system);
            currentLocation.setTransitTime(0.5); // 12 hours from the planet

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                currentLocation.setJumpPath(jumpPath);
                mekHQ.clearInvocations();

                currentLocation.newDay(campaign);

                assertEquals(0.0, currentLocation.getTransitTime());
                assertNull(currentLocation.getJumpPath());
                assertTrue(currentLocation.isOnPlanet());
                verify(campaign).addReport(eq(DailyReportType.GENERAL), contains("Test reached."));
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(TransitStatusChangedEvent.class)), times(1));
                mekHQ.verify(() -> MekHQ.triggerEvent(isA(LocationChangedEvent.class)), never());
            }
        }
    }

    }

    @Nested
    class PersonChildSerialization {

        @Test
        void writeToXML_includesPersonIdTagForPersonChild() {
            when(system.getId()).thenReturn("Outreach");
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            Person person = new Person("First", "Last", null, "MERC");
            person.setParent(loc);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            loc.writeToXML(new PrintWriter(baos, true), 0);

            assertTrue(baos.toString().contains("<personId>" + person.getId() + "</personId>"));
        }

        @Test
        void writeToXML_omitsPersonIdWhenNoPersonChildren() {
            when(system.getId()).thenReturn("Outreach");
            CurrentLocation loc = new CurrentLocation(system, 0.0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            loc.writeToXML(new PrintWriter(baos, true), 0);

            assertFalse(baos.toString().contains("personId"));
        }

        @Test
        void generateInstanceFromXML_populatesPendingPersonIds() throws Exception {
            UUID personId = UUID.randomUUID();
            String xml = "<location><system>Outreach</system><transitTime>0.0</transitTime>"
                               + "<personId>" + personId + "</personId></location>";
            Node node = parseXml(xml);

            Campaign mockCampaign = mock(Campaign.class);
            when(mockCampaign.getSystemById("Outreach")).thenReturn(mock(PlanetarySystem.class));

            CurrentLocation loc = CurrentLocation.generateInstanceFromXML(node, mockCampaign);
            assertNotNull(loc);

            List<UUID> ids = loc.drainPendingPersonIds();
            assertEquals(1, ids.size());
            assertEquals(personId, ids.get(0));
        }

        @Test
        void drainPendingPersonIds_clearsListOnSecondCall() throws Exception {
            UUID personId = UUID.randomUUID();
            String xml = "<location><system>Outreach</system><transitTime>0.0</transitTime>"
                               + "<personId>" + personId + "</personId></location>";
            Node node = parseXml(xml);

            Campaign mockCampaign = mock(Campaign.class);
            when(mockCampaign.getSystemById("Outreach")).thenReturn(mock(PlanetarySystem.class));
            CurrentLocation loc = CurrentLocation.generateInstanceFromXML(node, mockCampaign);

            loc.drainPendingPersonIds();
            assertTrue(loc.drainPendingPersonIds().isEmpty());
        }

        private Node parseXml(String xml) throws Exception {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return db.parse(new ByteArrayInputStream(xml.getBytes())).getDocumentElement();
        }
    }
}
