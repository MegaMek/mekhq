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

package mekhq.campaign;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.events.LocationAddedEvent;
import mekhq.campaign.events.LocationRemovedEvent;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;

/**
 * Manages the collection of {@link AbstractLocation} travel/fixed nodes and {@link PlayerBase} objects owned by a
 * {@link Campaign}.
 * <p>
 * Campaign holds a single instance of this manager and exposes facade methods that delegate here.</p>
 */
public class CampaignLocationManager {

    private final List<AbstractLocation> locations = new ArrayList<>();
    private final Set<PlayerBase> playerBases = new LinkedHashSet<>();

    public void addLocation(AbstractLocation location) {
        if (location != null && !locations.contains(location)) {
            locations.add(location);
        }
    }

    public void removeLocation(AbstractLocation location) {
        locations.remove(location);
    }

    public List<AbstractLocation> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    /**
     * Adds {@code base} to the set of player bases, registers its parent location if any, and fires a
     * {@link LocationAddedEvent} when the base was not already present.
     */
    public void addPlayerBase(@Nullable PlayerBase base) {
        if (base == null) {
            return;
        }
        boolean added = playerBases.add(base);
        if (base.getParent() instanceof AbstractLocation parent) {
            addLocation(parent);
        }
        if (added) {
            MekHQ.triggerEvent(new LocationAddedEvent(base));
        }
    }

    /**
     * Removes {@code base} from the set of player bases, firing a {@link LocationRemovedEvent} when the base was
     * present.
     */
    public void removePlayerBase(@Nullable PlayerBase base) {
        if (base == null) {
            return;
        }
        if (playerBases.remove(base)) {
            MekHQ.triggerEvent(new LocationRemovedEvent(base));
        }
    }

    public Set<PlayerBase> getPlayerBases() {
        return Collections.unmodifiableSet(playerBases);
    }

    /**
     * Removes any {@link AbstractLocation} entries that have no personnel, parts, or units at any depth in their
     * subtree, excluding the campaign's own current location.
     *
     * <p>This handles two leak paths: {@link CurrentLocation} travel nodes whose passengers all
     * died or were removed before arriving, and {@link FixedLocation}/{@link AcademyCampusLocation} pairs that were
     * never cleaned up after the last student graduated.</p>
     *
     * <p>Call this once per day after all personnel processing has completed.</p>
     */
    public void pruneEmptyLocations(Campaign campaign) {
        AbstractLocation mainLocation = campaign.getCurrentLocation();
        locations.removeIf(location -> {
            if (location == mainLocation) {
                return false;
            }
            if (!location.fetchPersonnelAtLocation().isEmpty()
                      || !location.fetchPartsAtLocation().isEmpty()
                      || !location.fetchUnitsAtLocation().isEmpty()) {
                return false;
            }
            if (location instanceof CurrentLocation) {
                location.setParent(null);
            } else if (location instanceof FixedLocation) {
                for (ILocation child : new ArrayList<>(location.getChildLocations())) {
                    if (child instanceof AcademyCampusLocation campus) {
                        campus.setParent(null);
                    }
                }
            }
            return true;
        });
    }

    /**
     * Creates a {@link FixedLocation} with an {@link AcademyCampusLocation} child at the given system and registers it
     * in the locations list.
     *
     * @return the newly created campus location, or {@code null} if {@code systemId} could not be resolved
     */
    @Nullable
    public AcademyCampusLocation addCampusLocation(Campaign campaign, String academySet, String academyName,
          String systemId) {
        PlanetarySystem system = campaign.getSystemById(systemId);
        if (system == null) {
            return null;
        }
        FixedLocation fixedLocation = new FixedLocation(system);
        AcademyCampusLocation campus = new AcademyCampusLocation(academySet, academyName);
        LocationNode.LocationManager.setLocation(campus, fixedLocation);
        locations.add(fixedLocation);
        return campus;
    }

    /**
     * Returns the existing {@link AcademyCampusLocation} for the given campus at the given system, creating it on
     * demand if it does not yet exist.
     */
    public AcademyCampusLocation getOrCreateCampusLocation(Campaign campaign, String academySet, String academyName,
          String systemId) {
        for (AbstractLocation location : locations) {
            if (!(location instanceof FixedLocation fixedLocation)) {
                continue;
            }
            if (!fixedLocation.getCurrentSystem().getId().equals(systemId)) {
                continue;
            }
            for (ILocation child : fixedLocation.getChildLocations()) {
                if (child instanceof AcademyCampusLocation campus
                          && academySet.equals(campus.getAcademySet())
                          && academyName.equals(campus.getAcademyName())) {
                    return campus;
                }
            }
        }
        return addCampusLocation(campaign, academySet, academyName, systemId);
    }

    /**
     * Returns the existing local {@link AcademyCampusLocation} (home-school or unit-education) parented directly under
     * the campaign, creating it on demand if it does not yet exist.
     *
     * <p>Local campuses travel with the campaign and are not anchored to a {@link FixedLocation}.
     * Use {@link #getOrCreateCampusLocation} for academies at a fixed planetary system.</p>
     */
    public AcademyCampusLocation getOrCreateLocalCampusLocation(Campaign campaign, String academySet,
          String academyName) {
        for (ILocation child : campaign.getChildLocations()) {
            if (child instanceof AcademyCampusLocation campus
                      && academySet.equals(campus.getAcademySet())
                      && academyName.equals(campus.getAcademyName())) {
                return campus;
            }
        }
        AcademyCampusLocation campus = new AcademyCampusLocation(academySet, academyName);
        LocationNode.LocationManager.setLocation(campus, campaign);
        return campus;
    }

    /**
     * Returns the existing {@link AcademyCampusLocation} parented under {@code parent}, creating it on demand if it
     * does not yet exist.
     *
     * <p>Used for home-school campuses that travel with the campaign or a player base rather than being anchored to a
     * fixed planetary system.</p>
     */
    public AcademyCampusLocation getOrCreateCampusUnderLocation(String academySet, String academyName,
          ILocation parent) {
        for (ILocation child : parent.getChildLocations()) {
            if (child instanceof AcademyCampusLocation campus
                      && academySet.equals(campus.getAcademySet())
                      && academyName.equals(campus.getAcademyName())) {
                return campus;
            }
        }
        AcademyCampusLocation campus = new AcademyCampusLocation(academySet, academyName);
        LocationNode.LocationManager.setLocation(campus, parent);
        return campus;
    }

    /**
     * Writes the {@code <locations>} and {@code <playerBases>} XML blocks.
     */
    public void writeToXML(Campaign campaign, PrintWriter pw, int indent) {
        AbstractLocation mainForceLocation = campaign.getCurrentLocation();
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "locations");
        for (AbstractLocation location : locations) {
            // Skip locations parented to another node — they are serialized inside their parent's XML.
            // Skip the main force's current location — written separately by ForceLocationManager as <location>.
            if (location.isParented() || location == mainForceLocation) {
                continue;
            }
            location.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "locations");
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "playerBases");
        for (PlayerBase base : playerBases) {
            base.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "playerBases");
    }
}
