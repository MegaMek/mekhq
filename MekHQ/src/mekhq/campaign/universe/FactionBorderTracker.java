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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.joda.time.DateTime;

import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.event.LocationChangedEvent;
import mekhq.campaign.event.NewDayEvent;

/**
 * Checks all planets within a given region of space and can report which factions control one or more
 * planets in the region, which planets they control, and which foreign-controlled planets are within a
 * certain distance of a friendly planet. For improved performance the region is defined as a hexagon
 * rather than a circle, with the radius being the distance from the center to each vertex.
 * 
 * Recalculates in the background whenever the date or the bound of the region to examine change. By
 * default queries made while the background thread is working will block until it finishes, but thresholds
 * can be set for a number of days or a distance, any query made while a change falls under that threshold
 * will use the partially updated data.
 * 
 * Changes in campaign date or location will update automatically on each new campaign day if the instance
 * is registered with the event bus.
 * 
 * @author Neoancient
 *
 */
public class FactionBorderTracker {
    
    private final RegionHex regionHex;
    private DateTime lastUpdate;
    private DateTime now;
    
    private Map<Faction, FactionBorders> borders;
    private Map<Faction, Map<Faction, List<Planet>>> borderPlanets;
    
    private double isBorderSize = 60;
    private double peripheryBorderSize = 90;
    private double clanBorderSize = 90;
    private Map<Faction, Double> factionBorderSize = new HashMap<>();;
    
    private int dayThreshold = 0;
    private double distanceThreshold = 0;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean invalid = true;
    private volatile boolean cancelTask = false;

    /**
     * Constructs a FactionBorderTracker with the default region of a 1000 ly radius around Terra.
     */
    public FactionBorderTracker() {
        this(0, 0, 1000);
    }
    
    /**
     * Constructs a FactionBorderTracker with the supplied region limits.
     * 
     * @param x
     * @param y
     * @param radius
     */
    public FactionBorderTracker(double x, double y, double radius) {
        regionHex = new RegionHex(x, y, radius);
        now = new DateTime();
        lastUpdate = now;
        
        borders = new ConcurrentHashMap<>();
        borderPlanets = new ConcurrentHashMap<>();
        recalculate();
    }
    
    /**
     * @return The X coordinate of the bounding hex
     */
    public double getCenterX() {
        return regionHex.center[0];
    }

    /**
     * @return The Y coordinate of the bounding hex
     */
    public double getCenterY() {
        return regionHex.center[1];
    }
    
    /**
     * @return The radius coordinate of the bounding hex (distance from the center to each vertex)
     */
    public double getRadius() {
        return regionHex.radius;
    }

    /**
     * Sets the center of the region's bounding hex and recalculates the faction borders
     * if it has moved.
     * 
     * @param x
     * @param y
     */
    public void setRegionCenter(double x, double y) {
        double distance = regionHex.distanceTo(x, y);
        if (distance > RegionPerimeter.EPSILON) {
            synchronized (this) {
                invalid |= regionHex.distanceTo(x, y) > distanceThreshold;
                regionHex.setCenter(x, y);
                recalculate();
            }
        }
    }
    
    /**
     * Sets the size of the region's bounding hex and recalculates the faction borders if it has changed.
     * Any value less than zero will include the entire map.
     * 
     * @param radius The distance from the center of the hex to each vertex.
     */
    public void setRegionRadius(double radius) {
        double delta = Math.abs(regionHex.radius - radius);
        if (delta > RegionPerimeter.EPSILON) {
            synchronized (this) {
                invalid |= delta > distanceThreshold;
                regionHex.setRadius(radius);
                recalculate();
            }
        }
    }
    
    /**
     * Sets the current date and recalculates the faction borders if it has changed.
     * 
     * @param when The campaign date
     */
    public synchronized void setDate(DateTime when) {
        invalid |= now.plusDays(dayThreshold).isAfter(when)
                || now.minusDays(dayThreshold).isBefore(when);
        now = when;
        recalculate();
    }
    
    /**
     * @return The campaign date of the last completed border calculation
     */
    public synchronized DateTime getLastUpdated() {
        return lastUpdate;
    }
    
    /**
     * Retrieves a {@code Set} of all factions that control at least one planet in the region.
     * 
     * If the borders are being recalculated, this method may block until the calculation is complete.
     * If the change that caused the borders to be recalculated are under the time or distance thresholds,
     * the return value will be current if it has already been calculated, otherwise it will be the value
     * determined by the last completed recalculation.
     * 
     * @return  A {@code Set} of the factions present in the region
     *                              
     * @see #setDayThreshold(int)
     * @see #setDistanceThreshold(double)
     */
            
    public synchronized Set<Faction> getFactionsInRegion() {
        while (invalid) {
            try {
                wait();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return Collections.unmodifiableSet(borders.keySet());
    }

    /**
     * Retrieves a FactionBorders object for the given faction.
     * 
     * If the borders are being recalculated, this method may block until the calculation is complete.
     * If the change that caused the borders to be recalculated are under the time or distance thresholds,
     * the return value will be current if it has already been calculated, otherwise it will be the value
     * determined by the last completed recalculation.
     * recalculation.
     * 
     * @param f A faction
     * @return  A {@link FactionBorders} instance for the faction, or null if the faction does not control
     *          any systems in the region's bounding hex
     *                              
     * @see #setDayThreshold(int)
     * @see #setDistanceThreshold(double)
     */
            
    @Nullable public synchronized FactionBorders getBorders(Faction f) {
        while (invalid) {
            try {
                wait();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return borders.get(f);
    }
    
    /**
     * Retrieves a FactionBorders object for the given faction.
     * 
     * If the borders are being recalculated, this method may block until the calculation is complete.
     * If the change that caused the borders to be recalculated are under the time or distance thresholds,
     * the return value will be current if it has already been calculated, otherwise it will be the value
     * determined by the last completed recalculation.
     * recalculation.
     * 
     * @param f A faction key
     * @return  A {@link FactionBorders} instance for the faction, or null if the faction does not control
     *          any systems in the region's bounding hex or the key is invalid
     *                              
     * @see #setDayThreshold(int)
     * @see #setDistanceThreshold(double)
     */
    @Nullable public synchronized FactionBorders getBorders(String fKey)
            throws InterruptedException {
        while (invalid) {
            try {
                wait();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        Faction f = Faction.getFaction(fKey);
        if (null != f) {
            return borders.get(f);
        }
        return null;
    }
    
    /**
     * Retrieves list of all planets controlled by one faction that are within a set distance of planets
     * controlled by another faction, all within the defined region. The distance used to determine the
     * border size is the larger of {@link #getBorderSize(Faction)} for the two factions.
     * 
     * If the borders are being recalculated, this method may block until the calculation is complete.
     * If the change that caused the borders to be recalculated are under the time or distance thresholds,
     * the return value will be current if it has already been calculated, otherwise it will be the value
     * determined by the last completed recalculation.
     * 
     * @param self  The faction whose planets are used to test proximity
     * @param other The faction whose planets are added to the returned {@code List} if they are within a certain
     *              distance. 
     * @return  A List of all planets in the region that are controlled by {@code other} that are considered
     *          to be within the border region.
     *                              
     * @see #setDayThreshold(int)
     * @see #setDistanceThreshold(double)
     */
    public synchronized List<Planet> getBorderPlanets(Faction self, Faction other) {
        while (invalid) {
            try {
                wait();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        if (borderPlanets.containsKey(self)
                && borderPlanets.get(self).containsKey(other)) {
            return borderPlanets.get(self).get(other);
        }
        return Collections.emptyList();
    }
    
    /**
     * Sets the distance threshold for blocking recalculations. If the size or position of the
     * bounding hex is changed by more than this distance, methods that access calculated border
     * data will block until the calculation is complete. Any distance less than this is considered
     * close enough that the previous data is accurate enough.
     * 
     * @param distance A distance in light years
     */
    public void setDistanceThreshold(double distance) {
        this.distanceThreshold = distance;
    }
    
    /**
     * Retrieves the distance threshold for blocking recalculations. If the size or position of the
     * bounding hex is changed by more than this distance, methods that access calculated border
     * data will block until the calculation is complete. Any distance less than this is considered
     * close enough that the previous data is accurate enough.
     * 
     * @return The distance in light years
     */
    public double getDistanceThreshold() {
        return distanceThreshold;
    }
    
    /**
     * Sets the time threshold for blocking recalculations. If the campaign date changes by more than
     * this in either direction, methods that access calculated border data will block until the
     * calculation is complete. Any distance less than this is considered close enough that the
     * previous data is accurate enough.
     * 
     * @param distance A number of days
     */
    public void setDayThreshold(int days) {
        this.dayThreshold = days;
    }
    
    /**
     * Retrieves the time threshold for blocking recalculations. If the campaign date changes by more than
     * this in either direction, methods that access calculated border data will block until the
     * calculation is complete. Any distance less than this is considered close enough that the
     * previous data is accurate enough.
     * 
     * @param distance A number of days
     */
    public int getDayThreshold() {
        return dayThreshold;
    }
    
    /**
     * The distance from a faction's borders to check for neighboring foreign planets. This defaults
     * to a set value depending on whether the faction is IS, Clan, or Periphery, but can be set
     * for factions individually.
     *  
     * @param f  A faction
     * @return   The distance in light years to look for neighboring foreign planets.
     * 
     * @see #getDefaultBorderSize(Faction)
     */
    public double getBorderSize(Faction f) {
        return factionBorderSize.getOrDefault(f, getDefaultBorderSize(f));
    }
    
    /**
     * The default distance from a faction's borders to check for neighboring foreign planets. This
     * is the value that is used if a specific value has not be set, and is based on whether the
     * faction is IS, Clan, or Periphery.
     * 
     * @param f A faction
     * @return  The distance in light years to look for neighboring foreign planets.
     */
    public double getDefaultBorderSize(Faction f) {
        if (f.isPeriphery()) {
            return peripheryBorderSize;
        } else if (f.isClan()) {
            return clanBorderSize;
        } else {
            return isBorderSize;
        }
    }
    
    /**
     * Sets the distance from a faction's borders to check for neighboring foreign planets. This
     * overrides the default distance for this faction.
     *  
     * @param faction    A faction
     * @param borderSize The distance in light years to look for neighboring foreign planets.
     * 
     * @see #setDefaultBorderSize(double, double, double)
     */
    public void setBorderSize(Faction f, double borderSize) {
        if (borderSize >= 0) {
            factionBorderSize.put(f, borderSize);
        } else {
            factionBorderSize.remove(f);
        }
    }
    
    /**
     * Sets the default border size for IS, Periphery, and Clan factions. This value will be used
     * as the distance from a faction's borders to check for neighboring foreign planets unless a
     * specific value is provided for this faction.
     * 
     * @param is          Default border size for Inner Sphere factions 
     * @param periphery   Default border size for Periphery factions
     * @param clan        Default border size for Clan factions
     */
    public void setDefaultBorderSize(double is, double periphery, double clan) {
        isBorderSize = is;
        peripheryBorderSize = periphery;
        clanBorderSize = clan;
    }
    
    /**
     * Recalculates planetary borders as a background task, after first canceling any that are currently
     * running.
     */
    private void recalculate() {
        cancelTask = true;
        executor.execute(() -> rebuildBorderData());
    }
    
    /**
     * Allows a child class to provide a custom list of planets.
     * 
     * @return A collection of all available planets.
     */
    protected Collection<Planet> getPlanetList() {
        return Planets.getInstance().getPlanets().values();
    }
    
    /**
     * The task that checks all planets within the region and notes which are controlled by which factions
     * and which are within a certain distance of another faction's systems.
     */
    private synchronized void rebuildBorderData() {
        cancelTask = false;
        try {
            List<Planet> planetList = new ArrayList<>();
            Set<Faction> factionSet = new HashSet<>();
            Set<Faction> oldFactions = new HashSet<>(borders.keySet());
            for (Planet planet : getPlanetList()) {
                if ((regionHex.radius < 0)
                        || regionHex.contains(planet.getX(), planet.getY())) {
                    planetList.add(planet);
                    factionSet.addAll(planet.getFactionSet(now));
                }
                if (cancelTask) {
                    return;
                }
            }
            for (Faction f : factionSet) {
                borders.put(f, new FactionBorders(f, now, planetList));
                oldFactions.remove(f);
            }
            for (Faction f : oldFactions) {
                borders.remove(f);
                borderPlanets.remove(f);
            }
            if (cancelTask) {
                return;
            }
            for (Faction us : factionSet) {
                Map<Faction, List<Planet>> borderMap = new HashMap<>();
                for (Faction them : factionSet) {
                    if (!us.equals(them)) {
                        double borderSize = Math.max(getBorderSize(us), getBorderSize(them));
                        List<Planet> planets = borders.get(us).getBorderPlanets(borders.get(them), borderSize);
                        borderMap.put(them, planets);
                    }
                }
                borderPlanets.put(us, borderMap);
                if (cancelTask) {
                    return;
                }
            }
            if (cancelTask) {
                return;
            }
            lastUpdate = now;
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), "recalculate()", ex.getMessage());
        } finally {
            invalid = false;
            notify();
        }
    }
    
    /**
     * If this instance has been registered with the event bus, listens for new day events and
     * starts the recalculation process.
     *  
     * @param event
     */
    @Subscribe
    public synchronized void handleNewDayEvent(NewDayEvent event) {
        now = Utilities.getDateTimeDay(event.getCampaign().getCalendar());
        invalid |= now.minusDays(dayThreshold).isAfter(lastUpdate)
                || now.plusDays(dayThreshold).isBefore(lastUpdate);

        Planet loc = event.getCampaign().getLocation().getCurrentPlanet();
        if (!regionHex.isCenter(loc.getX(), loc.getY())) {
            invalid |= (distanceThreshold > 0)
                    && (regionHex.distanceTo(loc.getX(), loc.getY()) > distanceThreshold);
            regionHex.setCenter(loc.getX(), loc.getY());
        }
        recalculate();
    }
    
    /**
     * If this instance has been registered with the event bus, listens for location change events
     * and starts the recalculation thread.
     *  
     * @param event
     */
    @Subscribe
    public synchronized void handleLocationChangedEvent(LocationChangedEvent ev) {
        if (!ev.isKFJump()) {
            Planet loc = ev.getLocation().getCurrentPlanet();
            if (!regionHex.isCenter(loc.getX(), loc.getY())) {
                invalid |= (distanceThreshold > 0)
                        && (regionHex.distanceTo(loc.getX(), loc.getY()) > distanceThreshold);
                regionHex.setCenter(loc.getX(), loc.getY());
            }
            recalculate();
        }
    }
    
    /**
     * Class for the region bounding hex.
     *
     */
    static class RegionHex {
        private static final int LEFT_MID = 0;
        private static final int LEFT_BOTTOM = 1;
        private static final int RIGHT_BOTTOM = 2;
        private static final int RIGHT_MID = 3;
        private static final int RIGHT_TOP = 4;
        private static final int LEFT_TOP = 5;
        private static final double HEIGHT_FACTOR = Math.sqrt(3.0) / 2;
        
        double[] center;
        double radius;
        double[][] vertices;
        
        RegionHex(double x, double y, double radius) {
            vertices = new double[6][];
            center = new double[] { x, y };
            this.radius = radius;
            calcVertices();
        }
        
        void calcVertices() {
            final double halfRadius = radius * 0.5;
            final double dy = radius * HEIGHT_FACTOR;
            vertices[LEFT_TOP] = new double[] { center[0] - halfRadius, center[1] + dy }; 
            vertices[RIGHT_TOP] = new double[] { center[0] + halfRadius, center[1] + dy }; 
            vertices[LEFT_MID] = new double[] { center[0] - radius, center[1] };
            vertices[RIGHT_MID] = new double[] { center[0] + radius, center[1] };
            vertices[LEFT_BOTTOM] = new double[] { center[0] - halfRadius, center[1] - dy }; 
            vertices[RIGHT_BOTTOM] = new double[] { center[0] + halfRadius, center[1] - dy }; 
        }
        
        void setCenter(double x, double y) {
            center[0] = x;
            center[1] = y;
            calcVertices();
        }
        
        void setRadius(double radius) {
            this.radius = radius;
            calcVertices();
        }
        
        boolean contains(double x, double y) {
            if ((x < vertices[LEFT_MID][0]) || (x > vertices[RIGHT_MID][0])
                    || (y > vertices[LEFT_TOP][1]) || (y < vertices[LEFT_BOTTOM][1])) {
                return false;
            }
            if ((x >= vertices[LEFT_TOP][0]) && (x <= vertices[RIGHT_TOP][0])) {
                return true;
            }
            if (y > center[1]) {
                if (x < center[0]) {
                    return isInside(x, y, vertices[LEFT_TOP], vertices[LEFT_MID]);
                } else {
                    return isInside(x, y, vertices[RIGHT_MID], vertices[RIGHT_TOP]);
                }
            } else {
                if (x < center[0]) {
                    return isInside(x, y, vertices[LEFT_MID], vertices[LEFT_BOTTOM]);
                } else {
                    return isInside(x, y, vertices[RIGHT_BOTTOM], vertices[RIGHT_MID]);
                }
            }
        }
        
        private boolean isInside(double x, double y, double[] p1, double[] p2) {
            return (p2[0] - p1[0]) * (y - p1[1])
                    > (p2[1] - p1[1]) * (x - p1[0]);
        }
        
        double distanceTo(double x, double y) {
            return Math.sqrt(Math.pow(center[0] - x, 2) + Math.pow(center[1] - y, 2));
        }
        
        boolean isCenter(double x, double y) {
            return (Math.abs(vertices[LEFT_MID][1]) - y < 0.001)
                    && (Math.abs((vertices[LEFT_MID][0] + vertices[RIGHT_MID][0]) / 2.0 - x) < 0.001);
        }
    }
}
