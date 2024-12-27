package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;

public class PrisonersNagLogic {
    /**
     * Checks if the current campaign has prisoners of war (POWs).
     *
     * <p>
     * This method evaluates the state of the campaign to determine if there are prisoners present.
     * If the campaign does not have an active contract, the method checks the campaign's list of
     * current prisoners. If the list is not empty, the method returns {@code true}.
     * </p>
     *
     * @return {@code true} if there are prisoners in the campaign; otherwise, {@code false}.
     */
    public static boolean hasPrisoners(Campaign campaign) {
        if (!campaign.hasActiveContract()) {
            return !campaign.getCurrentPrisoners().isEmpty();
        }

        return false;
    }
}
