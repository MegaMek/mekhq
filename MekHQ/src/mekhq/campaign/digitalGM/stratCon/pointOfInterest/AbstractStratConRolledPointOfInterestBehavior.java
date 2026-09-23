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

import static megamek.common.units.UnitType.CONV_FIGHTER;
import static megamek.common.units.UnitType.MEK;
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
import mekhq.campaign.digitalGM.stratCon.StratConScenarioFactory;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;

/**
 * The shared rules of a point of interest that a deploying formation follows up with the usual scenario roll.
 *
 * <p>When a formation that can follow it up (see {@link #canFollowUp}) deploys onto its hex, the usual scenario roll
 * is made (or skipped, if a scenario is certain - see {@link #isScenarioCertain}). If no scenario breaks out, the type
 * decides what happens (see {@link #onNoScenario}). If one does, a scenario is placed on the hex and linked to the
 * point of interest (see {@link #placeScenario}), and the deploying formation is then assigned to it like any scenario
 * found on the hex. How the linked scenario's end is handled is left to the type (see
 * {@link IStratConPointOfInterestBehavior#onLinkedScenarioEnded}).</p>
 *
 * <p>A formation joining a scenario already linked here does not roll again.</p>
 *
 * <p>Each type's player-facing text lives in the {@code StratConRulesManager} resource bundle, under its key prefix
 * (see {@link #getResourceKeyPrefix}): at least {@code .objective}, {@code .secured.report}, and the report named by
 * {@link #getScenarioReportKeySuffix}, each report taking the point of interest's name and its sector's name.</p>
 *
 * <p>The two families built on this are {@link StratConContestedPointOfInterestBehavior} and
 * {@link StratConAmbushPointOfInterestBehavior}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract class AbstractStratConRolledPointOfInterestBehavior implements IStratConPointOfInterestBehavior {
    private static final MMLogger LOGGER = MMLogger.create(AbstractStratConRolledPointOfInterestBehavior.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    /**
     * @return the prefix of this type's keys in the {@code StratConRulesManager} resource bundle, such as
     *       {@code StratConDataCacheBehavior}
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected abstract String getResourceKeyPrefix();

    /**
     * @return the key, after this type's prefix, of the report made when a scenario breaks out here, such as
     *       {@code contested.report}
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected abstract String getScenarioReportKeySuffix();

    /**
     * Called when a formation follows the point of interest up and no scenario breaks out.
     *
     * @param pointOfInterest the point of interest
     * @param track           the sector it sits in
     * @param contract        the contract whose map holds the sector
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected abstract void onNoScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign);

    /**
     * @return the file name of the scenario template fought over this type of point of interest, such as
     *       {@code Recon Evasion.json}; or, by default, {@code null} for the deploying formation to be ambushed in a
     *       template suited to ambushing its unit type
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected @Nullable String getScenarioTemplateName() {
        return null;
    }

    /**
     * @return {@code true} if the scenario that breaks out here is an ambush: a Crisis, and never a Turning Point. By
     *       default, only a scenario drawn from the ambush templates (see {@link #getScenarioTemplateName}) is.
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean isScenarioAnAmbush() {
        return getScenarioTemplateName() == null;
    }

    /**
     * Decides whether a formation can follow this type of point of interest up. One that cannot deploys as it would
     * onto any other occupied hex. By default, any formation can.
     *
     * @param formationId the ID of the deploying formation
     * @param campaign    the current campaign
     *
     * @return {@code true} if the formation can follow the point of interest up
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean canFollowUp(int formationId, Campaign campaign) {
        return true;
    }

    /**
     * @return {@code true} if dealing with this type of point of interest pays the contract's combat bonus; by default,
     *       it does. A type whose contract keeps its Essential scenarios leaves the bonus to them.
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean isCombatBonusPaid() {
        return true;
    }

    /**
     * @return {@code true} if a scenario can break out here even while the enemy is routed, rolled against the usual
     *       odds as if it were not; by default, it cannot - a routed enemy fights over nothing. Civil unrest, which
     *       does not answer to the enemy's morale, can.
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean isScenarioPossibleWhileRouted() {
        return false;
    }

    /**
     * Decides whether a scenario is certain to break out over this point of interest, skipping the roll. By default, it
     * never is.
     *
     * @param pointOfInterest the point of interest being followed up
     *
     * @return {@code true} if the scenario breaks out without a roll
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean isScenarioCertain(StratConPointOfInterest pointOfInterest) {
        return false;
    }

    @Override
    public PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, Campaign campaign) {
        if (pointOfInterest.hasLinkedScenario() || !canFollowUp(formationId, campaign)) {
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract == null) {
            LOGGER.warn("No active contract holds the sector {} of point of interest {}.",
                  track.getDisplayableName(),
                  pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        boolean isScenarioBreakingOut = isScenarioCertain(pointOfInterest)
                                              || rollForScenario(track, contract, campaign);
        if (!isScenarioBreakingOut) {
            onNoScenario(pointOfInterest, track, contract, campaign);
            return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
        }

        StratConScenario scenario = placeScenario(pointOfInterest, track, formationId, contract, campaign);
        if (scenario == null) {
            // Nothing to fight over the point of interest, so leave it in place and let the deployment carry on.
            LOGGER.error("Could not create a scenario at point of interest {}.", pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        addReport(BATTLE, getScenarioReportKeySuffix(), pointOfInterest, track, campaign);
        announceScenario(pointOfInterest, track, formationId, contract, campaign);
        return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
    }

    private boolean rollForScenario(StratConTrackState track, AbstractContract contract, Campaign campaign) {
        boolean essentialScenariosOnly = campaign.getCampaignOptions().get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY);
        int targetNumber = StratConRulesManager.calculateScenarioOdds(track,
              contract,
              true,
              isScenarioPossibleWhileRouted());
        return StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.NO_EFFECT,
              essentialScenariosOnly,
              false,
              false,
              targetNumber);
    }

    @Override
    public @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return getTextAt(RESOURCE_BUNDLE, getResourceKeyPrefix() + ".objective");
    }

    /**
     * Places the scenario that breaks out over the point of interest on its hex, and links the point of interest to it.
     *
     * <p>By default, the scenario is drawn from this type's template (see {@link #getScenarioTemplateName}), or from
     * the templates suited to ambushing the deploying formation's unit type if there is none. It is set up without the
     * deploying formation, readied by {@link #prepareScenario}, and finalized as any scenario placed ahead of the
     * formation that will fight it is. An ambush (see {@link #isScenarioAnAmbush}) is then made a Crisis and never a
     * Turning Point.</p>
     *
     * @param pointOfInterest the point of interest
     * @param track           the sector it sits in
     * @param formationId     the ID of the deploying formation
     * @param contract        the contract whose map holds the sector
     * @param campaign        the current campaign
     *
     * @return the scenario, or {@code null} if none could be generated
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected @Nullable StratConScenario placeScenario(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, AbstractContract contract, Campaign campaign) {
        // With no usable template, a random scenario is fought instead; a missing named template is logged by the
        // factory.
        String templateName = getScenarioTemplateName();
        ScenarioTemplate template;
        if (templateName != null) {
            template = StratConScenarioFactory.getSpecificScenario(templateName);
        } else {
            Formation formation = campaign.getPlayerForce().getFormation(formationId);
            int unitType = (formation == null) ? MEK : formation.getPrimaryUnitType(campaign);
            template = StratConScenarioFactory.getRandomScenario(unitType, true, false);
        }

        // Facilities are ignored: these points of interest occupy their hex, so none can share it.
        StratConScenario scenario = StratConRulesManager.setupScenario(pointOfInterest.getCoords(),
              null,
              campaign,
              contract,
              track,
              template,
              true,
              null);
        if (scenario == null) {
            return null;
        }

        prepareScenario(scenario, track);

        // Finalized without auto-assignment, which also puts it on the track; the deploying formation is assigned to it
        // afterward, like any scenario found on the hex.
        StratConRulesManager.finalizeBackingScenario(campaign, contract, track, false, scenario);

        if (isScenarioAnAmbush()) {
            scenario.getBackingScenario().setIsCrisis(true);
            scenario.setTurningPoint(false);
        }

        StratConPointOfInterestRules.linkScenario(pointOfInterest, scenario);
        return scenario;
    }

    /**
     * Readies a newly set-up scenario before it is finalized - for example, making it Essential, so that finalizing it
     * treats it as one. By default, nothing.
     *
     * @param scenario the scenario, set up but not yet finalized or on the track
     * @param track    the sector it will sit in
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void prepareScenario(StratConScenario scenario, StratConTrackState track) {
    }

    /**
     * Tells the player, beyond the daily report, that a scenario has broken out here. By default, nothing more.
     *
     * @param pointOfInterest the point of interest the scenario is over
     * @param track           the sector it sits in
     * @param formationId     the ID of the deploying formation
     * @param contract        the contract whose map holds the sector
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void announceScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          int formationId, AbstractContract contract, Campaign campaign) {
    }

    /**
     * Secures the point of interest: resolves it, which meets any objective tied to it, takes it off the map, and - if
     * this type pays it (see {@link #isCombatBonusPaid}) - pays the contract's combat bonus. Then gives the type its
     * {@link #onSecured} hook.
     *
     * @param pointOfInterest the point of interest to secure
     * @param track           the sector it sits in
     * @param contract        the contract whose map holds the sector, or {@code null} if none does - in which case
     *                        no bonus is paid and {@link #onSecured} is not called
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void securePointOfInterest(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          @Nullable AbstractContract contract, Campaign campaign) {
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);
        track.removePointOfInterest(pointOfInterest.getId());
        addReport(GENERAL, "secured.report", pointOfInterest, track, campaign);

        if (contract == null) {
            LOGGER.warn("No active contract holds the sector {} of point of interest {}, so nothing more comes of "
                              + "securing it.",
                  track.getDisplayableName(),
                  pointOfInterest);
            return;
        }

        if (isCombatBonusPaid()) {
            StratConRulesManager.awardCombatBonus(campaign, contract);
        }

        onSecured(pointOfInterest, track, contract, campaign);
    }

    /**
     * Called once the point of interest has been secured, after its objective is met and any combat bonus paid, for
     * anything else a type's success brings. By default, nothing.
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
     * @param formationId the ID of the formation
     * @param campaign    the current campaign
     *
     * @return {@code true} if the formation fights on the ground: its primary unit type is not an aerospace or larger
     *       craft
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected static boolean isGroundFormation(int formationId, Campaign campaign) {
        Formation formation = campaign.getPlayerForce().getFormation(formationId);
        return (formation != null) && (formation.getPrimaryUnitType(campaign) < CONV_FIGHTER);
    }
}
