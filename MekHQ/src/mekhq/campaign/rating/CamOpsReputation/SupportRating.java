/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.rating.CamOpsReputation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import megamek.common.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class SupportRating {
    private static final MMLogger logger = MMLogger.create(SupportRating.class);
    private static final int VEHICLE_WEIGHT_DIVIDER = 15;

    /**
     * This method calculates the support rating for a campaign.
     *
     * @param campaign                   The campaign object on which the
     *                                   calculation is based.
     * @param transportationRequirements A map representing the transportation
     *                                   requirements.
     * @return A map containing maps with the calculated support rating, as well as
     *         individual requirements.
     */
    protected static Map<String, Map<String, ?>> calculateSupportRating(Campaign campaign,
            Map<String, Integer> transportationRequirements) {
        // Create a map to store the results
        Map<String, Map<String, ?>> supportRating = new HashMap<>();

        // Calculate the crew requirements for this campaign
        Map<String, Integer> crewRequirements = calculateCrewRequirements(campaign);
        // Calculate the technician requirements for this campaign
        Map<String, List<Integer>> technicianRequirements = calculateTechnicianRequirements(campaign,
                transportationRequirements);
        // Calculate the administration requirements for this campaign
        Map<String, Integer> administrationRequirements = calculateAdministratorRequirements(campaign,
                technicianRequirements.get("totals").get(0));

        // Add the calculated requirements into the supportRating map
        supportRating.put("administrationRequirements", administrationRequirements);
        supportRating.put("crewRequirements", crewRequirements);
        supportRating.put("technicianRequirements", technicianRequirements);

        // Calculate the total of requirements
        int total = administrationRequirements.get("total")
                + crewRequirements.get("crewRequirements")
                + technicianRequirements.get("rating").get(0);

        // Add the total value into the supportRating map
        supportRating.put("total", Map.of("total", total));

        logger.debug("Support Rating = {}", total);

        // Return the final map containing the calculated values
        return supportRating;
    }

    /**
     * Calculates the campaign's administrative requirements.
     *
     * @param campaign               The campaign for which to calculate the
     *                               administrative requirements.
     * @param technicianRequirements The number of technicians required by the
     *                               campaign.
     * @return A map containing the following information:
     *         - "totalPersonnelCount": The total number of personnel in the
     *         campaign.
     *         - "administratorCount": The number of administrators in the campaign.
     *         - "personnelCount": The calculated number of non-administrator
     *         personnel required based on the campaign faction.
     *         - "total": A calculated value indicating the total administrative
     *         requirement, where 0 indicates
     *         a sufficient of non-administrator personnel compared to
     *         administrators, and -5 indicates
     *         a shortage.
     */
    private static Map<String, Integer> calculateAdministratorRequirements(Campaign campaign,
            int technicianRequirements) {
        Map<String, Integer> administrationRequirements = new HashMap<>();

        // Get the total sums of personnel and administrators
        int totalPersonnelCount = getTotalPersonnelCount(campaign, technicianRequirements);

        int administratorCount = (int) campaign.getActivePersonnel().stream()
                .filter(Person::isAdministrator)
                .count();

        // Calculate personnel count based on campaign faction
        double divisor = campaign.getFaction().isPirate() || campaign.getFaction().isMercenary() ? 10 : 20;
        int personnelCount = (int) Math.ceil(totalPersonnelCount / divisor);

        administrationRequirements.put("totalPersonnelCount", totalPersonnelCount);
        administrationRequirements.put("administratorCount", administratorCount);
        administrationRequirements.put("personnelCount", personnelCount);

        // Calculate final total
        int total = personnelCount > administratorCount ? -5 : 0;
        administrationRequirements.put("total", total);

        logger.debug("Administrator Requirements = {}",
                administrationRequirements.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue() + '\n')
                        .collect(Collectors.joining()));

        return administrationRequirements;
    }

    /**
     * Calculates the total personnel count required for the campaign, taking into
     * account technician requirements.
     *
     * @param campaign               The campaign for which to calculate the total
     *                               personnel count.
     * @param technicianRequirements The number of technicians required for the
     *                               campaign.
     * @return The total personnel count required for the campaign.
     */
    private static int getTotalPersonnelCount(Campaign campaign, int technicianRequirements) {
        int totalPersonnelCount = technicianRequirements;

        // Count personnel
        for (Unit unit : campaign.getActiveUnits()) {
            if (unit.isMothballed()) {
                continue;
            }

            Entity entity = unit.getEntity();

            if (entity.isMek() || entity.isAerospaceFighter() || entity.isConventionalFighter()) {
                totalPersonnelCount++;
            } else if (entity.isVehicle()) {
                totalPersonnelCount += (int) Math.ceil(entity.getWeight() / VEHICLE_WEIGHT_DIVIDER);
            } else {
                totalPersonnelCount += unit.getFullCrewSize();
            }
        }

        return totalPersonnelCount;
    }

    /**
     * Calculates the crew requirements for the campaign.
     * If a not fully crewed LargeCraft is found, the crew requirements are set to
     * -5.
     * Otherwise, the crew requirements are set to 0.
     *
     * @param campaign the campaign for which to calculate the crew requirements
     * @return a map containing the crew requirements, with the key
     *         "crewRequirements" and the value either -5 or 0
     */
    private static Map<String, Integer> calculateCrewRequirements(Campaign campaign) {
        int crewRequirements = 0;

        // Iterate over all units in the campaign
        for (Unit unit : campaign.getActiveUnits()) {
            Entity entity = unit.getEntity();

            // Check if the unit is a LargeCraft and is not fully crewed
            if (entity.isLargeCraft() && !unit.isFullyCrewed()) {
                crewRequirements = -5;
                break; // Exit the loop as soon as a not fully crewed LargeCraft is found
            }
        }

        return Map.of("crewRequirements", crewRequirements);
    }

    /**
     * Calculates the technician requirements based on transportation requirements.
     *
     * @param campaign                   The campaign for which to calculate the
     *                                   technician requirements.
     * @param transportationRequirements The transportation requirements for the
     *                                   campaign.
     * @return A map where the keys represent the technician type and the values
     *         represent a list of the technician count and the tech count.
     */
    private static Map<String, List<Integer>> calculateTechnicianRequirements(Campaign campaign,
            Map<String, Integer> transportationRequirements) {
        Map<String, List<Integer>> technicianRequirements = new HashMap<>();

        // Calculate counts for each unit type
        int mekCount = transportationRequirements.get("mekCount");
        int vehicleCount = transportationRequirements.get("totalVehicleCount");
        int aeroCount = transportationRequirements.get("asfCount") + transportationRequirements.get("smallCraftCount");
        var baProtoCounts = calculateBattleArmorAndProtoMekCounts(campaign);
        int battleArmorCount = Math.round(baProtoCounts.get("battleArmorCount") / 5.0f);
        mekCount += Math.round(baProtoCounts.get("protoMekCount") / 5.0f);

        // Calculate tech counts
        var techCounts = calculateTechCounts(campaign);
        technicianRequirements.put("mek", List.of(mekCount, techCounts.get("techMekCount")));
        technicianRequirements.put("vehicle", List.of(vehicleCount, techCounts.get("techMechanicCount")));
        technicianRequirements.put("aero", List.of(aeroCount, techCounts.get("techAeroCount")));
        technicianRequirements.put("battleArmor", List.of(battleArmorCount, techCounts.get("techBattleArmorCount")));

        // Calculate total requirements and techs
        int totalRequirements = (mekCount + vehicleCount + aeroCount + battleArmorCount);
        int totalTechs = techCounts.values().stream().mapToInt(Integer::intValue).sum();
        int percentage = (int) ((float) totalTechs / totalRequirements * 100);

        if (totalRequirements == 0) {
            percentage = 100;
        }

        technicianRequirements.put("totals", List.of(totalRequirements, totalTechs));
        technicianRequirements.put("rating", List.of(calculateTechRating(percentage)));

        logger.debug("Technician Requirements = {}",
                technicianRequirements.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue() + '\n')
                        .collect(Collectors.joining()));

        return technicianRequirements;
    }

    /**
     * Calculates the total count of battle armor and proto meks in a given
     * campaign.
     *
     * @param campaign the campaign containing the units to be counted
     * @return a map containing the counts of battle armor and proto meks, with the
     *         keys "battleArmorCount" and "protoMekCount"
     */
    private static Map<String, Integer> calculateBattleArmorAndProtoMekCounts(Campaign campaign) {
        Map<String, Integer> counts = new HashMap<>();

        int battleArmorCount = 0;
        int protoMekCount = 0;

        for (Unit unit : campaign.getActiveUnits()) {
            Entity entity = unit.getEntity();

            if (entity.isBattleArmor()) {
                battleArmorCount += unit.getFullCrewSize();
            } else if (entity.isProtoMek()) {
                protoMekCount += unit.getFullCrewSize();
            }
        }

        counts.put("battleArmorCount", battleArmorCount);
        counts.put("protoMekCount", protoMekCount);

        return counts;
    }

    /**
     * Calculates the number of personnel with different tech roles in the given
     * campaign.
     *
     * @param campaign the campaign object from which to calculate the tech counts
     * @return a map that contains the counts of different tech roles. The keys are
     *         as follows:
     *         - "techMekCount" for personnel with the role "TechMek"
     *         - "techMechanicCount" for personnel with the role "TechMechanic"
     *         - "techAeroCount" for personnel with the role "TechAero"
     *         - "techBattleArmorCount" for personnel with the role "TechBA"
     */
    private static Map<String, Integer> calculateTechCounts(Campaign campaign) {
        Map<String, Integer> techCounts = new HashMap<>();

        techCounts.put("techMekCount", 0);
        techCounts.put("techMechanicCount", 0);
        techCounts.put("techAeroCount", 0);
        techCounts.put("techBattleArmorCount", 0);

        for (Person person : campaign.getActivePersonnel()) {
            updateCount(person::isTechMek, "techMekCount", techCounts);
            updateCount(person::isTechMechanic, "techMechanicCount", techCounts);
            updateCount(person::isTechAero, "techAeroCount", techCounts);
            updateCount(person::isTechBA, "techBattleArmorCount", techCounts);
        }

        return techCounts;
    }

    /**
     * Updates the count in the given map based on a condition.
     * If the condition is true, the count for the specified key in the map is
     * incremented by 1.
     *
     * @param condition The condition to check. If true, the count will be updated.
     * @param key       The key in the map for which to update the count.
     * @param counts    The map containing the counts.
     */
    private static void updateCount(Supplier<Boolean> condition, String key, Map<String, Integer> counts) {
        if (condition.get()) {
            counts.put(key, counts.get(key) + 1);
        }
    }

    /**
     * Calculates the technician rating based on the percentage of total technicians
     * to total requirements.
     *
     * @param percentage The percentage of total technicians to total requirements.
     * @return The technician rating based on the given percentage:
     *         - If the percentage is less than 100, returns -5.
     *         - If the percentage is between 100 and 150 (inclusive), returns 0.
     *         - If the percentage is between 151 and 175 (inclusive), returns 5.
     *         - If the percentage is between 176 and 200 (inclusive), returns 10.
     *         - If the percentage is greater than 200, returns 15.
     */
    private static int calculateTechRating(int percentage) {
        if (percentage < 100) {
            return -5;
        } else if (percentage <= 150) {
            return 0;
        } else if (percentage <= 175) {
            return 5;
        } else if (percentage <= 200) {
            return 10;
        }

        return 15;
    }
}
