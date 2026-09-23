package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a VIP: someone the player must extract, placed in place of every point of interest on an Extraction
 * Raid contract. Every VIP is a strategic objective.
 *
 * <p>When a ground formation deploys onto the VIP's hex, the usual scenario roll is made. If no scenario breaks out, the
 * VIP is extracted on the spot. If one does, a {@value #SCENARIO_TEMPLATE} scenario is fought to get them out: an
 * overall victory extracts them, and anything else - a defeat, a draw, or leaving the scenario unplayed - loses them.
 * Either way the VIP then leaves the map, and their objective is met or failed. Extracting one pays the contract's
 * combat bonus, standing in for the Essential scenarios such a contract does not get. (See
 * {@link StratConContestedPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A VIP not extracted in time is lost: they expire and their objective fails (their lifespan comes from their
 * definition), except while a scenario to get them out is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConVIPBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the VIP definition names. */
    public static final String BEHAVIOR_ID = "vip";

    /** The type ID of the VIP definition. */
    public static final String TYPE_ID = "VIP";

    /** The scenario template fought to extract a VIP. */
    static final String SCENARIO_TEMPLATE = "Breakout.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConVIPBehavior";
    }

    /**
     * With no scenario breaking out, the VIP is extracted at once.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onUncontested(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        securePointOfInterest(pointOfInterest, track, campaign);
    }
}
