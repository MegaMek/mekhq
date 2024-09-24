package mekhq.campaign.universe.fameAndInfamy;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Provides utility methods for working with clan factions within the Fame and Infamy module:
 * determining whether they engage in batchalling, retrieving greetings and version strings for
 * factions based on various conditions, such as the faction code, infamy level, and current year.
 * <p>
 * The class is stateless and all methods are static, so it doesn't need to be instantiated.
 * Therefore, all of its methods can be called directly on the class.
 */
public class BatchallFactions {
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        "mekhq.resources.FameAndInfamy",
        MekHQ.getMHQOptions().getLocale());

    /**
     * Determines whether a given faction engages in batchalling.
     *
     * @param factionCode The faction code to check eligibility for. Must be a non-null {@link String}.
     * @return {@code true} if the faction code engages in batchalling, {@code false} otherwise.
     */
    public static boolean usesBatchalls(String factionCode) {
        if (factionCode == null) {
            return false;
        }

        List<String> factionCodes = List.of("CBS", "CB", "CCC", "CCO", "CDS", "CFM", "CGB",
            "CGS", "CHH", "CIH", "CJF", "CMG", "CNC", "CSJ", "CSR", "CSA", "CSV", "CSL", "CWI", "CW",
            "CWE", "CWIE", "CEI", "RD", "RA", "CP", "AML", "CLAN");

        return factionCodes.contains(factionCode);
    }

    /**
     * Retrieves the greeting for faction based on infamy.
     *
     * @param campaign    The campaign for which to retrieve the greeting.
     * @param factionCode The faction code for which to retrieve the greeting.
     * @return The greeting message as a {@link String}.
     */
    public static String getGreeting(Campaign campaign, String factionCode) {
        final int infamy = MathUtility.clamp(campaign.getFameAndInfamy().getFameLevelForFaction(factionCode),
            0, 5);

        String version = getVersionString(campaign.getGameYear(), factionCode);

        String greeting;
        int type = 0;

        if (infamy == 5) {
            greeting = resources.getString("greetingCLANLevel5Type0.text");
        } else {
            if (!factionCode.equals("CLAN")) {
                type = Compute.randomInt(3);
            }
            String greetingReference = String.format(resources.getString("greetingFormatBatchall.text"),
                factionCode, version, infamy, type);
            greeting = resources.getString(greetingReference);
        }

        return '"' + greeting + '"';
    }

    /**
     * Retrieves the version string based on the given faction code and current year.
     *
     * @param currentYear The current year as an {@link Integer}.
     * @param factionCode The faction code as a {@link String}.
     * @return The version {@link String} for the given faction code and current year.
     */
    private static String getVersionString(int currentYear, String factionCode) {
        switch (factionCode) {
            case "CDS" -> {
                if (currentYear < 3100) {
                    return "Version 1";
                } else {
                    return "Version 2";
                }
            }
            case "CGB" -> {
                if (currentYear < 3060) {
                    return "Version 1";
                } else {
                    return "Version 2";
                }
            }
            case "CEI" -> {
                if (currentYear < 3141) {
                    return "Version 1";
                } else {
                    return "Version 2";
                }
            }
            default -> {
                return "";
            }
        }
    }
}
