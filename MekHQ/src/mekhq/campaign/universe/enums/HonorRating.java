package mekhq.campaign.universe.enums;

/**
 * Represents the honor of a Clan
 */
public enum HonorRating {
    NONE(0.0, Integer.MAX_VALUE),
    LIBERAL(1.25, Integer.MAX_VALUE),
    OPPORTUNISTIC(1.0, 5),
    STRICT(0.75, 0);

    private final double bvMultiplier;
    private final int bondsmanTargetNumber;

    /**
     * Constructor for HonorRating enum to initialize its properties.
     *
     * @param bvMultiplier       Battle Value multiplier associated with the honor level - used by
     *                          Clan Bidding
     * @param bondsmanTargetNumber Target number for determining bondsmen with this style
     */
    HonorRating(double bvMultiplier, int bondsmanTargetNumber) {
        this.bvMultiplier = bvMultiplier;
        this.bondsmanTargetNumber = bondsmanTargetNumber;
    }

    /**
     * Gets the Battle Value multiplier associated with this capture style.
     *
     * @return the bvMultiplier
     */
    public double getBvMultiplier() {
        return bvMultiplier;
    }

    /**
     * Gets the target number for becoming a bondsman.
     *
     * @return the bondsmanTargetNumber
     */
    public int getBondsmanTargetNumber() {
        return bondsmanTargetNumber;
    }
}
