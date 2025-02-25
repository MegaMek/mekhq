package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;

public class InsufficientAstechsNagLogic {
    /**
     * Determines whether the campaign has a need for astechs.
     *
     * <p>
     * This method checks the number of astechs needed in the given campaign
     * and returns {@code true} if the number of needed astechs is greater than zero,
     * indicating that there is a requirement for astechs. If the number is zero or negative,
     * the method returns {@code false}.
     * </p>
     *
     * @param campaign the campaign object to retrieve the astech need from
     * @return {@code true} if the campaign requires astechs (astech need > 0),
     *         {@code false} otherwise
     */
    public static boolean hasAsTechsNeeded(Campaign campaign) {
        int asTechsNeeded = campaign.getAstechNeed();

        return asTechsNeeded > 0;
    }
}
