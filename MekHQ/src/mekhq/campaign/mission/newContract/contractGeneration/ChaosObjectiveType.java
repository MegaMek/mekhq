package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.randomInt;

public enum ChaosObjectiveType {
    EXPEDITION(3),
    PIRATE_HUNT(3),
    GUERILLA_OPERATION(3),
    GARRISON(6),
    CADRE_DUTY(6),
    RAID(3),
    INVASION(6),
    PIRATE_RAID(3);

    // Hot Spots Draconis Reach pg 144 first printing
    private final int monthsLength;

    ChaosObjectiveType(final int monthsLength) {
        this.monthsLength = monthsLength;
    }

    public int getMonthsLength() {
        return monthsLength;
    }

    /**
     * Calculates the length of the contract in months.
     *
     * <p>If variable contract lengths are enabled, the length is calculated with randomization around the base
     * contract type's standard duration. Otherwise, the constant length defined by the contract type is used.</p>
     *
     * @param useVariableContractLengths whether to use variable length calculation
     *
     * @return the calculated contract length in months
     */
    public int calculateLength(final boolean useVariableContractLengths) {
        return useVariableContractLengths ? calculateVariableLength() : getMonthsLength();
    }

    /**
     * Calculates a variable contract length with randomization.
     *
     * <p>The length is calculated as 75% of the constant length plus a random variance of up to 50% of the constant
     * length. For example, a contract type with a constant length of 12 months would have a base of 9 months plus 0-6
     * months variance, resulting in a range of 9-15 months.</p>
     *
     * @return the calculated variable contract length in months
     */
    private int calculateVariableLength() {
        int baseLength = (int) round(monthsLength * 0.75);
        int variance = (int) round(monthsLength * 0.5);

        if (variance > 0) {
            return max(1, baseLength + randomInt(variance));
        } else {
            // If we can't determine variance return the constantLength
            return monthsLength;
        }
    }
}
