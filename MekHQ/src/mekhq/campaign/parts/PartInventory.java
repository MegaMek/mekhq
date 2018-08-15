package mekhq.campaign.parts;

/**
 * Describes the inventory details of a part.
 */
public class PartInventory {
    private int supply;
    private int transit;
    private int ordered;
    private String countModifier = "";

    /**
     * Gets the count of a part on hand.
     * @return part count on hand.
     */
    public int getSupply() {
        return supply;
    }

    /**
     * Sets the count of a part on hand.
     * @param count count of a part on hand.
     */
    public void setSupply(int count) {
        supply = count;
    }

    /**
     * Formats the count of a supply on hand as a String.
     * @return the count of a part's supply on hand as a String.
     */
    public String supplyAsString() {
        return supply + countModifier;
    }

    /**
     * Gets the count in transit of a part.
     * @return the count of a part in transit.
     */
    public int getTransit() {
        return transit;
    }

    /**
     * Sets the count in transit of a part.
     * @param count count in transit of a part.
     */
    public void setTransit(int count) {
        transit = count;
    }

    /**
     * Formats the count in transit of a part as a String.
     * @return the count in transit of a part as a String.
     */
    public String transitAsString() {
        return transit + countModifier;
    }

    /**
     * Gets the count ordered of a part.
     * @return count ordered of a part.
     */
    public int getOrdered() {
        return ordered;
    }

    /**
     * Sets the count ordered of a part.
     * @param count count ordered of a part.
     */
    public void setOrdered(int count) {
        ordered = count;
    }

    /**
     * Formats the count ordered of a part as a String.
     * @return count ordered of a part as a String.
     */
    public String orderedAsString() {
        return ordered + countModifier;
    }

    /**
     * Gets the modifier to display next to a count when formatted as a String.
     * @return modifier displayed next to a count when formatted as a String.
     */
    public String getCountModifier() {
        return countModifier;
    }

    /** 
     * Sets the modifier to display next to a count when formatted as a String.
     * @param countModifier modifier to display next to a count when formatted as a String.
     */
    public void setCountModifier(String countModifier) {
        if (countModifier != null && countModifier.length() > 0) {
            this.countModifier = " " + countModifier;
        }
    }

    /**
     * Gets the transit and ordered counts formatted as a String.
     * @return A String like, <code>&quot;XXX in transit, YYY on order&quot;</code>, describing 
     * the transit and ordered counts.
     * @see #transitAsString()
     * @see #orderedAsString()
     */
    public String getTransitOrderedDetails() {
        return transitAsString() + " in transit, " + orderedAsString() + " on order";
    }
}
