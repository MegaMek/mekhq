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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.time.LocalDate;

import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AbstractLocationTest {

    static class ConcreteLocation extends AbstractLocation {
        ConcreteLocation(PlanetarySystem system) {
            super(system);
        }

        @Override
        public void writeToXML(PrintWriter pw, int indent) {}
    }

    PlanetarySystem system;
    AbstractLocation location;

    @BeforeEach
    void setUp() {
        system = mock(PlanetarySystem.class);
        location = new ConcreteLocation(system);
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
    void isOnPlanet_defaultsToTrue() {
        assertTrue(location.isOnPlanet());
    }

    @Test
    void isAtJumpPoint_defaultsToFalse() {
        assertFalse(location.isAtJumpPoint());
    }

    @Test
    void isInTransit_defaultsToFalse() {
        assertFalse(location.isInTransit());
    }

    @Test
    void getPercentageTransit_defaultsToOne() {
        assertEquals(1.0, location.getPercentageTransit());
    }

    @Test
    void isJumpZenith_defaultsToFalse() {
        assertFalse(location.isJumpZenith());
    }

    @Test
    void getTransitTime_defaultsToZero() {
        assertEquals(0.0, location.getTransitTime());
    }

    @Test
    void getJumpPath_defaultsToNull() {
        assertNull(location.getJumpPath());
    }

    @Test
    void setTransitTime_isNoOp() {
        location.setTransitTime(99.0);
        assertEquals(0.0, location.getTransitTime());
    }

    @Test
    void setJumpPath_isNoOp() {
        location.setJumpPath(mock(JumpPath.class));
        assertNull(location.getJumpPath());
    }

    @Test
    void getPlanet_delegatesToPrimaryPlanet() {
        Planet planet = mock(Planet.class);
        when(system.getPrimaryPlanet()).thenReturn(planet);
        assertSame(planet, location.getPlanet());
    }

    /** Tests for {@link AbstractLocation#applyRechargeForHours(Campaign, java.time.LocalDate, boolean, double, boolean)}. */
    @Nested
    class ApplyRechargeForHours {

        Campaign campaign;
        LocalDate today;

        @BeforeEach
        void setUp() {
            campaign = mock(Campaign.class);
            today = LocalDate.of(3025, 1, 1);
        }

        @Test
        void noReportAndZeroReturnedWhenNoRechargeNeeded() {
            when(system.getRechargeTime(today, false)).thenReturn(0.0);

            double used = location.applyRechargeForHours(campaign, today, false, 24.0, false);

            assertEquals(0.0, used);
            verify(campaign, never()).addReport(any(), anyString());
        }

        @Test
        void returnsHoursAvailableAndAddsReportWhenPartialRecharge() {
            when(system.getRechargeTime(today, false)).thenReturn(176.0);

            double used = location.applyRechargeForHours(campaign, today, false, 24.0, false);

            assertEquals(24.0, used);
            verify(campaign).addReport(eq(GENERAL), anyString());
        }

        @Test
        void returnsOnlyNeededHoursWhenAvailableExceedsNeeded() {
            // neededRechargeTime = 10, availableHours = 24 → should only use 10
            // AbstractLocation.getRechargeTime() is always 0, so usedTime = min(24, 10-0) = 10
            when(system.getRechargeTime(today, false)).thenReturn(10.0);

            double used = location.applyRechargeForHours(campaign, today, false, 24.0, false);

            assertEquals(10.0, used);
        }

        @Test
        void noReportWhenSuppressed() {
            when(system.getRechargeTime(today, false)).thenReturn(176.0);

            location.applyRechargeForHours(campaign, today, false, 24.0, true);

            verify(campaign, never()).addReport(any(), anyString());
        }
    }
}
