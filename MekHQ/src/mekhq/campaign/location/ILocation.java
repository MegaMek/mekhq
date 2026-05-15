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
    default AbstractLocation getLocation() {
        return hasLocationNode() ? getLocationNode().getCurrentLocation() : null;
    }

    /**
     * Check if this location has an actual location {@link AbstractLocation}.
     *
     * @return {@code true} if this location has a location, otherwise {@code false}
     */
    default boolean hasLocation() {
        return getLocation() != null;
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

        return getLocation().isOnPlanet();
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

        return getLocation().isAtJumpPoint();
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

        return getLocation().getPercentageTransit();
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

        return getLocation().isInTransit();
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

        return getLocation().getCurrentSystem();
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

        return getLocation().getPlanet();
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

        return getLocation().getTransitTime();
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

        return getLocation().isJumpZenith();
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

        return getLocation().getJumpPath();
    }

    /**
     * Set the {@link JumpPath} for this location. If {@link #hasLocation()} is {@code false}, this method does
     * nothing.
     *
     * @param jumpPath the {@link JumpPath} to set
     */
    default void setJumpPath(JumpPath jumpPath) {
        if (hasLocation()) {
            getLocation().setJumpPath(jumpPath);
        }
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
