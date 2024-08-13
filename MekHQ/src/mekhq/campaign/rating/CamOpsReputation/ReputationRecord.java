package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.enums.SkillLevel;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * This class stores various reputation calculations and ratings.
 */
public record ReputationRecord(
        SkillLevel averageSkillLevel,
        int averageExperienceRating,
        int atbModifier,
        Map<String, Integer> commanderMap,
        int commanderRating,
        Map<String, Integer> combatRecordMap,
        int combatRecordRating,
        Map<String, Integer> transportationCapacities,
        Map<String, Integer> transportationRequirements,
        Map<String, Integer> transportationValues,
        int transportationRating,
        Map<String, Integer> administrationRequirements,
        Map<String, Integer> crewRequirements,
        Map<String, List<Integer>> technicianRequirements,
        int supportRating,
        Map<String, Integer> financialRatingMap,
        int financialRating,
        Map<String, Integer> crimeRatingMap,
        LocalDate dateOfLastCrime,
        int crimeRating,
        Map<String, Integer> otherModifiersMap,
        int otherModifiers,
        int reputationRating
) {}
