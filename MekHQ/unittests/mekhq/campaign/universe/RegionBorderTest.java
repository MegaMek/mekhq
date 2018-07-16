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

public class RegionBorderTest {
    
    private Planet createMockPlanet(final double x, final double y) {
        Planet mockPlanet = mock(Planet.class);
        when(mockPlanet.getX()).thenReturn(x);
        when(mockPlanet.getY()).thenReturn(y);
        when(mockPlanet.getId()).thenReturn(x + "," + y);
        return mockPlanet;
    }
    
    @Test
    public void testRimwardSorter() {
        List<RegionBorder.Point> list = new ArrayList<>();
        list.add(new RegionBorder.Point(-1, 3));
        list.add(new RegionBorder.Point(2, -1));
        list.add(new RegionBorder.Point(-1, -1));
        
        Collections.sort(list, RegionBorder.leastYSorter);
        
        assertEquals(list.get(0).getY(), -1, 0.05);
        assertEquals(list.get(0).getX(), -1, 0.05);
        assertEquals(list.get(1).getX(), 2, 0.05);
    }
    
    @Test
    public void testVectorCrossProductSameQuadrant() {
        RegionBorder.Point origin = new RegionBorder.Point(0, 0);
        RegionBorder.Point p1 = new RegionBorder.Point(1, 1);
        RegionBorder.Point p2 = new RegionBorder.Point(1, 2);
        
        assertTrue(RegionBorder.vectorCrossProduct(origin, p1, p2) > 0);
        assertTrue(RegionBorder.vectorCrossProduct(origin, p2, p1) < 0);
    }

    @Test
    public void testVectorCrossProductDifferentQuadrant() {
        RegionBorder.Point origin = new RegionBorder.Point(0, 0);
        RegionBorder.Point p1 = new RegionBorder.Point(1, 1);
        RegionBorder.Point p2 = new RegionBorder.Point(-1, 2);
        
        assertTrue(RegionBorder.vectorCrossProduct(origin, p1, p2) > 0);
        assertTrue(RegionBorder.vectorCrossProduct(origin, p2, p1) < 0);
    }

    @Test
    public void testVectorCrossProductCollinear() {
        RegionBorder.Point origin = new RegionBorder.Point(0, 0);
        RegionBorder.Point p1 = new RegionBorder.Point(1, 1);
        RegionBorder.Point p2 = new RegionBorder.Point(2, 2);
        
        assertEquals(RegionBorder.vectorCrossProduct(origin, p1, p2), 0, 0.05);
    }

    @Test
    public void testGrahamScanPointSorter() {
        Comparator<RegionBorder.Point> sorter = new RegionBorder.GrahamScanPointSorter(new RegionBorder.Point(0, 0));
        List<RegionBorder.Point> list = new ArrayList<>();
        RegionBorder.Point[] points = new RegionBorder.Point[] {
            new RegionBorder.Point(1, 0),
            new RegionBorder.Point(1, 1),
            new RegionBorder.Point(0, 1),
            new RegionBorder.Point(-1, 1)
        };
        list.add(points[1]);
        list.add(points[3]);
        list.add(points[0]);
        list.add(points[2]);
        
        Collections.sort(list, sorter);
        
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i).getX(), points[i].getX(), 0.05);
            assertEquals(list.get(i).getY(), points[i].getY(), 0.05);
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
        
        RegionBorder border = new RegionBorder(list);
        
        for (RegionBorder.Point p : border.getVertices()) {
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
        RegionBorder border = new RegionBorder(hexagon);

        assertTrue(border.isInsideRegion(new RegionBorder.Point(0, 0.5)));
        assertTrue(border.isInsideRegion(new RegionBorder.Point(0, -0.5)));
        assertTrue(border.isInsideRegion(new RegionBorder.Point(0, 0)));
        assertFalse(border.isInsideRegion(new RegionBorder.Point(0, 2)));
        assertFalse(border.isInsideRegion(new RegionBorder.Point(0, -2)));
        assertFalse(border.isInsideRegion(new RegionBorder.Point(-3, 0)));
        assertFalse(border.isInsideRegion(new RegionBorder.Point(3, 0)));
    }
}
