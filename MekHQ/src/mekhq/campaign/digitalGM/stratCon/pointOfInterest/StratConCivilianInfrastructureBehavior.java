package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConEscalation;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.gui.dialog.StratConAmbushedDialog;

/**
 * The behavior of civilian infrastructure: a civilian target for the player to destroy, placed in place of every point
 * of interest on a Terrorism contract. Every piece of civilian infrastructure is a strategic objective, and together
 * they replace the contract's Essential scenarios.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * infrastructure is destroyed on the spot: its objective is met, the contract's combat bonus is paid, and the contract's
 * Escalation rises by 3d6. If one does, its defenders ambush the attackers, in a template suited to ambushing their unit
 * type (see {@link StratConAmbushes}): an overall victory still destroys the infrastructure, meeting its objective,
 * paying the bonus, and raising Escalation by 2d6 - 3d6 in all, with the usual +1d6 for winning - while anything else - a
 * defeat, a draw, or leaving the ambush unplayed - fails it. (See {@link StratConContestedPointOfInterestBehavior} for
 * the rules it shares, and {@link StratConEscalation}.)</p>
 *
 * <p>Civilian infrastructure is hidden until scouted, and never expires: it waits until it is hit.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConCivilianInfrastructureBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the civilian infrastructure definition names. */
    public static final String BEHAVIOR_ID = "civilianInfrastructure";

    /** The type ID of the civilian infrastructure definition. */
    public static final String TYPE_ID = "CivilianInfrastructure";

    /** The Escalation dice destroying civilian infrastructure without a fight adds. */
    static final int DESTROYED_ESCALATION_DICE = 3;

    /** The Escalation dice winning the ambush adds, on top of the usual +1d6 for winning. */
    static final int AMBUSH_WON_ESCALATION_DICE = 2;

    /**
     * Civilian infrastructure's defenders ambush the attackers, in a template suited to their unit type.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable String getScenarioTemplateName() {
        return null;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConCivilianInfrastructureBehavior";
    }

    /**
     * Any formation can attack civilian infrastructure.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean canFollowUp(int formationId, Campaign campaign) {
        return true;
    }

    /**
     * With no scenario breaking out, the infrastructure is destroyed at once, raising Escalation by 3d6.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onUncontested(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        securePointOfInterest(pointOfInterest, track, campaign);
        StratConEscalation.increaseEscalationByDice(campaign, contract, DESTROYED_ESCALATION_DICE);
    }

    /**
     * The ambush has ended: a win destroys the infrastructure and raises Escalation by 2d6, and anything else fails it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        super.onLinkedScenarioEnded(pointOfInterest, track, isVictory, campaign);

        if (isVictory) {
            StratConEscalation.increaseEscalationByDice(campaign,
                  StratConPointOfInterestRules.getContract(track, campaign),
                  AMBUSH_WON_ESCALATION_DICE);
        }
    }

    /**
     * The attackers are warned they have been ambushed, as with any other ambush.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void announceContest(StratConPointOfInterest pointOfInterest, StratConTrackState track, int formationId,
          AbstractContract contract, Campaign campaign) {
        new StratConAmbushedDialog(campaign, formationId, false);
    }
}
