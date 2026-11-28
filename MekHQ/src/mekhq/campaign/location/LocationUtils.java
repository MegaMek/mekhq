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

import jakarta.annotation.Nullable;
import mekhq.campaign.AbstractMobileLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.GroundTransitLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * A collection of stateless static utility methods for reasoning about locations and travel. These methods only read
 * and compute from their inputs — they never mutate the {@link ILocation} tree or dispatch travelers (see
 * {@link LocationDispatch} for state-changing dispatch operations).
 *
 * <p>The utilities fall into two groups:</p>
 * <ul>
 *   <li><b>Co-location reasoning</b> over the {@link ILocation} tree
 *       ({@link #areSameEffectiveLocation}, {@link #findEffectivePlace}, {@link #findEffectiveBase},
 *       {@link #isInTransit}, {@link #isOnTheWay}, {@link #isAwayFromMainForce}).</li>
 *   <li><b>Travel computations</b> for planning a journey's route and duration
 *       ({@link #computeStartTransit}, {@link #planJumpPath}, {@link #computeJourneyDays}).</li>
 * </ul>
 *
 * <p>Co-location definition</p>
 * Two {@link ILocation} items are <em>co-located</em> when:
 * <ol>
 *   <li>Neither is currently in transit (no active {@link JumpPath} anywhere in
 *       their ancestor chain).</li>
 *   <li>They share the same effective place in the {@link LocationNode} tree ({@link #findEffectivePlace}): the
 *       same {@link Detachment}, {@link AbstractBase} or remote {@link AcademyCampusLocation}. An item with no
 *       effective place predates locations and counts as being with a detachment.</li>
 * </ol>
 */
public final class LocationUtils {

    private LocationUtils() {}

    /**
     * Returns {@code true} if {@code a} and {@code b} are at the same effective location.
     *
     * <ol>
     *     <li>If either input is null, it is not the same effective location.</li>
     *     <li>If either input is in transit, they're only the same effective location if they share an {@code
     *     AbstractLocation}</li>
     *     <li>Otherwise they're in the same location if they have the same {@linkplain #findEffectivePlace effective
     *     place}. An input with no effective place counts as being with a {@link Detachment}.</li>
     * </ol>
     *
     * @param firstLocation  first location item
     * @param secondLocation second location item
     *
     * @return {@code true} if the two items are co-located
     */
    public static boolean areSameEffectiveLocation(@Nullable ILocation firstLocation, @Nullable ILocation secondLocation) {
        // A null location isn't in the same location as anything
        if (firstLocation == null || secondLocation == null) {
            return false;
        }

        // When in transit, only consider two things to have the same location if it's the exact same location. Space
        // is big and transit time is abstracted, so even if two things have two locations with identical values, the
        // transit time is ambiguous and we can't consider them in the same place. Unless it's the exact same location.
        if (isInTransit(firstLocation) || isInTransit(secondLocation)) {
            // Intentionally using "==" over .equals - When stuff is in transit, let's explicitly check if these have
            // the same AbstractLocation object
            return firstLocation.getCurrentLocation() == secondLocation.getCurrentLocation();
        }
        IPlace firstPlace = findEffectivePlace(firstLocation);
        IPlace secondPlace = findEffectivePlace(secondLocation);
        if (firstPlace == secondPlace) {
            return true;
        }
        // An item placed nowhere predates locations and is with the main force, which is a detachment
        return ((firstPlace == null) && (secondPlace instanceof Detachment))
                     || ((secondPlace == null) && (firstPlace instanceof Detachment));
    }

    /**
     * Walks up the {@link LocationNode} tree and returns the place the item is effectively at: the nearest
     * {@link Detachment} or {@link AbstractBase} ancestor, or, failing both, the nearest
     * {@link AcademyCampusLocation}.
     *
     * <p>A home-school campus hangs under the detachment or base its students live with, so they count as being
     * there. A remote academy hangs under a planetary system of its own, so its students are at the campus.</p>
     *
     * @param location the location item to inspect
     *
     * @return the effective place, or {@code null} if the item is placed under none of them
     */
    public static @Nullable IPlace findEffectivePlace(@Nullable ILocation location) {
        if (location == null) {
            return null;
        }
        AcademyCampusLocation campus = null;
        LocationNode node = location.getLocationNode();
        while (node != null) {
            ILocation locatable = node.getLocatable();
            if ((locatable instanceof Detachment) || (locatable instanceof AbstractBase)) {
                return (IPlace) locatable;
            }
            if ((campus == null) && (locatable instanceof AcademyCampusLocation academyCampus)) {
                campus = academyCampus;
            }
            node = node.getParent();
        }
        return campus;
    }

    /**
     * Walks up the {@link LocationNode} tree and returns the nearest {@link AbstractBase} ancestor, or {@code null} if
     * the item belongs to the main force (no base ancestor).
     *
     * @param location the location item to inspect
     *
     * @return the nearest {@link AbstractBase}, or {@code null} for main force
     */
    public static @Nullable AbstractBase findEffectiveBase(@Nullable ILocation location) {
        if (location == null) {
            return null;
        }
        LocationNode node = location.getLocationNode();
        while (node != null) {
            if (node.getLocatable() instanceof AbstractBase base) {
                return base;
            }
            node = node.getParent();
        }
        return null;
    }

    /**
     * Returns {@code true} if the item — or any of its ancestors in the {@link LocationNode} tree — is
     * currently traveling: a {@link CurrentLocation} with an active {@link mekhq.campaign.JumpPath}, or a
     * {@link GroundTransitLocation} still counting down its overland transit time.
     *
     * <p>For interplanetary travel this intentionally checks for a non-empty {@link mekhq.campaign.JumpPath}
     * rather than {@link ILocation#isInTransit()}, which only returns {@code true} once the ship has actually
     * started moving (transit time &gt; 0). An item with an assigned but not-yet-started journey
     * must also be treated as in transit for co-location and assignment purposes. A ground convoy has no
     * jump path, so it is judged by its own {@link GroundTransitLocation#isInTransit()}.</p>
     *
     * @param location the location item to inspect
     *
     * @return {@code true} if the item is in transit
     */
    public static boolean isInTransit(@Nullable ILocation location) {
        if (location == null) {
            return false;
        }
        LocationNode node = location.getLocationNode();
        while (node != null) {
            ILocation locatable = node.getLocatable();
            if (locatable instanceof CurrentLocation currentLocation
                      && currentLocation.getJumpPath() != null && !currentLocation.getJumpPath().isEmpty()) {
                return true;
            }
            // A GroundTransitLocation has no jump path; it is in transit while its overland
            // transit time is still counting down.
            if (locatable instanceof GroundTransitLocation groundTransit && groundTransit.isInTransit()) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    /**
     * Returns {@code true} if the item is on its way to another place: its nearest {@link IPlace} is a travel node
     * ({@link CurrentLocation} or {@link GroundTransitLocation}) parented under a destination.
     *
     * <p>A dispatched unit or spare is filed in its destination's hangar or warehouse as soon as it leaves, so it
     * shows up there while it travels. It can't be used there until it lands. Unlike {@link #isInTransit}, this is
     * {@code false} for the main force itself when it has a jump planned, because the main force's own location is a
     * root node, not a travel node.</p>
     *
     * @param item the item to inspect
     *
     * @return {@code true} if the item is travelling to another place
     */
    public static boolean isOnTheWay(@Nullable ILocation item) {
        if (item == null) {
            return false;
        }
        IPlace place = item.getPlace();
        return (place instanceof AbstractMobileLocation travelNode) && travelNode.isParented();
    }

    /**
     * Returns {@code true} if the item is somewhere other than with the main force: at a base, at an academy campus,
     * or on its way somewhere, including on its way back to the main force.
     *
     * <p>An item with no place at all predates locations, and counts as being with the main force.</p>
     *
     * @param item     the item to inspect
     * @param campaign the campaign whose main force to compare against
     *
     * @return {@code true} if the item is away from the main force
     */
    public static boolean isAwayFromMainForce(@Nullable ILocation item, Campaign campaign) {
        if (item == null) {
            return false;
        }
        IPlace place = item.getPlace();
        if (place == null) {
            return false;
        }
        PlayerForce playerForce = campaign.getPlayerForce();
        return (playerForce != null) && (place != playerForce.getForceDetachment());
    }

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
        if (campaign.getPlayerForce().getForceDetachment().getCurrentLocation() != null) {
            return campaign.getPlayerForce().getForceDetachment().getCurrentLocation().getTransitTime();
        } else {
            return fromSystem.getTimeToJumpPoint(1.0);
        }
    }

    /**
     * Returns the {@link JumpPath} a dispatch would assign for travel from {@code fromSystem} to
     * {@code destinationSystem}, or {@code null} when the trip needs no inter-system jump and would land immediately
     * (same system, a missing system, or no calculable path).
     *
     * <p>This mirrors the path selection performed by {@link LocationDispatch} when dispatching, letting callers learn
     * the planned route (e.g. to compute a journey time) without actually dispatching the travelers.</p>
     *
     * @param fromSystem        the departure system, or {@code null}
     * @param destinationSystem the arrival system, or {@code null}
     * @param campaign          the active campaign; must not be {@code null}
     * @return the planned {@link JumpPath}, or {@code null} if travel would land immediately
     */
    public static @Nullable JumpPath planJumpPath(@Nullable PlanetarySystem fromSystem,
          @Nullable PlanetarySystem destinationSystem, Campaign campaign) {
        if (fromSystem == null || destinationSystem == null || fromSystem.equals(destinationSystem)) {
            return null;
        }
        JumpPath path = campaign.calculateJumpPath(fromSystem, destinationSystem);
        return (path == null || path.isEmpty()) ? null : path;
    }

    /**
     * Builds a single-system path, so a {@link CurrentLocation} already in {@code system} completes its in-system
     * transit to the planet. A travel node short of the planet with no path never advances.
     *
     * @param system the system the travel node is in and must arrive at; must not be {@code null}
     * @return a path containing only {@code system}
     */
    public static JumpPath inSystemPath(PlanetarySystem system) {
        JumpPath path = new JumpPath();
        path.addSystem(system);
        return path;
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
        int journeyDays = (int) Math.ceil(path.getTotalTime(date, startTransit, false));
        return Math.max(2, journeyDays);
    }
}
