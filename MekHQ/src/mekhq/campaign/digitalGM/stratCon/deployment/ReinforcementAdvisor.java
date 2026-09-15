package mekhq.campaign.digitalGM.stratCon.deployment;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.calculateReinforcementTargetNumber;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.getReinforcementType;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;

/**
 * Campaign-context helper that answers the reinforcement questions the inspector needs for a focused force: how it is
 * eligible to reinforce, and what its roll and odds are at a given support-point spend. It gathers the campaign inputs
 * (command liaison, contract, base target number) once and defers the arithmetic to {@link DeploymentEvaluator}.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ReinforcementAdvisor {
    private final Campaign campaign;
    private final StratConCampaignState campaignState;
    private final StratConTrackState track;
    private final int baseTargetNumber;

    public ReinforcementAdvisor(Campaign campaign, StratConCampaignState campaignState, StratConTrackState track) {
        this.campaign = campaign;
        this.campaignState = campaignState;
        this.track = track;
        this.baseTargetNumber = computeBaseTargetNumber();
    }

    /**
     * @return the contract-modified base target number shared by every reinforcement attempt on this contract, before
     *       any support-point spend
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getBaseTargetNumber() {
        return baseTargetNumber;
    }

    /**
     * @param forceId the force being evaluated
     *
     * @return how that force is eligible to reinforce this scenario (regular roll, improved roll, already deployed, or
     *       none)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ReinforcementEligibilityType getEligibility(int forceId) {
        return getReinforcementType(forceId, track, campaign, campaignState);
    }

    /**
     * @param chosenSupportPoints the support points the player would spend per force to improve the roll
     * @param instant             whether the reinforcements would arrive instantly
     *
     * @return the final target number and success odds for a single force at that spend
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ReinforcementRoll getRoll(int chosenSupportPoints, boolean instant) {
        int modifier = DeploymentEvaluator.reinforcementCost(chosenSupportPoints, instant, 1).targetNumberModifier();
        return DeploymentEvaluator.reinforcementRoll(baseTargetNumber, modifier);
    }

    private int computeBaseTargetNumber() {
        Person commandLiaison = campaign.getPlayerForce()
                                      .getHumanResources()
                                      .getSeniorAdminPerson(campaign.getCampaignOptions(),
                                            campaign.getPlayerForce().isClanForce(),
                                            campaign.getLocalDate());
        int base = campaign.getCampaignOptions().get(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER);
        AbstractContract contract = campaignState.getContract();
        TargetRoll targetRoll = calculateReinforcementTargetNumber(commandLiaison, contract, base);
        return targetRoll.getValue();
    }
}
