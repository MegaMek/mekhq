package mekhq.campaign.rating.CamOpsReputation;

import megamek.logging.MMLogger;
import mekhq.campaign.finances.Finances;

import java.util.Map;
import java.util.stream.Collectors;

public class FinancialRating {
    private static final MMLogger logger = MMLogger.create(FinancialRating.class);

    /**
     * Calculates the financial rating based on the current financial status.
     * Negative financial status (having a loan or a negative balance) affects the rating negatively.
     * @param finances the financial status.
     * @return a map of the financial rating.
     */
    protected static Map<String, Integer> calculateFinancialRating(Finances finances) {
        boolean hasLoan = finances.isInDebt();
        boolean inDebt = finances.getBalance().isNegative();

        Map<String, Integer> financeMap = Map.of(
                "hasLoan", hasLoan ? 1 : 0,
                "inDebt", inDebt ? 1 : 0,
                "total", (hasLoan || inDebt) ? -10 : 0
        );

        logger.info("Financial Rating = {}",
                financeMap.entrySet().stream()
                        .map(entry -> String.format("%s: %d\n", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining()));

        return financeMap;
    }
}
