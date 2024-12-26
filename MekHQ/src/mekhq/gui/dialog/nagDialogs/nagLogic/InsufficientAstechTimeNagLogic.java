package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class InsufficientAstechTimeNagLogic {
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
     * and stored in {@link #asTechsTimeDeficit}.
     */
    public static int getAsTechTimeDeficit(Campaign campaign) {
        // Calculate the total maintenance time needed using a traditional loop
        int need = 0;
        for (Unit unit : campaign.getHangar().getUnits()) { // Assuming getUnits() returns a collection of units
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
