package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mekhq.campaign.rating.CamOpsReputation.AverageExperienceRating.getReputationModifier;
import static mekhq.campaign.rating.CamOpsReputation.AverageExperienceRating.getSkillLevel;
import static mekhq.campaign.rating.CamOpsReputation.CombatRecordRating.calculateCombatRecordRating;
import static mekhq.campaign.rating.CamOpsReputation.CommandRating.calculateCommanderRating;
import static mekhq.campaign.rating.CamOpsReputation.CrimeRating.calculateCrimeRating;
import static mekhq.campaign.rating.CamOpsReputation.FinancialRating.calculateFinancialRating;
import static mekhq.campaign.rating.CamOpsReputation.OtherModifiers.calculateOtherModifiers;
import static mekhq.campaign.rating.CamOpsReputation.TransportationRating.calculateTransportationRating;

public class ReputationController {
    // average experience rating
    private SkillLevel averageSkillLevel = SkillLevel.NONE;
    private int averageExperienceRating = 0;

    // command rating
    private Map<String, Integer> commanderMap = new HashMap<>();
    private int commanderRating = 0;

    // combat record rating
    private Map<String, Integer> combatRecordMap = new HashMap<>();
    private int combatRecordRating = 0;

    // transportation rating
    private Map<String, Integer> transportationCapacities =  new HashMap<>();
    private Map<String, Integer> transportationRequirements =  new HashMap<>();
    private int transportationRating = 0;

    // financial rating
    private Map<String, Integer> financialRatingMap =  new HashMap<>();
    private int financialRating = 0;

    // crime rating
    private LocalDate dateOfLastCrime = null;
    private int crimeRating = 0;

    // other modifiers
    private Map<String, Integer> otherModifiersMap =  new HashMap<>();
    private int otherModifiers = 0;

    // total
    private int reputationRating = 0;

    //region Getters and Setters
    @SuppressWarnings(value = "unused")
    public SkillLevel getAverageSkillLevel() {
        return this.averageSkillLevel;
    }
    @SuppressWarnings(value = "unused")
    public void setAverageSkillLevel(SkillLevel averageSkillLevel) {
        this.averageSkillLevel = averageSkillLevel;
    }

    @SuppressWarnings(value = "unused")
    public int getAverageExperienceRating() {
        return this.averageExperienceRating;
    }
    @SuppressWarnings(value = "unused")
    public void setAverageExperienceRating(int rating) {
        this.averageExperienceRating = rating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getCommanderMap() {
        return this.commanderMap;
    }

    @SuppressWarnings(value = "unused")
    public void setCommanderMap(Map<String, Integer> commanderMap) {
        this.commanderMap = commanderMap;
    }

    @SuppressWarnings(value = "unused")
    public int getCommanderRating() {
        return this.commanderRating;
    }

    @SuppressWarnings(value = "unused")
    public void setCommanderRating(int commanderRating) {
        this.commanderRating = commanderRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getCombatRecordMap() {
        return this.combatRecordMap;
    }

    @SuppressWarnings(value = "unused")
    public void setCombatRecordMap(Map<String, Integer> combatRecordMap) {
        this.combatRecordMap = combatRecordMap;
    }

    @SuppressWarnings(value = "unused")
    public int getCombatRecordRating() {
        return this.combatRecordRating;
    }

    @SuppressWarnings(value = "unused")
    public void setCombatRecordRating(int combatRecordRating) {
        this.combatRecordRating = combatRecordRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getTransportationCapacities() {
        return this.transportationCapacities;
    }

    @SuppressWarnings(value = "unused")
    public void setTransportationCapacities(Map<String, Integer> transportationCapacities) {
        this.transportationCapacities = transportationCapacities;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getTransportationRequirements() {
        return this.transportationRequirements;
    }

    @SuppressWarnings(value = "unused")
    public void setTransportationRequirements(Map<String, Integer> transportationRequirements) {
        this.transportationRequirements = transportationRequirements;
    }

    @SuppressWarnings(value = "unused")
    public int getTransportationRating() {
        return this.transportationRating;
    }

    @SuppressWarnings(value = "unused")
    public void setTransportationRating(int transportationRating) {
        this.transportationRating = transportationRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getFinancialRatingMap() {
        return this.financialRatingMap;
    }

    @SuppressWarnings(value = "unused")
    public void setFinancialRatingMap(Map<String, Integer> financialRatingMap) {
        this.financialRatingMap = financialRatingMap;
    }

    @SuppressWarnings(value = "unused")
    public int getFinancialRating() {
        return this.financialRating;
    }

    @SuppressWarnings(value = "unused")
    public void setFinancialRating(int financialRating) {
        this.financialRating = financialRating;
    }

    @SuppressWarnings(value = "unused")
    public LocalDate getDateOfLastCrime() {
        return this.dateOfLastCrime;
    }

    @SuppressWarnings(value = "unused")
    public void setDateOfLastCrime(LocalDate dateOfLastCrime) {
        this.dateOfLastCrime = dateOfLastCrime;
    }

    @SuppressWarnings(value = "unused")
    public int getCrimeRating() {
        return this.crimeRating;
    }

    @SuppressWarnings(value = "unused")
    public void setCrimeRating(int crimeRating) {
        this.crimeRating = crimeRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getOtherModifiersMap() {
        return this.otherModifiersMap;
    }

    @SuppressWarnings(value = "unused")
    public void setOtherModifiersMap(Map<String, Integer> otherModifiersMap) {
        this.otherModifiersMap = otherModifiersMap;
    }

    @SuppressWarnings(value = "unused")
    public int getOtherModifiers() {
        return this.otherModifiers;
    }

    @SuppressWarnings(value = "unused")
    public void setOtherModifiers(int otherModifiers) {
        this.otherModifiers = otherModifiers;
    }

    @SuppressWarnings(value = "unused")
    public int getReputationRating() {
        return this.reputationRating;
    }

    @SuppressWarnings(value = "unused")
    public void setReputationRating(int reputationRating) {
        this.reputationRating = reputationRating;
    }
    //endregion Getters and Setters


    public void initializeReputation(Campaign campaign) {
        // step one: calculate average experience rating
        averageSkillLevel = getSkillLevel(campaign);
        averageExperienceRating = getReputationModifier(averageSkillLevel);

        // step two: calculate command rating
        // TODO add a campaign option to disable personality rating
        commanderMap = calculateCommanderRating(campaign, campaign.getFlaggedCommander());
        commanderRating = commanderMap.get("total");

        // step three: calculate combat record rating
        combatRecordMap = calculateCombatRecordRating(campaign);
        combatRecordRating = combatRecordMap.get("total");

        // step four: calculate transportation rating
        List<Map<String, Integer>> rawTransportationData = calculateTransportationRating(campaign);
        transportationCapacities = rawTransportationData.get(0);
        transportationRequirements = rawTransportationData.get(1);

        transportationRating = transportationCapacities.get("total");
        transportationCapacities.remove("total");

        // step five: calculate financial rating
        financialRatingMap = calculateFinancialRating(campaign.getFinances());
        financialRating = financialRatingMap.get("total");

        // step six: calculate crime rating
        dateOfLastCrime = campaign.getDateOfLastCrime();
        crimeRating = calculateCrimeRating(campaign);

        // step seven: calculate other modifiers
        otherModifiersMap = calculateOtherModifiers(campaign);
        otherModifiers = otherModifiersMap.get("total");

        // step eight: total everything
        calculateTotalReputation();
    }

    /**
     * Calculates the total reputation by adding up various ratings and modifiers.
     * This method updates the reputationRating variable.
     */
    private void calculateTotalReputation() {
        reputationRating += averageExperienceRating;
        reputationRating += commanderRating;
        reputationRating += combatRecordRating;
        reputationRating += transportationRating;
        reputationRating += financialRating;
        reputationRating += crimeRating;
        reputationRating += otherModifiers;
    }
}
