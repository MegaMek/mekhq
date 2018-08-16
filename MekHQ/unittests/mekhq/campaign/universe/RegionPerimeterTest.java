/*
 * Copyright (c) 2018 - The MegaMek Team
 * 
 * This file is part of MekHQ.
 * 
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

public class RegionPerimeterTest {
    
    private Planet createMockPlanet(final double x, final double y) {
        Planet mockPlanet = mock(Planet.class);
        when(mockPlanet.getX()).thenReturn(x);
        when(mockPlanet.getY()).thenReturn(y);
        when(mockPlanet.getId()).thenReturn(x + "," + y);
        return mockPlanet;
    }
    
    @Test
    public void testLeastYSorter() {
        List<RegionPerimeter.Point> list = new ArrayList<>();
        list.add(new RegionPerimeter.Point(-1, 3));
        list.add(new RegionPerimeter.Point(2, -1));
        list.add(new RegionPerimeter.Point(-1, -1));
        
        Collections.sort(list, RegionPerimeter.leastYSorter);
        
        assertEquals(list.get(0).getY(), -1, RegionPerimeter.EPSILON);
        assertEquals(list.get(0).getX(), -1, RegionPerimeter.EPSILON);
        assertEquals(list.get(1).getX(), 2, RegionPerimeter.EPSILON);
    }
    
    @Test
    public void testVectorCrossProductSameQuadrant() {
        RegionPerimeter.Point origin = new RegionPerimeter.Point(0, 0);
        RegionPerimeter.Point p1 = new RegionPerimeter.Point(1, 1);
        RegionPerimeter.Point p2 = new RegionPerimeter.Point(1, 2);
        
        assertTrue(RegionPerimeter.vectorCrossProduct(origin, p1, p2) > 0);
        assertTrue(RegionPerimeter.vectorCrossProduct(origin, p2, p1) < 0);
    }

    @Test
    public void testVectorCrossProductDifferentQuadrant() {
        RegionPerimeter.Point origin = new RegionPerimeter.Point(0, 0);
        RegionPerimeter.Point p1 = new RegionPerimeter.Point(1, 1);
        RegionPerimeter.Point p2 = new RegionPerimeter.Point(-1, 2);
        
        assertTrue(RegionPerimeter.vectorCrossProduct(origin, p1, p2) > 0);
        assertTrue(RegionPerimeter.vectorCrossProduct(origin, p2, p1) < 0);
    }

    @Test
    public void testVectorCrossProductCollinear() {
        RegionPerimeter.Point origin = new RegionPerimeter.Point(0, 0);
        RegionPerimeter.Point p1 = new RegionPerimeter.Point(1, 1);
        RegionPerimeter.Point p2 = new RegionPerimeter.Point(2, 2);
        
        assertEquals(RegionPerimeter.vectorCrossProduct(origin, p1, p2), 0, RegionPerimeter.EPSILON);
    }

    @Test
    public void testGrahamScanPointSorter() {
        Comparator<RegionPerimeter.Point> sorter = new RegionPerimeter.GrahamScanPointSorter(new RegionPerimeter.Point(0, 0));
        List<RegionPerimeter.Point> list = new ArrayList<>();
        RegionPerimeter.Point[] points = new RegionPerimeter.Point[] {
            new RegionPerimeter.Point(1, 0),
            new RegionPerimeter.Point(1, 1),
            new RegionPerimeter.Point(0, 1),
            new RegionPerimeter.Point(-1, 1)
        };
        list.add(points[1]);
        list.add(points[3]);
        list.add(points[0]);
        list.add(points[2]);
        
        Collections.sort(list, sorter);
        
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i).getX(), points[i].getX(), RegionPerimeter.EPSILON);
            assertEquals(list.get(i).getY(), points[i].getY(), RegionPerimeter.EPSILON);
        }
    }
    
    @Test
    public void testFindBorderOf3x3Grid() {
        List<Planet> list = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                list.add(createMockPlanet(x, y));
            }
        }
        
        RegionPerimeter border = new RegionPerimeter(list);
        
        for (RegionPerimeter.Point p : border.getVertices()) {
            assertTrue((Math.abs(p.getX()) == 1) || (Math.abs(p.getY()) == 1));
        }
    }
    
    @Test
    public void testIsInsideRegion() {
        List<Planet> hexagon = new ArrayList<>();
        hexagon.add(createMockPlanet(-1, -1));
        hexagon.add(createMockPlanet(1, -1));
        hexagon.add(createMockPlanet(2, 0));
        hexagon.add(createMockPlanet(1, 1));
        hexagon.add(createMockPlanet(-1, 1));
        hexagon.add(createMockPlanet(-2, 0));
        RegionPerimeter border = new RegionPerimeter(hexagon);

        assertTrue(border.isInsideRegion(new RegionPerimeter.Point(0, 0.5)));
        assertTrue(border.isInsideRegion(new RegionPerimeter.Point(0, -0.5)));
        assertTrue(border.isInsideRegion(new RegionPerimeter.Point(0, 0)));
        assertFalse(border.isInsideRegion(new RegionPerimeter.Point(0, 2)));
        assertFalse(border.isInsideRegion(new RegionPerimeter.Point(0, -2)));
        assertFalse(border.isInsideRegion(new RegionPerimeter.Point(-3, 0)));
        assertFalse(border.isInsideRegion(new RegionPerimeter.Point(3, 0)));
    }
    
    @Test
    public void testIntersectionTriangleClippedByRectangle() {
        List<RegionPerimeter.Point> triangle = new ArrayList<>();
        triangle.add(new RegionPerimeter.Point(0,  2));
        triangle.add(new RegionPerimeter.Point(-2,  -2));
        triangle.add(new RegionPerimeter.Point(2,  -2));
        List<RegionPerimeter.Point> rectangle = new ArrayList<>();
        rectangle.add(new RegionPerimeter.Point(-3, 0));
        rectangle.add(new RegionPerimeter.Point(3, 0));
        rectangle.add(new RegionPerimeter.Point(3, 4));
        rectangle.add(new RegionPerimeter.Point(-3, 4));
        
        List<RegionPerimeter.Point> intersection = RegionPerimeter.intersection(triangle, rectangle);
        
        assertTrue(intersection.contains(triangle.get(0)));
        assertFalse(intersection.contains(triangle.get(1)));
        assertFalse(intersection.contains(triangle.get(2)));
        assertTrue(intersection.contains(new RegionPerimeter.Point(-1, 0)));
        assertTrue(intersection.contains(new RegionPerimeter.Point(1, 0)));
    }
    
    @Test
    public void testIntersectionNonOverlappingRegions() {
        List<RegionPerimeter.Point> region1 = new ArrayList<>();
        region1.add(new RegionPerimeter.Point(3,  2));
        region1.add(new RegionPerimeter.Point(1,  -2));
        region1.add(new RegionPerimeter.Point(5,  -2));
        List<RegionPerimeter.Point> region2 = new ArrayList<>();
        region2.add(new RegionPerimeter.Point(-3, 2));
        region2.add(new RegionPerimeter.Point(-1, -2));
        region2.add(new RegionPerimeter.Point(-5, -2));
        
        List<RegionPerimeter.Point> intersection = RegionPerimeter.intersection(region1, region2);
        
        assertTrue(intersection.isEmpty());
    }
    
    @Test
    public void testScaledIntersection() {
        List<Planet> list = new ArrayList<>();
        list.add(createMockPlanet(-2, -2));
        list.add(createMockPlanet(0, -2));
        list.add(createMockPlanet(0, 2));
        list.add(createMockPlanet(-2, 2));
        RegionPerimeter r1 = new RegionPerimeter(list);
        list = new ArrayList<>();
        list.add(createMockPlanet(2, -2));
        list.add(createMockPlanet(0, -2));
        list.add(createMockPlanet(0, 2));
        list.add(createMockPlanet(2, 2));
        RegionPerimeter r2 = new RegionPerimeter(list);
        
        List<RegionPerimeter.Point> intersection = r1.intersection(r2, 1.0);
        
        assertEquals(intersection.size(), 4);
        assertTrue(intersection.contains(new RegionPerimeter.Point(-1, -3)));
        assertTrue(intersection.contains(new RegionPerimeter.Point(1, -3)));
        assertTrue(intersection.contains(new RegionPerimeter.Point(1, 3)));
        assertTrue(intersection.contains(new RegionPerimeter.Point(-1, 3)));
    }
}
