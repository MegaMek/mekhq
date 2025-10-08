/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mekhq.campaign.universe.RegionPerimeter.GrahamScanPointSorter;
import mekhq.campaign.universe.RegionPerimeter.Point;
import org.junit.jupiter.api.Test;

public class RegionPerimeterTest {

    private PlanetarySystem createMockSystem(final double x, final double y) {
        PlanetarySystem mockSystem = mock(PlanetarySystem.class);
        when(mockSystem.getX()).thenReturn(x);
        when(mockSystem.getY()).thenReturn(y);
        when(mockSystem.getId()).thenReturn(x + "," + y);
        return mockSystem;
    }

    @Test
    public void testLeastYSorter() {
        List<Point> list = new ArrayList<>();
        list.add(new Point(-1, 3));
        list.add(new Point(2, -1));
        list.add(new Point(-1, -1));

        list.sort(RegionPerimeter.leastYSorter);

        assertEquals(-1, list.get(0).y(), RegionPerimeter.EPSILON);
        assertEquals(-1, list.get(0).x(), RegionPerimeter.EPSILON);
        assertEquals(2, list.get(1).x(), RegionPerimeter.EPSILON);
    }

    @Test
    public void testVectorCrossProductSameQuadrant() {
        Point origin = new Point(0, 0);
        Point p1 = new Point(1, 1);
        Point p2 = new Point(1, 2);

        assertTrue(RegionPerimeter.vectorCrossProduct(origin, p1, p2) > 0);
        assertTrue(RegionPerimeter.vectorCrossProduct(origin, p2, p1) < 0);
    }

    @Test
    public void testVectorCrossProductDifferentQuadrant() {
        Point origin = new Point(0, 0);
        Point p1 = new Point(1, 1);
        Point p2 = new Point(-1, 2);

        assertTrue(RegionPerimeter.vectorCrossProduct(origin, p1, p2) > 0);
        assertTrue(RegionPerimeter.vectorCrossProduct(origin, p2, p1) < 0);
    }

    @Test
    public void testVectorCrossProductCollinear() {
        Point origin = new Point(0, 0);
        Point p1 = new Point(1, 1);
        Point p2 = new Point(2, 2);

        assertEquals(0, RegionPerimeter.vectorCrossProduct(origin, p1, p2), RegionPerimeter.EPSILON);
    }

    @Test
    public void testGrahamScanPointSorter() {
        Comparator<Point> sorter = new GrahamScanPointSorter(new Point(0, 0));
        List<Point> list = new ArrayList<>();
        Point[] points = new Point[] {
              new Point(1, 0),
              new Point(1, 1),
              new Point(0, 1),
              new Point(-1, 1)
        };
        list.add(points[1]);
        list.add(points[3]);
        list.add(points[0]);
        list.add(points[2]);

        list.sort(sorter);

        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i).x(), points[i].x(), RegionPerimeter.EPSILON);
            assertEquals(list.get(i).y(), points[i].y(), RegionPerimeter.EPSILON);
        }
    }

    @Test
    public void testFindBorderOf3x3Grid() {
        List<PlanetarySystem> list = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                list.add(createMockSystem(x, y));
            }
        }

        RegionPerimeter border = new RegionPerimeter(list);

        for (Point p : border.getVertices()) {
            assertTrue((Math.abs(p.x()) == 1) || (Math.abs(p.y()) == 1));
        }
    }

    @Test
    public void testIsInsideRegion() {
        List<PlanetarySystem> hexagon = new ArrayList<>();
        hexagon.add(createMockSystem(-1, -1));
        hexagon.add(createMockSystem(1, -1));
        hexagon.add(createMockSystem(2, 0));
        hexagon.add(createMockSystem(1, 1));
        hexagon.add(createMockSystem(-1, 1));
        hexagon.add(createMockSystem(-2, 0));
        RegionPerimeter border = new RegionPerimeter(hexagon);

        assertTrue(border.isInsideRegion(new Point(0, 0.5)));
        assertTrue(border.isInsideRegion(new Point(0, -0.5)));
        assertTrue(border.isInsideRegion(new Point(0, 0)));
        assertFalse(border.isInsideRegion(new Point(0, 2)));
        assertFalse(border.isInsideRegion(new Point(0, -2)));
        assertFalse(border.isInsideRegion(new Point(-3, 0)));
        assertFalse(border.isInsideRegion(new Point(3, 0)));
    }

    @Test
    public void testIntersectionTriangleClippedByRectangle() {
        List<Point> triangle = new ArrayList<>();
        triangle.add(new Point(0, 2));
        triangle.add(new Point(-2, -2));
        triangle.add(new Point(2, -2));
        List<Point> rectangle = new ArrayList<>();
        rectangle.add(new Point(-3, 0));
        rectangle.add(new Point(3, 0));
        rectangle.add(new Point(3, 4));
        rectangle.add(new Point(-3, 4));

        List<Point> intersection = RegionPerimeter.intersection(triangle, rectangle);

        assertTrue(intersection.contains(triangle.get(0)));
        assertFalse(intersection.contains(triangle.get(1)));
        assertFalse(intersection.contains(triangle.get(2)));
        assertTrue(intersection.contains(new Point(-1, 0)));
        assertTrue(intersection.contains(new Point(1, 0)));
    }

    @Test
    public void testIntersectionNonOverlappingRegions() {
        List<Point> region1 = new ArrayList<>();
        region1.add(new Point(3, 2));
        region1.add(new Point(1, -2));
        region1.add(new Point(5, -2));
        List<Point> region2 = new ArrayList<>();
        region2.add(new Point(-3, 2));
        region2.add(new Point(-1, -2));
        region2.add(new Point(-5, -2));

        List<Point> intersection = RegionPerimeter.intersection(region1, region2);

        assertTrue(intersection.isEmpty());
    }

    @Test
    public void testScaledIntersection() {
        List<PlanetarySystem> list = new ArrayList<>();
        list.add(createMockSystem(-2, -2));
        list.add(createMockSystem(0, -2));
        list.add(createMockSystem(0, 2));
        list.add(createMockSystem(-2, 2));
        RegionPerimeter r1 = new RegionPerimeter(list);
        list = new ArrayList<>();
        list.add(createMockSystem(2, -2));
        list.add(createMockSystem(0, -2));
        list.add(createMockSystem(0, 2));
        list.add(createMockSystem(2, 2));
        RegionPerimeter r2 = new RegionPerimeter(list);

        List<Point> intersection = r1.intersection(r2, 1.0);

        assertEquals(4, intersection.size());
        assertTrue(intersection.contains(new Point(-1, -3)));
        assertTrue(intersection.contains(new Point(1, -3)));
        assertTrue(intersection.contains(new Point(1, 3)));
        assertTrue(intersection.contains(new Point(-1, 3)));
    }
}
