package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
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
    private static final MMLogger logger = MMLogger.create(ReputationController.class);

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
    private Map<String, Integer> transportationValues =  new HashMap<>();
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

    public String getDescription(Campaign campaign) {
        StringBuilder description = new StringBuilder();

        description.append("<font size='6'><b>Unit Reputation: ").append(reputationRating).append("</b></font><br><br>");

        // AVERAGE EXPERIENCE RATING
        description.append(String.format("<b><font size='5'>Average Experience Rating: %s</font></b><br>", averageExperienceRating));
        description.append(String.format("<b>     Experience Level: </b>%s<br><br>", averageSkillLevel.toString()));

        // COMMAND RATING
        description.append(String.format("<b><font size='5'>Command Rating: %s</font></b><br>", commanderRating));
        description.append(String.format("<b>     Leadership: </b>%s<br>", commanderMap.get("leadership")));
        description.append(String.format("<b>     Tactics: </b>%s<br>", commanderMap.get("tactics")));
        description.append(String.format("<b>     Strategy: </b>%s<br>", commanderMap.get("strategy")));
        description.append(String.format("<b>     Negotiation: </b>%s<br>", commanderMap.get("negotiation")));
        description.append(String.format("<b>     Traits: </b>%s <i>Not Implemented</i><br>", commanderMap.get("traits")));

        // TODO: this will also need to confirm that the option to enable personality modifiers is enabled
        if (campaign.getCampaignOptions().isUseRandomPersonalities()) {
            description.append(String.format("<b>     Personality: </b>%s<br>", commanderMap.get("personality"))).append("<br>");
        } else {
            description.append("<br>");
        }

        // COMBAT RECORD RATING
        description.append(String.format("<b><font size='5'>Combat Record Rating: %s</font></b><br>", combatRecordRating));

        description.append(getMissionString("successes", "Successes", 5));
        description.append(getMissionString("partialSuccesses", "Partial Successes", 0));
        description.append(getMissionString("failures", "Failures", -10));
        description.append(getMissionString("contractsBreached", "Contracts Breached", -25));

        if (campaign.getRetainerStartDate() != null) {
            description.append(getMissionString("retainerDuration", "Retainer Duration", 5)).append("<br>");
        } else {
            description.append("<br>");
        }

        // TRANSPORTATION RATING
        description.append("<b><font size='5'>Transportation Rating: ").append(transportationRating).append("</font></b><br>");

        if (transportationCapacities.get("hasJumpShipOrWarShip") == 1) {
            description.append("<b>     Has JumpShip or WarShip: </b>+10<br>");
        }

        description.append(getDropShipString());
        description.append(getTransportString("smallCraftCount", "smallCraftBays", "smallCraft", "Small Craft", true));
        description.append(getTransportString("asfCount", "asfBays", "asf", "Fighters", false));
        description.append(getTransportString("mechCount", "mechBays", "mech", "BattleMechs", false));
        description.append(getTransportString("superHeavyVehicleCount", "superHeavyVehicleBays", "superHeavyVehicle", "Vehicles (Super Heavy)", true));
        description.append(getTransportString("heavyVehicleCount", "heavyVehicleBays", "heavyVehicle", "Vehicles (Heavy)", true));
        description.append(getTransportString("lightVehicleCount", "lightVehicleBays", "lightVehicle", "Vehicles (Light)", false));
        description.append(getTransportString("protoMechCount", "protoMechBays", "protoMech", "ProtoMechs", false));
        description.append(getTransportString("battleArmorCount", "battleArmorBays", "battleArmor", "Battle Armor", false));
        description.append(getTransportString("infantryCount", "infantryBays", "infantry", "Infantry", false));
        description.append("<i>* Lighter units will occupy spare bays</i><br><br>");

        // FINANCIAL RATING
        description.append(String.format("<b><font size='5'>Financial Rating: %s</font></b><br>", transportationRating));

        if ((financialRatingMap.get("hasLoan") + financialRatingMap.get("inDebt")) > 0) {
            description.append("<b>     Has Loan or Debt: -10</b><br><br>");
        } else {
            description.append("<br>");
        }

        // CRIME RATING
        description.append(String.format("<b><font size='5'>Crime Rating: %s</font></b><br>", crimeRating));

        if (crimeRating < 0) {
            description.append(String.format("<b>     Date of Last Crime: </b>%s<br><br>", dateOfLastCrime));
        } else {
            description.append("<br>");
        }

        // OTHER MODIFIERS
        description.append(String.format("<b><font size='5'>Other Modifiers: %s</font></b><br>", otherModifiers));

        int inactiveYears = otherModifiersMap.get("inactiveYears");

        if (inactiveYears > 0) {
            description.append(String.format("<b>     Inactivity: </b>%d<br>", -inactiveYears * 5));
        }

        int customModifier = otherModifiersMap.get("customModifier");

        if (customModifier != 0) {
            String modifier = String.format("(%+d)", customModifier);
            description.append(String.format("<b>     Custom Modifier: </b>%s", modifier));
        }

        return description.toString();
    }

    /**
     * Generates the mission string with the given key, label, and multiplier.
     *
     * @param key        the key for accessing the combat record count
     * @param label      the label to be displayed in the mission string
     * @param multiplier the multiplier to apply to the count
     * @return the generated mission string, formatted as
     * "<b>     label: </b>count (+multiplier)<br>", or null if the count is <= 0
     */
    private String getMissionString(String key, String label, int multiplier) {
        int count = combatRecordMap.get(key);

        if (count > 0) {
            return String.format("<b>     %s: </b>%d (+%d)<br>",
                    label,
                    count,
                    count * multiplier);
        } else {
            return "";
        }
    }


    /**
     * Generates DropShip string information for the unit report
     *
     * @return the generated string in HTML format, formatted as
     * "<b>     DropShips: </b>unitCount / bayCapacity Docking Collars (modifier)<br>"
     */
    private String getDropShipString() {
        int unitCount = transportationRequirements.get("dropShipCount");
        int bayCapacity = transportationCapacities.get("dockingCollars");
        String modifier = "0";

        if (unitCount == 0) {
            modifier = "No DropShip: -5";
        } else if (bayCapacity >= unitCount) {
            modifier = "+5";
        }

        return String.format("<b>     DropShips: </b>%d / %d Docking Collars (%s)<br>",
                unitCount,
                bayCapacity,
                modifier);
    }

    /**
     * Generates a transport string with the given unitKey, bayKey, valueKey, label, and displayAsterisk.
     *
     * @param unitKey         the key to access the unit count in the transportationRequirements map
     * @param bayKey          the key to access the bay capacity in the transportationCapacities map
     * @param valueKey        the key to access the rating in the transportationValues map
     * @param label           the label to be displayed in the transport string
     * @param displayAsterisk whether to display an asterisk in the transport string
     * @return the generated transport string in HTML format, formatted as
     * "<b>     label: </b>unitCount / bayCount Bays* modifier<br>", or an empty string if unitCount and bayCount
     *  are both 0
     */
    private String getTransportString(String unitKey, String bayKey, String valueKey, String label, boolean displayAsterisk) {
        int unitCount = transportationRequirements.get(unitKey);
        int bayCount = transportationCapacities.get(bayKey);
        int rating = transportationValues.get(valueKey);

        String asterisk = displayAsterisk ? "*" : "";
        String modifier = "";

        if (unitCount > 0 || bayCount > 0) {
            if (rating > 0) {
                modifier = String.format("(+%d)", rating);
            } else if (rating < 0) {
                modifier = String.format("(-%d)", rating);
            }

            return String.format("<b>     %s: </b>%d / %d Bays%s %s<br>",
                    label,
                    unitCount,
                    bayCount,
                    asterisk,
                    modifier);
        } else {
            return "";
        }
    }

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

    /**
     * Performs and stores all reputation calculations.
     *
     * @param campaign the campaign for which to initialize the reputation
     */
    public void initializeReputation(Campaign campaign) {
        // step one: calculate average experience rating
        averageSkillLevel = getSkillLevel(campaign, true);
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
        transportationValues = rawTransportationData.get(2);

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
        logger.info("TOTAL REPUTATION = {}", reputationRating);
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
