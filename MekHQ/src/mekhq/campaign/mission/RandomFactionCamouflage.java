package mekhq.campaign.mission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import megamek.common.icons.Camouflage;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Factions;

public class RandomFactionCamouflage {
    private static final MMLogger LOGGER = MMLogger.create(RandomFactionCamouflage.class);

    /**
     * Selects random camouflage for the given faction based on the faction code and year. If there are no available
     * files in the faction directory, it logs a warning and uses default camouflage.
     *
     * @param currentYear the current year in the game.
     * @param factionCode the code representing the faction for which the camouflage is to be selected.
     */
    public static Camouflage pickRandomCamouflage(int currentYear, String factionCode) {
        // Define the root directory and get the faction-specific camouflage directory
        final String ROOT_DIRECTORY = "data/images/camo/";

        String camouflageDirectory = "Standard Camouflage";

        if (factionCode != null) {
            camouflageDirectory = getCamouflageDirectory(currentYear, factionCode);
        }

        // Gather all files
        List<Path> allPaths = null;

        try (Stream<Path> stream = Files.find(Paths.get(ROOT_DIRECTORY + camouflageDirectory + '/'),
              Integer.MAX_VALUE,
              (path, bfa) -> bfa.isRegularFile())) {
            allPaths = stream.toList();
        } catch (IOException e) {
            LOGGER.error("Error getting list of camouflages", e);
        }

        // Select a random file to set camouflage, if there are files available
        if ((null != allPaths) && (!allPaths.isEmpty())) {
            Path randomPath = allPaths.get(new Random().nextInt(allPaths.size()));

            String fileName = randomPath.getFileName().toString();
            String fileCategory = randomPath.getParent()
                                        .toString()
                                        .replaceAll("\\\\", "/"); // This is necessary for Windows machines
            fileCategory = fileCategory.replace(ROOT_DIRECTORY, "");

            return new Camouflage(fileCategory, fileName);
        } else {
            // Log if no files were found in the directory
            LOGGER.warn("No files in directory {} - using default camouflage", camouflageDirectory);
            return new Camouflage(); // return no camouflage
        }
    }

    /**
     * Returns the directory for the camouflages of a faction based on the year and faction code.
     *
     * @param year        The year
     * @param factionCode The code representing the faction, e.g. FS or HL
     *
     * @return The directory under data/images/camo for the camouflages of the faction
     */
    private static String getCamouflageDirectory(int year, String factionCode) {
        return Factions.getInstance().getFaction(factionCode)
                     .getCamosFolder(year)
                     .orElse("Standard Camouflage");
    }
}
