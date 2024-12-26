package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;

public class UnresolvedStratConContactsNagLogic {
    /**
     * Checks if there are any unresolved contacts in the current report.
     *
     * <p>
     * This method inspects the {@code unresolvedContactsReport} and determines whether
     * it contains any unresolved contacts. If the report is not empty, it indicates
     * that there are unresolved contacts, and the method returns {@code true};
     * otherwise, it returns {@code false}.
     * </p>
     *
     * @return {@code true} if there are unresolved contacts in the report;
     *         {@code false} otherwise.
     */
    public static boolean hasUnresolvedContacts(Campaign campaign) {
        String unresolvedContactsReport = determineUnresolvedContacts(campaign);
        return !unresolvedContactsReport.isEmpty();
    }

    /**
     * Determines unresolved StratCon contacts for the campaign and generates a report.
     *
     * <p>
     * This method checks all active AtB contracts in the campaign and iterates over their
     * StratCon tracks to find unresolved scenarios. Scenarios are considered unresolved if:
     * <ul>
     *     <li>Their current state is {@link StratconScenario.ScenarioState#UNRESOLVED}.</li>
     *     <li>Their deployment date matches the current campaign date.</li>
     * </ul>
     * A formatted report is created, summarizing all unresolved scenarios and marking critical ones.
     */
    public static String determineUnresolvedContacts(Campaign campaign) {
        StringBuilder unresolvedContacts = new StringBuilder();

        // check every track attached to an active contract for unresolved scenarios
        // to which the player can deploy forces
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (contract.getStratconCampaignState() == null) {
                continue;
            }

            for (StratconTrackState track : contract.getStratconCampaignState().getTracks()) {
                for (StratconScenario scenario : track.getScenarios().values()) {
                    if ((scenario.getCurrentState() == StratconScenario.ScenarioState.UNRESOLVED)
                        && (campaign.getLocalDate().equals(scenario.getDeploymentDate()))) {
                        unresolvedContacts.append(String.format("<br><b>- %s</b>, %s, %s-%s %s",
                            scenario.getName(), contract.getName(),
                            track.getDisplayableName(), scenario.getCoords().toBTString(),
                            scenario.isRequiredScenario() ? " (Critical)" : ""));
                    }
                }
            }
        }

        return unresolvedContacts.toString();
    }
}
