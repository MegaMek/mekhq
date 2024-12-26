package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class InsufficientAstechTimeNagLogic {
    /**
     * Determines whether there is a deficit in available astech time for the campaign.
     *
     * <p>
     * This method calculates the astech time deficit for the given campaign and
     * returns {@code true} if there is a positive deficit, indicating that the
     * campaign's available astech time is insufficient. If the deficit is zero
     * or negative, it returns {@code false}.
     * </p>
     *
     * @param campaign the campaign object to retrieve the astech time deficit information from
     * @return {@code true} if there is a positive astech time deficit (deficit > 0),
     *         {@code false} otherwise
     */
    public static boolean hasAsTechTimeDeficit(Campaign campaign) {
        int asTechsTimeDeficit = getAsTechTimeDeficit(campaign);
        return asTechsTimeDeficit > 0;
    }

    /**
     * Calculates the astech time deficit for the campaign.
     *
     * <p>
     * This method determines the total maintenance time required for all valid hangar units
     * in the campaign and compares it to the available astech work time. Units are only considered
     * valid if they meet the following conditions:
     * <ul>
     *   <li>Not marked as unmaintained.</li>
     *   <li>Present in the hangar.</li>
     *   <li>Not self-crewed (units maintained by their own crew are excluded).</li>
     * </ul>
     * Each valid unit requires six astechs per unit of maintenance time. If overtime is allowed in
     * the campaign, it is added to the available astech pool. The deficit is calculated, rounded up,
     * and returned.
     */
    public static int getAsTechTimeDeficit(Campaign campaign) {
        // Calculate the total maintenance time needed using a traditional loop
        int need = 0;
        for (Unit unit : campaign.getHangar().getUnits()) {
            if (!unit.isUnmaintained() && unit.isPresent() && !unit.isSelfCrewed()) {
                need += unit.getMaintenanceTime() * 6;
            }
        }

        int available = campaign.getPossibleAstechPoolMinutes();
        if (campaign.isOvertimeAllowed()) {
            available += campaign.getPossibleAstechPoolOvertime();
        }

        // Ensure deficit is non-negative
        return Math.max(0, (int) Math.ceil((need - available) / (double) Person.PRIMARY_ROLE_SUPPORT_TIME));
    }
}
