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

import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics;
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
 * <p>When any formation deploys onto its hex, the usual scenario roll is made (or skipped, if a scenario is certain -
 * see {@link #isScenarioCertain}). If no scenario breaks out, the type decides what happens (see
 * {@link #onNoScenario}). If one does, a scenario is placed on the hex and linked to the point of interest (see
 * {@link #placeScenario}), and the deploying formation is then assigned to it like any scenario found on the hex. How
 * the linked scenario's end is handled is left to the type (see
 * {@link IStratConPointOfInterestBehavior#onLinkedScenarioEnded}).</p>
 *
 * <p>A formation joining a scenario already linked here does not roll again.</p>
 *
 * <p>Each type's player-facing text lives in the {@code StratConPointOfInterest} resource bundle, under its behavior
 * ID (see {@link #getBehaviorId}): at least {@code .objective}, {@code .secured.report}, and the report named by
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

    private final String behaviorId;
    // null for an ambush suited to the deploying formation's unit type
    private final String scenarioTemplateName;

    /**
     * @param behaviorId           the ID this behavior is registered under, which also prefixes its keys in the
     *                             {@code StratConPointOfInterest} resource bundle
     * @param scenarioTemplateName the file name of the scenario template fought here, such as
     *                             {@code Recon Evasion.json}; or {@code null} for the deploying formation to be
     *                             ambushed in a template suited to ambushing its unit type
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected AbstractStratConRolledPointOfInterestBehavior(String behaviorId, @Nullable String scenarioTemplateName) {
        this.behaviorId = behaviorId;
        this.scenarioTemplateName = scenarioTemplateName;
    }

    /**
     * @return the ID this behavior is registered under, such as {@code dataCache}; its keys in the
     *       {@code StratConPointOfInterest} resource bundle start with it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public String getBehaviorId() {
        return behaviorId;
    }

    /**
     * @return the key, after this type's behavior ID, of the report made when a scenario breaks out here, such as
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
     *       {@code Recon Evasion.json}; or {@code null} for the deploying formation to be ambushed in a template suited
     *       to ambushing its unit type
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable String getScenarioTemplateName() {
        return scenarioTemplateName;
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
     * Decides whether dealing with this point of interest pays the contract's combat bonus. By default, it does exactly
     * when the contract's special points of interest replaced its Essential scenarios (see
     * {@link StratConContractMechanics#areEssentialScenariosReplaced}): the bonus those scenarios would have paid is
     * paid here instead. A contract that keeps its Essential scenarios leaves the bonus to them.
     *
     * @param contract the contract whose map holds the point of interest
     *
     * @return {@code true} if the combat bonus is paid
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean isCombatBonusPaid(AbstractContract contract) {
        return StratConContractMechanics.areEssentialScenariosReplaced(contract);
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

        boolean isScenarioBreakingOut = isScenarioCertain(pointOfInterest)
                                              || rollForScenario(track, contract);
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

    /**
     * Rolls the usual scenario odds. "Essential Scenarios Only" does not rule this roll out: these scenarios are the
     * contract's own objective fights, standing in for the Essential scenarios most such contracts do not get, rather
     * than random encounters.
     */
    private boolean rollForScenario(StratConTrackState track, AbstractContract contract) {
        int targetNumber = StratConRulesManager.calculateScenarioOdds(track,
              contract,
              true,
              isScenarioPossibleWhileRouted());
        return StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.NO_EFFECT,
              false,
              false,
              false,
              targetNumber);
    }

    @Override
    public @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return getTextAt(StratConPointOfInterestRules.RESOURCE_BUNDLE, behaviorId + ".objective");
    }

    /**
     * Places the scenario that breaks out over the point of interest on its hex, and links the point of interest to it.
     *
     * <p>The scenario is drawn from this type's template (see {@link #getScenarioTemplateName}), or from the templates
     * suited to ambushing the deploying formation's unit type if there is none. It is set up with the deploying
     * formation present, so its opposition is sized against that formation, readied by {@link #prepareScenario}, and
     * finalized without the formation, which is then assigned to it like any scenario found on the hex. Anything a
     * type adds once the scenario is finalized is added by {@link #onScenarioFinalized}. An ambush (see
     * {@link #isScenarioAnAmbush}) is then made a Crisis and never a Turning Point.</p>
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
    // Package-private rather than private so placement can be tested without building a scenario from a template.
    @Nullable StratConScenario placeScenario(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, AbstractContract contract, Campaign campaign) {
        // With no usable template, a random scenario is fought instead; a missing named template is logged by the
        // factory.
        ScenarioTemplate template;
        if (scenarioTemplateName != null) {
            template = StratConScenarioFactory.getSpecificScenario(scenarioTemplateName);
        } else {
            Formation formation = campaign.getPlayerForce().getFormation(formationId);
            int unitType = (formation == null) ? MEK : formation.getPrimaryUnitType(campaign);
            template = StratConScenarioFactory.getRandomScenario(unitType, true, false);
        }

        // Set up with the deploying formation as its seed, as every other StratCon scenario is: the opposition is
        // generated when the scenario is finalized, and is sized against the player forces in it at that moment.
        // Facilities are ignored: these points of interest occupy their hex, so none can share it.
        StratConScenario scenario = StratConRulesManager.setupScenario(pointOfInterest.getCoords(),
              formationId,
              campaign,
              contract,
              track,
              template,
              true,
              getDaysUntilDeployment());
        if (scenario == null) {
            return null;
        }

        prepareScenario(scenario, track);

        // Finalized without auto-assignment, which also puts it on the track and takes the formation back out of the
        // backing scenario. Clear it from the primary forces too, so that assigning it to the scenario found on the hex
        // - as the deployment does next - does not list it twice.
        StratConRulesManager.finalizeBackingScenario(campaign, contract, track, false, scenario);
        scenario.getPrimaryForceIDs().remove(Integer.valueOf(formationId));

        onScenarioFinalized(scenario, contract, campaign);

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
     * @return how many days until the scenario placed here deploys, or {@code null} for the sector's usual deployment
     *       time; by default, the usual time
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected @Nullable Integer getDaysUntilDeployment() {
        return null;
    }

    /**
     * Adds anything a type needs to a scenario placed here, once it has been finalized - for example, the civilian mobs
     * of a riot, whose force only exists once the scenario is finalized. By default, nothing.
     *
     * @param scenario the scenario, finalized and on the track, but not yet linked to the point of interest
     * @param contract the contract whose map holds the sector
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void onScenarioFinalized(StratConScenario scenario, AbstractContract contract, Campaign campaign) {
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
     * the bonus is paid here (see {@link #isCombatBonusPaid}) - pays the contract's combat bonus. Then gives the type
     * its {@link #onSecured} hook.
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

        if (isCombatBonusPaid(contract)) {
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
     * @param keySuffix       the key after this type's behavior ID, such as {@code secured.report}
     * @param pointOfInterest the point of interest the report is about
     * @param track           the sector it sits in
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void addReport(DailyReportType reportType, String keySuffix, StratConPointOfInterest pointOfInterest,
          StratConTrackState track, Campaign campaign) {
        campaign.addReport(reportType, getFormattedTextAt(StratConPointOfInterestRules.RESOURCE_BUNDLE,
              behaviorId + '.' + keySuffix,
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));
    }
}
