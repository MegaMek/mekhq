package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;

import java.time.LocalDate;

public class InvalidFactionNagLogic {
    /**
     * Checks whether the campaign's faction is invalid for the current in-game date.
     *
     * <p>
     * This method retrieves the campaign's faction using {@link Campaign#getFaction()} and evaluates
     * its validity for the current date using {@link Faction#validIn(LocalDate)}. A faction is considered
     * invalid if it is not valid for the campaign's local date.
     * </p>
     *
     * @return {@code true} if the faction is invalid for the campaign's current date; otherwise,
     * {@code false}.
     */
    public static boolean isFactionInvalid(Campaign campaign) {
        Faction campaignFaction = campaign.getFaction();
        LocalDate today = campaign.getLocalDate();

        return !campaignFaction.validIn(today);
    }
}
