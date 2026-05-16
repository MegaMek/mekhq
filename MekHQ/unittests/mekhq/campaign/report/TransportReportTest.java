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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.HangarStatistics;
import org.junit.jupiter.api.Test;

class TransportReportTest {
    @Test
    void getTransportDetailsShowsHeavyVehiclesPlacedInSuperHeavyBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK)).thenReturn(2);
        when(hangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(3);

        String report = new TransportReport(campaign).getTransportDetails();
        String heavyVehicleLine = report.lines()
                                        .filter(line -> line.startsWith("Heavy Vehicle Bays (Occupied):"))
                                        .findFirst()
                                        .orElseThrow();
        String superHeavyVehicleLine = report.lines()
                                             .filter(line -> line.startsWith("Super Heavy Vehicle Bays (Occupied):"))
                                             .findFirst()
                                             .orElseThrow();
        String heavyInSuperHeavyVehicleLine = report.lines()
                                                       .filter(line -> line.startsWith("   Heavy in Super Heavy Bays:"))
                                                       .findFirst()
                                                       .orElseThrow();

        assertTrue(report.contains("Super Heavy Vehicle Bays (Occupied):"));
        assertTrue(report.contains("Heavy in Super Heavy Bays:"));
        assertTrue(heavyVehicleLine.endsWith("   0"));
        assertTrue(superHeavyVehicleLine.contains("3 (   2)"));
        assertEquals(heavyVehicleLine.indexOf("0 ("), superHeavyVehicleLine.indexOf("3 ("));
        assertEquals(heavyVehicleLine.lastIndexOf('0'), superHeavyVehicleLine.lastIndexOf('0'));
        assertTrue(heavyInSuperHeavyVehicleLine.matches(".*\\s2"));
        assertFalse(heavyInSuperHeavyVehicleLine.contains("("));
        assertFalse(heavyInSuperHeavyVehicleLine.contains("Not Transported"));
        assertFalse(report.contains("will be placed"));
    }

    @Test
    void getTransportDetailsSeparatesSuperHeavyOccupantsFromHeavyOverflow() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK)).thenReturn(2);
        when(hangarStatistics.getNumberOfSuperHeavyVehicles()).thenReturn(1);
        when(hangarStatistics.getOccupiedSuperHeavyVehicleBays()).thenReturn(1);
        when(hangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(3);

        String report = new TransportReport(campaign).getTransportDetails();
        String superHeavyVehicleLine = report.lines()
                                             .filter(line -> line.startsWith("Super Heavy Vehicle Bays (Occupied):"))
                                             .findFirst()
                                             .orElseThrow();
        String heavyInSuperHeavyVehicleLine = report.lines()
                                                       .filter(line -> line.startsWith("   Heavy in Super Heavy Bays:"))
                                                       .findFirst()
                                                       .orElseThrow();

        assertTrue(superHeavyVehicleLine.contains("3 (   3)"));
        assertTrue(heavyInSuperHeavyVehicleLine.matches(".*\\s2"));
        assertFalse(heavyInSuperHeavyVehicleLine.contains("("));
        assertFalse(heavyInSuperHeavyVehicleLine.contains("Not Transported"));
    }

    @Test
    void getTransportDetailsShowsLightVehiclesPlacedInHeavyBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true)).thenReturn(2);
        when(hangarStatistics.getTotalHeavyVehicleBays()).thenReturn(5);

        String report = new TransportReport(campaign).getTransportDetails();
        String heavyVehicleLine = report.lines()
                        .filter(line -> line.startsWith("Heavy Vehicle Bays (Occupied):"))
                        .findFirst()
                        .orElseThrow();
        String lightInHeavyVehicleLine = report.lines()
                                                .filter(line -> line.startsWith("   Light in Heavy Vehicle Bays:"))
                                                .findFirst()
                                                .orElseThrow();

        assertTrue(heavyVehicleLine.contains("5 (   2)"));
        assertTrue(heavyVehicleLine.endsWith("   0"));
        assertTrue(lightInHeavyVehicleLine.matches(".*\\s2"));
        assertFalse(lightInHeavyVehicleLine.contains("("));
        assertFalse(lightInHeavyVehicleLine.contains("Not Transported"));
    }

    @Test
    void getTransportDetailsShowsLightVehiclesPlacedInSuperHeavyBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true)).thenReturn(3);
        when(hangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(5);

        String report = new TransportReport(campaign).getTransportDetails();
        String lightInSuperHeavyVehicleLine = report.lines()
                                                     .filter(line -> line.startsWith("   Light in Super Heavy Bays:"))
                                                     .findFirst()
                                                     .orElseThrow();

        assertTrue(lightInSuperHeavyVehicleLine.matches(".*\\s3"));
        assertFalse(lightInSuperHeavyVehicleLine.contains("("));
        assertFalse(lightInSuperHeavyVehicleLine.contains("Not Transported"));
    }

    @Test
    void getTransportDetailsShowsSuperHeavyVehiclesInSuperHeavyBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfSuperHeavyVehicles()).thenReturn(1);
        when(hangarStatistics.getOccupiedSuperHeavyVehicleBays()).thenReturn(1);
        when(hangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(3);

        String report = new TransportReport(campaign).getTransportDetails();
        String superHeavyVehicleLine = report.lines()
                                             .filter(line -> line.startsWith("Super Heavy Vehicle Bays (Occupied):"))
                                             .findFirst()
                                             .orElseThrow();

        assertTrue(superHeavyVehicleLine.contains("3 (   1)"));
        assertTrue(superHeavyVehicleLine.endsWith("   0"));
    }
}