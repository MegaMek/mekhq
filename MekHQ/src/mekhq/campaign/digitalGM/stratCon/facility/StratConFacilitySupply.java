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
package mekhq.campaign.digitalGM.stratCon.facility;

import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestPlacer;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConSupplyInterdictionBehavior;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConHexGeometry;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConPathfinder;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConPathfinder.Pathing;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConPathfinder.StepCost;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConRoadPlacer;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * Supply lines: the routes that keep each side's facilities supplied.
 *
 * <p>Each side has a supply network on each sector (see {@link StratConSupplyNetwork}): a route from every one of its
 * facilities back to a source of its supply. Its sources are its Comms Centers, Bases of Operations and Space Ports,
 * and every road that leaves the sector. Routes are laid by the same search that lays the roads, at the costs of
 * {@link StratConRoadPlacer#supplyRouteCost}: they keep to the roads where those serve and strike out across country
 * where they don't, but never pass through a facility the other side holds. Each of a side's Supply Depots on its
 * network also links that side's facilities within {@value #DEPOT_RANGE} hexes straight to itself, without a road.</p>
 *
 * <p>Routes are laid once and kept. A facility that turns up, or changes sides, is joined to its side's network by the
 * cheapest way there. A facility taken by the other side, or a source that is lost, sends every route that ran through
 * it the long way round, since that loss lasts. A cut does not: no route is laid around a hex where the player has cut
 * the enemy's supply (see {@link StratConRoadCut}), or where an enemy Supply Interdiction sits across the player's
 * (see {@link StratConSupplyInterdictionBehavior}), so either blocks everything beyond it on the route.</p>
 *
 * <p>A facility whose route reaches a source unblocked is Connected. One whose route is blocked, or that has no route
 * at all, is Cut: it gets no monthly upkeep, and loses a condition step at the start of each month.</p>
 *
 * <p>The enemy also sends Supply Interdictions against the player's routes, as often as it sends engineers to build
 * outposts (see {@link #processSupplyInterdictions}).</p>
 *
 * <p>Supply lines apply only with the improved sector generator, and only while the "Supply Lines" option is on. A
 * sector with no roads has none either.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilitySupply {
    private static final MMLogger LOGGER = MMLogger.create(StratConFacilitySupply.class);

    static final String RESOURCE_BUNDLE = StratConEnemyFacilityActivity.RESOURCE_BUNDLE;

    /**
     * How many hexes a Supply Depot reaches without a road. Public so the map can tell which hexes a depot's straight
     * link to a facility may cross.
     */
    public static final int DEPOT_RANGE = 3;

    /** How many days a won convoy interdiction cuts the enemy's supply through its hex. */
    static final int ROAD_CUT_DAYS = 28;

    private StratConFacilitySupply() {
    }

    /**
     * @param campaign the current campaign
     *
     * @return {@code true} if supply lines are in use: the "Supply Lines" option is on, sectors are laid out by the
     *       improved generator, and play is not mapless
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isSupplyLinesActive(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        return Boolean.TRUE.equals(campaignOptions.get(CampaignOption.USE_SUPPLY_LINES))
                     && Boolean.TRUE.equals(campaignOptions.get(CampaignOption.USE_STRAT_CON_ALTERNATE_SECTOR_TERRAIN))
                     && !campaignOptions.isUseStratConMaplessMode();
    }

    /**
     * @param facilityType a facility type
     *
     * @return {@code true} if a facility of the type is a source of its side's supply
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isSupplySource(FacilityType facilityType) {
        return (facilityType == FacilityType.CommandCenter)
                     || (facilityType == FacilityType.BaseOfOperations)
                     || (facilityType == FacilityType.SpacePort);
    }

    /**
     * Lays both sides' supply routes in a sector afresh, from nothing. Call when the sector itself is laid out - when
     * its roads are laid or relaid - since the routes follow the roads; anything else only needs
     * {@link #reconcileNetworks}.
     *
     * @param track the sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void rebuildNetworks(StratConTrackState track) {
        track.getPlayerSupplyNetwork().clear();
        track.getEnemySupplyNetwork().clear();
        reconcileNetworks(track);
    }

    /**
     * Brings both sides' supply routes in a sector up to date with the facilities now on it, changing only what has to
     * change:
     *
     * <ul>
     *     <li>the roots become the side's sources as they now stand;</li>
     *     <li>routes through a hex the other side now holds are dropped, as are routes that no longer lead from one of
     *     the side's facilities to a root;</li>
     *     <li>every facility of the side left without a route is joined to the network, if a way can be found.</li>
     * </ul>
     *
     * <p>Cuts are left alone: they block a route without changing it. A sector with no roads keeps no routes.</p>
     *
     * @param track the sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void reconcileNetworks(StratConTrackState track) {
        reconcileNetwork(track, true);
        reconcileNetwork(track, false);
    }

    private static void reconcileNetwork(StratConTrackState track, boolean isPlayerSide) {
        StratConSupplyNetwork network = track.getSupplyNetwork(isPlayerSide);
        if (track.getRoads().isEmpty()) {
            network.clear();
            return;
        }

        network.replaceRoots(getSourceHexes(track, isPlayerSide));
        // What the other side now holds is lost to this one for good, so the routes through it are laid again.
        network.removeHexes(coords -> isHeldByOtherSide(track, coords, isPlayerSide));

        List<StratConCoords> facilityHexes = getFacilityHexes(track, isPlayerSide);
        network.retainRoutesFrom(facilityHexes);
        connectUnroutedFacilities(track, network, facilityHexes, isPlayerSide);
    }

    /**
     * @return the hexes a side's supply starts from: its sources, and every road exit the other side doesn't hold
     */
    private static Set<StratConCoords> getSourceHexes(StratConTrackState track, boolean isPlayerSide) {
        Set<StratConCoords> sources = new HashSet<>();
        for (StratConCoords roadExit : track.getRoadExits()) {
            if (!isHeldByOtherSide(track, roadExit, isPlayerSide)) {
                sources.add(roadExit);
            }
        }
        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            StratConFacility facility = entry.getValue();
            if (isOnSide(facility, isPlayerSide)
                      && !facility.isDefinitionMissing()
                      && isSupplySource(facility.getFacilityType())) {
                sources.add(entry.getKey());
            }
        }
        return sources;
    }

    private static List<StratConCoords> getFacilityHexes(StratConTrackState track, boolean isPlayerSide) {
        List<StratConCoords> facilityHexes = new ArrayList<>();
        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            if (isOnSide(entry.getValue(), isPlayerSide)) {
                facilityHexes.add(entry.getKey());
            }
        }
        return facilityHexes;
    }

    /**
     * Joins each of a side's facilities that has no route to the network, nearest first, as the roads join cities
     * (Prim's): Supply Depot links first, since they cost nothing, then the cheapest way from the network as it stands
     * to the nearest facility still without a route, again and again. A facility no way reaches is left without one.
     */
    private static void connectUnroutedFacilities(StratConTrackState track, StratConSupplyNetwork network,
          List<StratConCoords> facilityHexes, boolean isPlayerSide) {
        Set<StratConCoords> unrouted = new LinkedHashSet<>();
        for (StratConCoords facilityHex : facilityHexes) {
            if (!network.isRouted(facilityHex)) {
                unrouted.add(facilityHex);
            }
        }

        StepCost stepCost = StratConRoadPlacer.supplyRouteCost(track,
              coords -> isHeldByOtherSide(track, coords, isPlayerSide));
        while (!unrouted.isEmpty()) {
            linkToSupplyDepots(track, network, unrouted, isPlayerSide);
            if (unrouted.isEmpty()) {
                break;
            }

            List<StratConCoords> networkHexes = new ArrayList<>(network.hexes());
            if (networkHexes.isEmpty()) {
                break;
            }

            Pathing pathing = StratConPathfinder.pathfind(track, networkHexes, stepCost);
            StratConCoords nearest = pathing.nearest(unrouted);
            if (nearest == null) {
                break;
            }

            // The path runs from the facility back to the network, the way its supply links point.
            List<StratConCoords> path = pathing.pathTo(nearest);
            for (int step = 0; step < (path.size() - 1); step++) {
                network.addLink(path.get(step), path.get(step + 1));
            }
            // The new route may have passed through others of the side's facilities on its way.
            unrouted.removeIf(network::isRouted);
        }
    }

    /**
     * Links every facility still without a route that lies within {@value #DEPOT_RANGE} hexes of one of its side's
     * Supply Depots on the network straight to that depot, until no more can be linked.
     */
    private static void linkToSupplyDepots(StratConTrackState track, StratConSupplyNetwork network,
          Set<StratConCoords> unrouted, boolean isPlayerSide) {
        boolean isLinked = true;
        while (isLinked && !unrouted.isEmpty()) {
            isLinked = false;
            for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
                StratConFacility depot = entry.getValue();
                StratConCoords depotCoords = entry.getKey();
                if (!isOnSide(depot, isPlayerSide)
                          || depot.isDefinitionMissing()
                          || (depot.getFacilityType() != FacilityType.SupplyDepot)
                          || !network.isRouted(depotCoords)) {
                    continue;
                }

                for (StratConCoords nearby : StratConHexGeometry.withinRadius(track, depotCoords, DEPOT_RANGE)) {
                    if (unrouted.remove(nearby)) {
                        network.addLink(nearby, depotCoords);
                        isLinked = true;
                    }
                }
            }
        }
    }

    /**
     * Works out which facilities in a sector are cut off, marking every facility with a route to one of its side's
     * roots as on the supply lines. Reads the routes as they stand; {@link #processSupply} brings them up to date
     * first.
     *
     * @param track the sector
     *
     * @return the hexes of the facilities that are cut off; empty for a sector with no roads
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Set<StratConCoords> updateCutOffFacilities(StratConTrackState track) {
        Set<StratConCoords> cutOff = new HashSet<>();
        if (track.getRoads().isEmpty()) {
            return cutOff;
        }

        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            StratConFacility facility = entry.getValue();
            boolean isPlayerSide;
            if (facility.isOwnerAlliedToPlayer()) {
                isPlayerSide = true;
            } else if (facility.getOwner() == ForceAlignment.Opposing) {
                isPlayerSide = false;
            } else {
                continue;
            }

            if (track.getSupplyNetwork(isPlayerSide).isRouted(entry.getKey())) {
                facility.setNetworked(true);
            }
            if (!isSupplied(track, entry.getKey(), isPlayerSide)) {
                cutOff.add(entry.getKey());
            }
        }
        return cutOff;
    }

    /**
     * @param track        the sector
     * @param coords       a facility's hex
     * @param isPlayerSide {@code true} if the facility is on the player's side, {@code false} if on the enemy's
     *
     * @return {@code true} if the facility's route reaches one of its side's roots with nothing blocking it: no hex the
     *       other side holds and, for the enemy, no hex where the player has cut its supply, or for the player, no hex
     *       held by an enemy Supply Interdiction
     */
    static boolean isSupplied(StratConTrackState track, StratConCoords coords, boolean isPlayerSide) {
        StratConSupplyNetwork network = track.getSupplyNetwork(isPlayerSide);
        List<StratConCoords> route = network.routeFrom(coords);
        if (!network.isRoot(route.get(route.size() - 1))) {
            return false;
        }

        for (StratConCoords hex : route) {
            boolean isCut = isPlayerSide ?
                                  StratConSupplyInterdictionBehavior.isInterdicted(track, hex) :
                                  track.isRoadCut(hex);
            if (isCut || isHeldByOtherSide(track, hex, isPlayerSide)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param track        the sector
     * @param coords       a hex
     * @param isPlayerSide {@code true} for the player's side's routes, {@code false} for the enemy's
     *
     * @return {@code true} if one of that side's supply routes runs through the hex, or its supply starts there
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isOnSupplyNetwork(StratConTrackState track, StratConCoords coords, boolean isPlayerSide) {
        return track.getSupplyNetwork(isPlayerSide).contains(coords);
    }

    private static boolean isHeldByOtherSide(StratConTrackState track, StratConCoords coords, boolean isPlayerSide) {
        StratConFacility facility = track.getFacility(coords);
        return (facility != null) && !isOnSide(facility, isPlayerSide);
    }

    private static boolean isOnSide(StratConFacility facility, boolean isPlayerSide) {
        return isPlayerSide ? facility.isOwnerAlliedToPlayer() : (facility.getOwner() == ForceAlignment.Opposing);
    }

    /**
     * The daily supply step for a sector: ends road cuts whose time is up, brings the supply routes up to date, works
     * out which facilities are cut off, reports those newly cut off or reconnected that the player can see, and at the
     * start of a month costs every cut off facility a condition step. With supply lines out of use, no facility is cut
     * off.
     *
     * @param track          the sector
     * @param campaign       the current campaign
     * @param isStartOfMonth {@code true} on the first day of the month
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processSupply(StratConTrackState track, Campaign campaign, boolean isStartOfMonth) {
        LocalDate today = campaign.getLocalDate();
        track.getRoadCuts().removeIf(roadCut -> !today.isBefore(roadCut.getEndDate()));

        if (!isSupplyLinesActive(campaign)) {
            track.getCutOffFacilities().clear();
            return;
        }

        reconcileNetworks(track);

        // Only a facility still with the side it was cut off on can be back on its supply lines: changing sides clears
        // its mark, so it must be read before the check below sets the mark afresh.
        Set<StratConCoords> previouslyCutOff = track.getCutOffFacilities();
        Set<StratConCoords> stillOnSameSide = new HashSet<>();
        for (StratConCoords coords : previouslyCutOff) {
            StratConFacility facility = track.getFacility(coords);
            if ((facility != null) && facility.isNetworked()) {
                stillOnSameSide.add(coords);
            }
        }

        Set<StratConCoords> cutOff = updateCutOffFacilities(track);
        for (StratConCoords coords : cutOff) {
            if (!previouslyCutOff.contains(coords)) {
                reportFacility(campaign, track, coords, "report.cutOff");
            }
        }
        for (StratConCoords coords : stillOnSameSide) {
            if (!cutOff.contains(coords)) {
                reportFacility(campaign, track, coords, "report.reconnected");
            }
        }
        previouslyCutOff.clear();
        previouslyCutOff.addAll(cutOff);

        if (isStartOfMonth) {
            for (StratConCoords coords : cutOff) {
                StratConFacility facility = track.getFacility(coords);
                facility.setCondition(facility.getCondition().worsened());
                reportFacility(campaign, track, coords, "report.starved");
            }
        }
    }

    /**
     * The contract-level daily step for enemy Supply Interdictions: schedules the next month's once the last month's
     * schedule runs out - as many as the enemy sends engineers (see
     * {@link StratConEnemyFacilityActivity#getMonthlyEngineerCount}) - and places those due today across the player's
     * supply routes in a random sector. One that finds no route to sit on is dropped. None are sent while supply lines
     * are out of use or the enemy is idle, and none are placed against a routed enemy.
     *
     * @param campaign      the current campaign
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processSupplyInterdictions(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState) {
        List<LocalDate> interdictionDates = campaignState.getSupplyInterdictionDates();
        // Anything scheduled before supply lines were turned off, or the enemy turned idle, is called off.
        if (!isSupplyLinesActive(campaign) || (StratConEnemyFacilityActivity.getActivity(campaign) <= 0)) {
            interdictionDates.clear();
            return;
        }

        LocalDate today = campaign.getLocalDate();
        LocalDate scheduledUntil = campaignState.getSupplyInterdictionsScheduledUntil();
        if ((scheduledUntil == null) || !today.isBefore(scheduledUntil)) {
            scheduleSupplyInterdictions(campaign, contract, campaignState, today);
        }

        int dueCount = 0;
        for (LocalDate interdictionDate : interdictionDates) {
            if (!interdictionDate.isAfter(today)) {
                dueCount++;
            }
        }
        if (dueCount == 0) {
            return;
        }
        interdictionDates.removeIf(interdictionDate -> !interdictionDate.isAfter(today));

        // The enemy is in no state to raid while routed.
        if (contract.getMoraleLevel().isRouted()) {
            return;
        }

        List<StratConPointOfInterest> placed = new ArrayList<>();
        for (int interdiction = 0; interdiction < dueCount; interdiction++) {
            StratConPointOfInterest pointOfInterest = placeSupplyInterdiction(campaignState, today);
            if (pointOfInterest == null) {
                LOGGER.info("No sector of contract {} had a supply route to interdict.", contract.getName());
            } else {
                placed.add(pointOfInterest);
            }
        }
        StratConPointOfInterestRules.announceNewPointsOfInterest(campaign, contract, placed);
    }

    private static void scheduleSupplyInterdictions(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState, LocalDate today) {
        LocalDate monthEnd = today.plusMonths(1);
        campaignState.setSupplyInterdictionsScheduledUntil(monthEnd);

        LocalDate contractEnd = contract.getEndingDate();
        if ((contractEnd != null) && contractEnd.isBefore(monthEnd)) {
            monthEnd = contractEnd;
        }
        int dayCount = (int) ChronoUnit.DAYS.between(today, monthEnd);
        if (dayCount <= 0) {
            return;
        }

        int interdictionCount = StratConEnemyFacilityActivity.getMonthlyEngineerCount(contract.getScale(),
              campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION),
              StratConEnemyFacilityActivity.getActivity(campaign));
        for (int interdiction = 0; interdiction < interdictionCount; interdiction++) {
            campaignState.getSupplyInterdictionDates().add(today.plusDays(Compute.randomInt(dayCount)));
        }
    }

    private static @Nullable StratConPointOfInterest placeSupplyInterdiction(StratConCampaignState campaignState,
          LocalDate today) {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(
              StratConSupplyInterdictionBehavior.TYPE_ID);
        if (definition == null) {
            return null;
        }

        List<StratConTrackState> tracks = new ArrayList<>(campaignState.getTracks());
        while (!tracks.isEmpty()) {
            StratConTrackState track = tracks.remove(Compute.randomInt(tracks.size()));
            List<StratConCoords> candidates = getInterdictionCandidates(track, definition);
            if (candidates.isEmpty()) {
                continue;
            }

            StratConPointOfInterest pointOfInterest = StratConPointOfInterestPlacer.place(track,
                  StratConSupplyInterdictionBehavior.TYPE_ID,
                  candidates.get(Compute.randomInt(candidates.size())),
                  today);
            if (pointOfInterest != null) {
                return pointOfInterest;
            }
        }
        return null;
    }

    /**
     * @param track      a sector
     * @param definition the Supply Interdiction definition
     *
     * @return the hexes of the sector a Supply Interdiction could sit on: those along the player's side's supply routes
     *       that hold no facility, that its definition allows, and where no player formation stands. A root no route
     *       runs from carries no supply, so it doesn't count.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<StratConCoords> getInterdictionCandidates(StratConTrackState track,
          StratConPointOfInterestDefinition definition) {
        Set<StratConCoords> routeHexes = new LinkedHashSet<>();
        for (StratConSupplyLink link : track.getPlayerSupplyNetwork().getLinks()) {
            routeHexes.add(link.getFrom());
            routeHexes.add(link.getTo());
        }

        Collection<StratConCoords> forceCoords = track.getAssignedForceCoords().values();
        List<StratConCoords> candidates = new ArrayList<>();
        for (StratConCoords coords : routeHexes) {
            if ((track.getFacility(coords) == null)
                      && !forceCoords.contains(coords)
                      && StratConPointOfInterestPlacer.canPlace(track, definition, coords)) {
                candidates.add(coords);
            }
        }
        return candidates;
    }

    /**
     * @param track  the sector
     * @param coords a facility's hex
     *
     * @return {@code true} if the facility was cut off when supply was last checked
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isCutOff(StratConTrackState track, StratConCoords coords) {
        return track.getCutOffFacilities().contains(coords);
    }

    /**
     * Cuts the enemy's supply through a hex on its supply routes for {@value #ROAD_CUT_DAYS} days, after the player
     * wins a fight over the enemy's convoys there. Cutting a hex already cut starts its time again.
     *
     * @param campaign the current campaign
     * @param track    the sector
     * @param coords   the hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void cutRoad(Campaign campaign, StratConTrackState track, StratConCoords coords) {
        track.getRoadCuts().removeIf(roadCut -> coords.equals(roadCut.getCoords()));
        track.getRoadCuts().add(new StratConRoadCut(coords, campaign.getLocalDate().plusDays(ROAD_CUT_DAYS)));
        campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
              "report.roadCut",
              track.getDisplayableName(),
              coords.toBTString(),
              ROAD_CUT_DAYS));

        // Checked at once, so the player sees what the cut did without waiting a day.
        if (isSupplyLinesActive(campaign)) {
            processSupply(track, campaign, false);
        }
    }

    private static void reportFacility(Campaign campaign, StratConTrackState track, StratConCoords coords,
          String key) {
        StratConFacility facility = track.getFacility(coords);
        if ((facility == null) || !facility.getVisible()) {
            return;
        }
        campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
              key,
              facility.getDisplayableName(),
              track.getDisplayableName(),
              coords.toBTString()));
    }
}
