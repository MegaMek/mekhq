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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConHexGeometry;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * Supply lines: the road network, read as the routes that keep each side's facilities supplied.
 *
 * <p>Each side's supply starts from its sources: its Command Centers, Bases of Operations and Spaceports, and every
 * road that leaves the sector. It runs along road hexes and through that side's own facilities, and jumps from each of
 * that side's Supply Depots to that side's facilities within {@value #DEPOT_RANGE} hexes. The other side's facilities
 * block it, and so does a road hex where the player has cut the enemy's supply (see {@link StratConRoadCut}), for the
 * enemy only.</p>
 *
 * <p>A facility its side's supply reaches is Connected, and is marked as on the supply lines. One that was on them but
 * is no longer reached is Cut: it gets no weekly upkeep, and loses a condition step at the start of each month. A
 * facility never on the supply lines supplies itself, so there is nothing to cut.</p>
 *
 * <p>Supply lines apply only with the improved sector generator, which lays roads, and only while the "Supply Lines"
 * option is on. A sector with no roads has none either.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilitySupply {
    static final String RESOURCE_BUNDLE = StratConEnemyFacilityActivity.RESOURCE_BUNDLE;

    /** How many hexes a Supply Depot reaches without a road. */
    static final int DEPOT_RANGE = 3;

    /** How many days a won convoy interdiction cuts the enemy's supply through its hex. */
    static final int ROAD_CUT_DAYS = 28;

    private StratConFacilitySupply() {}

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
     * Works out which facilities in a sector are cut off, marking every facility its side's supply reaches as on the
     * supply lines.
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

        Set<StratConCoords> playerSupplied = getSuppliedHexes(track, true);
        Set<StratConCoords> enemySupplied = getSuppliedHexes(track, false);
        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            StratConFacility facility = entry.getValue();
            Set<StratConCoords> supplied;
            if (facility.isOwnerAlliedToPlayer()) {
                supplied = playerSupplied;
            } else if (facility.getOwner() == ForceAlignment.Opposing) {
                supplied = enemySupplied;
            } else {
                continue;
            }

            if (supplied.contains(entry.getKey())) {
                facility.setNetworked(true);
            } else if (facility.isNetworked()) {
                cutOff.add(entry.getKey());
            }
        }
        return cutOff;
    }

    /**
     * @param track        the sector
     * @param isPlayerSide {@code true} for the player's side, {@code false} for the enemy's
     *
     * @return every hex that side's supply reaches: road hexes, and that side's facilities
     *
     * @author Illiani
     * @since 0.51.01
     */
    static Set<StratConCoords> getSuppliedHexes(StratConTrackState track, boolean isPlayerSide) {
        Set<StratConCoords> supplied = new HashSet<>();
        Deque<StratConCoords> frontier = new ArrayDeque<>();

        for (StratConCoords roadExit : track.getRoadExits()) {
            if (isPassable(track, roadExit, isPlayerSide) && supplied.add(roadExit)) {
                frontier.add(roadExit);
            }
        }
        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            StratConFacility facility = entry.getValue();
            if (isOnSide(facility, isPlayerSide)
                      && !facility.isDefinitionMissing()
                      && isSupplySource(facility.getFacilityType())
                      && supplied.add(entry.getKey())) {
                frontier.add(entry.getKey());
            }
        }

        while (!frontier.isEmpty()) {
            StratConCoords current = frontier.poll();
            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, current)) {
                if (!supplied.contains(neighbor) && isPassable(track, neighbor, isPlayerSide)) {
                    supplied.add(neighbor);
                    frontier.add(neighbor);
                }
            }

            StratConFacility facility = track.getFacility(current);
            if ((facility != null)
                      && isOnSide(facility, isPlayerSide)
                      && !facility.isDefinitionMissing()
                      && (facility.getFacilityType() == FacilityType.SupplyDepot)) {
                for (StratConCoords nearby : StratConHexGeometry.withinRadius(track, current, DEPOT_RANGE)) {
                    StratConFacility nearbyFacility = track.getFacility(nearby);
                    if ((nearbyFacility != null) && isOnSide(nearbyFacility, isPlayerSide) && supplied.add(nearby)) {
                        frontier.add(nearby);
                    }
                }
            }
        }
        return supplied;
    }

    private static boolean isPassable(StratConTrackState track, StratConCoords coords, boolean isPlayerSide) {
        StratConFacility facility = track.getFacility(coords);
        if (facility != null) {
            return isOnSide(facility, isPlayerSide);
        }
        if (!track.isRoad(coords)) {
            return false;
        }
        return isPlayerSide || !track.isRoadCut(coords);
    }

    private static boolean isOnSide(StratConFacility facility, boolean isPlayerSide) {
        return isPlayerSide ? facility.isOwnerAlliedToPlayer() : (facility.getOwner() == ForceAlignment.Opposing);
    }


    /**
     * The daily supply step for a sector: ends road cuts whose time is up, works out which facilities are cut off,
     * reports those newly cut off or reconnected that the player can see, and at the start of a month costs every cut
     * off facility a condition step. With supply lines out of use, no facility is cut off.
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

        Set<StratConCoords> cutOff = updateCutOffFacilities(track);
        Set<StratConCoords> previouslyCutOff = track.getCutOffFacilities();
        for (StratConCoords coords : cutOff) {
            if (!previouslyCutOff.contains(coords)) {
                reportFacility(campaign, track, coords, "report.cutOff");
            }
        }
        for (StratConCoords coords : previouslyCutOff) {
            StratConFacility facility = track.getFacility(coords);
            // One that has changed sides since is no longer on the supply lines it was cut from, so it is not back.
            if (!cutOff.contains(coords) && (facility != null) && facility.isNetworked()) {
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
     * Cuts the enemy's supply through a road hex for {@value #ROAD_CUT_DAYS} days, after the player wins a fight over
     * the enemy's convoys there. Cutting a hex already cut starts its time again.
     *
     * @param campaign the current campaign
     * @param track    the sector
     * @param coords   the road hex
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
