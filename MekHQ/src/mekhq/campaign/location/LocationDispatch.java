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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
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
     * Returns the transit time from {@code fromSystem} that should be used as the start-transit
     * parameter when calculating a new journey's duration.
     *
     * @param fromSystem the departure system; must not be {@code null}
     * @param campaign   the active campaign; must not be {@code null}
     * @return transit time in days
     */
    public static double computeStartTransit(PlanetarySystem fromSystem, Campaign campaign) {
        return campaign.getCurrentLocation() != null
                     ? campaign.getCurrentLocation().getTransitTime()
                     : fromSystem.getTimeToJumpPoint(1.0);
    }

    /**
     * Computes the total journey duration in days from a {@link JumpPath}, applying a minimum of 2 days.
     *
     * @param path         the route to calculate; must not be {@code null}
     * @param date         the in-game date (used for pirate-point availability); must not be {@code null}
     * @param startTransit the already-elapsed transit time at the origin, in days
     *
     * @return journey length in whole days, always at least 2
     */
    public static int computeJourneyDays(JumpPath path, LocalDate date, double startTransit) {
        return Math.max(2, (int) Math.ceil(path.getTotalTime(date, startTransit, false)));
    }

    /**
     * Removes a completed travel node from the campaign: detaches it from the location tree and
     * de-registers it from the campaign's location list.
     *
     * <p>Safe to call with a {@code null} argument (no-op).</p>
     *
     * @param travelNode the node to remove, or {@code null}
     * @param campaign   the active campaign; must not be {@code null}
     */
    public static void removeTravelNode(@Nullable CurrentLocation travelNode, Campaign campaign) {
        if (travelNode == null) {
            return;
        }
        travelNode.setParent(null);
        campaign.removeLocation(travelNode);
    }

    /**
     * Completes the arrival of all persons, units, and parts carried by a finished travel node,
     * then removes the node from the location tree and campaign registry.
     *
     * <p>Persons are reparented to {@code personDestination}. Units and parts have their
     * {@link LocationNode} moved to {@code unitDestination} and {@code partDestination} respectively; their
     * hangar and warehouse data structures are assumed to have already been updated at dispatch
     * time.</p>
     *
     * @param travelLoc  the completed travel node; must not be {@code null}
     * @param personDestination destination container for persons; if {@code null}, persons fall back to
     *                   {@code campaign} with a warning
     * @param unitDestination   destination container for units; if {@code null}, units fall back to
     *                   {@code campaign} with a warning
     * @param partDestination   destination container for parts; if {@code null}, parts fall back to
     *                   {@code campaign} with a warning
     * @param campaign   the active campaign; must not be {@code null}
     */
    public static void landFromTravelNode(CurrentLocation travelLoc,
          @Nullable ILocation personDestination,
          @Nullable ILocation unitDestination,
          @Nullable ILocation partDestination,
          Campaign campaign) {
        if (personDestination == null) {
            LOGGER.warn("landFromTravelNode: null personDestination; landing persons at Campaign root");
        }
        if (unitDestination == null) {
            LOGGER.warn("landFromTravelNode: null unitDestination; landing units at Campaign root");
        }
        if (partDestination == null) {
            LOGGER.warn("landFromTravelNode: null partDestination; landing parts at Campaign root");
        }
        landFromTravelNodeImpl(travelLoc,
              personDestination != null ? personDestination : campaign,
              unitDestination != null ? unitDestination : campaign,
              partDestination != null ? partDestination : campaign,
              campaign);
    }

    private static void landFromTravelNodeImpl(CurrentLocation travelLoc,
          ILocation personDestination,
          ILocation unitDestination,
          ILocation partDestination,
          Campaign campaign) {
        for (Person person : new ArrayList<>(travelLoc.fetchPersonnelAtLocation())) {
            person.setParent(personDestination);
        }
        for (Unit unit : new ArrayList<>(travelLoc.fetchUnitsAtLocation())) {
            LocationNode.LocationManager.setLocation(unit, unitDestination);
        }
        for (Part part : new ArrayList<>(travelLoc.fetchPartsAtLocation())) {
            LocationNode.LocationManager.setLocation(part, partDestination);
        }
        removeTravelNode(travelLoc, campaign);
    }

    /**
     * Builds a cross-system travel node from {@code fromSystem} toward {@code destinationSystem}, parented under
     * {@code destination}, and registers it with the campaign.
     *
     * <p>Returns an empty {@link Optional} (without modifying state) when no path exists or the
     * path is empty — callers should fall back to direct placement in that case.</p>
     */
    private static Optional<CurrentLocation> buildTravelNode(PlanetarySystem fromSystem,
          PlanetarySystem destinationSystem,
          ILocation destination,
          Campaign campaign,
          String logContext) {
        JumpPath path = campaign.calculateJumpPath(fromSystem, destinationSystem);
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        double startTransit = computeStartTransit(fromSystem, campaign);
        CurrentLocation travelLoc = new CurrentLocation(fromSystem, startTransit);
        travelLoc.setJumpPath(path);
        if (!travelLoc.setParent(destination)) {
            LOGGER.warn("{}: setParent failed for travelLoc → {}; "
                              + "items may display as Main Force after save/load",
                  logContext, destination.getClass().getSimpleName());
        }
        campaign.addLocation(travelLoc);
        return Optional.of(travelLoc);
    }

    /**
     * Dispatches {@code people} to {@code destination}, grouping by departure system so that
     * everyone leaving from the same system shares one {@link CurrentLocation} for the journey.
     *
     * @param people      the persons to dispatch; must not be {@code null}
     * @param destination the target {@link ILocation}; must not be {@code null}
     * @param campaign    the active campaign; must not be {@code null}
     */
    public static void dispatchToLocation(Collection<Person> people,
          ILocation destination,
          Campaign campaign) {

        PlanetarySystem destinationSystem = destination.getCurrentSystem();

        Map<PlanetarySystem, List<Person>> bySystem = people.stream()
              .collect(Collectors.groupingBy(p -> {
                  PlanetarySystem system = p.getCurrentSystem();
                  return system != null ? system : campaign.getCurrentSystem();
              }));

        for (Map.Entry<PlanetarySystem, List<Person>> entry : bySystem.entrySet()) {
            PlanetarySystem fromSystem = entry.getKey();
            List<Person> group = entry.getValue();

            if (destinationSystem == null || fromSystem.equals(destinationSystem)) {
                group.forEach(p -> p.setParent(destination));
                continue;
            }

            Optional<CurrentLocation> maybeTravelLoc = buildTravelNode(
                  fromSystem, destinationSystem, destination, campaign, "dispatchToLocation");
            if (maybeTravelLoc.isEmpty()) {
                group.forEach(p -> p.setParent(destination));
                continue;
            }
            group.forEach(p -> p.setParent(maybeTravelLoc.get()));
        }
    }

}
