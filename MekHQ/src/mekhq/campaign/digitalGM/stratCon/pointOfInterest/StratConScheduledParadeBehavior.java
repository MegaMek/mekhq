package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.randomEvents.other.RiotScenario;

/**
 * The behavior of a scheduled parade: a public show of the employer's strength, placed in place of every point of
 * interest on a Retainer contract. Every scheduled parade is a strategic objective.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made - even against a routed enemy, since civil
 * unrest does not answer to the enemy's morale. If no scenario breaks out, the parade is held: its objective is met and
 * the contract's combat bonus is paid, standing in for the Essential scenarios such a contract does not get. If one
 * does, a riot breaks out, and the deploying formation is caught up in a {@value #SCENARIO_TEMPLATE} scenario, set up
 * as a riot's is (see {@link RiotScenario}). A disrupted parade is called off whatever the scenario's result: it leaves
 * the map and its objective is removed, neither met nor failed. (See {@link StratConAmbushPointOfInterestBehavior} for
 * the rules it shares.)</p>
 *
 * <p>Any formation can be caught up in a riot - an aerospace flyover included.</p>
 *
 * <p>A parade not held in time is missed: it expires and its objective fails (its lifespan comes from its definition),
 * except while a riot there is still to be dealt with.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConScheduledParadeBehavior extends StratConAmbushPointOfInterestBehavior {
    /** The behavior ID the scheduled parade definition names. */
    public static final String BEHAVIOR_ID = "scheduledParade";

    /** The type ID of the scheduled parade definition. */
    public static final String TYPE_ID = "ScheduledParade";

    /** The scenario template a disrupted parade is fought as. */
    static final String SCENARIO_TEMPLATE = StratConRiots.SCENARIO_TEMPLATE;

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConScheduledParadeBehavior";
    }

    @Override
    protected @Nullable String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    /**
     * A riot can break out at a parade whatever state the enemy is in.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isAmbushPossibleWhileRouted() {
        return true;
    }

    /**
     * Sets up the riot as every riot is set up (see {@link StratConRiots#placeRiot}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable StratConScenario placeAmbush(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, AbstractContract contract, Campaign campaign) {
        return StratConRiots.placeRiot(pointOfInterest, track, formationId, contract, campaign);
    }

    /**
     * A riot is announced as every riot is (see {@link StratConRiots#announceRiot}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void announceAmbush(StratConPointOfInterest pointOfInterest, StratConTrackState track, int formationId,
          AbstractContract contract, Campaign campaign) {
        StratConRiots.announceRiot(pointOfInterest, track, contract, campaign);
    }
}
