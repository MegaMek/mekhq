package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConEscalation;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a high profile target: a conspicuous enemy asset whose attack draws the enemy's eye, placed in place
 * of every point of interest on a Diversionary Raid contract. High profile targets are not strategic objectives
 * themselves: a Diversionary Raid's objective is to raise its Escalation (see {@link StratConEscalation}), and striking
 * them is the quickest way to do it.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the target is
 * struck on the spot, raising the contract's Escalation by 3d6. If one does, the enemy was waiting, and the deploying
 * formation is ambushed in a {@value #SCENARIO_TEMPLATE} scenario; an ambushed target is spent whatever the ambush's
 * result, and leaves the map. Striking one pays no combat bonus: the contract's Essential scenarios still pay it. (See
 * {@link StratConAmbushPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A high profile target not struck in time is gone: it expires (its lifespan comes from its definition), except
 * while an ambush there is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConHighProfileTargetBehavior extends StratConAmbushPointOfInterestBehavior {
    /** The behavior ID the high profile target definition names. */
    public static final String BEHAVIOR_ID = "highProfileTarget";

    /** The type ID of the high profile target definition. */
    public static final String TYPE_ID = "HighProfileTarget";

    /** The scenario template the ambush at a high profile target is fought as. */
    static final String SCENARIO_TEMPLATE = "Decoy Engagement.json";

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConHighProfileTargetBehavior";
    }

    @Override
    protected @Nullable String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    /**
     * Striking a high profile target pays no combat bonus: a Diversionary Raid contract keeps its Essential scenarios,
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
     * Striking a high profile target raises the contract's Escalation by 3d6.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onSecured(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        StratConEscalation.onHighProfileTargetStruck(campaign, contract);
    }
}
