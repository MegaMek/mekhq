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
import java.util.*;

import jakarta.annotation.Nonnull;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.events.LocationAddedEvent;
import mekhq.campaign.events.LocationRemovedEvent;
import mekhq.campaign.events.TransitCompleteEvent;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.units.UnitChangedEvent;
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

    /** Returns the player base with the given id, or {@code null} if none matches. */
    public @Nullable PlayerBase getPlayerBaseById(UUID id) {
        for (PlayerBase base : playerBases) {
            if (id.equals(base.getId())) {
                return base;
            }
        }
        return null;
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
            removeExistingPendingTravel(traveler);
            TravelRoute route = new TravelRoute(traveler.getCurrentLocation(), destination);
            pendingTravel.computeIfAbsent(route, key -> new ArrayList<>()).add(traveler);
            fireLocationChanged(traveler);
        }
    }

    /**
     * Removes {@code traveler} from any route it is already queued on, logging at debug level when it is found. Called
     * before re-queuing so a later {@link #queueTravel} supersedes an earlier one rather than leaving the traveler
     * bound to two destinations (which would dispatch it twice). A route left empty by the removal is dropped so it
     * does not linger in {@link #pendingTravel}.
     */
    private void removeExistingPendingTravel(ILocation traveler) {
        Iterator<Map.Entry<TravelRoute, List<ILocation>>> iterator = pendingTravel.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TravelRoute, List<ILocation>> entry = iterator.next();
            if (entry.getValue().remove(traveler)) {
                LOGGER.debug("queueTravel: {} was already queued for travel to {} — removing prior pending travel",
                      traveler.getClass().getSimpleName(), entry.getKey().destination());
                if (entry.getValue().isEmpty()) {
                    iterator.remove();
                }
                return;
            }
        }
    }

    /**
     * Returns {@code true} if {@code traveler} is currently queued for travel that has not yet been dispatched by
     * {@link #dispatchPendingTravel(Campaign)}. A queued traveler still sits at its origin location; it has not been
     * moved toward its destination.
     */
    public boolean isQueuedForTravel(ILocation traveler) {
        for (List<ILocation> travelers : pendingTravel.values()) {
            if (containsByIdentity(travelers, traveler)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the destination {@code traveler} is queued to depart for, or {@code null} if it is not queued. Unlike
     * {@link #removeFromPendingTravel}, this leaves the queue untouched. A queued traveler still sits at its origin; it
     * has not begun moving toward this destination.
     */
    public @Nullable ILocation getQueuedDestination(ILocation traveler) {
        for (Map.Entry<TravelRoute, List<ILocation>> entry : pendingTravel.entrySet()) {
            if (containsByIdentity(entry.getValue(), traveler)) {
                return entry.getKey().destination();
            }
        }
        return null;
    }

    /**
     * GM override: dispatches {@code items} to {@code destination} and lands them immediately, with zero transit time.
     *
     * <p>This composes the ordinary {@link LocationDispatch#dispatchTravelers} (which starts the journey and updates
     * hangar/warehouse bookkeeping) with {@link #gmCompleteTravel}, which collapses the freshly-created travel node to
     * arrived and runs the shared arrival pass. Intended to be reachable only from a GM-gated menu path.</p>
     */
    public void gmTeleport(Campaign campaign, Collection<? extends ILocation> items, ILocation destination) {
        LocationDispatch.dispatchTravelers(items, destination, campaign);
        gmCompleteTravel(campaign, items);
    }

    /**
     * GM override: force-completes travel for {@code items} that are queued or already in transit, landing them at
     * their destination now.
     *
     * <p>Selected items still sitting in the pending-travel queue are started immediately so they gain a travel node.
     * Every selected item's in-transit node is then collapsed to arrived — mirroring the finalize in
     * {@link CurrentLocation#newDay} — and the shared {@link #processAllArrivals} pass lands all arrived nodes through
     * the same path the daily cycle uses. Because a travel node is shared by everyone who departed together, completing
     * travel for one item arrives all of its co-travelers. Items that are neither queued nor traveling are left
     * untouched. Intended to be reachable only from a GM-gated menu path.</p>
     */
    public void gmCompleteTravel(Campaign campaign, Collection<? extends ILocation> items) {
        // Start any selected items still queued, so they gain a travel node to collapse below.
        for (ILocation item : items) {
            ILocation queuedDestination = removeFromPendingTravel(item);
            if (queuedDestination != null) {
                LocationDispatch.dispatchTravelers(List.of(item), queuedDestination, campaign);
            }
        }

        // Collapse each distinct in-transit node to arrived, mirroring CurrentLocation.newDay's finalize. Capture every
        // traveler carried by those nodes first — collapsing a shared node arrives co-travelers that were not selected,
        // and they must be gathered before processAllArrivals empties the node.
        Set<ILocation> toRefresh = Collections.newSetFromMap(new IdentityHashMap<>());
        toRefresh.addAll(items);
        for (AbstractMobileLocation node : collectInTransitNodes(items)) {
            toRefresh.addAll(node.fetchPersonnelAtLocation());
            toRefresh.addAll(node.fetchUnitsAtLocation());
            toRefresh.addAll(node.fetchPartsAtLocation());
            node.setTransitTime(0);
            if (node instanceof CurrentLocation currentLocation) {
                currentLocation.setJumpPath(null);
            }
            MekHQ.triggerEvent(new TransitCompleteEvent(node));
        }

        // Land every arrived node through the shared new-day arrival pass.
        processAllArrivals(campaign);

        // Refresh the tables' location columns now — the daily cycle relies on a later full GUI refresh, but a GM
        // override lands mid-session, so fire the per-item changed event each table listens for. Includes co-travelers
        // swept along by a shared node, not just the selected items.
        for (ILocation item : toRefresh) {
            fireLocationChanged(item);
        }
    }

    /**
     * Counts the travelers a GM {@link #gmCompleteTravel} on {@code items} would also arrive but that are not themselves
     * in {@code items} — co-travelers sharing an in-transit node with a selected item. Collapsing a shared travel node
     * arrives everyone it carries, so this lets the menu warn the GM with a "(+ n others)" hint. Queued (not-yet-departed)
     * items contribute nothing, since completing one queued traveler dispatches only that traveler.
     */
    public int countUnselectedCoTravelers(Collection<? extends ILocation> items) {
        Set<ILocation> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        selected.addAll(items);

        Set<ILocation> affected = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractMobileLocation node : collectInTransitNodes(items)) {
            affected.addAll(node.fetchPersonnelAtLocation());
            affected.addAll(node.fetchUnitsAtLocation());
            affected.addAll(node.fetchPartsAtLocation());
        }
        affected.removeAll(selected);
        return affected.size();
    }

    /**
     * Returns the distinct in-transit travel nodes carrying {@code items}, keyed by object identity. An item that has
     * arrived or is not on a mobile node contributes nothing.
     */
    private static Set<AbstractMobileLocation> collectInTransitNodes(Collection<? extends ILocation> items) {
        Set<AbstractMobileLocation> nodes = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ILocation item : items) {
            if (item.getCurrentLocation() instanceof AbstractMobileLocation node && !node.hasArrived()) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * Fires the type-specific "changed" event for {@code item} so the personnel, hangar, and warehouse tables and detail
     * panels refresh their location display immediately after its travel state changes (queued for travel, or a GM
     * travel override). Items that are not a {@link Person}, {@link Unit}, or {@link Part} have no such view and are
     * ignored.
     */
    private static void fireLocationChanged(ILocation item) {
        switch (item) {
            case Person person -> MekHQ.triggerEvent(new PersonChangedEvent(person));
            case Unit unit -> MekHQ.triggerEvent(new UnitChangedEvent(unit));
            case Part part -> MekHQ.triggerEvent(new PartChangedEvent(part));
            default -> {}
        }
    }

    /**
     * Removes {@code traveler} from the pending-travel queue by object identity, pruning any route left empty.
     *
     * @return the destination the traveler was queued for, or {@code null} if it was not queued
     */
    private @Nullable ILocation removeFromPendingTravel(ILocation traveler) {
        for (Iterator<Map.Entry<TravelRoute, List<ILocation>>> it = pendingTravel.entrySet().iterator();
                it.hasNext(); ) {
            Map.Entry<TravelRoute, List<ILocation>> entry = it.next();
            List<ILocation> travelers = entry.getValue();
            if (removeByIdentity(travelers, traveler)) {
                if (travelers.isEmpty()) {
                    it.remove();
                }
                return entry.getKey().destination();
            }
        }
        return null;
    }

    /**
     * Identity ({@code ==}) membership test. {@link ILocation} subtypes such as {@code Personnel} extend
     * {@code LinkedHashMap}, so {@link List#contains} would compare map contents rather than object identity.
     */
    private static boolean containsByIdentity(List<ILocation> list, ILocation target) {
        for (ILocation candidate : list) {
            if (candidate == target) {
                return true;
            }
        }
        return false;
    }

    /** Identity ({@code ==}) removal of the first matching element; see {@link #containsByIdentity}. */
    private static boolean removeByIdentity(List<ILocation> list, ILocation target) {
        for (Iterator<ILocation> it = list.iterator(); it.hasNext(); ) {
            if (it.next() == target) {
                it.remove();
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
            case Person person -> {yield campaign.getPerson(person.getId()) != null;}
            case Unit unit -> campaign.getUnit(unit.getId()) != null;
            case Part part -> findPartAnywhere(campaign, part.getId()) != null;
            default -> true;
        };
    }

    /**
     * Lands every arrived traveler: each registered {@link AbstractLocation}, then each {@link PlayerBase}, then the
     * campaign itself has {@link ILocation#processArrivals} run, draining any travel node that has reached its
     * destination into that destination's personnel/hangar/warehouse.
     *
     * <p>This is the common arrival pass shared by the daily new-day cycle and the GM travel overrides
     * ({@link #gmTeleport}, {@link #gmCompleteTravel}); only nodes that report {@code hasArrived()} land, so it is safe
     * to call at any time.</p>
     */
    public void processAllArrivals(Campaign campaign) {
        for (AbstractLocation location : new ArrayList<>(locations)) {
            location.processArrivals(campaign);
        }
        for (PlayerBase base : playerBases) {
            base.processArrivals(campaign);
        }
        campaign.getPlayerForce().getDetachmentLocationManager().processArrivals(campaign);
    }

    /** Searches the campaign warehouse then all base warehouses for a part by ID. */
    public @Nullable Part findPartAnywhere(Campaign campaign, int partId) {
        Part part = campaign.getPlayerForce().getWarehouse().getPart(partId);
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
     * Removes any tracked top-level {@link AbstractLocation} that is no longer {@link ILocation#isInUse() in use} —
     * one with no {@link Campaign}, {@link mekhq.campaign.base.AbstractBase}, person, unit, or part anywhere in its
     * subtree. The main force's current location is retained automatically because the campaign node sits below it.
     *
     * <p>This handles two leak paths: {@link CurrentLocation} travel nodes whose passengers all
     * died or were removed before arriving, and {@link FixedLocation}/{@link AcademyCampusLocation} pairs that were
     * never cleaned up after the last student graduated.</p>
     *
     * <p>A location whose subtree still holds a queued pending-travel destination is retained even when otherwise
     * empty: dispatching that queued travel later would dereference the destination, so removing it here would leave
     * {@link #dispatchPendingTravel} with a detached node.</p>
     *
     * <p>Call this once per day after all personnel processing has completed.</p>
     */
    public void pruneEmptyLocations() {
        Set<ILocation> pendingDestinations = collectPendingDestinations();
        locations.removeIf(location -> {
            if (location.isInUse() || subtreeHoldsPendingDestination(location, pendingDestinations)) {
                return false;
            }
            if (location instanceof AbstractMobileLocation) {
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
     * Returns {@code true} if {@code location} or any node in its subtree is queued as a pending-travel destination, so
     * that a caller about to remove {@code location} from the tree can spare it — dispatching that queued travel later
     * would otherwise dereference a detached destination.
     */
    public boolean holdsPendingTravelDestination(ILocation location) {
        return subtreeHoldsPendingDestination(location, collectPendingDestinations());
    }

    /** Collects the destinations of all queued pending travel into an identity set. */
    private Set<ILocation> collectPendingDestinations() {
        Set<ILocation> destinations = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TravelRoute route : pendingTravel.keySet()) {
            if (route.destination() != null) {
                destinations.add(route.destination());
            }
        }
        return destinations;
    }

    /**
     * Returns {@code true} if {@code location} or any node in its subtree is one of {@code pendingDestinations}. Used to
     * spare an otherwise-empty node that queued travel is still bound for.
     */
    private static boolean subtreeHoldsPendingDestination(ILocation location, Set<ILocation> pendingDestinations) {
        if (pendingDestinations.isEmpty()) {
            return false;
        }
        if (pendingDestinations.contains(location)) {
            return true;
        }
        for (ILocation child : location.getChildLocations()) {
            if (subtreeHoldsPendingDestination(child, pendingDestinations)) {
                return true;
            }
        }
        return false;
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
        for (ILocation child : campaign.getPlayerForce().getForceDetachment().getChildLocations()) {
            if (child instanceof AcademyCampusLocation campus
                      && academySet.equals(campus.getAcademySet())
                      && academyName.equals(campus.getAcademyName())) {
                return campus;
            }
        }
        AcademyCampusLocation campus = new AcademyCampusLocation(academySet, academyName);
        LocationNode.LocationManager.setLocation(campus, campaign.getPlayerForce().getForceDetachment());
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
        AbstractLocation mainForceLocation = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "locations");
        for (AbstractLocation location : locations) {
            // Skip locations parented to another node — they are serialized inside their parent's XML.
            // Skip the main force's current location — written separately by DetachmentLocationManager as <location>.
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
     * {@link ILocation#writeReferenceToXML}; a destination type that is not referable is logged and skipped.
     */
    private void writePendingTravelToXML(PrintWriter pw, int indent) {
        if (pendingTravel.isEmpty()) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "pendingTravel");
        for (Map.Entry<TravelRoute, List<ILocation>> entry : pendingTravel.entrySet()) {
            ILocation destination = entry.getKey().destination();
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "route");
            if (destination != null && destination.writeReferenceToXML(pw, indent)) {
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
