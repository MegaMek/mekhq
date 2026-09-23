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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.STRATCON_SECTOR_COMMAND_STRING;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.ButtonLabelTooltipPair;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

/**
 * The StratCon rules that act on points of interest during play: the points where the rest of StratCon hands control
 * to a point of interest's {@link IStratConPointOfInterestBehavior}.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConPointOfInterestRules {
    /** The resource bundle holding every point of interest type's player-facing text, keyed by behavior ID. */
    static final String RESOURCE_BUNDLE = "mekhq.resources.StratConPointOfInterest";

    private StratConPointOfInterestRules() {
    }

    /**
     * Tells the player what has just happened at a point of interest: adds the report to the daily report and, when
     * the campaign is being played through the GUI, also shows it as a small notification dialog so it is not missed.
     *
     * <p>Every point of interest report should go through this rather than straight to the daily report.</p>
     *
     * @param campaign   the current campaign
     * @param reportType the daily report tab the report belongs on
     * @param report     the report text
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void reportToPlayer(Campaign campaign, DailyReportType reportType, String report) {
        campaign.addReport(reportType, report);

        // No GUI means no player watching (for example, a headless or test campaign), so the daily report is enough.
        if (campaign.getGUI() != null) {
            new ImmersiveDialogNotification(campaign, report, ImmersiveDialogWidth.SMALL, true);
        }
    }

    /**
     * Has the contract's liaison tell the player about points of interest that have just appeared on the contract's
     * map, all in one dialog. Each is listed with a link that opens its sector on the StratCon tab. Points of interest
     * the player cannot see yet are left out, so a hidden one is not given away; if none are left, nothing is shown.
     *
     * <p>The dialog is not modal, so the player can follow the links while it is open. Nothing is shown when the
     * campaign has no GUI (for example, a headless or test campaign).</p>
     *
     * @param campaign                 the current campaign
     * @param contract                 the contract the points of interest belong to
     * @param placedPointsOfInterest   the points of interest just placed on that contract's map
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void announceNewPointsOfInterest(Campaign campaign, AbstractContract contract,
          List<StratConPointOfInterest> placedPointsOfInterest) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if ((campaign.getGUI() == null) || (campaignState == null) || placedPointsOfInterest.isEmpty()) {
            return;
        }

        List<StratConTrackState> tracks = campaignState.getTracks();
        StringBuilder entries = new StringBuilder();
        int announcedCount = 0;
        for (StratConPointOfInterest pointOfInterest : placedPointsOfInterest) {
            for (int trackIndex = 0; trackIndex < tracks.size(); trackIndex++) {
                StratConTrackState track = tracks.get(trackIndex);
                if ((track.getPointOfInterest(pointOfInterest.getId()) == null)
                          || !pointOfInterest.isVisibleToPlayer(track)) {
                    continue;
                }

                String sectorLink = String.format("<a href='%s:%s:%d'>%s</a>",
                      STRATCON_SECTOR_COMMAND_STRING,
                      contract.getId(),
                      trackIndex,
                      track.getDisplayableName());
                entries.append(getFormattedTextAt(RESOURCE_BUNDLE,
                      "newPointOfInterest.entry",
                      pointOfInterest.getDisplayableName(),
                      sectorLink,
                      pointOfInterest.getCoords().toBTString()));
                announcedCount++;
                break;
            }
        }

        if (announcedCount == 0) {
            return;
        }

        String messageKey = (announcedCount == 1) ? "newPointOfInterest.single" : "newPointOfInterest.multiple";
        String message = getFormattedTextAt(RESOURCE_BUNDLE,
              messageKey,
              campaign.getCommanderAddress(),
              entries.toString());

        new ImmersiveDialogCore(campaign,
              contract.getEmployerLiaison(),
              null,
              message,
              List.of(new ButtonLabelTooltipPair(getText("Understood.text"), null)),
              getTextAt(RESOURCE_BUNDLE, "newPointOfInterest.ooc"),
              null,
              false,
              null,
              null,
              false);
    }

    /**
     * Marks a point of interest as resolved - dealt with by the player - and records every strategic objective tied to
     * it as met. Recording the objectives means they stay met even if the point of interest is removed from the map
     * afterward, so a type may clear away a point of interest once it has been dealt with.
     *
     * <p>Behaviors should resolve points of interest through this rather than by setting their status directly.</p>
     *
     * @param track           the sector the point of interest sits in
     * @param pointOfInterest the point of interest to resolve
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void resolvePointOfInterest(StratConTrackState track, StratConPointOfInterest pointOfInterest) {
        pointOfInterest.setStatus(PointOfInterestStatus.RESOLVED);

        for (StratConStrategicObjective objective : getStrategicObjectives(track, pointOfInterest)) {
            objective.setCurrentObjectiveCount(objective.getDesiredObjectiveCount());
        }
    }

    /**
     * Takes a point of interest off the map as lost - taken, destroyed, or otherwise gone before the player could deal
     * with it - and records every strategic objective tied to it as failed. Recording the failure means the objectives
     * stay failed however they are checked afterward.
     *
     * <p>Behaviors should lose points of interest through this rather than by removing them directly.</p>
     *
     * @param track           the sector the point of interest sits in
     * @param pointOfInterest the point of interest to lose
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void losePointOfInterest(StratConTrackState track, StratConPointOfInterest pointOfInterest) {
        for (StratConStrategicObjective objective : getStrategicObjectives(track, pointOfInterest)) {
            objective.setCurrentObjectiveCount(StratConStrategicObjective.OBJECTIVE_FAILED);
        }

        track.removePointOfInterest(pointOfInterest.getId());
    }

    /**
     * Takes a point of interest off the map as if it had never counted - a decoy, say - and removes every strategic
     * objective tied to it, so those objectives are neither met nor failed.
     *
     * <p>Behaviors should withdraw points of interest through this rather than by removing them directly.</p>
     *
     * @param track           the sector the point of interest sits in
     * @param pointOfInterest the point of interest to withdraw
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void withdrawPointOfInterest(StratConTrackState track, StratConPointOfInterest pointOfInterest) {
        for (StratConStrategicObjective objective : getStrategicObjectives(track, pointOfInterest)) {
            track.removeStrategicObjective(objective);
        }

        track.removePointOfInterest(pointOfInterest.getId());
    }

    /**
     * Ties a point of interest's fate to a scenario: when the scenario ends, the point of interest's
     * {@link IStratConPointOfInterestBehavior#onLinkedScenarioEnded} hook is told how it went (see
     * {@link #processScenarioEnded}). While it waits, the point of interest does not expire.
     *
     * @param pointOfInterest the point of interest
     * @param scenario        the scenario whose outcome decides it; it must already be registered with the campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void linkScenario(StratConPointOfInterest pointOfInterest, StratConScenario scenario) {
        pointOfInterest.setLinkedScenarioId(scenario.getBackingScenarioID());
    }

    /**
     * Tells every point of interest in a sector waiting on the given scenario that it has ended, clearing each one's
     * link first (see {@link IStratConPointOfInterestBehavior#onLinkedScenarioEnded}).
     *
     * <p>Call this when a scenario is resolved, before it is removed from the sector.</p>
     *
     * @param track             the sector the scenario was in
     * @param backingScenarioId the ID of the scenario's backing scenario
     * @param isVictory         {@code true} if the scenario was an overall victory for the player
     * @param campaign          the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processScenarioEnded(StratConTrackState track, int backingScenarioId, boolean isVictory,
          Campaign campaign) {
        // Copied, so a hook that resolves, loses, or removes its point of interest is safe.
        for (StratConPointOfInterest pointOfInterest : new ArrayList<>(track.getPointsOfInterest())) {
            Integer linkedScenarioId = pointOfInterest.getLinkedScenarioId();
            if ((linkedScenarioId == null) || (linkedScenarioId != backingScenarioId)) {
                continue;
            }

            pointOfInterest.setLinkedScenarioId(null);

            if (pointOfInterest.isActive()) {
                pointOfInterest.getBehavior().onLinkedScenarioEnded(pointOfInterest, track, isVictory, campaign);
            }
        }
    }

    /**
     * @param track    a sector
     * @param campaign the current campaign
     *
     * @return the active contract whose StratCon map holds that sector, or {@code null} if none does
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable AbstractContract getContract(StratConTrackState track, Campaign campaign) {
        for (AbstractContract contract : campaign.getActiveContracts()) {
            StratConCampaignState campaignState = contract.getStratConCampaignState();
            if ((campaignState != null) && campaignState.getTracks().contains(track)) {
                return contract;
            }
        }

        return null;
    }

    /**
     * @param track  a sector
     * @param coords a hex in it
     *
     * @return {@code true} if the hex holds a point of interest that is still in play
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean hasActivePointOfInterest(StratConTrackState track, StratConCoords coords) {
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest(coords)) {
            if (pointOfInterest.isActive()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param track           the sector the point of interest sits in
     * @param pointOfInterest the point of interest
     *
     * @return the strategic objectives of the sector tied to that point of interest; usually none or one
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConStrategicObjective> getStrategicObjectives(StratConTrackState track,
          StratConPointOfInterest pointOfInterest) {
        List<StratConStrategicObjective> objectives = new ArrayList<>();
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            if ((objective.getObjectiveType() == StrategicObjectiveType.PointOfInterest) &&
                      pointOfInterest.getId().equals(objective.getPointOfInterestId())) {
                objectives.add(objective);
            }
        }
        return objectives;
    }

    /**
     * Runs the daily point of interest step for one sector. A point of interest whose linked scenario has left the map
     * without being resolved - ignored, or removed by a GM - is told it ended in defeat (see
     * {@link #processScenarioEnded}). Then every active point of interest whose expiry date has come expires (see
     * {@link #expirePointOfInterest}), unless it is still waiting on a linked scenario; every other active one gets its
     * {@link IStratConPointOfInterestBehavior#onNewDay} hook. Resolved and expired points of interest are left alone.
     *
     * @param track    the sector to process
     * @param campaign the current campaign, whose date is today
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processNewDay(StratConTrackState track, Campaign campaign) {
        LocalDate today = campaign.getLocalDate();

        // Copied, so a hook that adds, moves, or removes points of interest is safe.
        for (StratConPointOfInterest pointOfInterest : new ArrayList<>(track.getPointsOfInterest())) {
            // A hook earlier in this pass may have removed it, or ended it some other way.
            if ((track.getPointOfInterest(pointOfInterest.getId()) == null) || !pointOfInterest.isActive()) {
                continue;
            }

            Integer linkedScenarioId = pointOfInterest.getLinkedScenarioId();
            if ((linkedScenarioId != null) && !track.getBackingScenariosMap().containsKey(linkedScenarioId)) {
                processScenarioEnded(track, linkedScenarioId, false, campaign);

                if ((track.getPointOfInterest(pointOfInterest.getId()) == null) || !pointOfInterest.isActive()) {
                    continue;
                }
            }

            if (pointOfInterest.hasReachedExpiryDate(today) && !pointOfInterest.hasLinkedScenario()) {
                expirePointOfInterest(track, pointOfInterest, campaign);
            } else {
                pointOfInterest.getBehavior().onNewDay(pointOfInterest, track, campaign);
            }
        }
    }

    /**
     * Expires a point of interest: calls its {@link IStratConPointOfInterestBehavior#onExpired} hook, then - unless the
     * hook resolved or removed it - marks it as expired, and removes it from the map if its definition says so. If the
     * player could see it, the daily report says it has gone.
     *
     * @param track           the sector it sits in
     * @param pointOfInterest the point of interest expiring
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void expirePointOfInterest(StratConTrackState track, StratConPointOfInterest pointOfInterest,
          Campaign campaign) {
        // Decided before anything changes: once it is removed, it can no longer tell whether the player saw it.
        boolean visibleToPlayer = pointOfInterest.isVisibleToPlayer(track);

        pointOfInterest.getBehavior().onExpired(pointOfInterest, track, campaign);

        if ((track.getPointOfInterest(pointOfInterest.getId()) == null) || !pointOfInterest.isActive()) {
            return;
        }

        pointOfInterest.setStatus(PointOfInterestStatus.EXPIRED);

        StratConPointOfInterestDefinition definition = pointOfInterest.getDefinition();
        if ((definition != null) && definition.isRemoveOnExpiry()) {
            track.removePointOfInterest(pointOfInterest.getId());
        }

        if (visibleToPlayer) {
            // Expiry is routine upkeep rather than something the player just did, so it stays out of their way.
            campaign.addReport(BATTLE, getFormattedTextAt(RESOURCE_BUNDLE,
                  "pointOfInterest.expired.report",
                  pointOfInterest.getDisplayableName(),
                  track.getDisplayableName()));
        }
    }

    /**
     * Tells every active point of interest on a hex that a player formation has just deployed onto it (see
     * {@link IStratConPointOfInterestBehavior#onFormationDeployed}), and combines what they want done to the random
     * scenario roll.
     *
     * @param track       the sector deployed into
     * @param coords      the hex deployed onto
     * @param formationId the ID of the deploying formation
     * @param campaign    the current campaign
     *
     * @return the combined outcome (see {@link PointOfInterestDeploymentOutcome#combineWith}); no effect if the hex has
     *       no active point of interest, or already holds a scenario (which the formation joins instead)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static PointOfInterestDeploymentOutcome processFormationDeployment(StratConTrackState track,
          StratConCoords coords, int formationId, Campaign campaign) {
        PointOfInterestDeploymentOutcome combinedOutcome = PointOfInterestDeploymentOutcome.NO_EFFECT;

        // A formation arriving on a hex that already holds a scenario joins that scenario; nothing here may place a
        // second one on the same hex, which would push the first off the map.
        if (track.getScenario(coords) != null) {
            return combinedOutcome;
        }

        // Copied, so a hook that removes its point of interest or places a scenario on the hex is safe.
        for (StratConPointOfInterest pointOfInterest : new ArrayList<>(track.getPointsOfInterest(coords))) {
            // One point of interest earlier in this pass may have placed a scenario here; the formation joins that,
            // and no other may place a second.
            if (track.getScenario(coords) != null) {
                break;
            }

            if (!pointOfInterest.isActive()) {
                continue;
            }

            PointOfInterestDeploymentOutcome outcome = pointOfInterest.getBehavior()
                                                             .onFormationDeployed(pointOfInterest,
                                                                   track,
                                                                   formationId,
                                                                   campaign);
            combinedOutcome = combinedOutcome.combineWith(outcome);
        }

        return combinedOutcome;
    }

    /**
     * Reveals every point of interest on the given hex that has not been revealed yet, and calls each one's
     * {@link IStratConPointOfInterestBehavior#onRevealed} hook. A point of interest already revealed is left alone, so
     * the hook fires once per point of interest however often its hex is scouted.
     *
     * <p>Call this whenever a player force scouts a hex.</p>
     *
     * @param track    the sector being scouted
     * @param coords   the hex being scouted
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void revealPointsOfInterest(StratConTrackState track, StratConCoords coords, Campaign campaign) {
        // Copied, so a behavior that removes or moves its point of interest from inside the hook is safe.
        List<StratConPointOfInterest> pointsOfInterestOnHex = new ArrayList<>(track.getPointsOfInterest(coords));

        for (StratConPointOfInterest pointOfInterest : pointsOfInterestOnHex) {
            revealPointOfInterest(track, pointOfInterest, campaign);
        }
    }

    /**
     * Reveals a single point of interest and calls its {@link IStratConPointOfInterestBehavior#onRevealed} hook, unless
     * it has already been revealed. Used by scouting and by the GM tools, so both act the same way.
     *
     * @param track           the sector the point of interest sits in
     * @param pointOfInterest the point of interest to reveal
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void revealPointOfInterest(StratConTrackState track, StratConPointOfInterest pointOfInterest,
          Campaign campaign) {
        if (!pointOfInterest.isRevealed()) {
            pointOfInterest.setRevealed(true);
            pointOfInterest.getBehavior().onRevealed(pointOfInterest, track, campaign);
        }
    }

    /**
     * Hides every point of interest in the sector again, without calling any hooks. Used when a GM resets the
     * sector's fog of war to re-test scouting, so points of interest can be found (and their hooks fire) a second time.
     *
     * @param track the sector whose points of interest to hide
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void hideAllPointsOfInterest(StratConTrackState track) {
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            pointOfInterest.setRevealed(false);
        }
    }
}
