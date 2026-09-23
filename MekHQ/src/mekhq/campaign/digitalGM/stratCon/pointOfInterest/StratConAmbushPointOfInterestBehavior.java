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
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.gui.dialog.StratConAmbushedDialog;

/**
 * The shared rules of a point of interest that any formation can act on, but that may hide an ambush. Every such point
 * of interest is a strategic objective.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the point of
 * interest is secured on the spot: its objective is met and, by default, the contract's combat bonus is paid, standing
 * in for the Essential scenarios such a contract does not get (see {@link #isCombatBonusPaid}). If one does, the
 * deploying formation is ambushed (see {@link #getScenarioTemplateName}). An ambushed point
 * of interest is spent whatever the ambush's result: it leaves the map and its objective is removed, neither met nor
 * failed.</p>
 *
 * <p>Each type's player-facing text lives in the {@code StratConRulesManager} resource bundle, under its key prefix
 * (see {@link #getResourceKeyPrefix}): {@code .objective}, {@code .secured.report}, {@code .ambush.report}, and
 * {@code .ambushSpent.report}, each report taking the point of interest's name and its sector's name.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract class StratConAmbushPointOfInterestBehavior implements IStratConPointOfInterestBehavior {
    private static final MMLogger LOGGER = MMLogger.create(StratConAmbushPointOfInterestBehavior.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    /**
     * @return the prefix of this type's keys in the {@code StratConRulesManager} resource bundle, such as
     *       {@code StratConVulnerableInfrastructureBehavior}
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected abstract String getResourceKeyPrefix();

    /**
     * @return the file name of the scenario template the ambush is always drawn from, such as
     *       {@code Decoy Engagement.json}; or, by default, {@code null} to draw it from the templates suited to
     *       ambushing the deploying formation's unit type
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected @Nullable String getScenarioTemplateName() {
        return null;
    }

    /**
     * @return {@code true} if securing this type of point of interest pays the contract's combat bonus; by default, it
     *       does. A type whose contract keeps its Essential scenarios leaves the bonus to them.
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean isCombatBonusPaid() {
        return true;
    }

    /**
     * @return {@code true} if an ambush can break out here even while the enemy is routed, rolled against the usual
     *       odds as if it were not; by default, it cannot - a routed enemy mounts no ambushes. Civil unrest, which does
     *       not answer to the enemy's morale, can.
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean isAmbushPossibleWhileRouted() {
        return false;
    }

    /**
     * Any formation deploying onto the point of interest rolls for a scenario the usual way. With no scenario, the
     * point of interest is secured at once; with one, an ambush is placed on its hex and linked to it, and the deploying
     * formation is then assigned to it like any scenario found on the hex. Either way, no other random scenario is
     * rolled.
     *
     * <p>A formation joining an ambush already sprung here does not roll again.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, Campaign campaign) {
        if (pointOfInterest.hasLinkedScenario()) {
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract == null) {
            LOGGER.warn("No active contract holds the sector {} of point of interest {}.",
                  track.getDisplayableName(),
                  pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        boolean essentialScenariosOnly = campaign.getCampaignOptions().get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY);
        int targetNumber = StratConRulesManager.calculateScenarioOdds(track,
              contract,
              true,
              isAmbushPossibleWhileRouted());
        boolean isAmbush = StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.NO_EFFECT,
              essentialScenariosOnly,
              false,
              false,
              targetNumber);

        if (!isAmbush) {
            securePointOfInterest(pointOfInterest, track, contract, campaign);
            return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
        }

        StratConScenario ambush = placeAmbush(pointOfInterest, track, formationId, contract, campaign);
        if (ambush == null) {
            // Nothing to spring the ambush with, so leave the point of interest and let the deployment carry on.
            LOGGER.error("Could not create an ambush at point of interest {}.", pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        addReport(BATTLE, "ambush.report", pointOfInterest, track, campaign);
        announceAmbush(pointOfInterest, track, formationId, contract, campaign);
        return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
    }

    /**
     * The ambush here has ended - won, lost, or left unplayed. The point of interest is spent whatever the result: it
     * leaves the map and its objective is removed.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
        addReport(GENERAL, "ambushSpent.report", pointOfInterest, track, campaign);
    }

    @Override
    public @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return getTextAt(RESOURCE_BUNDLE, getResourceKeyPrefix() + ".objective");
    }

    /**
     * Adds one of this type's reports to the daily report.
     *
     * @param reportType      the daily report tab it belongs on
     * @param keySuffix       the key after this type's prefix, such as {@code secured.report}
     * @param pointOfInterest the point of interest the report is about
     * @param track           the sector it sits in
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void addReport(DailyReportType reportType, String keySuffix, StratConPointOfInterest pointOfInterest,
          StratConTrackState track, Campaign campaign) {
        campaign.addReport(reportType, getFormattedTextAt(RESOURCE_BUNDLE,
              getResourceKeyPrefix() + '.' + keySuffix,
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));
    }

    /**
     * Tells the player an ambush has been sprung here. By default, the ambushed dialog, as for any other ambush.
     *
     * @param pointOfInterest the point of interest the ambush is at
     * @param track           the sector it sits in
     * @param formationId     the ID of the ambushed formation
     * @param contract        the contract whose map holds the sector
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void announceAmbush(StratConPointOfInterest pointOfInterest, StratConTrackState track, int formationId,
          AbstractContract contract, Campaign campaign) {
        new StratConAmbushedDialog(campaign, formationId, false);
    }

    /**
     * Places the ambush on the point of interest's hex and links the point of interest to it. The ambush is drawn from
     * this type's own template, if it has one (see {@link #getScenarioTemplateName}), or else from the templates suited
     * to ambushing the deploying formation's unit type; and - like any ambush - is a Crisis and never a Turning Point.
     * It is not Essential.
     *
     * @return the ambush, or {@code null} if none could be generated
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected @Nullable StratConScenario placeAmbush(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, AbstractContract contract, Campaign campaign) {
        return StratConAmbushes.placeAmbush(pointOfInterest, track, formationId, contract, campaign,
              getScenarioTemplateName());
    }

    /**
     * Secures the point of interest: resolves it, which meets its objective, takes it off the map, and - if this type
     * pays it (see {@link #isCombatBonusPaid}) - pays the contract's combat bonus.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void securePointOfInterest(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);
        track.removePointOfInterest(pointOfInterest.getId());
        addReport(GENERAL, "secured.report", pointOfInterest, track, campaign);

        if (isCombatBonusPaid()) {
            StratConRulesManager.awardCombatBonus(campaign, contract);
        }

        onSecured(pointOfInterest, track, contract, campaign);
    }

    /**
     * Called once the point of interest has been secured without an ambush, after its objective is met and any combat
     * bonus paid, for anything else a type's success brings. By default, nothing.
     *
     * @param pointOfInterest the point of interest just secured; already off the map
     * @param track           the sector it sat in
     * @param contract        the contract whose map holds the sector
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void onSecured(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
    }
}
