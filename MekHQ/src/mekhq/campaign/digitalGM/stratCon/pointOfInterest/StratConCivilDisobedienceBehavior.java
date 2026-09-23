package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of civil disobedience: unrest the player's forces must put down before it turns violent, placed in place
 * of every point of interest on a Riot Duty contract. Every outbreak of civil disobedience is a strategic objective.
 * Together they replace the contract's Essential scenarios, and the riots they can turn into replace the old weekly
 * riot event.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made - even against a routed enemy, since civil
 * unrest does not answer to the enemy's morale. If no scenario breaks out, the disobedience is defused: its objective is
 * met and the contract's combat bonus is paid. If one does, it has turned into a riot, set up as every riot is (see
 * {@link StratConRiots}): an overall victory puts the riot down, meeting the objective and paying the bonus, and
 * anything else - a defeat, a draw, or leaving the riot unplayed - fails it. Either way it then leaves the map. (See
 * {@link StratConContestedPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>Civil disobedience not responded to in time dies down: it expires and its objective fails (its lifespan comes from
 * its definition), except while a riot there is still to be put down.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConCivilDisobedienceBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the civil disobedience definition names. */
    public static final String BEHAVIOR_ID = "civilDisobedience";

    /** The type ID of the civil disobedience definition. */
    public static final String TYPE_ID = "CivilDisobedience";

    @Override
    protected String getScenarioTemplateName() {
        return StratConRiots.SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConCivilDisobedienceBehavior";
    }

    /**
     * Any formation can respond to civil disobedience.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean canFollowUp(int formationId, Campaign campaign) {
        return true;
    }

    /**
     * Civil disobedience can turn into a riot whatever state the enemy is in.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isContestPossibleWhileRouted() {
        return true;
    }

    /**
     * With no riot breaking out, the disobedience is defused at once.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onUncontested(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        securePointOfInterest(pointOfInterest, track, campaign);
    }

    /**
     * Sets up the riot as every riot is set up (see {@link StratConRiots#placeRiot}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable StratConScenario placeContestingScenario(StratConPointOfInterest pointOfInterest,
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
    protected void announceContest(StratConPointOfInterest pointOfInterest, StratConTrackState track, int formationId,
          AbstractContract contract, Campaign campaign) {
        StratConRiots.announceRiot(pointOfInterest, track, contract, campaign);
    }
}
