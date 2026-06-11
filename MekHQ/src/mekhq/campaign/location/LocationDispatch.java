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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
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

    private static final String LOG_DISPATCH_PERSONS = "dispatchToLocation";
    private static final String LOG_DISPATCH_UNITS = "dispatchUnitsToLocation";
    private static final String LOG_DISPATCH_PARTS = "dispatchPartsToLocation";

    private LocationDispatch() {}

    /**
     * Returns the transit time from {@code fromSystem} that should be used as the start-transit
     * parameter when calculating a new journey's duration.
     *
     * <p>When already in-system, we inherit its current transit progress.
     * When there is no location, we assume the
     * traveler starts at the outer jump point of their system.</p>
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
     * Computes the total journey duration in days from a {@link JumpPath}, applying a minimum of
     * 2 days.
     *
     * @param path         the route to calculate; must not be {@code null}
     * @param date         the in-game date (used for pirate-point availability); must not be {@code null}
     * @param startTransit the already-elapsed transit time at the origin, in days
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
     * @param travelNode        the completed travel node; must not be {@code null}
     * @param personDestination destination container for persons; if {@code null}, persons fall back to
     *                          {@code campaign} with a warning
     * @param unitDestination   destination container for units; if {@code null}, units fall back to
     *                          {@code campaign} with a warning
     * @param partDestination   destination container for parts; if {@code null}, parts fall back to
     *                          {@code campaign} with a warning
     * @param campaign          the active campaign; must not be {@code null}
     */
    public static void landFromTravelNode(CurrentLocation travelNode,
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
        landFromTravelNodeImpl(travelNode,
              personDestination != null ? personDestination : campaign,
              unitDestination != null ? unitDestination : campaign,
              partDestination != null ? partDestination : campaign,
              campaign);
    }

    private static void landFromTravelNodeImpl(CurrentLocation travelNode,
          ILocation personDestination,
          ILocation unitDestination,
          ILocation partDestination,
          Campaign campaign) {
        for (Person person : new ArrayList<>(travelNode.fetchPersonnelAtLocation())) {
            person.setParent(personDestination);
        }
        for (Unit unit : new ArrayList<>(travelNode.fetchUnitsAtLocation())) {
            LocationNode.LocationManager.setLocation(unit, unitDestination);
        }
        for (Part part : new ArrayList<>(travelNode.fetchPartsAtLocation())) {
            LocationNode.LocationManager.setLocation(part, partDestination);
        }
        removeTravelNode(travelNode, campaign);
    }

    /**
     * Builds a zero-transit "arrived" node parented under {@code destination} and registers it
     * with the campaign. The node has {@code transitTime = 0}, so {@code isOnPlanet()} is
     * immediately {@code true} and {@link IPlace#processArrivals} will land all carried items on
     * its next call.
     */
    private static CurrentLocation buildArrivedNode(PlanetarySystem system,
          ILocation destination,
          Campaign campaign,
          String logContext) {
        CurrentLocation arrivedLocation = new CurrentLocation(system, 0.0);
        if (!arrivedLocation.setParent(destination)) {
            LOGGER.warn("{}: setParent failed for arrivedLocation → {}",
                  logContext, destination.getClass().getSimpleName());
        }
        campaign.addLocation(arrivedLocation);
        return arrivedLocation;
    }

    /**
     * Builds a cross-system travel node from {@code fromSystem} toward {@code destinationSystem},
     * parented under {@code destination}, and registers it with the campaign.
     *
     * <p>Returns an empty {@link Optional} (without modifying state) when no path exists or the
     * path is empty — callers should fall back to direct placement in that case.</p>
     */
    private static Optional<CurrentLocation> buildTravelNode(PlanetarySystem fromSystem,
          PlanetarySystem destinationSystem, ILocation destination, Campaign campaign, String logContext) {
        JumpPath path = campaign.calculateJumpPath(fromSystem, destinationSystem);
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        double startTransit = computeStartTransit(fromSystem, campaign);
        CurrentLocation travelNode = new CurrentLocation(fromSystem, startTransit);
        travelNode.setJumpPath(path);
        if (!travelNode.setParent(destination)) {
            LOGGER.warn("{}: setParent failed for travelNode → {}; "
                  + "items may display as Main Force after save/load",
                  logContext, destination.getClass().getSimpleName());
        }
        campaign.addLocation(travelNode);
        return Optional.of(travelNode);
    }

    /**
     * Dispatches {@code people} to {@code destination}, grouping by departure system so that
     * everyone leaving from the same system shares one {@link CurrentLocation} for the journey.
     *
     * @param people      the persons to dispatch; must not be {@code null}
     * @param destination the target {@link ILocation}; must not be {@code null}
     * @param campaign    the active campaign; must not be {@code null}
     */
    public static void dispatchToLocation(Collection<Person> people, ILocation destination, Campaign campaign) {
        dispatch(people, destination, campaign, LOG_DISPATCH_PERSONS, destination, null);
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
        Warehouse arrivalWarehouse = (destination instanceof AbstractBase base)
              ? base.getBaseWarehouse()
              : campaign.getWarehouse();

        // Move data structures immediately so hangar and warehouse filters stay correct.
        dispatch(units, destination, campaign, LOG_DISPATCH_UNITS, arrivalHangar, group -> {
            for (Unit unit : group) {
                Hangar sourceHangar = unit.getHangar();
                (sourceHangar != null ? sourceHangar : campaign.getHangar()).removeUnit(unit.getId());
                arrivalHangar.addUnit(unit);
                // Installed parts live in the warehouse local to their unit; move them along.
                for (Part part : unit.getParts()) {
                    Warehouse sourceWarehouse = part.getWarehouse();
                    if (sourceWarehouse != arrivalWarehouse) {
                        (sourceWarehouse != null ? sourceWarehouse : campaign.getWarehouse()).removePart(part);
                        arrivalWarehouse.addPart(part);
                    }
                }
            }
        });
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

        // Move the data structure immediately so warehouse filters stay correct.
        dispatch(parts, destination, campaign, LOG_DISPATCH_PARTS, arrivalWarehouse, group -> {
            for (Part part : group) {
                Warehouse sourceWarehouse = part.getWarehouse();
                (sourceWarehouse != null ? sourceWarehouse : campaign.getWarehouse()).removePart(part);
                arrivalWarehouse.addPart(part);
            }
        });
    }

    /**
     * Shared dispatch loop for persons, units, and parts.
     *
     * <p>Groups {@code items} by departure system. Each group's containers are moved first (via
     * {@code moveContainers}, when the item type owns one), then the group either lands
     * immediately (same system, or no calculable jump path) or is parented under a shared
     * {@link CurrentLocation} travel node. All reparenting goes through the validated
     * {@link ILocation#setParent} path; rejected moves are logged rather than silently skipped.</p>
     *
     * @param items               the items to dispatch; must not be {@code null}
     * @param destination         the target {@link ILocation}; must not be {@code null}
     * @param campaign            the active campaign; must not be {@code null}
     * @param logMarker           marker naming the public entry point, for travel-node logs
     * @param directLandingTarget the parent used when landing somewhere that is not a base
     * @param moveContainers      moves each group between hangars/warehouses, or {@code null} if
     *                            the item type has no container to maintain
     */
    private static <T extends ILocation> void dispatch(Collection<T> items, ILocation destination, Campaign campaign,
          String logMarker, ILocation directLandingTarget, @Nullable Consumer<List<T>> moveContainers) {

        PlanetarySystem destinationSystem = destination.getCurrentSystem();

        Map<PlanetarySystem, List<T>> bySystem = items.stream()
              .collect(Collectors.groupingBy(item -> {
                  PlanetarySystem system = item.getCurrentSystem();
                  return system != null ? system : campaign.getCurrentSystem();
              }));

        for (Map.Entry<PlanetarySystem, List<T>> entry : bySystem.entrySet()) {
            PlanetarySystem fromSystem = entry.getKey();
            List<T> group = entry.getValue();

            if (moveContainers != null) {
                moveContainers.accept(group);
            }

            if (destinationSystem == null || fromSystem.equals(destinationSystem)) {
                PlanetarySystem system = destinationSystem != null ? destinationSystem : fromSystem;
                land(group, destination, directLandingTarget, system, campaign, logMarker);
                continue;
            }

            Optional<CurrentLocation> maybeTravelLocation = buildTravelNode(
                  fromSystem, destinationSystem, destination, campaign, logMarker);
            if (maybeTravelLocation.isEmpty()) {
                land(group, destination, directLandingTarget, destinationSystem, campaign, logMarker);
                continue;
            }
            CurrentLocation travelLocation = maybeTravelLocation.get();
            group.forEach(item -> reparent(item, travelLocation));
        }
    }

    /**
     * Lands {@code group} at {@code destination}: bases receive an arrived {@link CurrentLocation}
     * node so arrival accounting still runs; anywhere else the group is parented directly under
     * {@code directLandingTarget}.
     */
    private static void land(List<? extends ILocation> group, ILocation destination, ILocation directLandingTarget,
          PlanetarySystem system, Campaign campaign, String logMarker) {
        if (destination instanceof AbstractBase) {
            CurrentLocation arrivedLocation = buildArrivedNode(system, destination, campaign, logMarker);
            group.forEach(item -> reparent(item, arrivedLocation));
        } else {
            group.forEach(item -> reparent(item, directLandingTarget));
        }
    }

    private static void reparent(ILocation item, ILocation target) {
        if (!item.setParent(target)) {
            LOGGER.error("Could not move {} under {}: rejected by canSetParent", item, target);
        }
    }

}
