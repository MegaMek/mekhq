ç
package mekhq.campaign.mission.mission.contractGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import org.jspecify.annotations.NonNull;

public enum GlobalEmployerTableValue {
    INDEPENDENT("INDEPENDENT", Integer.MIN_VALUE, 5),
    MINOR_POWER("MINOR_POWER", 6, 7),
    MAJOR_POWER("MAJOR_POWER", 8, 10),
    SUPER_POWER("SUPER_POWER", 11, Integer.MAX_VALUE);

    private final String lookupName;
    private final String label;
    private final String tooltip;
    private final int lowerBand;
    private final int upperBand;

    private final String RESOURCE_BUNDLE = "mekhq.resources.GlobalEmployerTableValue";
    private final static MMLogger LOGGER = MMLogger.create(GlobalEmployerTableValue.class);

    GlobalEmployerTableValue(final String lookupName, final int lowerBand, final int upperBand) {
        this.lookupName = lookupName;
        this.label = generateLabel(lookupName);
        this.tooltip = generateTooltip(lookupName);
        this.lowerBand = lowerBand;
        this.upperBand = upperBand;
    }

    private @NonNull String generateTooltip(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "GlobalEmployerTableValue." + lookupName + ".tooltip");
    }

    private @NonNull String generateLabel(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "GlobalEmployerTableValue." + lookupName + ".name");
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

    public static GlobalEmployerTableValue getEmployerForRoll(int roll) {
        for (GlobalEmployerTableValue employer : values()) {
            if (employer.isWithinRange(roll)) {
                return employer;
            }
        }
        LOGGER.warn("Roll {} is outside of any employer range. Returning MAJOR_POWER", roll);

        return MAJOR_POWER;
    }

    public @Nullable GlobalEmployerTableValue getNextLowestEmployerType() {
        return switch (this) {
            case INDEPENDENT -> null;
            case MINOR_POWER -> INDEPENDENT;
            case MAJOR_POWER -> MINOR_POWER;
            case SUPER_POWER -> MAJOR_POWER;
        };
    }

    public static GlobalEmployerTableValue fromString(String text) {
        try {
            return GlobalEmployerTableValue.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        for (GlobalEmployerTableValue value : values()) {
            if (value.lookupName.equals(text)) {
                return value;
            }
        }

        try {
            return GlobalEmployerTableValue.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}

        LOGGER.error("Unknown GlobalEmployerTableValue ordinal: {} - returning {}.", text, MAJOR_POWER.lookupName);

        return MAJOR_POWER;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
