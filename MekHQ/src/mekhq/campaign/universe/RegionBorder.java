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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Given a list of planets, uses a convex hull algorithm to select the planets that form a polygon that
 * completely enclose the space.
 * 
 * @author Neoancient
 *
 */
public class RegionBorder {
    
    private final List<Planet> border;
    
    /**
     * Calculates the border polygon for the list of Planets provided.
     * 
     * @param planets A list of planets that define the region.
     */
    public RegionBorder(List<Planet> planets) {
        border = performGrahamScan(planets);
    }
    
    /**
     * @return A list of planets that mark the vertices of a convex polygon that contains the entire region
     */
    public List<Planet> getVertices() {
        return Collections.unmodifiableList(border);
    }
    
    /**
     * Method to compute the convex hull of a list of planets. Starts by determining the planet
     * with the lowest y coordinate, selecting the lowest x coordinate if there is more than one that
     * shares the lowest y. The remaining planets are sorted according to the angle made by the X axis
     * and a line from the reference point. Planets are then added to a stack in the sorted order. If
     * adding a planet would make a concavity in the polygon, previous planets are popped off the
     * stack until adding the current planet makes a convex angle.
     * 
     * @param planets The list of planets in the region
     * @return        A list of planets whose coordinates define a convex polygon surrounding
     *                all the planets in the list.
     */
    List<Planet> performGrahamScan(List<Planet> planets) {
        Optional<Planet> start = planets.stream().min(rimwardSorter);
        if (!start.isPresent()) {
            return Collections.emptyList();
        }
        final Planet origin = start.get();
        Comparator<Planet> planetSorter = new GrahamScanPlanetSorter(origin);
        List<Planet> sortedPlanets = planets.stream()
                .filter(p -> !p.getId().equals(origin.getId()))
                .sorted(planetSorter)
                .collect(Collectors.toList());
        LinkedList<Planet> stack = new LinkedList<Planet>();
        stack.add(origin);
        if (sortedPlanets.size() > 0) {
            stack.add(sortedPlanets.get(0));
        }
        if (sortedPlanets.size() > 1) {
            stack.add(sortedPlanets.get(1));
        }
        for (int i = 2; i < sortedPlanets.size(); i++) {
            while (vectorCrossProduct(stack.get(stack.size() - 2),
                    stack.getLast(), sortedPlanets.get(i)) <= 0) {
                stack.removeLast();
            }
            stack.add(sortedPlanets.get(i));
        }
        return stack;
    }

    /**
     * Computes the cross product of two vectors from p1 -> p2 and p1 -> p3. This can
     * be used to determine if a path from the origin to p1 to p2 is clockwise ( < 0 ), anticlockwise
     * ( > 0 ) or a straight line ( 0 ).
     * 
     * @param p1 First planet in sequence
     * @param p2 Second planet in sequence
     * @param p3 Third planet in sequence
     * @return   The cross product of the vectors p1->p2 and p1->p3
     */
    static double vectorCrossProduct(Planet p1, Planet p2, Planet p3) {
        return (p2.getX() - p1.getX()) * (p3.getY() - p1.getY())
                - (p2.getY() - p1.getY()) * (p3.getX() - p1.getX());
    }

    /**
     * Sorts planets from lowest Y coordinate to highest. If Y coordinates are equal, sorts
     * from lowest to highest X.
     */
    final static Comparator<Planet> rimwardSorter = (p1, p2) -> {
        int retVal = Double.compare(p1.getY(), p2.getY());
        if (retVal == 0) {
            return Double.compare(p1.getX(), p2.getX());
        }
        return retVal;
    };

    /**
     * Used to sort planets according to their angle from a third planet
     */
    static class GrahamScanPlanetSorter implements Comparator<Planet> {
        private final Planet origin;
        
        GrahamScanPlanetSorter(Planet origin) {
            this.origin = origin;
        }

        @Override
        public int compare(Planet p1, Planet p2) {
            double cp = vectorCrossProduct(origin, p1, p2);
            if (cp > 0) {
                return -1;
            } else if (cp < 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    
}
