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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;

import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class JumpPathTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);
    private static final double PRIMARY_TRANSIT = 6.0;
    private static final double TARGET_TRANSIT = 9.0;
    private static final double ORIGIN_TRANSIT = 4.0;

    private static PlanetarySystem system(final String id, final double timeToJumpPoint) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getId()).thenReturn(id);
        when(system.getTimeToJumpPoint(1.0)).thenReturn(timeToJumpPoint);
        return system;
    }

    private static JumpPath pathOf(final PlanetarySystem... systems) {
        JumpPath jumpPath = new JumpPath();
        for (PlanetarySystem system : systems) {
            jumpPath.addSystem(system);
        }
        return jumpPath;
    }

    @Test
    void getEndTimeUsesTheDestinationPrimaryWorldByDefault() {
        JumpPath jumpPath = pathOf(system("DEST", PRIMARY_TRANSIT));
        assertEquals(PRIMARY_TRANSIT, jumpPath.getEndTime(), 1.0e-9);
    }

    @Test
    void getEndTimeUsesTheTargetPlanetWhenSet() {
        JumpPath jumpPath = pathOf(system("DEST", PRIMARY_TRANSIT));
        Planet target = mock(Planet.class);
        when(target.getTimeToJumpPoint(1.0)).thenReturn(TARGET_TRANSIT);
        jumpPath.setTargetPlanet(target);

        assertEquals(TARGET_TRANSIT, jumpPath.getEndTime(), 1.0e-9);
    }

    @Test
    void clearingTheTargetPlanetRestoresThePrimaryWorld() {
        JumpPath jumpPath = pathOf(system("DEST", PRIMARY_TRANSIT));
        Planet target = mock(Planet.class);
        when(target.getTimeToJumpPoint(1.0)).thenReturn(TARGET_TRANSIT);

        jumpPath.setTargetPlanet(target);
        jumpPath.setTargetPlanet(null);

        assertEquals(PRIMARY_TRANSIT, jumpPath.getEndTime(), 1.0e-9);
    }

    @Test
    void getEndTimeIsZeroForAnEmptyPath() {
        assertEquals(0.0, new JumpPath().getEndTime(), 1.0e-9);
    }

    @Test
    void getTotalTimeReflectsTheTargetPlanet() {
        PlanetarySystem origin = system("ORIGIN", ORIGIN_TRANSIT);
        PlanetarySystem destination = system("DEST", PRIMARY_TRANSIT);
        JumpPath jumpPath = pathOf(origin, destination);

        // Two-system path: no intermediate recharge, start = origin transit, end = destination transit.
        assertEquals(ORIGIN_TRANSIT + PRIMARY_TRANSIT, jumpPath.getTotalTime(TEST_DATE, 0.0, false), 1.0e-9);

        Planet target = mock(Planet.class);
        when(target.getTimeToJumpPoint(1.0)).thenReturn(TARGET_TRANSIT);
        jumpPath.setTargetPlanet(target);

        assertEquals(ORIGIN_TRANSIT + TARGET_TRANSIT, jumpPath.getTotalTime(TEST_DATE, 0.0, false), 1.0e-9);
    }

    @Test
    void writeToXMLIncludesTheTargetPlanetOnlyWhenSet() {
        JumpPath jumpPath = pathOf(system("DEST", PRIMARY_TRANSIT));
        assertFalse(toXml(jumpPath).contains("targetPlanetId"),
              "A path with no target planet should not write a targetPlanetId element");

        Planet target = mock(Planet.class);
        when(target.getId()).thenReturn("DEST-3");
        jumpPath.setTargetPlanet(target);

        String xml = toXml(jumpPath);
        assertTrue(xml.contains("<targetPlanetId>DEST-3</targetPlanetId>"),
              "A path with a target planet should write its id: " + xml);
    }

    private static String toXml(final JumpPath jumpPath) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            jumpPath.writeToXML(printWriter, 0);
        }
        return stringWriter.toString();
    }
}
