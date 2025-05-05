package mekhq.campaign.market.personnelMarket.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

public enum PersonnelMarketStyle {
    NONE("", ""),
    MEKHQ("clanMarketMekHQ.yaml", "innerSphereMarketMekHQ.yaml"),
    CAMPAIGN_OPERATIONS("clanMarketCamOps.yaml", "innerSphereMarketCamOps.yaml");

    final private static String RESOURCE_BUNDLE = "mekhq.resources." + PersonnelMarketStyle.class.getSimpleName();

    private final String fileNameClan;
    private final String fileNameInnerSphere;

    PersonnelMarketStyle(String fileNameClan, String fileNameInnerSphere) {
        this.fileNameClan = fileNameClan;
        this.fileNameInnerSphere = fileNameInnerSphere;
    }

    public String getFileNameClan() {
        return fileNameClan;
    }

    public String getFileNameInnerSphere() {
        return fileNameInnerSphere;
    }

    public static PersonnelMarketStyle fromString(String text) {
        try {
            // Attempt to parse as string with case/space adjustments.
            return PersonnelMarketStyle.valueOf(text.trim().toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        try {
            // Attempt to parse as an integer and use as ordinal.
            return PersonnelMarketStyle.values()[MathUtility.parseInt(text)];
        } catch (Exception ignored) {
        }

        // Log error if parsing fails and return default value.
        MMLogger logger = MMLogger.create(PersonnelMarketStyle.class);
        logger.error("Unknown PersonnelMarketStyle ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }

    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, name() + ".label");
    }
}
