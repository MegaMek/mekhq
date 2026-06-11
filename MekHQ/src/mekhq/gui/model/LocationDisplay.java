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
package mekhq.gui.model;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.Objects;

import megamek.common.annotations.Nullable;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Generic display helpers for any {@link ILocation} item (unit, part, person, etc.).
 *
 * <p>All methods walk the item's {@link LocationNode} tree — the same mechanism used by
 * {@code PersonnelTableModelColumn} for persons — so they work correctly for every
 * {@code ILocation} subtype without any per-type special-casing.</p>
 *
 * <p>The six display fields map to the six Location-view table columns shared between the
 * Hangar, Warehouse, and Personnel tabs:</p>
 * <ol>
 *   <li>{@link #getLocationSystem} — the planetary system where the item is <em>right now</em></li>
 *   <li>{@link #getLocationPlanet} — the primary planet of that system</li>
 *   <li>{@link #getLocationName} — a human-readable label (base name, "In Transit (%d days…)", etc.)</li>
 *   <li>{@link #getDestinationSystem} — the destination system when in transit, else {@code "-"}</li>
 *   <li>{@link #getDestinationPlanet} — the destination planet when in transit, else {@code "-"}</li>
 *   <li>{@link #getDestinationName} — the destination label (base name / system name) when in transit</li>
 * </ol>
 */
public final class LocationDisplay {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GUI";
    private static final double HOURS_PER_DAY = 24.0;

    private LocationDisplay() {}

    /** The base's display name, or the localized "unnamed base" placeholder when blank. */
    public static String formatBaseName(AbstractBase base) {
        String name = base.getDisplayName();
        return (name != null && !name.isBlank())
                     ? name
                     : getTextAt(RESOURCE_BUNDLE, "LocationDisplay.unnamedBase.text");
    }

    /**
     * A human-readable label for where the item <em>currently</em> is.
     *
     * <ul>
     *   <li>In transit with a {@link JumpPath}: returns a travel-status string identical to the
     *       one shown for persons (e.g. {@code "In Transit (3 days to jump point)"}).</li>
     *   <li>Arrived at a base: returns {@link AbstractBase#getDisplayName()}.</li>
     *   <li>Main force / unknown: returns {@link Campaign#getName()}.</li>
     * </ul>
     */
    public static String getLocationName(ILocation item, Campaign campaign, LocalDate today) {
        LocationNode node = item.getLocationNode();
        AbstractLocation location = node != null ? node.getNearestAbstractLocation() : null;
        boolean isTraveling = location instanceof CurrentLocation currentLocation
                                    && currentLocation.getJumpPath() != null && !currentLocation.getJumpPath().isEmpty();

        if (node != null) {
            ILocation mainForcePersonnel = campaign.getMainForcePersonnel();
            LocationNode cursor = node;
            while (cursor != null) {
                if (cursor.getLocatable() == mainForcePersonnel) {
                    return campaign.getName();
                }
                cursor = cursor.getParent();
            }
        }

        if (!isTraveling && node != null) {
            AbstractBase base = LocationUtils.findEffectiveBase(item);
            if (base != null) {
                return formatBaseName(base);
            }
            LocationNode cursor = node;
            while (cursor != null) {
                if (cursor.getLocatable() instanceof AcademyCampusLocation campus) {
                    return campus.getAcademyName();
                }
                cursor = cursor.getParent();
            }
        }

        if (isTraveling) {
            CurrentLocation currentLocation = (CurrentLocation) location;
            JumpPath path = currentLocation.getJumpPath();
            PlanetarySystem system = currentLocation.getCurrentSystem();

            if (path.size() > 1 && currentLocation.isAtJumpPoint()) {
                double neededHours = system.getRechargeTime(today, currentLocation.computeIsUseCommandCircuit(campaign));
                double remainingHours = neededHours - currentLocation.getRechargeTime();
                if (remainingHours > 0) {
                    int days = (int) Math.ceil(remainingHours / HOURS_PER_DAY);
                    return getFormattedTextAt(RESOURCE_BUNDLE,
                          "PersonnelTableModelColumn.LOCATION_NAME.inTransit.recharging.text",
                          days);
                }
                return getTextAt(RESOURCE_BUNDLE,
                      "PersonnelTableModelColumn.LOCATION_NAME.inTransit.readyToJump.text");
            } else if (path.size() == 1) {
                int days = (int) Math.ceil(currentLocation.getTransitTime());
                return getFormattedTextAt(RESOURCE_BUNDLE,
                      "PersonnelTableModelColumn.LOCATION_NAME.inTransit.toPlanet.text",
                      days);
            } else {
                double daysToJP = system.getTimeToJumpPoint(1.0) - currentLocation.getTransitTime();
                int days = (int) Math.ceil(daysToJP);
                return getFormattedTextAt(RESOURCE_BUNDLE,
                      "PersonnelTableModelColumn.LOCATION_NAME.inTransit.toJumpPoint.text",
                      days);
            }
        }

        return campaign.getName();
    }

    /**
     * The printable name of the planetary system where the item is <em>right now</em>.
     *
     * <p>For an item in transit this is the jump ship's current system (which changes
     * as jumps complete), not the destination.</p>
     */
    public static String getLocationSystem(ILocation item, LocalDate today, Campaign campaign) {
        LocationNode node = item.getLocationNode();
        AbstractLocation location = node != null ? node.getNearestAbstractLocation() : null;
        if (location != null) {
            PlanetarySystem system = location.getCurrentSystem();
            return system != null ? system.getPrintableName(today) : "-";
        }
        PlanetarySystem system = campaign.getCurrentSystem();
        return system != null ? system.getPrintableName(today) : "-";
    }

    /**
     * The printable name of the primary planet in the item's current system.
     *
     * <p>Falls back to the campaign's current system when the item has no location node.</p>
     */
    public static String getLocationPlanet(ILocation item, LocalDate today, Campaign campaign) {
        LocationNode node = item.getLocationNode();
        AbstractLocation location = node != null ? node.getNearestAbstractLocation() : null;
        if (location != null) {
            Planet planet = location.getPlanet();
            return planet != null ? planet.getPrintableName(today) : "-";
        }
        PlanetarySystem system = campaign.getCurrentSystem();
        if (system != null) {
            Planet planet = system.getPrimaryPlanet();
            return planet != null ? planet.getPrintableName(today) : "-";
        }
        return "-";
    }

    /**
     * A human-readable label for the item's <em>destination</em> when in transit.
     *
     * <p>Walks the {@link CurrentLocation}'s parent chain for an {@link AbstractBase}; if found
     * returns the base's display name, otherwise the last system in the {@link JumpPath}.
     * Returns {@code "-"} when the item is not traveling.</p>
     */
    public static String getDestinationName(ILocation item, Campaign campaign, LocalDate today) {
        AbstractLocation location = getNearestAbstractLocation(item);
        if (location instanceof CurrentLocation currentLocation) {
            JumpPath path = currentLocation.getJumpPath();
            if (path != null && !path.isEmpty()) {
                PlanetarySystem destination = path.getLastSystem();
                LocationNode currentLocationNode = currentLocation.getLocationNode();
                if (currentLocationNode != null) {
                    LocationNode parent = currentLocationNode.getParent();
                    while (parent != null) {
                        if (parent.getLocatable() instanceof AbstractBase base) {
                            return formatBaseName(base);
                        }
                        if (parent.getLocatable() instanceof AcademyCampusLocation campus) {
                            LocationNode fixedNode = parent.getParent();
                            if (fixedNode != null
                                      && fixedNode.getLocatable() instanceof AbstractLocation campusLocation
                                      && Objects.equals(campusLocation.getCurrentSystem(), destination)) {
                                return campus.getAcademyName();
                            }
                            return campaign.getName();
                        }
                        parent = parent.getParent();
                    }
                }
                if (destination != null) {
                    for (PlayerBase base : campaign.getPlayerBases()) {
                        if (destination.equals(base.getCurrentSystem())) {
                            return formatBaseName(base);
                        }
                    }
                    return destination.getPrintableName(today);
                }
            }
        }
        return "-";
    }

    /**
     * The printable name of the destination system when in transit, or {@code "-"} otherwise.
     */
    public static String getDestinationSystem(ILocation item, LocalDate today) {
        PlanetarySystem destination = getJumpDestination(item);
        return destination != null ? destination.getPrintableName(today) : "-";
    }

    /**
     * The printable name of the destination system's primary planet when in transit,
     * or {@code "-"} otherwise.
     */
    public static String getDestinationPlanet(ILocation item, LocalDate today) {
        PlanetarySystem destination = getJumpDestination(item);
        if (destination != null) {
            Planet planet = destination.getPrimaryPlanet();
            return planet != null ? planet.getPrintableName(today) : "-";
        }
        return "-";
    }

    /**
     * Returns the last system in {@code item}'s active {@link JumpPath}, or {@code null} if the item has no active
     * travel node or the path is empty/null.
     */
    private static @Nullable PlanetarySystem getJumpDestination(ILocation item) {
        AbstractLocation location = getNearestAbstractLocation(item);
        if (!(location instanceof CurrentLocation currentLocation)) {
            return null;
        }
        JumpPath path = currentLocation.getJumpPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path.getLastSystem();
    }

    private static AbstractLocation getNearestAbstractLocation(ILocation item) {
        LocationNode node = item.getLocationNode();
        return node != null ? node.getNearestAbstractLocation() : null;
    }
}
