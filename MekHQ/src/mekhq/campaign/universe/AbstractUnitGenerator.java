package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import megamek.client.RandomUnitGenerator;
import megamek.common.MechSummary;
import mekhq.campaign.rating.IUnitRating;

/**
 * Base class for unit generators containing common functionality.
 * Currently, only turret-related code.
 * @author NickAragua
 *
 */
public abstract class AbstractUnitGenerator {
    private Map<Integer, String> ratRatingMappings = null;
    private TreeSet<Integer> turretRatYears = new TreeSet<>();
    private Map<Integer, Map<String, String>> turretRatNames = new HashMap<>();

    /**
     * Worker function to initialize the mapping between a numeric quality rating level
     * and an alphabetic one (such as one used in the RATs)
     */
    private void initializeRatRatingMappings() {
        if(ratRatingMappings == null) {
            ratRatingMappings = new HashMap<>();
            ratRatingMappings.put(IUnitRating.DRAGOON_ASTAR, "A");
            ratRatingMappings.put(IUnitRating.DRAGOON_A, "A");
            ratRatingMappings.put(IUnitRating.DRAGOON_B, "B");
            ratRatingMappings.put(IUnitRating.DRAGOON_C, "C");
            ratRatingMappings.put(IUnitRating.DRAGOON_D, "D");
            ratRatingMappings.put(IUnitRating.DRAGOON_F, "F");
        }
    }

    /**
     * Generates a list of turrets given a skill level, quality and year
     * @param num How many turrets to generate
     * @param skill The skill level of the turret operator
     * @param quality The quality level of the turret
     * @param currentYear The current year
     * @return List of turrets
     */
    public List<MechSummary> generateTurrets(int num, int skill, int quality, int currentYear) {
        int ratYear = 2500;

        // less dirty hack
        // we loop through the names of available turret RATs
        // and pick the latest one
        // turret rat file names appear to follow the pattern of "Turrets YYYY Q"
        // where YYYY is the four-digit year
        // and Q is the quality level of the force.
        // This way, as long as the turret RAT names follow the above-described pattern, we can handle any number of them.
        initializeRatRatingMappings();

        for(Iterator<String> rats = RandomUnitGenerator.getInstance().getRatList(); rats.hasNext();) {
            String currentName = rats.next();
            if(currentName.contains("Turrets")) {
                String turretQuality = currentName.substring(currentName.length() - 1);
                int year = Integer.parseInt(currentName.replaceAll("\\D", ""));

                turretRatYears.add(year);

                if(!turretRatNames.containsKey(year)) {
                    turretRatNames.put(year, new HashMap<String, String>());
                }

                turretRatNames.get(year).put(turretQuality, currentName);
            }
        }

        // we don't have rats for *every* year, so we find the nearest previous one.
        ratYear = turretRatYears.floor(currentYear);

        // now that we have the year, we need to determine which turret RAT we're going to use
        String ratName = turretRatNames.get(ratYear).get(ratRatingMappings.get(quality));

        RandomUnitGenerator.getInstance().setChosenRAT(ratName);
        ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(num);
        return msl;
    }
}
