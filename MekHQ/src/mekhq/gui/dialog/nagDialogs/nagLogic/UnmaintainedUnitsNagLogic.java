package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

public class UnmaintainedUnitsNagLogic {
    /**
     * Checks whether the campaign has any unmaintained units in the hangar.
     *
     * <p>
     * This method iterates over the units in the campaign's hangar and identifies units
     * that meet the following criteria:
     * <ul>
     *     <li>The unit is classified as unmaintained ({@link Unit#isUnmaintained()}).</li>
     *     <li>The unit is not marked as salvage ({@link Unit#isSalvage()}).</li>
     * </ul>
     * If any units match these conditions, the method returns {@code true}.
     *
     * @return {@code true} if unmaintained units are found, otherwise {@code false}.
     */
    public static boolean campaignHasUnmaintainedUnits(Campaign campaign) {
        for (Unit unit : campaign.getHangar().getUnits()) {
            if ((unit.isUnmaintained()) && (!unit.isSalvage())) {
                return true;
            }
        }
        return false;
    }
}
