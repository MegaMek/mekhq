package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;

import java.time.LocalDate;

public class EndContractNagLogic {
    /**
     * Checks if any contract in the current campaign has its end date set to today.
     *
     * <p>
     * This method is used to detect whether there are any active contracts
     * ending on the campaign's current local date. It iterates over the active
     * contracts for the campaign and compares each contract's ending date to today's date.
     * </p>
     *
     * @return {@code true} if a contract's end date matches today's date, otherwise {@code false}.
     */
    public static boolean isContractEnded(Campaign campaign) {
        LocalDate today = campaign.getLocalDate();

        // we can't use 'is date y after x', as once the end date has been passed,
        // the contract is removed from the list of active contracts

        // there is no reason to use a stream here, as there won't be enough iterations to warrant it
        for (Contract contract : campaign.getActiveContracts()) {
            if (contract.getEndingDate().equals(today)) {
                return true;
            }
        }

        return false;
    }
}
