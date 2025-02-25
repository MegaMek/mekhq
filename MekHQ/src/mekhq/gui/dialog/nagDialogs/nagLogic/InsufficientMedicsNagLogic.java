package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;

public class InsufficientMedicsNagLogic {
    /**
     * Checks if there is a need for medics in the campaign.
     *
     * <p>
     * This method evaluates whether the number of required medics is greater than zero.
     * If {@code medicsRequired} is greater than zero, it means that additional medics
     * are needed to meet the campaign's requirements.
     * </p>
     *
     * @return {@code true} if the number of required medics ({@code medicsRequired}) is greater than zero;
     *         {@code false} otherwise.
     */
    public static boolean hasMedicsNeeded(Campaign campaign) {
        int medicsRequired = campaign.getMedicsNeed();

        return medicsRequired > 0;
    }
}
