package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;

public class NoCommanderNagLogic {
    /**
     * Checks if the campaign has no assigned commander.
     *
     * <p>
     * This method determines whether the campaign has a flagged commander assigned or not.
     * If {@code campaign.getFlaggedCommander()} returns {@code null}, it indicates
     * that no commander has been assigned.
     * </p>
     *
     * @return {@code true} if the campaign has no flagged commander
     * ({@code campaign.getFlaggedCommander()} is {@code null}); {@code false} otherwise.
     */
    public static boolean hasNoCommander(Campaign campaign) {
        return campaign.getFlaggedCommander() == null;
    }
}
