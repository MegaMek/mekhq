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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;

import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            loc.setTransitTime(7.0);
            assertEquals(7.0, loc.getTransitTime(), 1e-9);
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
            loc.setJumpPath(path);
            assertSame(path, loc.getJumpPath());
        }
    }

    /**
     * Tests for {@link AbstractLocation#applyRechargeForHours(Campaign, java.time.LocalDate, boolean, double)} via
     * virtual dispatch on {@link CurrentLocation}.
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
        }

        @Test
        void rechargeAccumulatesAcrossMultipleCalls() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            loc.applyRechargeForHours(campaign, today, false, 24.0);
            loc.applyRechargeForHours(campaign, today, false, 24.0);
            assertEquals(48.0, loc.getRechargeTime(), 1e-9);
        }

        @Test
        void fullyChargedReportAddedWhenRechargeComplete() {
            CurrentLocation loc = new CurrentLocation(system, 0.0);
            // neededRechargeTime = 20, give 24 hours → fully charged
            when(system.getRechargeTime(today, false)).thenReturn(20.0);
            loc.applyRechargeForHours(campaign, today, false, 24.0);
            // Two reports: spending hours + fully charged
            verify(campaign, times(2)).addReport(eq(GENERAL), anyString());
        }
    }

    /** Tests for {@link CurrentLocation#writeToXML(java.io.PrintWriter, int)}. */
    @Nested
    class WriteToXml {
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
    }
}
