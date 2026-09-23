package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static mekhq.campaign.enums.DailyReportType.GENERAL;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a lead on an assassination target, placed in place of every point of interest on an Assassination
 * contract. It works much as a Mole Hunting contract's potential lead does (see {@link StratConPotentialLeadBehavior}),
 * but no lead is ever a dud - every one is fought as a {@value #SCENARIO_TEMPLATE} scenario - and most of the
 * targets they turn up are body doubles.
 *
 * <p>Only one lead per point of the contract's scale points to the real target, settled when the contract is accepted
 * (see {@link #REAL_TARGET_STATE_KEY}); the player cannot tell which, as every lead looks alike and is a strategic
 * objective. No lead is a dud, so following one up always brings a fight; that the fight happened gives nothing
 * away.</p>
 *
 * <p>Winning the fight against the real target meets its objective and pays the combat bonus; losing lets the target
 * escape, failing it. Winning against a body double also pays the combat bonus, but only then is the double revealed,
 * and its objective is withdrawn - neither met nor failed, as it was never the target. Losing to a double pays nothing,
 * and its objective is withdrawn all the same. A lead of either kind not followed up in time fails its objective, as the
 * player cannot know which it was.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConAssassinationLeadBehavior extends StratConPotentialLeadBehavior {
    /** The behavior ID the assassination lead definition names. */
    public static final String BEHAVIOR_ID = "assassinationLead";

    /** The type ID of the assassination lead definition. */
    public static final String TYPE_ID = "AssassinationLead";

    /** The state key marking a lead that points to the real target, rather than a body double. */
    public static final String REAL_TARGET_STATE_KEY = "realTarget";

    /** The scenario template fought over a lead that pans out. */
    static final String SCENARIO_TEMPLATE = "Assassination.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConAssassinationLeadBehavior";
    }

    /**
     * @param pointOfInterest an assassination lead
     *
     * @return {@code true} if the lead points to the real target rather than a body double
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isRealTarget(StratConPointOfInterest pointOfInterest) {
        return Boolean.parseBoolean(pointOfInterest.getStateValue(REAL_TARGET_STATE_KEY));
    }

    /**
     * No lead is a dud: the real target and its body doubles alike always stand and fight.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isContestCertain(StratConPointOfInterest pointOfInterest) {
        return true;
    }

    /**
     * The real target is secured or lost as any potential lead is. A body double is revealed only now: it is withdrawn
     * whatever the result, and beating it still pays the combat bonus.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        if (isRealTarget(pointOfInterest)) {
            super.onLinkedScenarioEnded(pointOfInterest, track, isVictory, campaign);
            return;
        }

        StratConPointOfInterestRules.withdrawPointOfInterest(track, pointOfInterest);
        if (!isVictory) {
            addReport(GENERAL, "doubleEscaped.report", pointOfInterest, track, campaign);
            return;
        }

        addReport(GENERAL, "bodyDouble.report", pointOfInterest, track, campaign);
        AbstractContract contract = StratConPointOfInterestRules.getContract(track, campaign);
        if (contract != null) {
            StratConRulesManager.awardCombatBonus(campaign, contract);
        }
    }
}
