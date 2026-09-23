package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static megamek.common.units.UnitType.CONV_FIGHTER;
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
 * The shared rules of a point of interest that a ground formation follows up, and that a scenario of one set template
 * may break out over. Every such point of interest is a strategic objective.
 *
 * <p>When a ground formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * type decides what happens (see {@link #onUncontested}). If one does, a scenario of the type's template (see
 * {@link #getScenarioTemplateName}) is placed on the hex and linked to the point of interest: an overall victory
 * secures it (see {@link #securePointOfInterest}), and anything else - a defeat, a draw, or leaving the scenario
 * unplayed - loses it, failing its objective. The scenario is not Essential. By default, securing one pays the
 * combat bonus, standing in for the Essential scenarios its contract does not get (see {@link #isCombatBonusPaid}).</p>
 *
 * <p>By default, a formation that is not a ground formation cannot follow the point of interest up, and deploys as it
 * would onto any other hex (see {@link #canFollowUp}). Nor does a formation joining a scenario already fought over it
 * roll again.</p>
 *
 * <p>Each type's player-facing text lives in the {@code StratConRulesManager} resource bundle, under its key prefix
 * (see {@link #getResourceKeyPrefix}): {@code .objective}, {@code .contested.report}, {@code .secured.report}, and
 * {@code .lost.report}, each report taking the point of interest's name and its sector's name.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract class StratConContestedPointOfInterestBehavior implements IStratConPointOfInterestBehavior {
    private static final MMLogger LOGGER = MMLogger.create(StratConContestedPointOfInterestBehavior.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    /**
     * @return the file name of the scenario template fought over this type of point of interest, such as
     *       {@code Recon Evasion.json}
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected abstract String getScenarioTemplateName();

    /**
     * @return the prefix of this type's keys in the {@code StratConRulesManager} resource bundle, such as
     *       {@code StratConDataCacheBehavior}
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected abstract String getResourceKeyPrefix();

    /**
     * Called when a ground formation follows the point of interest up and no scenario breaks out.
     *
     * @param pointOfInterest the point of interest
     * @param track           the sector it sits in
     * @param contract        the contract whose map holds the sector
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected abstract void onUncontested(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign);

    /**
     * Decides whether a formation can follow this type of point of interest up. By default, only a ground formation
     * can (see {@link #isGroundFormation}).
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
        return isGroundFormation(formationId, campaign);
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

        boolean essentialScenariosOnly = campaign.getCampaignOptions().get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY);
        int targetNumber = StratConRulesManager.calculateScenarioOdds(track, contract, true);
        boolean isContested = StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.NO_EFFECT,
              essentialScenariosOnly,
              false,
              false,
              targetNumber);

        if (!isContested) {
            onUncontested(pointOfInterest, track, contract, campaign);
            return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
        }

        StratConScenario scenario = placeContestingScenario(pointOfInterest, track, contract, campaign);
        if (scenario == null) {
            // Nothing to fight over the point of interest, so leave it in place and let the deployment carry on.
            LOGGER.error("Could not create a scenario over point of interest {}.", pointOfInterest);
            return PointOfInterestDeploymentOutcome.NO_EFFECT;
        }

        addReport(BATTLE, "contested.report", pointOfInterest, track, campaign);
        return PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO;
    }

    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        if (isVictory) {
            securePointOfInterest(pointOfInterest, track, campaign);
            return;
        }

        StratConPointOfInterestRules.losePointOfInterest(track, pointOfInterest);
        addReport(GENERAL, "lost.report", pointOfInterest, track, campaign);
    }

    @Override
    public @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return getTextAt(RESOURCE_BUNDLE, getResourceKeyPrefix() + ".objective");
    }

    /**
     * Secures the point of interest: resolves it, which meets its objective, takes it off the map, and - if this type
     * pays it (see {@link #isCombatBonusPaid}) - pays the contract's combat bonus, the reward an Essential scenario
     * would otherwise have given.
     *
     * @param pointOfInterest the point of interest to secure
     * @param track           the sector it sits in
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected void securePointOfInterest(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          Campaign campaign) {
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);
        track.removePointOfInterest(pointOfInterest.getId());
        addReport(GENERAL, "secured.report", pointOfInterest, track, campaign);

        if (!isCombatBonusPaid()) {
            return;
        }

        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract == null) {
            LOGGER.warn("No active contract holds the sector {} of point of interest {}, so no combat bonus is paid.",
                  track.getDisplayableName(),
                  pointOfInterest);
            return;
        }

        StratConRulesManager.awardCombatBonus(campaign, contract);
    }

    /**
     * Adds one of this type's reports to the daily report.
     *
     * @param reportType      the daily report tab it belongs on
     * @param keySuffix       the key after this type's prefix, such as {@code lost.report}
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
     * Places the scenario fought over a contested point of interest on its hex and links the point of interest to it.
     *
     * @return the scenario, or {@code null} if none could be generated
     *
     * @author Illiani
     * @since 0.51.01
     */
    private @Nullable StratConScenario placeContestingScenario(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, AbstractContract contract, Campaign campaign) {
        // A missing template is logged by the factory; a random scenario is fought instead.
        ScenarioTemplate template = StratConScenarioFactory.getSpecificScenario(getScenarioTemplateName());

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

        track.addScenario(scenario);
        StratConPointOfInterestRules.linkScenario(pointOfInterest, scenario);
        return scenario;
    }

    /**
     * @return {@code true} if the formation fights on the ground, so can follow a point of interest up: its primary
     *       unit type is not an aerospace or larger craft
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected static boolean isGroundFormation(int formationId, Campaign campaign) {
        Formation formation = campaign.getPlayerForce().getFormation(formationId);
        return (formation != null) && (formation.getPrimaryUnitType(campaign) < CONV_FIGHTER);
    }
}
