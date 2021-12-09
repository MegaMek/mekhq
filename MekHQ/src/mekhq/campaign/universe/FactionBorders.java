/*
 * Copyright (c) 2018 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds all planets controlled by a given faction at a particular date and can find all planets
 * controlled by another faction within a set distance.
 *
 * @author Neoancient
 */
public class FactionBorders {
    private final Faction faction;
    private Set<PlanetarySystem> systems;
    private RegionPerimeter border;

    /**
     * Creates a FactionBorders object for a faction using all the known planets.
     *
     * @param faction The faction to calculate the border for
     * @param when    The date to use to determine planet control.
     */
    public FactionBorders(Faction faction, LocalDate when) {
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
    public FactionBorders(Faction faction, LocalDate when, Collection<PlanetarySystem> region) {
        this.faction = faction;
        calculateRegion(when, region);
    }

    /**
     * Finds all planets currently owned (completely or partially) by the faction and finds
     * its border.
     *
     * @param when The date for testing faction ownership.
     */
    public void calculateRegion(LocalDate when) {
        calculateRegion(when, Systems.getInstance().getSystems().values());
    }

    /**
     * Finds all planets within the supplied collection currently owned (completely or partially)
     * by the faction and finds its border. This can be used to narrow the area to one section of
     * the galaxy.
     *
     * @param when    The date for testing faction ownership.
     * @param systems The set of <code>planetarySystem</code>'s to include in the region.
     */
    public void calculateRegion(LocalDate when, Collection<PlanetarySystem> systems) {
        this.systems = systems.stream()
                .filter(p -> p.getFactionSet(when).contains(faction))
                .collect(Collectors.toSet());
        border = new RegionPerimeter(systems);
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
    public Set<PlanetarySystem> getSystems() {
        return Collections.unmodifiableSet(systems);
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
    List<PlanetarySystem> getBorderSystems(FactionBorders other, double borderSize) {
        List<RegionPerimeter.Point> intersection = border.intersection(other.getBorder(), borderSize);
        if (intersection.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlanetarySystem> theirSystems = other.getSystems().stream()
                .filter(p -> RegionPerimeter.isInsideRegion(p.getX(), p.getY(), intersection))
                .sorted(Comparator.comparingDouble(PlanetarySystem::getX))
                .collect(Collectors.toList());
        List<PlanetarySystem> ourSystems = getSystems().stream()
                .filter(p -> RegionPerimeter.isInsideRegion(p.getX(), p.getY(), intersection))
                .sorted(Comparator.comparingDouble(PlanetarySystem::getX))
                .collect(Collectors.toList());

        List<PlanetarySystem> retVal = new ArrayList<>();
        int start = 0;
        for (PlanetarySystem p : theirSystems) {
            // As we work through the list of potential enemy planets we move the start index
            // of the friendly planets so we don't have to iterate over the ones out of range.
            while ((start < ourSystems.size()) && (p.getX() > ourSystems.get(start).getX() + borderSize)) {
                start++;
            }
            if (start >= ourSystems.size()) {
                break;
            }
            for (int i = start; i < ourSystems.size(); i++) {
                final PlanetarySystem p2 = ourSystems.get(i);
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
