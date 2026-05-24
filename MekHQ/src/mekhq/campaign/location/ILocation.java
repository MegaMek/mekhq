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

import java.util.Set;
import java.util.stream.Collectors;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Interface for classes that have a location.
 * <p>
 * Classes that implement this interface will have a {@link LocationNode} that lets {@code ILocation} implementations
 * exist within a parent-child location tree. The root of any given tree should be an {@link AbstractLocation}. This
 * tree reduces granular location housekeeping - We don't need to update the location of every {@link Person},
 * {@link Unit}, or {@link Part}, we only need to update the location(s) of their parent. Because
 * {@link mekhq.campaign.Campaign} implements {@code ILocation}, the entire main force only needs to maintain one
 * location for every object in it.
 * </p>
 * <p>
 * Documentation and descriptions will omit the usage of {@code LocationNode} whenever possible. Technically, there is a
 * tree of {@code LocationNode} classes, and each {@code LocationNode} has a 1:1 relationship with a {@code ILocation}.
 * It can therefore be thought of as a tree of {@code ILocation} implementations. This avoids needing to clarify every
 * time that it's the {@code ILocation}'s {@code LocationNode}'s relative {@code LocationNode}'s {@code ILocation}.
 * </p>
 */
public interface ILocation {

    LocationNode getLocationNode();

    default boolean hasLocationNode() {
        return getLocationNode() != null;
    }

    /**
     * Get the current location of this location.
     *
     * @return {@link AbstractLocation}, or {@code null} if it doesn't have a {@code AbstractLocation}
     */
    default AbstractLocation getCurrentLocation() {
        return hasLocationNode() ? getLocationNode().getCurrentLocation() : null;
    }

    /**
     * Check if this location has an actual location {@link AbstractLocation}.
     *
     * @return {@code true} if this location has a location, otherwise {@code false}
     */
    default boolean hasLocation() {
        return getCurrentLocation() != null;
    }

    /**
     * Check if this location is currently on a planet. If {@link #hasLocation()} is {@code false}, this method returns
     * {@code false}.
     *
     * @return {@code true} if this location is on a planet, otherwise {@code false}
     */
    default boolean isOnPlanet() {
        if (!hasLocation()) {
            return false;
        }

        return getCurrentLocation().isOnPlanet();
    }

    /**
     * Check if this location is currently at a jump point. If {@link #hasLocation()} is {@code false}, this method
     * returns {@code false}.
     *
     * @return {@code true} if this location is at a jump point, otherwise {@code false}
     */
    default boolean isAtJumpPoint() {
        if (!hasLocation()) {
            return false;
        }

        return getCurrentLocation().isAtJumpPoint();
    }

    /**
     * Get the percentage of transit completed for this location. If {@link #hasLocation()} is {@code false}, this
     * method returns {@code 0.0}.
     *
     * @return the percentage of transit completed as a {@code double}
     */
    default double getPercentageTransit() {
        if (!hasLocation()) {
            return 0.0;
        }

        return getCurrentLocation().getPercentageTransit();
    }

    /**
     * Check if this location is currently in transit. If {@link #hasLocation()} is {@code false}, this method returns
     * {@code false}.
     *
     * @return {@code true} if this location is in transit, otherwise {@code false}
     */
    default boolean isInTransit() {
        if (!hasLocation()) {
            return false;
        }

        return getCurrentLocation().isInTransit();
    }

    /**
     * Get the current {@link PlanetarySystem} for this location. If {@link #hasLocation()} is {@code false}, this
     * method returns {@code null}.
     *
     * @return the current {@link PlanetarySystem}, or {@code null} if no location exists
     */
    default PlanetarySystem getCurrentSystem() {
        if (!hasLocation()) {
            return null;
        }

        return getCurrentLocation().getCurrentSystem();
    }

    /**
     * Get the current {@link Planet} for this location. If {@link #hasLocation()} is {@code false}, this method returns
     * {@code null}.
     *
     * @return the current {@link Planet}, or {@code null} if no location exists
     */
    default Planet getPlanet() {
        if (!hasLocation()) {
            return null;
        }

        return getCurrentLocation().getPlanet();
    }

    /**
     * Get the remaining transit time for this location. If {@link #hasLocation()} is {@code false}, this method returns
     * {@code 0.0}.
     *
     * @return the remaining transit time as a {@code double}
     */
    default double getTransitTime() {
        if (!hasLocation()) {
            return 0.0;
        }

        return getCurrentLocation().getTransitTime();
    }

    /**
     * Check if the JumpShip is at the zenith jump point. If {@link #hasLocation()} is {@code false}, this method
     * returns {@code false}.
     *
     * @return {@code true} if the JumpShip is at the zenith point, otherwise {@code false}
     */
    default boolean isJumpZenith() {
        if (!hasLocation()) {
            return false;
        }

        return getCurrentLocation().isJumpZenith();
    }

    /**
     * Get the current {@link JumpPath} for this location. If {@link #hasLocation()} is {@code false}, this method
     * returns {@code null}.
     *
     * @return the current {@link JumpPath}, or {@code null} if no location exists
     */
    default JumpPath getJumpPath() {
        if (!hasLocation()) {
            return null;
        }

        return getCurrentLocation().getJumpPath();
    }

    /**
     * Set the {@link JumpPath} for this location. If {@link #hasLocation()} is {@code false}, this method does
     * nothing.
     *
     * @param jumpPath the {@link JumpPath} to set
     */
    default void setJumpPath(JumpPath jumpPath) {
        if (hasLocation()) {
            getCurrentLocation().setJumpPath(jumpPath);
        }
    }

    /**
     * Checks whether {@code parent} can safely be set as this location's parent.
     *
     * <p>The operation is valid when all of the following hold:</p>
     * <ul>
     *   <li>{@code parent} is {@code null} (detaching is always safe), or</li>
     *   <li>both this location and {@code parent} have a {@link LocationNode}, and</li>
     *   <li>attaching would not create a cycle, and</li>
     *   <li>the root-most node of {@code parent}'s chain has an {@link AbstractLocation} as its locatable.</li>
     * </ul>
     *
     * @param parent the proposed parent, or {@code null} to detach
     *
     * @return {@code true} if the operation is valid
     */
    default boolean canSetParent(ILocation parent) {
        if (parent == null) {
            return true;
        }
        if (!hasLocationNode() || !parent.hasLocationNode()) {
            return false;
        }
        if (wouldCreateCycle(getLocationNode(), parent.getLocationNode())) {
            return false;
        }
        return findRoot(parent.getLocationNode()).getLocatable() instanceof AbstractLocation;
    }

    /**
     * Checks whether {@code child} can safely be adopted as a child of this location.
     *
     * <p>Delegates to {@code child.canSetParent(this)}; see that method for the validity rules.</p>
     *
     * @param child the proposed child; {@code null} is always invalid
     *
     * @return {@code true} if the operation is valid
     */
    default boolean canSetChild(ILocation child) {
        if (child == null) {
            return false;
        }
        return child.canSetParent(this);
    }

    /**
     * Sets {@code parent} as this location's parent in the {@link LocationNode} tree.
     *
     * <p>If {@link #canSetParent(ILocation)} returns {@code false} the tree is left unchanged
     * and this method returns {@code false}.</p>
     *
     * @param parent the new parent, or {@code null} to detach from the current parent
     *
     * @return {@code true} if the tree was updated, {@code false} if the operation was rejected
     */
    default boolean setParent(ILocation parent) {
        if (!canSetParent(parent)) {
            return false;
        }
        LocationNode.LocationManager.setLocation(this, parent);
        return true;
    }

    /**
     * Adopts {@code child} as a child of this location in the {@link LocationNode} tree.
     *
     * <p>If {@link #canSetChild(ILocation)} returns {@code false} the tree is left unchanged
     * and this method returns {@code false}.</p>
     *
     * @param child the location to adopt
     *
     * @return {@code true} if the tree was updated, {@code false} if the operation was rejected
     */
    default boolean setChild(ILocation child) {
        if (!canSetChild(child)) {
            return false;
        }
        return child.setParent(this);
    }

    /** Walks up via {@link LocationNode#getParent()} until reaching the root node. */
    private static LocationNode findRoot(LocationNode node) {
        while (node.getParent() != null) {
            node = node.getParent();
        }
        return node;
    }

    /**
     * Returns {@code true} if {@code node} appears anywhere in the ancestor chain of {@code potentialAncestor}, which
     * would indicate that making {@code potentialAncestor} an ancestor of {@code node} would create a cycle.
     */
    private static boolean wouldCreateCycle(LocationNode node, LocationNode potentialAncestor) {
        LocationNode cursor = potentialAncestor;
        while (cursor != null) {
            if (cursor == node) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    default Set<Person> getPersonnelAtLocation() {
        if (!hasLocationNode()) {
            return Set.of();
        }
        return getLocationNode().getChildren().stream()
                     .flatMap(loc -> loc.getLocatable().getPersonnelAtLocation().stream())
                     .collect(Collectors.toSet());
    }

    default Set<Unit> getUnitsAtLocation() {
        if (!hasLocationNode()) {
            return Set.of();
        }
        return getLocationNode().getChildren().stream()
                     .flatMap(loc -> loc.getLocatable().getUnitsAtLocation().stream())
                     .collect(Collectors.toSet());
    }

    default Set<Part> getPartsAtLocation() {
        if (!hasLocationNode()) {
            return Set.of();
        }
        return getLocationNode().getChildren().stream()
                     .flatMap(loc -> loc.getLocatable().getPartsAtLocation().stream())
                     .collect(Collectors.toSet());
    }
}
