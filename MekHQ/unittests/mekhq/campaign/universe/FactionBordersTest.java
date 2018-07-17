package mekhq.campaign.universe;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import mekhq.campaign.Campaign;

public class FactionBordersTest {
    
    private ConcurrentMap<String, Planet> planetList;
    @Mock private boolean initialized = true;
    @InjectMocks private Planets planets;
    
    private Faction factionUs;
    private Faction factionThem;
    
    @Before
    public void init() {
        planetList = new ConcurrentHashMap<>();
        factionUs = createFaction("us", false);
        factionThem = createFaction("them", false);
        planets = mock(Planets.class);
        when (planets.getPlanets()).thenReturn(planetList);
        
        try {
            Field field = Planets.class.getDeclaredField("planets");
            boolean isAccessible = field.isAccessible();
            field.setAccessible(true);
            field.set(null, planets);
            field.setAccessible(isAccessible);
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private Faction createFaction(final String key, final boolean periphery) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(key);
        when(faction.isPeriphery()).thenReturn(periphery);
        return faction;
    }
    
    private Planet createPlanet(final double x, final double y, Faction owner) {
        Planet planet = mock(Planet.class);
        when(planet.getX()).thenReturn(x);
        when(planet.getY()).thenReturn(y);
        String id = String.format("%f, %f", x, y);
        when(planet.getId()).thenReturn(id);
        when(planet.getFactionSet(any())).thenReturn(Collections.singleton(owner));
        planetList.put(id, planet);
        return planet;
    }

    @Test
    public void testGetBorderPlanetsFactionBorders() {
        Campaign c = mock(Campaign.class);
        GregorianCalendar calendar = new GregorianCalendar();
        when(c.getCalendar()).thenReturn(calendar);
        for (int x = -3; x <= 3; x += 2) {
            for (int y = -2; y <= 2; y += 2) {
                createPlanet(x, y, factionThem);
            }
        }
        createPlanet(0, 0, factionUs);
        FactionBorders us = new FactionBorders(factionUs, c);
        FactionBorders them = new FactionBorders(factionThem, c);
        
        List<Planet> border = us.getBorderPlanets(them, 1.1);
        
        assertEquals(border.size(), 2);
        for (Planet p : border) {
            assertEquals(Math.abs(p.getX()), 1, 0.001);
            assertEquals(p.getY(), 0, 0.001);
        }
    }

}
