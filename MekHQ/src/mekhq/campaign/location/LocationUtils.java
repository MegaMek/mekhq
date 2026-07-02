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
import java.util.Objects;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * A collection of stateless static utility methods for reasoning about locations and travel. These methods only read
 * and compute from their inputs — they never mutate the {@link ILocation} tree or dispatch travelers (see
 * {@link LocationDispatch} for state-changing dispatch operations).
 *
 * <p>The utilities fall into two groups:</p>
 * <ul>
 *   <li><b>Co-location reasoning</b> over the {@link ILocation} tree
 *       ({@link #areSameEffectiveLocation}, {@link #findEffectiveBase}, {@link #isInTransit}).</li>
 *   <li><b>Travel computations</b> for planning a journey's route and duration
 *       ({@link #computeStartTransit}, {@link #planJumpPath}, {@link #computeJourneyDays}).</li>
 * </ul>
 *
 * <p>Co-location definition</p>
 * Two {@link ILocation} items are <em>co-located</em> when:
 * <ol>
 *   <li>Neither is currently in transit (no active {@link JumpPath} anywhere in
 *       their ancestor chain).</li>
 *   <li>They share the same {@link AbstractBase} ancestor in the {@link LocationNode} tree —
 *       or neither has an {@link AbstractBase} ancestor (both are in the main force).</li>
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
     *     <li>If both inputs have the same {@code Base} they're in the same location.</li>
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
        return Objects.equals(findEffectiveBase(firstLocation), findEffectiveBase(secondLocation));
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
     * Returns {@code true} if the item — or any of its ancestors in the {@link LocationNode} tree — is a
     * {@link CurrentLocation} with an active {@link mekhq.campaign.JumpPath} (i.e. the item is currently traveling).
     *
     * <p>This intentionally checks for a non-empty {@link mekhq.campaign.JumpPath} rather than
     * {@link ILocation#isInTransit()}, which only returns {@code true} once the ship has actually
     * started moving (transit time &gt; 0). An item with an assigned but not-yet-started journey
     * must also be treated as in transit for co-location and assignment purposes.</p>
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
            if (node.getLocatable() instanceof CurrentLocation currentLocation
                      && currentLocation.getJumpPath() != null && !currentLocation.getJumpPath().isEmpty()) {
                return true;
            }
            node = node.getParent();
        }
        return false;
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
        return campaign.getCurrentLocation() != null
              ? campaign.getCurrentLocation().getTransitTime()
              : fromSystem.getTimeToJumpPoint(1.0);
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
}
