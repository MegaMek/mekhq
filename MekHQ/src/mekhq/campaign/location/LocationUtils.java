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

import java.util.Objects;

import megamek.common.annotations.Nullable;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.base.AbstractBase;

/**
 * Static utilities for reasoning about "same effective location" across the {@link ILocation} tree.
 *
 * <p>Co-location definition</p>
 * Two {@link ILocation} items are <em>co-located</em> when:
 * <ol>
 *   <li>Neither is currently in transit (no active {@link mekhq.campaign.JumpPath} anywhere in
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
     * <p>Both items must be non-null, neither may currently be in transit, and they must share
     * the same nearest {@link AbstractBase} ancestor (or both have none, meaning main force).</p>
     *
     * @param firstLocation  first location item
     * @param secondLocation second location item
     *
     * @return {@code true} if the two items are co-located
     */
    public static boolean areSameEffectiveLocation(@Nullable ILocation firstLocation, @Nullable ILocation secondLocation) {
        if (firstLocation == null || secondLocation == null) {
            return false;
        }
        if (isInTransit(firstLocation) || isInTransit(secondLocation)) {
            return false;
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
}
