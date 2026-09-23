package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConEscalation;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a show of force: a chance for a garrison to parade its strength where the locals and the enemy alike
 * will see it, placed in place of every point of interest on a Garrison Duty contract. Shows of force are not strategic
 * objectives: they calm the contract's Escalation - the unrest the garrison must settle, which starts at its maximum
 * (see {@link StratConEscalation}).
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the show of
 * force goes off without a hitch, lowering the contract's Escalation by 3d6. If one does, the enemy has used the
 * spectacle as cover, and the deploying formation is ambushed in a scenario suited to its unit type; an ambushed show
 * of force is spent whatever the ambush's result, calms nothing, and leaves the map. Making one pays no combat bonus:
 * the contract's Essential scenarios still pay it. (See {@link StratConAmbushPointOfInterestBehavior} for the rules it
 * shares.)</p>
 *
 * <p>A show of force not made in time is gone: it expires (its lifespan comes from its definition), except while an
 * ambush there is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConShowOfForceBehavior extends StratConAmbushPointOfInterestBehavior {
    /** The behavior ID the show of force definition names. */
    public static final String BEHAVIOR_ID = "showOfForce";

    /** The type ID of the show of force definition. */
    public static final String TYPE_ID = "ShowOfForce";

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConShowOfForceBehavior";
    }

    /**
     * Making a show of force pays no combat bonus: a Garrison Duty contract keeps its Essential scenarios, which still
     * pay it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isCombatBonusPaid() {
        return false;
    }

    /**
     * Making a show of force lowers the contract's Escalation by 3d6.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onSecured(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        StratConEscalation.onShowOfForce(campaign, contract);
    }
}
