package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Money;

import java.util.Objects;

public class UnableToAffordJumpNagLogic {
    /**
     * Determines whether the campaign's current funds are insufficient to cover the cost
     * of the next jump.
     *
     * <p>
     * This method compares the campaign's available funds with the calculated cost
     * of the next jump stored in the {@code nextJumpCost} field. If the funds are less than
     * the jump cost, it returns {@code true}, indicating that the jump cannot be afforded;
     * otherwise, it returns {@code false}.
     * </p>
     *
     * @return {@code true} if the campaign's funds are less than the cost of the next jump;
     *         {@code false} otherwise.
     */
    public static boolean unableToAffordNextJump(Campaign campaign) {
        Money nextJumpCost = getNextJumpCost(campaign);
        return campaign.getFunds().isLessThan(nextJumpCost);
    }

    /**
     * Calculates the cost of the next jump based on the campaign's location and financial settings.
     *
     * <p>
     * This method retrieves the {@link JumpPath} for the campaign's current location and only
     * calculates the jump cost if the next system on the path differs from the current system.
     * The actual jump cost is determined by the campaign's settings, particularly whether
     * contracts base their costs on the value of units in the player's TOE (Table of Equipment).
     * </p>
     */
    public static Money getNextJumpCost(Campaign campaign) {
        CurrentLocation location = campaign.getLocation();
        JumpPath jumpPath = location.getJumpPath();

        if (jumpPath == null) {
            return Money.zero();
        }

        if (Objects.equals(jumpPath.getLastSystem(), location.getCurrentSystem())) {
            return Money.zero();
        }

        boolean isContractPayBasedOnToeUnitsValue = campaign.getCampaignOptions().isEquipmentContractBase();

        return campaign.calculateCostPerJump(true, isContractPayBasedOnToeUnitsValue);
    }
}
