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
package mekhq.campaign.location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.Warehouse;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Utility for moving groups of {@link ILocation} items (persons, units, parts) to a destination
 * via a shared {@link CurrentLocation} travel node.
 *
 * <p>Items departing from the same system are batched into a single {@code CurrentLocation}
 * so that they travel together. Items already in the destination system are reparented
 * directly without creating a travel node.</p>
 */
public final class LocationDispatch {
    private static final MMLogger LOGGER = MMLogger.create(LocationDispatch.class);

    private LocationDispatch() {}

    /**
     * Dispatches {@code people} to {@code destination}, grouping by departure system so that
     * everyone leaving from the same system shares one {@link CurrentLocation} for the journey.
     *
     * <p>Mirrors the travel logic in {@code EducationController.enrollPerson()}.</p>
     *
     * @param people      the people to dispatch; must not be {@code null}
     * @param destination the target {@link ILocation}; must not be {@code null}
     * @param campaign    the active campaign; must not be {@code null}
     */
    public static void dispatchToLocation(Collection<Person> people,
          ILocation destination,
          Campaign campaign) {

        // Persons who have arrived go into basePersonnel; travel nodes go directly under the base
        // so they can carry units and parts alongside people in the future.
        ILocation arrivalDestination = (destination instanceof AbstractBase base)
                                             ? base.getBasePersonnel()
                                             : destination;

        PlanetarySystem destSystem = destination.getCurrentSystem();

        Map<PlanetarySystem, List<Person>> bySystem = people.stream()
              .collect(Collectors.groupingBy(p -> {
                  PlanetarySystem sys = p.getCurrentSystem();
                  return sys != null ? sys : campaign.getCurrentSystem();
              }));

        for (Map.Entry<PlanetarySystem, List<Person>> entry : bySystem.entrySet()) {
            PlanetarySystem fromSystem = entry.getKey();
            List<Person> group = entry.getValue();

            if (destSystem == null || fromSystem.equals(destSystem)) {
                group.forEach(p -> p.setParent(arrivalDestination));
                continue;
            }

            JumpPath path = campaign.calculateJumpPath(fromSystem, destSystem);
            if (path == null || path.isEmpty()) {
                group.forEach(p -> p.setParent(arrivalDestination));
                continue;
            }

            double startTransit = campaign.getCurrentLocation() != null
                                        ? campaign.getCurrentLocation().getTransitTime()
                  : fromSystem.getTimeToJumpPoint(1.0);

            CurrentLocation travelLoc = new CurrentLocation(fromSystem, startTransit);
            travelLoc.setJumpPath(path);
            if (!travelLoc.setParent(destination)) {
                LOGGER.warn("dispatchToLocation: setParent failed for travelLoc → {}; "
                      + "persons may display as Main Force after save/load",
                      destination.getClass().getSimpleName());
            }
            group.forEach(p -> p.setParent(travelLoc));
            campaign.addLocation(travelLoc);
        }
    }

    /**
     * Dispatches {@code units} to {@code destination}, grouping by departure system so that units leaving from the same
     * system share one {@link CurrentLocation} for the journey.
     *
     * <p>The hangar data structure is updated immediately at dispatch time — units are removed from
     * the campaign hangar and added to the arrival hangar (base or campaign) right away. The {@link LocationNode} tree,
     * however, reflects the journey: cross-system dispatches create a shared {@code CurrentLocation} travel node (child
     * of {@code destination}) so that the unit's location columns show in-transit status until the jump path completes.
     * Same-system dispatches (or dispatches with no calculable path) skip the travel node and land the unit
     * immediately.</p>
     *
     * @param units       the units to dispatch; must not be {@code null}
     * @param destination the target {@link ILocation}; must not be {@code null}
     * @param campaign    the active campaign; must not be {@code null}
     */
    public static void dispatchUnitsToLocation(Collection<Unit> units,
          ILocation destination,
          Campaign campaign) {

        Hangar arrivalHangar = (destination instanceof AbstractBase base)
                                     ? base.getBaseHangar()
                                     : campaign.getHangar();

        PlanetarySystem destSystem = destination.getCurrentSystem();

        Map<PlanetarySystem, List<Unit>> bySystem = units.stream()
                                                          .collect(Collectors.groupingBy(u -> {
                                                              PlanetarySystem sys = u.getCurrentSystem();
                                                              return sys != null ? sys : campaign.getCurrentSystem();
                                                          }));

        for (Map.Entry<PlanetarySystem, List<Unit>> entry : bySystem.entrySet()) {
            PlanetarySystem fromSystem = entry.getKey();
            List<Unit> group = entry.getValue();

            // Move data structure immediately so hangar filters stay correct.
            for (Unit unit : group) {
                campaign.getHangar().removeUnit(unit.getId());
                arrivalHangar.addUnit(unit);
            }

            if (destSystem == null || fromSystem.equals(destSystem)) {
                group.forEach(u -> LocationNode.LocationManager.setLocation(u, arrivalHangar));
                continue;
            }

            JumpPath path = campaign.calculateJumpPath(fromSystem, destSystem);
            if (path == null || path.isEmpty()) {
                group.forEach(u -> LocationNode.LocationManager.setLocation(u, arrivalHangar));
                continue;
            }

            double startTransit = campaign.getCurrentLocation() != null
                                        ? campaign.getCurrentLocation().getTransitTime()
                                        : fromSystem.getTimeToJumpPoint(1.0);

            CurrentLocation travelLoc = new CurrentLocation(fromSystem, startTransit);
            travelLoc.setJumpPath(path);
            if (!travelLoc.setParent(destination)) {
                LOGGER.warn("dispatchUnitsToLocation: setParent failed for travelLoc → {}; "
                      + "units may display as Main Force after save/load",
                      destination.getClass().getSimpleName());
            }
            group.forEach(u -> LocationNode.LocationManager.setLocation(u, travelLoc));
            campaign.addLocation(travelLoc);
        }
    }

    /**
     * Dispatches spare {@code parts} to {@code destination}, grouping by departure system so that parts leaving from
     * the same system share one {@link CurrentLocation} for the journey.
     *
     * <p>The warehouse data structure is updated immediately at dispatch time — parts are removed
     * from the campaign warehouse and added to the arrival warehouse right away. The {@link LocationNode} tree reflects
     * the journey: cross-system dispatches create a shared {@code CurrentLocation} travel node (child of
     * {@code destination}) so that location columns show in-transit status. Same-system dispatches skip the travel
     * node.</p>
     *
     * @param parts       the parts to dispatch; should all be spare parts; must not be {@code null}
     * @param destination the target {@link ILocation}; must not be {@code null}
     * @param campaign    the active campaign; must not be {@code null}
     */
    public static void dispatchPartsToLocation(Collection<Part> parts,
          ILocation destination,
          Campaign campaign) {

        Warehouse arrivalWarehouse = (destination instanceof AbstractBase base)
                                           ? base.getBaseWarehouse()
                                           : campaign.getWarehouse();

        PlanetarySystem destSystem = destination.getCurrentSystem();

        Map<PlanetarySystem, List<Part>> bySystem = parts.stream()
                                                          .collect(Collectors.groupingBy(p -> {
                                                              PlanetarySystem sys = p.getCurrentSystem();
                                                              return sys != null ? sys : campaign.getCurrentSystem();
                                                          }));

        for (Map.Entry<PlanetarySystem, List<Part>> entry : bySystem.entrySet()) {
            PlanetarySystem fromSystem = entry.getKey();
            List<Part> group = entry.getValue();

            // Move data structure immediately so warehouse filters stay correct.
            for (Part part : group) {
                campaign.getWarehouse().removePart(part);
                arrivalWarehouse.addPart(part);
            }

            if (destSystem == null || fromSystem.equals(destSystem)) {
                group.forEach(p -> LocationNode.LocationManager.setLocation(p, arrivalWarehouse));
                continue;
            }

            JumpPath path = campaign.calculateJumpPath(fromSystem, destSystem);
            if (path == null || path.isEmpty()) {
                group.forEach(p -> LocationNode.LocationManager.setLocation(p, arrivalWarehouse));
                continue;
            }

            double startTransit = campaign.getCurrentLocation() != null
                                        ? campaign.getCurrentLocation().getTransitTime()
                                        : fromSystem.getTimeToJumpPoint(1.0);

            CurrentLocation travelLoc = new CurrentLocation(fromSystem, startTransit);
            travelLoc.setJumpPath(path);
            if (!travelLoc.setParent(destination)) {
                LOGGER.warn("dispatchPartsToLocation: setParent failed for travelLoc → {}; "
                      + "parts may display as Main Force after save/load",
                      destination.getClass().getSimpleName());
            }
            group.forEach(p -> LocationNode.LocationManager.setLocation(p, travelLoc));
            campaign.addLocation(travelLoc);
        }
    }

}
