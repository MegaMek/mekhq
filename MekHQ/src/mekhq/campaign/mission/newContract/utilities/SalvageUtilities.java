package mekhq.campaign.mission.newContract.utilities;

import java.math.RoundingMode;

import mekhq.campaign.finances.Money;

public class SalvageUtilities {
    /**
     * Computes the player's share of the total salvage value as an integer percentage, using
     * {@link RoundingMode#CEILING} (i.e. any fractional percentage rounds up to the next whole percent).
     *
     * <p>Rounding up is intentional from a gameplay standpoint: the percentage is compared against the contract's
     * salvage cap, and a true value of e.g. 50.001% against a 50% cap is a breach and must be surfaced as such. It also
     * fixes the truncation artifacts that previously could cause the displayed value to shift by a full percentage
     * point after a small change to the salvage assignment (see issue #5683).</p>
     *
     * @param playerShare   the salvage value assigned to the player (mercs)
     * @param employerShare the salvage value assigned to the employer
     *
     * @return integer percentage in the range {@code [0, 100]}, or {@code 0} if there is no salvage to split
     */
    public static int calculateSalvagePercentage(Money playerShare, Money employerShare) {
        Money total = playerShare.plus(employerShare);
        if (!total.isPositive()) {
            return 0;
        }
        return playerShare.multipliedBy(100)
                     .getAmount()
                     .divide(total.getAmount(), 0, RoundingMode.CEILING)
                     .intValue();
    }
}
