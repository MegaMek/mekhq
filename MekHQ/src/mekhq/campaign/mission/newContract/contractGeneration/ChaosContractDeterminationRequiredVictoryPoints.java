package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.ceil;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.INDEPENDENT_COMMAND_RIGHTS_REQUIRED_VICTORY_POINTS;

import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.newContract.AbstractContract;

public class ChaosContractDeterminationRequiredVictoryPoints {
    /**
     * Calculates the number of required Victory Points (VP) needed to achieve overall success for this StratCon
     * contract.
     *
     * <p>The final result estimates the expected number of Turning Points the player must win for overall contract
     * success. If the player loses a handful of Turning Points, they should still be able to win the contract by being
     * proactive in the Area of Operations.</p>
     *
     * @return the required number of Victory Points, rounded up to the nearest integer
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int getRequiredVictoryPoints(AbstractContract contract) {
        if (contract.getStratConCampaignState() == null) {
            return 0;
        }

        if (contract.getCommandRights().isIndependent()) {
            return INDEPENDENT_COMMAND_RIGHTS_REQUIRED_VICTORY_POINTS;
        }

        double baseRequirement = contract.getScale();

        int duration = contract.getLengthInMonths();
        if (contract.getObjectiveType().isGarrisonType()) {
            duration = (int) ceil(duration * 0.75); // We assume around 25% of the contract will be peaceful
        }

        double trackCount = 0;
        int totalScenarioOdds = 0;
        for (StratConTrackState trackState : contract.getStratConCampaignState().getTracks()) {
            trackCount++;
            totalScenarioOdds += trackState.getScenarioOdds();
        }

        double meanScenarioOdds = totalScenarioOdds / trackCount;
        double scenarioOdds = meanScenarioOdds / 100.0;
        double turningPointChance = (contract.getCommandRights() == ContractCommandRights.INTEGRATED ? 1.0 : 0.33);

        // This result gives us the average number of Turning Points expected for the contract
        return (int) ceil(baseRequirement * duration * scenarioOdds * turningPointChance);
    }
}
