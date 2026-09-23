package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static mekhq.campaign.enums.DailyReportType.GENERAL;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a potential lead: a tip that may point to an enemy mole, placed in place of every point of interest
 * on a Mole Hunting contract. Every potential lead is a strategic objective.
 *
 * <p>When a ground formation deploys onto the lead's hex, the usual scenario roll is made. If no scenario breaks out,
 * the lead is a dud: it leaves the map and its objective is removed, neither met nor failed. If one does, the lead has
 * panned out, and a {@value #SCENARIO_TEMPLATE} scenario is fought over it: an overall victory meets its objective and
 * pays the contract's combat bonus, standing in for the Essential scenarios such a contract does not get; anything else
 * - a defeat, a draw, or leaving the scenario unplayed - fails it. (See
 * {@link StratConContestedPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A lead that is not followed up in time goes cold: it expires and its objective fails (its lifespan comes from its
 * definition), except while a scenario over it is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConPotentialLeadBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the potential lead definition names. */
    public static final String BEHAVIOR_ID = "potentialLead";

    /** The type ID of the potential lead definition. */
    public static final String TYPE_ID = "PotentialLead";

    /** The scenario template fought over a lead that pans out. */
    static final String SCENARIO_TEMPLATE = "Mole Hunt.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConPotentialLeadBehavior";
    }

    /**
     * With no scenario breaking out, the lead is a dud: it leaves the map and its objective is removed.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onUncontested(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
        addReport(GENERAL, "dud.report", pointOfInterest, track, campaign);
    }
}
