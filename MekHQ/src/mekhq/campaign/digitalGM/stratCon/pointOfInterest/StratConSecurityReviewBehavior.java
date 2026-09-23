package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a security review: an inspection of a site's defenses, placed in place of every point of interest on
 * a Security Duty contract. Every security review is a strategic objective, alongside the contract's Essential
 * scenarios rather than in place of them.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the review is
 * passed on the spot: its objective is met. If one does, the reviewers run into enemy forces probing the site and fight
 * a {@value #SCENARIO_TEMPLATE} scenario: an overall victory passes the review, and anything else - a defeat, a draw, or
 * leaving the scenario unplayed - fails it. Either way the review then leaves the map, and its objective is met or
 * failed. Passing one pays no combat bonus: the contract's Essential scenarios still pay it. (See
 * {@link StratConContestedPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A security review not carried out in time lapses: it expires and its objective fails (its lifespan comes from its
 * definition), except while a fight there is still to be had.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConSecurityReviewBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the security review definition names. */
    public static final String BEHAVIOR_ID = "securityReview";

    /** The type ID of the security review definition. */
    public static final String TYPE_ID = "SecurityReview";

    /** The scenario template fought when a review turns up enemy forces. */
    static final String SCENARIO_TEMPLATE = "Engagement.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConSecurityReviewBehavior";
    }

    /**
     * Any formation can carry out a security review.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean canFollowUp(int formationId, Campaign campaign) {
        return true;
    }

    /**
     * Passing a security review pays no combat bonus: a Security Duty contract keeps its Essential scenarios, which
     * still pay it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isCombatBonusPaid() {
        return false;
    }

    /**
     * With no scenario breaking out, the review is passed at once.
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
