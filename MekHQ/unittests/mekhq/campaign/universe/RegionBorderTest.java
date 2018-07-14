package mekhq.campaign.universe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import mekhq.campaign.universe.RegionBorder.GrahamScanPlanetSorter;

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
        List<Planet> list = new ArrayList<>();
        list.add(createMockPlanet(-1, 3));
        list.add(createMockPlanet(2, -1));
        list.add(createMockPlanet(-1, -1));
        
        Collections.sort(list, RegionBorder.rimwardSorter);
        
        assertEquals(list.get(0).getY(), -1, 0.05);
        assertEquals(list.get(0).getX(), -1, 0.05);
        assertEquals(list.get(1).getX(), 2, 0.05);
    }
    
    @Test
    public void testVectorCrossProductSameQuadrant() {
        Planet origin = createMockPlanet(0, 0);
        Planet p1 = createMockPlanet(1, 1);
        Planet p2 = createMockPlanet(1, 2);
        
        assertTrue(RegionBorder.vectorCrossProduct(origin, p1, p2) > 0);
        assertTrue(RegionBorder.vectorCrossProduct(origin, p2, p1) < 0);
    }

    @Test
    public void testVectorCrossProductDifferentQuadrant() {
        Planet origin = createMockPlanet(0, 0);
        Planet p1 = createMockPlanet(1, 1);
        Planet p2 = createMockPlanet(-1, 2);
        
        assertTrue(RegionBorder.vectorCrossProduct(origin, p1, p2) > 0);
        assertTrue(RegionBorder.vectorCrossProduct(origin, p2, p1) < 0);
    }

    @Test
    public void testVectorCrossProductCollinear() {
        Planet origin = createMockPlanet(0, 0);
        Planet p1 = createMockPlanet(1, 1);
        Planet p2 = createMockPlanet(2, 2);
        
        assertEquals(RegionBorder.vectorCrossProduct(origin, p1, p2), 0, 0.05);
    }

    @Test
    public void testGrahamScanPlanetSorter() {
        Comparator<Planet> sorter = new GrahamScanPlanetSorter(createMockPlanet(0, 0));
        List<Planet> list = new ArrayList<>();
        Planet[] mocks = new Planet[] {
            createMockPlanet(1, 0),
            createMockPlanet(1, 1),
            createMockPlanet(0, 1),
            createMockPlanet(-1, 1)
        };
        list.add(mocks[1]);
        list.add(mocks[3]);
        list.add(mocks[0]);
        list.add(mocks[2]);
        
        Collections.sort(list, sorter);
        
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i).getX(), mocks[i].getX(), 0.05);
            assertEquals(list.get(i).getY(), mocks[i].getY(), 0.05);
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
        
        for (Planet p : border.getVertices()) {
            assertTrue((Math.abs(p.getX()) == 1) || (Math.abs(p.getY()) == 1));
        }
    }
}
