package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioFactory;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
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
    static final String SCENARIO_TEMPLATE = "Crowd Control.json";

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
     * Sets up the riot as a riot random event does (see {@link RiotScenario}): the scenario is generated and finalized
     * with the deploying formation present, so the opposition is sized against it, and the riot's civilian mobs are then
     * added to its "Civilians" force. The formation is then handed back, to be assigned to the riot like any scenario
     * found on the hex.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable StratConScenario placeAmbush(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, AbstractContract contract, Campaign campaign) {
        // A missing template is logged by the factory. Without it there is no riot to fight.
        ScenarioTemplate template = StratConScenarioFactory.getSpecificScenario(SCENARIO_TEMPLATE);
        if (template == null) {
            return null;
        }

        // Facilities are ignored: a parade occupies its hex, so none can share it. The riot breaks out at once.
        StratConScenario riot = StratConRulesManager.setupScenario(pointOfInterest.getCoords(),
              formationId,
              campaign,
              contract,
              track,
              template,
              true,
              0);
        if (riot == null) {
            return null;
        }

        // Finalized as a riot's scenario is, which also generates its "Civilians" force for the mobs to join.
        StratConRulesManager.finalizeBackingScenario(campaign, contract, track, false, riot);
        AtBDynamicScenario backingScenario = riot.getBackingScenario();
        RiotScenario.addRiotingMobs(campaign, contract.getEnemyFaction(), backingScenario);

        // Finalizing without auto-assignment has already taken the formation out of the backing scenario; clear it from
        // the primary forces too, so that assigning it to the riot on the hex does not list it twice.
        riot.getPrimaryForceIDs().remove(Integer.valueOf(formationId));

        StratConPointOfInterestRules.linkScenario(pointOfInterest, riot);
        return riot;
    }

    /**
     * A riot is announced as a riot random event's is (see {@link RiotScenario#reportRiot}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void announceAmbush(StratConPointOfInterest pointOfInterest, StratConTrackState track, int formationId,
          AbstractContract contract, Campaign campaign) {
        RiotScenario.reportRiot(campaign, contract, track, pointOfInterest.getCoords());
    }
}
