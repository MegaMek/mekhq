package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a strategic position: ground whose capture shapes the assault, placed in place of every point of
 * interest on a Planetary Assault contract. Every strategic position is a strategic objective, alongside the contract's
 * Essential scenarios rather than in place of them.
 *
 * <p>When a ground formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * position is captured on the spot. If one does, a {@value #SCENARIO_TEMPLATE} scenario is fought for it: an overall
 * victory captures it, and anything else - a defeat, a draw, or leaving the scenario unplayed - loses it. Either way the
 * position then leaves the map, and its objective is met or failed. Capturing one pays no combat bonus: the contract's
 * Essential scenarios still pay it. (See {@link StratConContestedPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A strategic position not taken in time is lost: it expires and its objective fails (its lifespan comes from its
 * definition), except while a scenario for it is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConStrategicPositionBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the strategic position definition names. */
    public static final String BEHAVIOR_ID = "strategicPosition";

    /** The type ID of the strategic position definition. */
    public static final String TYPE_ID = "StrategicPosition";

    /** The scenario template fought for a contested strategic position. */
    static final String SCENARIO_TEMPLATE = "Pivotal Engagement.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConStrategicPositionBehavior";
    }

    /**
     * Capturing a strategic position pays no combat bonus: a Planetary Assault contract keeps its Essential scenarios,
     * which still pay it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isCombatBonusPaid() {
        return false;
    }

    /**
     * With no scenario breaking out, the position is captured at once.
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
