package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

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
        int targetNumber = StratConRulesManager.calculateScenarioOdds(track, contract, true);
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
        new StratConAmbushedDialog(campaign, formationId, false);
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
    private @Nullable StratConScenario placeAmbush(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, AbstractContract contract, Campaign campaign) {
        // With no usable template, a random scenario springs the ambush instead; a missing named one is logged.
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
        StratConScenario ambush = StratConRulesManager.setupScenario(pointOfInterest.getCoords(),
              null,
              campaign,
              contract,
              track,
              template,
              true,
              null);
        if (ambush == null) {
            return null;
        }

        ambush.getBackingScenario().setIsCrisis(true);
        ambush.setTurningPoint(false);
        track.addScenario(ambush);
        StratConPointOfInterestRules.linkScenario(pointOfInterest, ambush);
        return ambush;
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
