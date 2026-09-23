package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.performResupply;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType.RESUPPLY_LOOT;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConEscalation;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;

/**
 * The behavior of a plunder target: a soft target - a warehouse, a trade post, a farming co-op - for the player's
 * pirates to raid, placed in place of every point of interest on a Pirate Raid contract. Every plunder target is a
 * strategic objective, and together they replace the contract's Essential scenarios.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the target is
 * plundered: its objective is met, the contract's combat bonus is paid, the loot arrives as a size 1 Resupply, and the
 * contract's Escalation rises by 3d6. If one does, the raiders are ambushed, in a template suited to ambushing their unit
 * type; an ambushed target is spent whatever the ambush's result: it leaves the map with no loot, and its objective is
 * removed, neither met nor failed. (See {@link StratConAmbushPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A plunder target not raided in time is gone: it expires and its objective fails (its lifespan comes from its
 * definition), except while an ambush there is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConPlunderTargetBehavior extends StratConAmbushPointOfInterestBehavior {
    /** The behavior ID the plunder target definition names. */
    public static final String BEHAVIOR_ID = "plunderTarget";

    /** The type ID of the plunder target definition. */
    public static final String TYPE_ID = "PlunderTarget";

    /** The size of the Resupply a plundered target's loot arrives as. */
    static final int LOOT_SIZE = 1;

    /** The Escalation dice plundering a target adds. */
    static final int PLUNDERED_ESCALATION_DICE = 3;

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConPlunderTargetBehavior";
    }

    /**
     * A plundered target's loot arrives as a size 1 Resupply, and the plunder raises Escalation by 3d6.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onSecured(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        performResupply(new Resupply(campaign, contract, RESUPPLY_LOOT), contract, LOOT_SIZE);
        StratConEscalation.increaseEscalationByDice(campaign, contract, PLUNDERED_ESCALATION_DICE);
    }
}
