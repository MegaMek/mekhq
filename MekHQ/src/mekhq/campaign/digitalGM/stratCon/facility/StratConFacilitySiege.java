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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The rules for sieges: formations given the {@link FacilityOperation#SIEGE} order from a hex next to an enemy
 * facility, grinding its garrison down without fighting all of it.
 *
 * <ul>
 *     <li><b>Start.</b> The formation stays deployed until the siege ends. When the first formation starts a siege on a
 *     facility that is not cut off from its supply lines, the enemy may bring a counterattack forward as a relief force
 *     against it, on the same odds as any counterattack.</li>
 *     <li><b>Each Monday.</b> Every besieging formation costs {@link #SIEGE_WEEKLY_COST} support point, and gains
 *     fatigue as a Patrol does; a formation the player cannot pay for lifts its siege. The facility loses a garrison
 *     step per besieging formation, at most {@link #MAXIMUM_WEEKLY_GARRISON_LOSS}, and gets no weekly upkeep. Then,
 *     unless it is cut off, the garrison may sortie on the sector's usual scenario odds, against one besieging
 *     formation.</li>
 *     <li><b>Fights.</b> Winning a sortie costs the facility another garrison step; beating a relief force changes
 *     nothing. Losing either breaks the siege.</li>
 *     <li><b>End.</b> At no garrison, the facility surrenders: the player takes it without a fight and chooses its
 *     fate, as after a capture. The player can lift a siege at any time.</li>
 * </ul>
 *
 * <p>A cut-off facility can neither sortie nor be relieved, so a siege on one ends in surrender unless the player lifts
 * it.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilitySiege {
    /** The support points each besieging formation costs each Monday. */
    static final int SIEGE_WEEKLY_COST = 1;

    /** The most garrison steps a siege takes from a facility each week, however many formations besiege it. */
    static final int MAXIMUM_WEEKLY_GARRISON_LOSS = 2;

    private StratConFacilitySiege() {
    }

    /**
     * @param track  a sector
     * @param coords a facility's hex
     *
     * @return the siege orders on that facility, in the order they were given
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConFacilityOrder> getSieges(StratConTrackState track, StratConCoords coords) {
        List<StratConFacilityOrder> sieges = new ArrayList<>();
        for (StratConFacilityOrder order : track.getFacilityOrders()) {
            if ((order.getOperation() == FacilityOperation.SIEGE) && order.getTargetCoords().equals(coords)) {
                sieges.add(order);
            }
        }
        return sieges;
    }

    /**
     * @param track  a sector
     * @param coords a facility's hex
     *
     * @return {@code true} if any formation is besieging the facility
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isBesieged(StratConTrackState track, StratConCoords coords) {
        return !getSieges(track, coords).isEmpty();
    }

    /**
     * @param besiegerCount how many formations are besieging a facility
     *
     * @return how many garrison steps the facility loses this week
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getWeeklyGarrisonLoss(int besiegerCount) {
        return Math.max(0, Math.min(besiegerCount, MAXIMUM_WEEKLY_GARRISON_LOSS));
    }

    /**
     * Starts a siege, once the order has been paid for: the formation holds its position, and if it is the first to
     * besiege the facility, the enemy may send a relief force (see {@link #tryRelief}).
     *
     * @param campaign    the current campaign
     * @param contract    the contract whose map holds the sector
     * @param track       the sector
     * @param coords      the besieged facility's hex
     * @param formationId the ID of the besieging formation
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void startSiege(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords coords, int formationId) {
        boolean isFirstBesieger = !isBesieged(track, coords);
        // A siege has no set end; the order's date records the day it began.
        track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.SIEGE,
              formationId,
              coords,
              campaign.getLocalDate(),
              null));
        track.addStickyForce(formationId);

        StratConFacility facility = track.getFacility(coords);
        StratConFacilityOperations.report(campaign, "report.siege.started", facility.getDisplayableName());

        if (isFirstBesieger) {
            tryRelief(campaign, contract, track, coords, formationId);
        }
    }

    /**
     * Rolls whether the enemy brings a counterattack forward as a relief force against a new siege, on the same odds
     * as any counterattack (see {@link StratConEnemyFacilityActivity#isCounterattack}). A relief force counts as the
     * enemy's latest counterattack. A cut-off facility cannot be relieved.
     *
     * @param campaign    the current campaign
     * @param contract    the contract whose map holds the sector
     * @param track       the sector
     * @param coords      the besieged facility's hex
     * @param formationId the ID of the besieging formation the relief force falls on
     *
     * @return the relief force's scenario, or {@code null} if none came
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable StratConScenario tryRelief(Campaign campaign, AbstractContract contract,
          StratConTrackState track, StratConCoords coords, int formationId) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if ((campaignState == null) || StratConFacilitySupply.isCutOff(track, coords)) {
            return null;
        }

        long daysSinceLastAttack = StratConEnemyFacilityActivity.getDaysSinceLastCounterattack(contract,
              campaignState,
              campaign.getLocalDate());
        boolean isRelieved = StratConEnemyFacilityActivity.isCounterattack(
              StratConEnemyFacilityActivity.getActivity(campaign),
              daysSinceLastAttack,
              Compute.randomInt(100));
        if (!isRelieved) {
            return null;
        }

        StratConCoords formationCoords = track.getAssignedForceCoords().get(formationId);
        if (formationCoords == null) {
            return null;
        }

        StratConScenario scenario = StratConRulesManager.startSiegeScenario(campaign,
              contract,
              track,
              formationCoords,
              formationId,
              coords,
              false);
        if (scenario != null) {
            campaignState.setLastCounterattackDate(campaign.getLocalDate());
            StratConFacilityOperations.report(campaign,
                  "report.siege.relief",
                  track.getFacility(coords).getDisplayableName());
        }
        return scenario;
    }

    /**
     * Lifts one formation's siege, at the player's word. The support points already paid are not returned.
     *
     * @param campaign    the current campaign
     * @param track       the sector
     * @param formationId the ID of the besieging formation
     *
     * @return {@code true} if the formation was besieging anything
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean liftSiege(Campaign campaign, StratConTrackState track, int formationId) {
        StratConFacilityOrder order = track.getFacilityOrder(formationId);
        if ((order == null) || (order.getOperation() != FacilityOperation.SIEGE)) {
            return false;
        }

        StratConFacilityOperations.endOrder(track, order);
        StratConFacilityOperations.report(campaign,
              "report.siege.lifted",
              StratConFacilityOperations.getFormationName(campaign, formationId));
        return true;
    }

    /**
     * Ends every siege on a facility.
     *
     * @param track  the sector
     * @param coords the besieged facility's hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void endSieges(StratConTrackState track, StratConCoords coords) {
        for (StratConFacilityOrder order : getSieges(track, coords)) {
            StratConFacilityOperations.endOrder(track, order);
        }
    }

    /**
     * The Monday step for every siege in a sector, before the enemy's weekly upkeep (see the class description).
     *
     * @param track    the sector
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processSieges(StratConTrackState track, Campaign campaign) {
        if (track.getFacilityOrders().isEmpty()) {
            return;
        }

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        StratConCampaignState campaignState = (contract == null) ? null : contract.getStratConCampaignState();
        if (campaignState == null) {
            return;
        }

        Set<StratConCoords> besiegedCoords = new LinkedHashSet<>();
        for (StratConFacilityOrder order : track.getFacilityOrders()) {
            if (order.getOperation() == FacilityOperation.SIEGE) {
                besiegedCoords.add(order.getTargetCoords());
            }
        }

        for (StratConCoords coords : besiegedCoords) {
            StratConFacility facility = track.getFacility(coords);
            if ((facility == null) || facility.isOwnerAlliedToPlayer()) {
                // The daily order check ends these; nothing to settle.
                continue;
            }

            List<Integer> besiegerIds = payBesiegers(campaign, campaignState, track, coords);
            if (besiegerIds.isEmpty()) {
                continue;
            }

            facility.setGarrison(facility.getGarrison() - getWeeklyGarrisonLoss(besiegerIds.size()));
            if (facility.getGarrison() <= 0) {
                surrender(campaign, contract, track, coords);
                continue;
            }

            StratConFacilityOperations.report(campaign, "report.siege.weakened", facility.getDisplayableName());
            trySortie(campaign, contract, track, coords, besiegerIds, Compute.randomInt(100));
        }
    }

    /**
     * Charges this week's support points for each formation besieging a facility, and tires its crews as a Patrol
     * does. A formation the player cannot pay for lifts its siege.
     *
     * @return the IDs of the formations still besieging the facility
     */
    private static List<Integer> payBesiegers(Campaign campaign, StratConCampaignState campaignState,
          StratConTrackState track, StratConCoords coords) {
        List<Integer> besiegerIds = new ArrayList<>();
        for (StratConFacilityOrder order : getSieges(track, coords)) {
            int formationId = order.getFormationId();
            if (campaignState.getSupportPoints() < SIEGE_WEEKLY_COST) {
                StratConFacilityOperations.endOrder(track, order);
                StratConFacilityOperations.report(campaign,
                      "report.siege.unpaid",
                      StratConFacilityOperations.getFormationName(campaign, formationId));
                continue;
            }

            campaignState.changeSupportPoints(-SIEGE_WEEKLY_COST);
            if (campaign.getPlayerForce().getFormation(formationId) != null) {
                StratConRulesManager.increaseFatigue(formationId, campaign);
            }
            besiegerIds.add(formationId);
        }
        return besiegerIds;
    }

    /**
     * Rolls whether a besieged garrison sorties this week, on the sector's usual scenario odds. It never does while
     * cut off, nor while a fight over the siege is already under way.
     *
     * @param campaign    the current campaign
     * @param contract    the contract whose map holds the sector
     * @param track       the sector
     * @param coords      the besieged facility's hex
     * @param besiegerIds the IDs of the formations besieging it
     * @param roll        a roll from 0 to 99
     *
     * @return the sortie's scenario, or {@code null} if there was none
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable StratConScenario trySortie(Campaign campaign, AbstractContract contract,
          StratConTrackState track, StratConCoords coords, List<Integer> besiegerIds, int roll) {
        if (besiegerIds.isEmpty()
                  || StratConFacilitySupply.isCutOff(track, coords)
                  || isSiegeFightUnderway(track, coords)
                  || (roll > StratConRulesManager.calculateScenarioOdds(track, contract, false))) {
            return null;
        }

        int formationId = besiegerIds.get(Compute.randomInt(besiegerIds.size()));
        StratConCoords formationCoords = track.getAssignedForceCoords().get(formationId);
        if (formationCoords == null) {
            return null;
        }

        StratConScenario scenario = StratConRulesManager.startSiegeScenario(campaign,
              contract,
              track,
              formationCoords,
              formationId,
              coords,
              true);
        if (scenario != null) {
            StratConFacilityOperations.report(campaign,
                  "report.siege.sortie",
                  track.getFacility(coords).getDisplayableName());
        }
        return scenario;
    }

    /**
     * @param track  a sector
     * @param coords a besieged facility's hex
     *
     * @return {@code true} if a sortie or relief force against the siege on that facility is still to be fought
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isSiegeFightUnderway(StratConTrackState track, StratConCoords coords) {
        for (StratConScenario scenario : track.getScenarios().values()) {
            if (coords.equals(scenario.getSiegeCoords())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Settles a fight over a siege. A loss breaks the siege; a won sortie costs the facility a garrison step, which
     * may make it surrender.
     *
     * @param campaign       the current campaign
     * @param contract       the contract whose map holds the sector
     * @param track          the sector
     * @param facilityCoords the besieged facility's hex
     * @param isSortie       {@code true} for a sortie, {@code false} for a relief force
     * @param isVictory      {@code true} if the player won
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void resolveSiegeScenario(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords facilityCoords, boolean isSortie, boolean isVictory) {
        StratConFacility facility = track.getFacility(facilityCoords);
        if ((facility == null) || facility.isOwnerAlliedToPlayer() || !isBesieged(track, facilityCoords)) {
            return;
        }

        String facilityName = facility.getDisplayableName();
        if (!isVictory) {
            endSieges(track, facilityCoords);
            StratConFacilityOperations.report(campaign,
                  isSortie ? "report.siege.brokenBySortie" : "report.siege.brokenByRelief",
                  facilityName);
            return;
        }

        if (!isSortie) {
            StratConFacilityOperations.report(campaign, "report.siege.reliefBeaten", facilityName);
            return;
        }

        facility.setGarrison(facility.getGarrison() - 1);
        if (facility.getGarrison() <= 0) {
            surrender(campaign, contract, track, facilityCoords);
        } else {
            StratConFacilityOperations.report(campaign, "report.siege.sortieBeaten", facilityName);
        }
    }

    /**
     * A besieged facility with no garrison left surrenders: every siege on it ends, and the player takes it without a
     * fight and chooses what to do with it, as after a capture.
     *
     * @param campaign the current campaign
     * @param contract the contract whose map holds the sector
     * @param track    the sector
     * @param coords   the facility's hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void surrender(Campaign campaign, AbstractContract contract, StratConTrackState track,
          StratConCoords coords) {
        StratConFacility facility = track.getFacility(coords);
        endSieges(track, coords);
        if (facility == null) {
            return;
        }

        facility.raiseIntel(StratConFacility.FacilityIntel.LOCATED);
        StratConFacilityOperations.report(campaign, "report.siege.surrendered", facility.getDisplayableName());
        StratConFacilityOperations.resolveCapture(campaign,
              contract,
              track,
              coords,
              StratConFacilityOperations.askCaptureChoice(campaign, facility));
    }
}
