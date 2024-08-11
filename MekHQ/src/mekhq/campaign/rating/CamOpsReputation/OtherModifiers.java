package mekhq.campaign.rating.CamOpsReputation;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OtherModifiers {
    private static final MMLogger logger = MMLogger.create(OtherModifiers.class);

    /**
     * Calculates the 'other modifiers' used by CamOps Reputation
     *
     * @param campaign The campaign for which to calculate the modifiers.
     * @return A map representing the calculated modifiers. The map contains two entries:
     *         - "inactiveYears": The number of inactive years calculated from the campaign options.
     *         - "total": The total value calculated based on the number of inactive years.
     */
    protected static Map<String, Integer> calculateOtherModifiers(Campaign campaign) {
        // Calculate inactive years if campaign options allow
        int inactiveYears = campaign.getCampaignOptions().isUseAtB() ? getInactiveYears(campaign) : 0;
        int manualModifier = campaign.getCampaignOptions().getManualUnitRatingModifier();

        // Crime rating improvements are handled on New Day, so are not included here.

        // Create a map for modifiers with "inactive years" and "total" calculated from inactive years
        Map<String, Integer> modifierMap = Map.of(
                "inactiveYears", inactiveYears,
                "customModifier", manualModifier,
                "total", manualModifier - (inactiveYears * 5)
        );

        // Log the calculated modifiers
        logger.debug("Other Modifiers = {}",
                modifierMap.entrySet().stream()
                        .map(entry -> String.format("%s: %d\n", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining()));

        // Return the calculated modifier map
        return modifierMap;
    }

    /**
     * @return the number of years between the oldest mission date and the current date.
     *
     * @param campaign the current campaign
     */
    private static int getInactiveYears(Campaign campaign) {
        LocalDate today = campaign.getLocalDate();

        // Build a list of completed contracts, excluding Garrison and Cadre contracts
        List<AtBContract> contracts = getSuitableContracts(campaign);

        // Decide the oldest mission date based on the earliest completion date of the contracts
        // or the campaign start date if there are no completed contracts
        LocalDate oldestMissionDate = contracts.isEmpty() ? campaign.getCampaignStartDate()
                : contracts.stream()
                .map(AtBContract::getEndingDate)
                .min(LocalDate::compareTo)
                .orElse(today);

        // Calculate and return the number of years between the oldest mission date and today
        return Math.max(0, (int) ChronoUnit.YEARS.between(today, oldestMissionDate));
    }

    /**
     * Retrieves a list of suitable AtBContracts for the given Campaign.
     *
     * @param campaign The Campaign to retrieve contracts from.
     * @return A List of suitable AtBContracts.
     */
    private static List<AtBContract> getSuitableContracts(Campaign campaign) {
        // Filter mission of type AtBContract and with completed status, check if it's suitable
        return campaign.getMissions().stream()
                .filter(c -> (c instanceof AtBContract) && (c.getStatus().isCompleted()))
                .filter(c -> isSuitableContract((AtBContract) c))
                .map(c -> (AtBContract) c)
                .toList();
    }

    /**
     * Determines whether a given AtBContract is suitable.
     * CamOps excludes Garrison and Cadre contracts when calculating inactivity.
     *
     * @param contract The AtBContract to check.
     * @return true if the contract is suitable, false otherwise.
     */
    private static boolean isSuitableContract(AtBContract contract) {
        AtBContractType contractType = contract.getContractType();

        return (!contractType.isGarrisonType() && !contractType.isCadreDuty());
    }
}
