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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.events.LocationAddedEvent;
import mekhq.campaign.events.LocationRemovedEvent;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationDispatch;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;

/**
 * Manages the collection of {@link AbstractLocation} travel/fixed nodes and {@link PlayerBase} objects owned by a
 * {@link Campaign}.
 * <p>
 * Campaign holds a single instance of this manager and exposes facade methods that delegate here.</p>
 */
public class CampaignLocationManager {
    private static final MMLogger LOGGER = MMLogger.create(CampaignLocationManager.class);

    private final List<AbstractLocation> locations = new ArrayList<>();
    private final Set<PlayerBase> playerBases = new LinkedHashSet<>();
    /**
     * Travel queued during the day, keyed by origin and destination. It is dispatched at the start of the next new day
     * by {@link #dispatchPendingTravel(Campaign)} rather than immediately. Travel still queued at save time is persisted
     * via {@link #writePendingTravelToXML} and restored during campaign load.
     */
    private final Map<TravelRoute, List<ILocation>> pendingTravel = new LinkedHashMap<>();

    public void addLocation(AbstractLocation location) {
        if (location != null && !locations.contains(location)) {
            locations.add(location);
        }
    }

    public void removeLocation(AbstractLocation location) {
        locations.remove(location);
    }

    @Nonnull
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

    @Nonnull
    public Set<PlayerBase> getPlayerBases() {
        return Collections.unmodifiableSet(playerBases);
    }

    /**
     * Queues {@code travelers} to be relocated to {@code destination} when the day next advances, rather than
     * dispatching them immediately. Each traveler is bucketed by its own current (origin) location, so a single call
     * may produce several entries when the travelers start from different places. Queued travel is dispatched by
     * {@link #dispatchPendingTravel(Campaign)}.
     *
     * @param travelers  the persons, units, or parts to relocate; must not be {@code null}
     * @param destination the target {@link ILocation}; a {@code null} destination is refused with an error log, since
     *                    a queued null destination would fail dispatch on every subsequent new day
     */
    public void queueTravel(Collection<? extends ILocation> travelers, @Nullable ILocation destination) {
        if (destination == null) {
            LOGGER.error("queueTravel: null destination — refusing to queue {} traveler(s)", travelers.size());
            return;
        }
        for (ILocation traveler : travelers) {
            TravelRoute route = new TravelRoute(traveler.getCurrentLocation(), destination);
            pendingTravel.computeIfAbsent(route, key -> new ArrayList<>()).add(traveler);
        }
    }

    /**
     * Returns {@code true} if {@code traveler} is currently queued for travel that has not yet been dispatched by
     * {@link #dispatchPendingTravel(Campaign)}. A queued traveler still sits at its origin location; it has not been
     * moved toward its destination.
     */
    public boolean isQueuedForTravel(ILocation traveler) {
        for (List<ILocation> travelers : pendingTravel.values()) {
            if (travelers.contains(traveler)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatches all travel queued via {@link #queueTravel}, one {@link LocationDispatch} call per queued route.
     * Travelers that have left the campaign since they were queued (a removed person, a sold unit, a sold or consumed
     * part) are skipped with a warning — dispatching them would re-add them to the destination's hangar or warehouse.
     *
     * <p>Must run at the start of a new day, before location transit is advanced, so that the departure system resolved
     * at dispatch time matches where the travelers were when the travel was queued.</p>
     *
     * @param campaign the active campaign; must not be {@code null}
     */
    public void dispatchPendingTravel(Campaign campaign) {
        for (Map.Entry<TravelRoute, List<ILocation>> entry : pendingTravel.entrySet()) {
            List<ILocation> travelers = filterTravelersStillInCampaign(entry.getValue(), campaign);
            if (!travelers.isEmpty()) {
                LocationDispatch.dispatchTravelers(travelers, entry.getKey().destination(), campaign);
            }
        }
        pendingTravel.clear();
    }

    private List<ILocation> filterTravelersStillInCampaign(List<ILocation> travelers, Campaign campaign) {
        List<ILocation> stillPresent = new ArrayList<>();
        for (ILocation traveler : travelers) {
            if (isStillInCampaign(traveler, campaign)) {
                stillPresent.add(traveler);
            } else {
                LOGGER.warn("dispatchPendingTravel: queued {} left the campaign before dispatch — skipping",
                      traveler.getClass().getSimpleName());
            }
        }
        return stillPresent;
    }

    private boolean isStillInCampaign(ILocation traveler, Campaign campaign) {
        return switch (traveler) {
            case Person person -> campaign.getPerson(person.getId()) != null;
            case Unit unit -> campaign.getUnit(unit.getId()) != null;
            case Part part -> findPartAnywhere(campaign, part.getId()) != null;
            default -> true;
        };
    }

    /** Searches the campaign warehouse then all base warehouses for a part by ID. */
    public @Nullable Part findPartAnywhere(Campaign campaign, int partId) {
        Part part = campaign.getWarehouse().getPart(partId);
        if (part != null) {
            return part;
        }
        for (PlayerBase base : playerBases) {
            Part basePart = base.getBaseWarehouse().getPart(partId);
            if (basePart != null) {
                return basePart;
            }
        }
        return null;
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
     * Returns the existing {@link FixedLocation} at the given system, creating and registering one on demand.
     *
     * @return the existing or newly created location, or {@code null} if {@code systemId} could not be resolved
     */
    @Nullable
    public FixedLocation getOrCreateFixedLocation(Campaign campaign, String systemId) {
        for (AbstractLocation location : locations) {
            if (location instanceof FixedLocation fixedLocation
                      && fixedLocation.getCurrentSystem().getId().equals(systemId)) {
                return fixedLocation;
            }
        }
        PlanetarySystem system = campaign.getSystemById(systemId);
        if (system == null) {
            return null;
        }
        FixedLocation fixedLocation = new FixedLocation(system);
        locations.add(fixedLocation);
        return fixedLocation;
    }

    /**
     * Creates an {@link AcademyCampusLocation} under the {@link FixedLocation} at the given system (created on demand)
     * and registers it in the locations list.
     *
     * @return the newly created campus location, or {@code null} if {@code systemId} could not be resolved
     */
    @Nullable
    public AcademyCampusLocation addCampusLocation(Campaign campaign, String academySet, String academyName,
          String systemId) {
        FixedLocation fixedLocation = getOrCreateFixedLocation(campaign, systemId);
        if (fixedLocation == null) {
            return null;
        }
        AcademyCampusLocation campus = new AcademyCampusLocation(academySet, academyName);
        LocationNode.LocationManager.setLocation(campus, fixedLocation);
        return campus;
    }

    /**
     * Returns the existing {@link AcademyCampusLocation} for the given campus at the given system, creating it on
     * demand if it does not yet exist.
     *
     * @return the existing or newly created campus location, or {@code null} if {@code systemId} could not be resolved
     */
    @Nullable
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
    @Nonnull
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
    @Nonnull
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
        writePendingTravelToXML(pw, indent);
    }

    /**
     * Serializes queued-but-undrained travel. Only the destination and the travelers are written; the origin is
     * recomputed from each traveler's location when the saved travel is re-queued on load (see
     * {@code CampaignXmlParser}). Each destination serializes itself via
     * {@link ILocation#writePendingTravelDestinationToXML}; a destination type without serialization support is logged
     * and skipped.
     */
    private void writePendingTravelToXML(PrintWriter pw, int indent) {
        if (pendingTravel.isEmpty()) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "pendingTravel");
        for (Map.Entry<TravelRoute, List<ILocation>> entry : pendingTravel.entrySet()) {
            ILocation destination = entry.getKey().destination();
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "route");
            if (destination != null && destination.writePendingTravelDestinationToXML(pw, indent)) {
                for (ILocation traveler : entry.getValue()) {
                    switch (traveler) {
                        case Person person ->
                              MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personId", person.getId().toString());
                        case Unit unit ->
                              MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitId", unit.getId().toString());
                        case Part part -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partId", part.getId());
                        default -> LOGGER.error(
                              "writePendingTravel: cannot serialize queued traveler of type {} bound for {} — skipping",
                              traveler.getClass().getSimpleName(), destination.getClass().getSimpleName());
                    }
                }
            } else {
                LOGGER.warn("writePendingTravel: unsupported destination type {} — skipping route travelers",
                      destination == null ? "null" : destination.getClass().getSimpleName());
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "route");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "pendingTravel");
    }

    /** Queue key: an item's origin location paired with its travel destination. */
    private record TravelRoute(@Nullable ILocation origin, ILocation destination) {}
}
