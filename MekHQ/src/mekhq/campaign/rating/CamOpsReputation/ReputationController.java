package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mekhq.campaign.rating.CamOpsReputation.AverageExperienceRating.getAtBModifier;
import static mekhq.campaign.rating.CamOpsReputation.AverageExperienceRating.getReputationModifier;
import static mekhq.campaign.rating.CamOpsReputation.AverageExperienceRating.getSkillLevel;
import static mekhq.campaign.rating.CamOpsReputation.CombatRecordRating.calculateCombatRecordRating;
import static mekhq.campaign.rating.CamOpsReputation.CommandRating.calculateCommanderRating;
import static mekhq.campaign.rating.CamOpsReputation.CrimeRating.calculateCrimeRating;
import static mekhq.campaign.rating.CamOpsReputation.FinancialRating.calculateFinancialRating;
import static mekhq.campaign.rating.CamOpsReputation.OtherModifiers.calculateOtherModifiers;
import static mekhq.campaign.rating.CamOpsReputation.SupportRating.calculateSupportRating;
import static mekhq.campaign.rating.CamOpsReputation.TransportationRating.calculateTransportationRating;

public class ReputationController {
    private static final MMLogger logger = MMLogger.create(ReputationController.class);

    // average experience rating
    private SkillLevel averageSkillLevel = SkillLevel.NONE;
    private int averageExperienceRating = 0;
    private int atbModifier = 0;

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

    // support rating
    private Map<String, Integer> administrationRequirements =  new HashMap<>();
    private Map<String, Integer> crewRequirements =  new HashMap<>();
    private Map<String, List<Integer>> technicianRequirements =  new HashMap<>();
    private int supportRating = 0;

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

    public ReputationController(Campaign campaign) {
        initializeReputation(campaign);
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
    public void setAverageExperienceRating(final int rating) {
        this.averageExperienceRating = rating;
    }

    @SuppressWarnings(value = "unused")
    public int getAtbModifier() {
        return this.atbModifier;
    }
    @SuppressWarnings(value = "unused")
    public void setAtbModifier(final int atbModifier) {
        this.atbModifier = atbModifier;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getCommanderMap() {
        return this.commanderMap;
    }

    @SuppressWarnings(value = "unused")
    public void setCommanderMap(final Map<String, Integer> commanderMap) {
        this.commanderMap = commanderMap;
    }

    @SuppressWarnings(value = "unused")
    public int getCommanderRating() {
        return this.commanderRating;
    }

    @SuppressWarnings(value = "unused")
    public void setCommanderRating(final int commanderRating) {
        this.commanderRating = commanderRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getCombatRecordMap() {
        return this.combatRecordMap;
    }

    @SuppressWarnings(value = "unused")
    public void setCombatRecordMap(final Map<String, Integer> combatRecordMap) {
        this.combatRecordMap = combatRecordMap;
    }

    @SuppressWarnings(value = "unused")
    public int getCombatRecordRating() {
        return this.combatRecordRating;
    }

    @SuppressWarnings(value = "unused")
    public void setCombatRecordRating(final int combatRecordRating) {
        this.combatRecordRating = combatRecordRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getTransportationCapacities() {
        return this.transportationCapacities;
    }

    @SuppressWarnings(value = "unused")
    public void setTransportationCapacities(final Map<String, Integer> transportationCapacities) {
        this.transportationCapacities = transportationCapacities;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getTransportationRequirements() {
        return this.transportationRequirements;
    }

    @SuppressWarnings(value = "unused")
    public void setTransportationRequirements(final Map<String, Integer> transportationRequirements) {
        this.transportationRequirements = transportationRequirements;
    }

    @SuppressWarnings(value = "unused")
    public int getTransportationRating() {
        return this.transportationRating;
    }

    @SuppressWarnings(value = "unused")
    public void setTransportationRating(final int transportationRating) {
        this.transportationRating = transportationRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getAdministrationRequirements() {
        return this.administrationRequirements;
    }

    @SuppressWarnings(value = "unused")
    public void setAdministrationRequirements(final Map<String, Integer> administrationRequirements) {
        this.administrationRequirements = administrationRequirements;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getCrewRequirements() {
        return this.crewRequirements;
    }

    @SuppressWarnings(value = "unused")
    public void setCrewRequirements(final Map<String, Integer> crewRequirements) {
        this.crewRequirements = crewRequirements;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, List<Integer>> technicianRequirements() {
        return this.technicianRequirements;
    }

    @SuppressWarnings(value = "unused")
    public void technicianRequirements(final Map<String, List<Integer>> technicianRequirements) {
        this.technicianRequirements = technicianRequirements;
    }

    @SuppressWarnings(value = "unused")
    public int supportRating() {
        return this.supportRating;
    }

    @SuppressWarnings(value = "unused")
    public void supportRating(final int supportRating) {
        this.supportRating = supportRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getFinancialRatingMap() {
        return this.financialRatingMap;
    }

    @SuppressWarnings(value = "unused")
    public void setFinancialRatingMap(final Map<String, Integer> financialRatingMap) {
        this.financialRatingMap = financialRatingMap;
    }

    @SuppressWarnings(value = "unused")
    public int getFinancialRating() {
        return this.financialRating;
    }

    @SuppressWarnings(value = "unused")
    public void setFinancialRating(final int financialRating) {
        this.financialRating = financialRating;
    }

    @SuppressWarnings(value = "unused")
    public LocalDate getDateOfLastCrime() {
        return this.dateOfLastCrime;
    }

    @SuppressWarnings(value = "unused")
    public void setDateOfLastCrime(final LocalDate dateOfLastCrime) {
        this.dateOfLastCrime = dateOfLastCrime;
    }

    @SuppressWarnings(value = "unused")
    public int getCrimeRating() {
        return this.crimeRating;
    }

    @SuppressWarnings(value = "unused")
    public void setCrimeRating(final int crimeRating) {
        this.crimeRating = crimeRating;
    }

    @SuppressWarnings(value = "unused")
    public Map<String, Integer> getOtherModifiersMap() {
        return this.otherModifiersMap;
    }

    @SuppressWarnings(value = "unused")
    public void setOtherModifiersMap(final Map<String, Integer> otherModifiersMap) {
        this.otherModifiersMap = otherModifiersMap;
    }

    @SuppressWarnings(value = "unused")
    public int getOtherModifiers() {
        return this.otherModifiers;
    }

    @SuppressWarnings(value = "unused")
    public void setOtherModifiers(final int otherModifiers) {
        this.otherModifiers = otherModifiers;
    }

    @SuppressWarnings(value = "unused")
    public int getReputationRating() {
        return this.reputationRating;
    }

    @SuppressWarnings(value = "unused")
    public void setReputationRating(final int reputationRating) {
        this.reputationRating = reputationRating;
    }
    //endregion Getters and Setters

    /**
     * Performs and stores all reputation calculations.
     *
     * @param campaign the campaign for which to initialize the reputation
     */
    @SuppressWarnings(value = "unchecked")
    public void initializeReputation(Campaign campaign) {
        // step one: calculate average experience rating
        averageSkillLevel = getSkillLevel(campaign, true);
        averageExperienceRating = getReputationModifier(averageSkillLevel);
        atbModifier = getAtBModifier(campaign);

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

        // step five: support rating
        Map<String, Map<String, ?>> rawSupportData = calculateSupportRating(campaign, transportationRequirements);

        administrationRequirements = (Map<String, Integer>) rawSupportData.get("administrationRequirements");
        crewRequirements = (Map<String, Integer>) rawSupportData.get("crewRequirements");
        technicianRequirements = (Map<String, List<Integer>>) rawSupportData.get("technicianRequirements");

        supportRating = (int) rawSupportData.get("total").get("total");

        // step six: calculate financial rating
        financialRatingMap = calculateFinancialRating(campaign.getFinances());
        financialRating = financialRatingMap.get("total");

        // step seven: calculate crime rating
        dateOfLastCrime = campaign.getDateOfLastCrime();
        crimeRating = calculateCrimeRating(campaign);

        // step eight: calculate other modifiers
        otherModifiersMap = calculateOtherModifiers(campaign);
        otherModifiers = otherModifiersMap.get("total");

        // step nine: total everything
        calculateTotalReputation();
        logger.info("TOTAL REPUTATION = {}", reputationRating);
    }

    /**
     * Calculates the total reputation by adding up various ratings and modifiers.
     * This method updates the reputationRating variable.
     */
    private void calculateTotalReputation() {
        reputationRating = averageExperienceRating;
        reputationRating += commanderRating;
        reputationRating += combatRecordRating;
        reputationRating += transportationRating;
        reputationRating += supportRating;
        reputationRating += financialRating;
        reputationRating += crimeRating;
        reputationRating += otherModifiers;
    }


    /**
     * Retrieves the report text for the given campaign.
     *
     * @param campaign the campaign for which to generate the report
     * @return the report text as a string
     */
    public String getReportText(Campaign campaign) {
        StringBuilder description = new StringBuilder();

        description.append("<html><div style='font-size:11px;'>");

        description.append("<html><font size='7'><b>Unit Reputation: ").append(reputationRating).append("</b></font><br><br>");

        // AVERAGE EXPERIENCE RATING
        description.append(String.format("<b><font size='6'>Average Experience Rating: %s</font></b>", averageExperienceRating));
        description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Experience Level: </b>%s<br><br>", averageSkillLevel.toString()));

        // COMMAND RATING
        description.append(String.format("<b><font size='6'>Command Rating: %s</font></b>", commanderRating));
        description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Leadership: </b>%s", commanderMap.get("leadership")));
        description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Tactics: </b>%s", commanderMap.get("tactics")));
        description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Strategy: </b>%s", commanderMap.get("strategy")));
        description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Negotiation: </b>%s", commanderMap.get("negotiation")));
        description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Traits: </b>%s <i>Not Implemented</i>", commanderMap.get("traits")));

        // TODO: this will also need to confirm that the option to enable personality modifiers is enabled
        if (campaign.getCampaignOptions().isUseRandomPersonalities()) {
            description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Personality: </b>%s", commanderMap.get("personality"))).append("<br><br>");
        } else {
            description.append("<br><br>");
        }

        // COMBAT RECORD RATING
        description.append(String.format("<b><font size='6'>Combat Record Rating: %s</font></b>", combatRecordRating));

        description.append(getMissionString("successes", "Successes", 5));
        description.append(getMissionString("partialSuccesses", "Partial Successes", 0));
        description.append(getMissionString("failures", "Failures", -10));
        description.append(getMissionString("contractsBreached", "Contracts Breached", -25));

        if (campaign.getRetainerStartDate() != null) {
            description.append(getMissionString("retainerDuration", "Retainer Duration", 5)).append("<br><br>");
        } else {
            description.append("<br><br>");
        }

        // TRANSPORTATION RATING
        description.append("<b><font size='6'>Transportation Rating: ").append(transportationRating).append("</font></b>");

        if (transportationCapacities.get("hasJumpShipOrWarShip") == 1) {
            description.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Has JumpShip or WarShip: </b>+10");
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

        // SUPPORT RATING
        description.append(String.format("<b><font size='6'>Support Rating: %d </font></b>", supportRating));

        if (crewRequirements.get("crewRequirements") < 0) {
            description.append("Partially Crewed Large Craft: -5<br>");
        }

        description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Administration Requirements: </b>%d / %d (%d)<br>",
                administrationRequirements.get("personnelCount"),
                administrationRequirements.get("administratorCount"),
                administrationRequirements.get("total")));

        description.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Technician Requirements</b>");

        description.append(appendTechnicianRequirement("mech"));
        description.append(appendTechnicianRequirement("vehicle"));
        description.append(appendTechnicianRequirement("aero"));
        description.append(appendTechnicianRequirement("battleArmor"));

        description.append("<br><br>");

        // FINANCIAL RATING
        description.append(String.format("<b><font size='6'>Financial Rating: %s</font></b>", transportationRating));

        if ((financialRatingMap.get("hasLoan") + financialRatingMap.get("inDebt")) > 0) {
            description.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Has Loan or Debt: -10</b><br><br>");
        } else {
            description.append("<br><br>");
        }

        // CRIME RATING
        description.append(String.format("<b><font size='6'>Crime Rating: %s</font></b>", crimeRating));

        if (crimeRating < 0) {
            description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Date of Last Crime: </b>%s<br><br>", dateOfLastCrime));
        } else {
            description.append("<br><br>");
        }

        // OTHER MODIFIERS
        description.append(String.format("<b><font size='6'>Other Modifiers: %s</font></b>", otherModifiers));

        int inactiveYears = otherModifiersMap.get("inactiveYears");

        if (inactiveYears > 0) {
            description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Inactivity: </b>%d", -inactiveYears * 5));
        }

        int customModifier = otherModifiersMap.get("customModifier");

        if (customModifier != 0) {
            String modifier = String.format("(%+d)", customModifier);
            description.append(String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Custom Modifier: </b>%s", modifier));
        }

        description.append("</div></html>");

        return description.toString();
    }

    /**
     * Appends the technician requirement information for the given type.
     * If the technician requirement exceeds 0, it generates an HTML formatted string
     * with the technician label and the current count and maximum count of technicians.
     *
     * @param type the type of technician requirement (mech, vehicle, aero, battleArmor)
     * @return the generated technician requirement string in HTML format,
     *         or an empty string if either technicianRequirement value is 0.
     */
    private String appendTechnicianRequirement(String type) {
        List<Integer> technicianRequirement = technicianRequirements.get(type);

        if ((technicianRequirement.get(0) > 0) || (technicianRequirement.get(1) > 0)) {
            String label = switch (type) {
                case "mech" -> "BattleMechs & ProtoMechs";
                case "vehicle" -> "Vehicles";
                case "aero" -> "Fighters & Small Craft";
                case "battleArmor" -> "Battle Armor";
                default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/rating/CamOpsReputation/ReputationController.java/appendTechnicianRequirement: "
                        + type);
            };

            return String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s: </b>%d / %d<br>",
                    label,
                    technicianRequirement.get(1),
                    technicianRequirement.get(0));
        }

        return "";
    }

    /**
     * Generates the mission string with the given key, label, and multiplier.
     *
     * @param key        the key for accessing the combat record count
     * @param label      the label to be displayed in the mission string
     * @param multiplier the multiplier to apply to the count
     * @return the generated mission string, formatted as
     * "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label: </b>count (+multiplier)<br>", or null if the count is <= 0
     */
    private String getMissionString(String key, String label, int multiplier) {
        int count = combatRecordMap.get(key);

        if (count > 0) {
            return String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s: </b>%d (+%d)",
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
     * "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;DropShips: </b>unitCount / bayCapacity Docking Collars (modifier)<br>"
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

        return String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;DropShips: </b>%d / %d Docking Collars (%s)<br>",
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
     * "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label: </b>unitCount / bayCount Bays* modifier<br>", or an empty string if unitCount and bayCount
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

            return String.format("<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s: </b>%d / %d Bays%s %s",
                    label,
                    unitCount,
                    bayCount,
                    asterisk,
                    modifier);
        } else {
            return "";
        }
    }
}
