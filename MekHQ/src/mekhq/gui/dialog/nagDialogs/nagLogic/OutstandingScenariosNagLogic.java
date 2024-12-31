package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;

import java.time.LocalDate;
import java.util.List;

import static mekhq.campaign.stratcon.StratconCampaignState.getStratconScenarioFromAtBScenario;
import static mekhq.campaign.stratcon.StratconScenario.ScenarioState.UNRESOLVED;

public class OutstandingScenariosNagLogic {
    /**
     * Checks if there are any outstanding scenarios in the campaign.
     *
     * <p>
     * This method evaluates whether the {@code outstandingScenarios} string is blank or not.
     * If the string is not blank, it indicates that there are outstanding scenarios
     * that need to be addressed.
     * </p>
     *
     * @return {@code true} if {@code outstandingScenarios} is not blank, indicating there are
     * outstanding scenarios; {@code false} otherwise.
     */
    public static boolean hasOutStandingScenarios(Campaign campaign) {
        String outstandingScenarios = getOutstandingScenarios(campaign);
        return !outstandingScenarios.isBlank();
    }

    /**
     * Retrieves and processes the list of outstanding scenarios for the current campaign.
     *
     * <p>
     * This method iterates through all active contracts and their associated AtB scenarios,
     * identifying scenarios that are outstanding based on the following conditions:
     * <ul>
     *     <li>Whether the scenario's date matches the current campaign date.</li>
     *     <li>If the scenario is part of StratCon and is unresolved or critical.</li>
     *     <li>If it's associated with a track and includes detailed information about that track.</li>
     * </ul>
     * Scenarios are categorized into "critical" scenarios (e.g., required StratCon scenarios)
     * and others, with additional formatting for StratCon-specific scenarios where applicable.
     */
    public static String getOutstandingScenarios(Campaign campaign) {
        List<AtBContract> activeContracts = campaign.getActiveAtBContracts(true);
        LocalDate today = campaign.getLocalDate();
        StringBuilder activeScenarios = new StringBuilder();

        for (AtBContract contract : activeContracts) {
            for (AtBScenario scenario : contract.getCurrentAtBScenarios()) {
                LocalDate scenarioDate = scenario.getDate();

                // Skip scenarios not matching today's date
                if (!scenarioDate.equals(today)) {
                    continue;
                }

                if (scenario.getHasTrack()) {
                    StratconScenario stratconScenario = getStratconScenarioFromAtBScenario(campaign, scenario);

                    if (stratconScenario != null) {
                        // Skip if the scenario is unresolved
                        if (stratconScenario.getCurrentState() == UNRESOLVED) {
                            continue;
                        }

                        StratconTrackState track = stratconScenario.getTrackForScenario(campaign, null);

                        if (track != null) {
                            activeScenarios.append("<br>- ")
                                .append(scenario.getName())
                                .append(", ").append(contract.getName())
                                .append(", ").append(track.getDisplayableName())
                                .append('-').append(stratconScenario.getCoords().toBTString());

                            if (stratconScenario.isTurningPoint()) {
                                activeScenarios.append(" (Turning Point)");
                            }

                            continue;
                        }
                    }
                }

                // Add non-track scenarios
                activeScenarios.append("<br>- ")
                    .append(scenario.getName())
                    .append(", ").append(contract.getName());
            }
        }

        return activeScenarios.toString();
    }
}
