package mekhq.campaign.mission.mission;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.logging.MMLogger;
import mekhq.campaign.randomEvents.personalities.Aggression;
import org.jspecify.annotations.NonNull;

public enum IndependentEmployerTableValue {
    NOBLE("NOBLE", Integer.MIN_VALUE, 3),
    PLANETARY_GOVERNMENT("PLANETARY_GOVERNMENT", 4, 5),
    MERCENARY("MERCENARY", 6, 6),
    MAJOR_PERIPHERY("MAJOR_PERIPHERY", 7, 8),
    MINOR_PERIPHERY("MINOR_PERIPHERY", 9, 10),
    CORPORATION("CORPORATION", 11, Integer.MAX_VALUE);

    private final String lookupName;
    private final String label;
    private final String tooltip;
    private final int lowerBand;
    private final int upperBand;

    private final String RESOURCE_BUNDLE = "mekhq.resources.IndependentEmployerTableValue";

    // region Constructors
    IndependentEmployerTableValue(final String lookupName, final int lowerBand, final int upperBand) {
        this.lookupName = lookupName;
        this.label = getName(lookupName);
        this.tooltip = getTooltip(lookupName);
        this.lowerBand = lowerBand;
        this.upperBand = upperBand;
    }

    private @NonNull String getTooltip(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "IndependentEmployerTableValue." + lookupName + ".tooltip");
    }

    private @NonNull String getName(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "IndependentEmployerTableValue." + lookupName + ".name");
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getLabel() {
        return label;
    }

    public int getLowerBand() {
        return lowerBand;
    }

    public int getUpperBand() {
        return upperBand;
    }

    public boolean isWithinRange(int value) {
        return value >= lowerBand && value <= upperBand;
    }

    public boolean isIncludedForRoll(int value) {
        return value <= upperBand;
    }

    public static IndependentEmployerTableValue fromString(String text) {
        try {
            return IndependentEmployerTableValue.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        for (IndependentEmployerTableValue value : values()) {
            if (value.lookupName.equals(text)) {
                return value;
            }
        }

        try {
            return IndependentEmployerTableValue.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}

        MMLogger logger = MMLogger.create(Aggression.class);
        logger.error("Unknown IndependentEmployerTableValue ordinal: {} - returning {}.",
              text,
              PLANETARY_GOVERNMENT.lookupName);

        return PLANETARY_GOVERNMENT;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
