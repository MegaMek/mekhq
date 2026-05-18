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
    void getTransportDetailsShowsColumnHeaders() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);

        String report = new TransportReport(campaign).getTransportDetails();
        String headerLine = report.lines().findFirst().orElseThrow();

        assertTrue(headerLine.startsWith("Transport Capacity (Occupied)"));
        assertTrue(headerLine.contains("Total Units (Not Transported)"));
        assertFalse(report.contains("Transports\n"));
        assertFalse(report.contains("(Occupied):"));
        assertFalse(report.contains("Not Transported:"));
    }

    @Test
    void getTransportDetailsShowsHeavyVehiclesPlacedInSuperHeavyBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK)).thenReturn(2);
        when(hangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(3);

        String report = new TransportReport(campaign).getTransportDetails();
        String heavyVehicleLine = lineStartingWith(report, "Heavy Vehicle Bays:");
        String superHeavyVehicleLine = lineStartingWith(report, "Super Heavy Vehicle Bays:");
        String heavyInSuperHeavyVehicleLine = lineStartingWith(report, "   Heavy in Super Heavy Bays:");

        assertTrue(heavyVehicleLine.contains("0 (   0)"));
        assertTrue(heavyVehicleLine.contains("Heavy Vehicles:"));
        assertTrue(heavyVehicleLine.contains("2 (   0)"));
        assertTrue(superHeavyVehicleLine.contains("3 (   2)"));
        assertTrue(superHeavyVehicleLine.contains("Super Heavy Vehicles:"));
        assertTrue(superHeavyVehicleLine.contains("0 (   0)"));
        assertDetailLineHasCountOnly(heavyInSuperHeavyVehicleLine, 2);
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
        String superHeavyVehicleLine = lineStartingWith(report, "Super Heavy Vehicle Bays:");
        String heavyInSuperHeavyVehicleLine = lineStartingWith(report, "   Heavy in Super Heavy Bays:");

        assertTrue(superHeavyVehicleLine.contains("3 (   3)"));
        assertTrue(superHeavyVehicleLine.contains("1 (   0)"));
        assertDetailLineHasCountOnly(heavyInSuperHeavyVehicleLine, 2);
    }

    @Test
    void getTransportDetailsShowsLightBeforeHeavyInSuperHeavyBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true)).thenReturn(1);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK)).thenReturn(1);
        when(hangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(2);

        String report = new TransportReport(campaign).getTransportDetails();

        assertTrue(report.indexOf("   Light in Super Heavy Bays:")
                         < report.indexOf("   Heavy in Super Heavy Bays:"));
    }

    @Test
    void getTransportDetailsShowsLightVehiclesPlacedInHeavyBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true)).thenReturn(2);
        when(hangarStatistics.getTotalHeavyVehicleBays()).thenReturn(5);

        String report = new TransportReport(campaign).getTransportDetails();
        String heavyVehicleLine = lineStartingWith(report, "Heavy Vehicle Bays:");
        String lightVehicleLine = lineStartingWith(report, "Light Vehicle Bays:");
        String lightInHeavyVehicleLine = lineStartingWith(report, "   Light in Heavy Vehicle Bays:");

        assertTrue(heavyVehicleLine.contains("5 (   2)"));
        assertTrue(heavyVehicleLine.contains("0 (   0)"));
        assertTrue(lightVehicleLine.contains("Light Vehicles:"));
        assertTrue(lightVehicleLine.contains("2 (   0)"));
        assertDetailLineHasCountOnly(lightInHeavyVehicleLine, 2);
    }

    @Test
    void getTransportDetailsShowsLightVehiclesPlacedInSuperHeavyBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK, false, true)).thenReturn(3);
        when(hangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(5);

        String report = new TransportReport(campaign).getTransportDetails();
        String superHeavyVehicleLine = lineStartingWith(report, "Super Heavy Vehicle Bays:");
        String lightVehicleLine = lineStartingWith(report, "Light Vehicle Bays:");
        String lightInSuperHeavyVehicleLine = lineStartingWith(report, "   Light in Super Heavy Bays:");

        assertTrue(superHeavyVehicleLine.contains("5 (   3)"));
        assertTrue(lightVehicleLine.contains("3 (   0)"));
        assertDetailLineHasCountOnly(lightInSuperHeavyVehicleLine, 3);
    }

    @Test
    void getTransportDetailsShowsFightersPlacedInSmallCraftBays() {
        Campaign campaign = mock(Campaign.class);
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_AEROSPACE_FIGHTER)).thenReturn(2);
        when(hangarStatistics.getTotalSmallCraftBays()).thenReturn(2);

        String report = new TransportReport(campaign).getTransportDetails();
        String fighterLine = lineStartingWith(report, "Fighter Bays:");
        String smallCraftLine = lineStartingWith(report, "Small Craft Bays:");
        String fightersInSmallCraftLine = lineStartingWith(report, "   Fighters in Small Craft Bays:");

        assertTrue(fighterLine.contains("2 (   0)"));
        assertTrue(smallCraftLine.contains("2 (   2)"));
        assertDetailLineHasCountOnly(fightersInSmallCraftLine, 2);
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
        String superHeavyVehicleLine = lineStartingWith(report, "Super Heavy Vehicle Bays:");

        assertTrue(superHeavyVehicleLine.contains("3 (   1)"));
        assertTrue(superHeavyVehicleLine.contains("1 (   0)"));
    }

    private static String lineStartingWith(String report, String prefix) {
        return report.lines()
                     .filter(line -> line.startsWith(prefix))
                     .findFirst()
                     .orElseThrow();
    }

    private static void assertDetailLineHasCountOnly(String line, int count) {
        String trimmedLine = line.trim();
        String renderedCount = trimmedLine.substring(trimmedLine.lastIndexOf(' ') + 1);

        assertEquals(String.valueOf(count), renderedCount);
        assertFalse(line.contains("("));
        assertFalse(line.contains("Not Transported"));
    }
}
