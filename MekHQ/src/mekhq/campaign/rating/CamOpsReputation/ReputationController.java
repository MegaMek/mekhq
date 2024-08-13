package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

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
    // utilities
    private static final MMLogger logger = MMLogger.create(ReputationController.class);

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.CamOpsReputation",
            MekHQ.getMHQOptions().getLocale());

    // average experience rating
    private static SkillLevel averageSkillLevel = SkillLevel.NONE;
    private static int averageExperienceRating = 0;
    // while this can be converted to a local variable,
    // I think it's useful to have all the associated variables in one place
    private static int atbModifier = 0;

    // command rating
    private static Map<String, Integer> commanderMap = new HashMap<>();
    private static int commanderRating = 0;

    // combat record rating
    private static Map<String, Integer> combatRecordMap = new HashMap<>();
    private static int combatRecordRating = 0;

    // transportation rating
    private static Map<String, Integer> transportationCapacities =  new HashMap<>();
    private static Map<String, Integer> transportationRequirements =  new HashMap<>();
    private static Map<String, Integer> transportationValues =  new HashMap<>();
    private static int transportationRating = 0;

    // support rating
    private static Map<String, Integer> administrationRequirements =  new HashMap<>();
    private static Map<String, Integer> crewRequirements =  new HashMap<>();
    private static Map<String, List<Integer>> technicianRequirements =  new HashMap<>();
    private static int supportRating = 0;

    // financial rating
    private static Map<String, Integer> financialRatingMap =  new HashMap<>();
    private static int financialRating = 0;

    // crime rating
    private static LocalDate dateOfLastCrime = null;
    private static Map<String, Integer> crimeRatingMap =  new HashMap<>();
    private static int crimeRating = 0;

    // other modifiers
    private static Map<String, Integer> otherModifiersMap =  new HashMap<>();
    private static int otherModifiers = 0;

    // total
    private static int reputationRating = 0;

    /**
     * Performs and stores all reputation calculations.
     *
     * @param campaign the campaign for which to initialize the reputation
     * @return a ReputationRecord object containing all reputation calculations
     */
    @SuppressWarnings(value = "unchecked")
    public static ReputationRecord initializeReputation(Campaign campaign) {
        // step one: calculate average experience rating
        averageSkillLevel = getSkillLevel(campaign, true);
        averageExperienceRating = getReputationModifier(averageSkillLevel);
        atbModifier = getAtBModifier(campaign);

        // step two: calculate command rating
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
        crimeRatingMap = calculateCrimeRating(campaign);
        crimeRating = crimeRatingMap.get("total");
        dateOfLastCrime = campaign.getDateOfLastCrime();

        // step eight: calculate other modifiers
        otherModifiersMap = calculateOtherModifiers(campaign);
        otherModifiers = otherModifiersMap.get("total");

        // step nine: total everything
        calculateTotalReputation();
        logger.debug("TOTAL REPUTATION = {}", reputationRating);

        return new ReputationRecord (
                averageSkillLevel,
                averageExperienceRating,
                atbModifier,
                commanderMap,
                commanderMap.get("total"),
                combatRecordMap,
                combatRecordRating,
                transportationCapacities,
                transportationRequirements,
                transportationValues,
                transportationRating,
                administrationRequirements,
                crewRequirements,
                technicianRequirements,
                supportRating,
                financialRatingMap,
                financialRating,
                crimeRatingMap,
                dateOfLastCrime,
                crimeRating,
                otherModifiersMap,
                otherModifiers,
                reputationRating);
    }

    /**
     * Calculates the total reputation by adding up various ratings and modifiers.
     * This method updates the reputationRating variable.
     */
    private static void calculateTotalReputation() {
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
    public static String getReportText(Campaign campaign) {
        StringBuilder description = new StringBuilder();

        description.append("<html><div style='font-size:11px;'>");

        description.append(String.format(resources.getString("unitReputation.text"), reputationRating));

        // AVERAGE EXPERIENCE RATING
        description.append(String.format(resources.getString("averageExperienceRating.text"), averageExperienceRating));
        description.append(String.format(resources.getString("experienceLevel.text"), averageSkillLevel.toString()));

        // COMMAND RATING
        description.append(String.format(resources.getString("commandRating.text"), commanderRating));
        description.append(String.format(resources.getString("leadership.text"), commanderMap.get("leadership")));
        description.append(String.format(resources.getString("tactics.text"), commanderMap.get("tactics")));
        description.append(String.format(resources.getString("strategy.text"), commanderMap.get("strategy")));
        description.append(String.format(resources.getString("negotiation.text"), commanderMap.get("negotiation")));
        description.append(String.format(resources.getString("traits.text"), commanderMap.get("traits")));

        if (campaign.getCampaignOptions().isUseRandomPersonalities() && (campaign.getCampaignOptions().isUseRandomPersonalityReputation())) {
            description.append(String.format(resources.getString("personality.text"), commanderMap.get("personality"))).append("<br><br>");
        } else {
            description.append("<br><br>");
        }

        // COMBAT RECORD RATING
        description.append(String.format(resources.getString("combatRecordRating.text"), combatRecordRating));

        description.append(getMissionString("successes", resources.getString("successes.text"), 5));
        description.append(getMissionString("partialSuccesses", resources.getString("partialSuccesses.text"), 0));
        description.append(getMissionString("failures", resources.getString("failures.text"), -10));
        description.append(getMissionString("contractsBreached", resources.getString("contractsBreached.text"), -25));

        if (campaign.getRetainerStartDate() != null) {
            description.append(getMissionString("retainerDuration", resources.getString("retainerDuration.text"), 5)).append("<br><br>");
        } else {
            description.append("<br><br>");
        }

        // TRANSPORTATION RATING
        description.append(String.format(resources.getString("transportationRating.text"), transportationRating));

        if (transportationCapacities.get("hasJumpShipOrWarShip") == 1) {
            description.append(resources.getString("hasJumpShipOrWarShip.text"));
        }

        description.append(getDropShipString());
        description.append(getTransportString("smallCraftCount", "smallCraftBays", "smallCraft", resources.getString("smallCraft.text"), true));
        description.append(getTransportString("asfCount", "asfBays", "asf", resources.getString("fighters.text"), false));
        description.append(getTransportString("mechCount", "mechBays", "mech", resources.getString("battleMechs.text"), false));
        description.append(getTransportString("superHeavyVehicleCount", "superHeavyVehicleBays", "superHeavyVehicle", resources.getString("vehicleSuperHeavy.text"), true));
        description.append(getTransportString("heavyVehicleCount", "heavyVehicleBays", "heavyVehicle", resources.getString("vehicleHeavy.text"), true));
        description.append(getTransportString("lightVehicleCount", "lightVehicleBays", "lightVehicle", resources.getString("vehicleLight.text"), false));
        description.append(getTransportString("protoMechCount", "protoMechBays", "protoMech", resources.getString("protoMechs.text"), false));
        description.append(getTransportString("battleArmorCount", "battleArmorBays", "battleArmor", resources.getString("battleArmor.text"), false));
        description.append(getTransportString("infantryCount", "infantryBays", "infantry", resources.getString("infantry.text"), false));
        description.append(resources.getString("asterisk.text"));

        // SUPPORT RATING
        description.append(String.format(resources.getString("supportRating.text"), supportRating));

        if (crewRequirements.get("crewRequirements") < 0) {
            description.append(resources.getString("crewRequirements.text"));
        }

        description.append(String.format(resources.getString("administrationRequirements.text"),
                administrationRequirements.get("personnelCount"),
                administrationRequirements.get("administratorCount"),
                administrationRequirements.get("total")));

        description.append(String.format(resources.getString("technicianRequirements.text"), technicianRequirements.get("rating").get(0)));

        description.append(getTechnicianString("mech"));
        description.append(getTechnicianString("vehicle"));
        description.append(getTechnicianString("aero"));
        description.append(getTechnicianString("battleArmor"));

        description.append("<br>");

        // FINANCIAL RATING
        description.append(String.format(resources.getString("financialRating.text"), financialRating));

        if ((financialRatingMap.get("hasLoan") + financialRatingMap.get("inDebt")) > 0) {
            description.append(resources.getString("hasLoanOrDebt.text"));
        } else {
            description.append("<br>");
        }

        // CRIME RATING
        description.append(String.format(resources.getString("crimeRating.text"), crimeRating));

        if (crimeRating < 0) {
            int piracy = crimeRatingMap.get("piracy");
            if (piracy < 0) {
                description.append(String.format(resources.getString("piracy.text"), piracy));
            }

            int otherCrimes = crimeRatingMap.get("other");
            if (otherCrimes < 0) {
                description.append(String.format(resources.getString("otherCrimes.text"), otherCrimes));
            }

            description.append(String.format(resources.getString("dateOfLastCrime.text"), dateOfLastCrime));
        } else {
            description.append("<br>");
        }

        // OTHER MODIFIERS
        description.append(String.format(resources.getString("otherModifiers.text"), otherModifiers));

        int inactiveYears = otherModifiersMap.get("inactiveYears");

        if (inactiveYears > 0) {
            description.append(String.format(resources.getString("inactiveYears.text"), -inactiveYears * 5));
        }

        int customModifier = otherModifiersMap.get("customModifier");

        if (customModifier != 0) {
            String modifier = String.format("(%+d)", customModifier);
            description.append(String.format(resources.getString("customModifier.text"), modifier));
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
    private static String getTechnicianString(String type) {
        List<Integer> technicianRequirement = technicianRequirements.get(type);

        if ((technicianRequirement.get(0) > 0) || (technicianRequirement.get(1) > 0)) {
            String label = switch (type) {
                case "mech" -> resources.getString("battleMechsAndProtoMechs.text");
                case "vehicle" -> resources.getString("vehicles.text");
                case "aero" -> resources.getString("fightersAndSmallCraft.text");
                case "battleArmor" -> resources.getString("battleArmor.text");
                default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/rating/CamOpsReputation/ReputationController.java/getTechnicianString: "
                        + type);
            };

            return String.format(resources.getString("technicianString.text"),
                    label,
                    technicianRequirement.get(0),
                    technicianRequirement.get(1));
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
     * "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label: </b>count (count * multiplier)<br>", or null if the count is <= 0
     */
    private static String getMissionString(String key, String label, int multiplier) {
        int count = combatRecordMap.get(key);
        int total = count * multiplier;

        if (count > 0) {
            return String.format(resources.getString("mission.text"),
                    label,
                    count,
                    String.format(total > 0 ? "+%d" : "%d", total));
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
    private static String getDropShipString() {
        int unitCount = transportationRequirements.get("dropShipCount");
        int bayCapacity = transportationCapacities.get("dockingCollars");
        String modifier = "0";

        if (unitCount == 0) {
            modifier = resources.getString("noDropShip.text");
        } else if (bayCapacity >= unitCount) {
            modifier = "+5";
        }

        return String.format(resources.getString("dropShipString.text"),
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
    private static String getTransportString(String unitKey, String bayKey, String valueKey, String label, boolean displayAsterisk) {
        int unitCount = transportationRequirements.get(unitKey);
        int bayCount = transportationCapacities.get(bayKey);
        int rating = transportationValues.get(valueKey);

        String asterisk = displayAsterisk ? "*" : "";
        String modifier = "";

        if (unitCount > 0 || bayCount > 0) {
            if (rating > 0) {
                modifier = String.format("(+%d)", rating);
            } else if (rating < 0) {
                modifier = String.format("(%d)", rating);
            }

            return String.format(resources.getString("transportString.text"),
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
