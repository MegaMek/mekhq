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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;

/**
 * Finds all planets controlled by a given faction at a particular date and can find all planets
 * controlled by another faction within a set distance.
 * 
 * @author Neoancient
 *
 */
public class FactionBorders {
    
    private final Faction faction;
    private Set<Planet> planets;
    private RegionPerimeter border;
    
    /**
     * Creates a FactionBorders object for a faction using all the known planets.
     * 
     * @param faction The faction to calculate the border for
     * @param when    The date to use to determine planet control.
     */
    public FactionBorders(Faction faction, DateTime when) {
        this.faction = faction;
        calculateRegion(when);
    }
    
    /**
     * Creates a FactionBorders object for a faction using a particular set of planets
     * 
     * @param faction The faction to calculate the border for
     * @param when    The date to use to determine planet control.
     * @param region  A collection of planets within a region of space.
     */
    public FactionBorders(Faction faction, DateTime when, Collection<Planet> region) {
        this.faction = faction;
        calculateRegion(when, region);
    }
    
    /**
     * Finds all planets currently owned (completely or partially) by the faction and finds
     * its border.
     * 
     * @param when The date for testing faction ownership.
     */
    public void calculateRegion(DateTime when) {
        calculateRegion(when, Planets.getInstance().getPlanets().values());
    }
    
    /**
     * Finds all planets within the supplied collection currently owned (completely or partially)
     * by the faction and finds its border. This can be used to narrow the area to one section of
     * the galaxy.
     * 
     * @param when    The date for testing faction ownership.
     * @param planets The set of planets to include in the region.
     */
    public void calculateRegion(DateTime when, Collection<Planet> planets) {
        this.planets = planets.stream()
                .filter(p -> p.getFactionSet(when).contains(faction))
                .collect(Collectors.toSet());
        border = new RegionPerimeter(planets);
    }
    
    /**
     * @return The faction used to calculate the borders
     */
    public Faction getFaction() {
        return faction;
    }
    
    /**
     * @return The planets controlled by the indicated faction at the indicated time.
     */
    public Set<Planet> getPlanets() {
        return Collections.unmodifiableSet(planets);
    }
    
    /**
     * @return A polygon that surrounds the region controlled by this faction.
     */
    public RegionPerimeter getBorder() {
        return border;
    }
    
    /**
     * Finds planets of another faction that are on a border with this faction.
     * 
     * @param other       The other faction's region.
     * @param borderSize  The size of the border.
     * @return            All planets from the other faction that are within borderSize light years
     *                    of one of this faction's planets.
     */
    List<Planet> getBorderPlanets(FactionBorders other, double borderSize) {
        List<RegionPerimeter.Point> intersection = border.intersection(other.getBorder(), borderSize);
        if (intersection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Planet> theirPlanets = other.getPlanets().stream()
                .filter(p -> RegionPerimeter.isInsideRegion(p.getX(), p.getY(), intersection))
                .sorted((p1, p2) -> Double.compare(p1.getX(), p2.getX()))
                .collect(Collectors.toList());
        List<Planet> ourPlanets = getPlanets().stream()
                .filter(p -> RegionPerimeter.isInsideRegion(p.getX(), p.getY(), intersection))
                .sorted((p1, p2) -> Double.compare(p1.getX(), p2.getX()))
                .collect(Collectors.toList());
        
        List<Planet> retVal = new ArrayList<>();
        int start = 0;
        for (Planet p : theirPlanets) {
            // As we work through the list of potential enemy planets we move the start index
            // of the friendly planets so we don't have to iterate over the ones out of range.
            while ((start < ourPlanets.size()) && (p.getX() > ourPlanets.get(start).getX() + borderSize)) {
                start++;
            }
            if (start >= ourPlanets.size()) {
                break;
            }
            for (int i = start; i < ourPlanets.size(); i++) {
                final Planet p2 = ourPlanets.get(i);
                if (p.getX() < p2.getX() - borderSize) {
                    break;
                }
                // We're going to do a cheap bounding rectangle check first to determine whether
                // the more computationally expensive distance calculation is even necessary
                if ((p2.getY() > p.getY() - borderSize)
                        && (p2.getY() < p.getY() + borderSize)
                        && (p.getDistanceTo(p2) <= borderSize)) {
                    retVal.add(p);
                }
            }
        }
        
        return retVal;
    }

}
