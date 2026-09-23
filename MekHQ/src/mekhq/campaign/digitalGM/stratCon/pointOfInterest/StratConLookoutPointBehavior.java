package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

/**
 * The behavior of a lookout point: a vantage point from which the player can observe enemy movements, placed in place
 * of every point of interest on an Observation Raid contract. Every lookout point is a strategic objective.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * observation is made: its objective is met and the contract's combat bonus is paid, standing in for the Essential
 * scenarios such a contract does not get. If one does, the deploying formation is ambushed. An ambushed lookout point
 * is compromised whatever the ambush's result: it leaves the map and its objective is removed, neither met nor failed.
 * (See {@link StratConAmbushPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A lookout point that is not used in time expires and its objective fails (its lifespan comes from its
 * definition), except while an ambush there is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConLookoutPointBehavior extends StratConAmbushPointOfInterestBehavior {
    /** The behavior ID the lookout point definition names. */
    public static final String BEHAVIOR_ID = "lookoutPoint";

    /** The type ID of the lookout point definition. */
    public static final String TYPE_ID = "LookoutPoint";

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConLookoutPointBehavior";
    }
}
