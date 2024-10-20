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

import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static mekhq.campaign.rating.CamOpsReputation.AverageExperienceRating.getAtBModifier;
import static mekhq.campaign.rating.CamOpsReputation.AverageExperienceRating.getAverageExperienceModifier;
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

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.CamOpsReputation",
            MekHQ.getMHQOptions().getLocale());

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
    private Map<String, Integer> transportationCapacities = new HashMap<>();
    private Map<String, Integer> transportationRequirements = new HashMap<>();
    private Map<String, Integer> transportationValues = new HashMap<>();
    private int transportationRating = 0;

    // support rating
    private Map<String, Integer> administrationRequirements = new HashMap<>();
    private Map<String, Integer> crewRequirements = new HashMap<>();
    private Map<String, List<Integer>> technicianRequirements = new HashMap<>();
    private int supportRating = 0;

    // financial rating
    private Map<String, Integer> financialRatingMap = new HashMap<>();
    private int financialRating = 0;

    // crime rating
    private LocalDate dateOfLastCrime = null;
    private Map<String, Integer> crimeRatingMap = new HashMap<>();
    private int crimeRating = 0;

    // other modifiers
    private Map<String, Integer> otherModifiersMap = new HashMap<>();
    private int otherModifiers = 0;

    // total
    private int reputationRating = 0;

    // region Getters and Setters
    public SkillLevel getAverageSkillLevel() {
        return this.averageSkillLevel;
    }

    public int getAtbModifier() {
        return this.atbModifier;
    }

    public int getReputationRating() {
        return this.reputationRating;
    }

    public int getReputationFactor() {
        return (int) (getReputationModifier() * 0.2 + 0.5);
    }
    // endregion Getters and Setters

    /**
     * Initializes the ReputationController class with default values.
     */
    public ReputationController() {
    }

    /**
     * Performs and stores all reputation calculations.
     *
     * @param campaign the campaign for which to initialize the reputation
     */
    @SuppressWarnings(value = "unchecked")
    public void initializeReputation(Campaign campaign) {
        // step one: calculate average experience rating
        averageSkillLevel = getSkillLevel(campaign, true);
        averageExperienceRating = getAverageExperienceModifier(averageSkillLevel);
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
     * Retrieves the unit rating modifier as described in Campaign Operations. This value is equal
     * to the total reputation score divided by ten, rounded down to the nearest whole number.
     *
     * @return The unit rating modifier as described in Campaign Operations.
     */
    public int getReputationModifier() {
        return reputationRating / 10;
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

        if (campaign.getCampaignOptions().isUseRandomPersonalities()
                && (campaign.getCampaignOptions().isUseRandomPersonalityReputation())) {
            description.append(String.format(resources.getString("personality.text"), commanderMap.get("personality")))
                    .append("<br><br>");
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
            description.append(getMissionString("retainerDuration", resources.getString("retainerDuration.text"), 5))
                    .append("<br><br>");
        } else {
            description.append("<br><br>");
        }

        // TRANSPORTATION RATING
        description.append(String.format(resources.getString("transportationRating.text"), transportationRating));

        if (transportationCapacities.get("hasJumpShipOrWarShip") == 1) {
            description.append(resources.getString("hasJumpShipOrWarShip.text"));
        }

        description.append(getDropShipString());
        description.append(getTransportString("smallCraftCount", "smallCraftBays", "smallCraft",
                resources.getString("smallCraft.text"), true));
        description
                .append(getTransportString("asfCount", "asfBays", "asf", resources.getString("fighters.text"), false));
        // <50.01 compatibility handler
        try {
            description.append(getTransportString("mekCount", "mekBays", "mek",
                resources.getString("battleMeks.text"), false));
        } catch (Exception e) {
            description.append(getTransportString("mechCount", "mechBays", "mech",
                resources.getString("battleMeks.text"), false));
        }

        description.append(getTransportString("superHeavyVehicleCount", "superHeavyVehicleBays", "superHeavyVehicle",
                resources.getString("vehicleSuperHeavy.text"), true));
        description.append(getTransportString("heavyVehicleCount", "heavyVehicleBays", "heavyVehicle",
                resources.getString("vehicleHeavy.text"), true));
        description.append(getTransportString("lightVehicleCount", "lightVehicleBays", "lightVehicle",
                resources.getString("vehicleLight.text"), false));
        // <50.01 compatibility handler
        try {
            description.append(getTransportString("protoMekCount", "protoMekBays",
                "protoMek", resources.getString("protoMeks.text"), false));
        } catch (Exception e) {
            description.append(getTransportString("protoMechCount", "protoMechBays",
                "protoMech", resources.getString("protoMeks.text"), false));
        }

        description.append(getTransportString("battleArmorCount", "battleArmorBays", "battleArmor",
                resources.getString("battleArmor.text"), false));
        description.append(getTransportString("infantryCount", "infantryBays", "infantry",
                resources.getString("infantry.text"), false));
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

        description.append(String.format(resources.getString("technicianRequirements.text"),
                technicianRequirements.get("rating").get(0)));

        // <50.01 compatibility handler
        try {
            description.append(getTechnicianString("mek"));
        } catch (Exception e) {
            description.append(getTechnicianString("mech"));
        }
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
     * If the technician requirement exceeds 0, it generates an HTML formatted
     * string
     * with the technician label and the current count and maximum count of
     * technicians.
     *
     * @param type the type of technician requirement (mek, vehicle, aero,
     *             battleArmor)
     * @return the generated technician requirement string in HTML format,
     *         or an empty string if either technicianRequirement value is 0.
     */
    private String getTechnicianString(String type) {
        List<Integer> technicianRequirement = technicianRequirements.get(type);

        if ((technicianRequirement.get(0) > 0) || (technicianRequirement.get(1) > 0)) {
            String label = switch (type) {
                // <50.01 compatibility handler
                case "mek", "mech" -> resources.getString("battleMeksAndProtoMeks.text");
                case "vehicle" -> resources.getString("vehicles.text");
                case "aero" -> resources.getString("fightersAndSmallCraft.text");
                case "battleArmor" -> resources.getString("battleArmor.text");
                default -> throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/rating/CamOpsReputation/ReputationController.java/getTechnicianString: "
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
     *         "<br>
     *         &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label: </b>count (count *
     *         multiplier)<br>
     *         ", or null if the count is <= 0
     */
    private String getMissionString(String key, String label, int multiplier) {
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
     *         "<br>
     *         &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;DropShips: </b>unitCount / bayCapacity
     *         Docking Collars (modifier)<br>
     *         "
     */
    private String getDropShipString() {
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
     * Generates a transport string with the given unitKey, bayKey, valueKey, label,
     * and displayAsterisk.
     *
     * @param unitKey         the key to access the unit count in the
     *                        transportationRequirements map
     * @param bayKey          the key to access the bay capacity in the
     *                        transportationCapacities map
     * @param valueKey        the key to access the rating in the
     *                        transportationValues map
     * @param label           the label to be displayed in the transport string
     * @param displayAsterisk whether to display an asterisk in the transport string
     * @return the generated transport string in HTML format, formatted as
     *         "<br>
     *         &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;label: </b>unitCount / bayCount Bays*
     *         modifier<br>
     *         ", or an empty string if unitCount and bayCount
     *         are both 0
     */
    private String getTransportString(String unitKey, String bayKey, String valueKey, String label,
            boolean displayAsterisk) {
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

    /**
     * Writes the reputation ratings and values to an XML file.
     *
     * @param pw     the PrintWriter object used to write to XML
     * @param indent the number of spaces to indent the XML tags
     */
    public void writeReputationToXML(final PrintWriter pw, int indent) {
        // average experience rating
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "averageSkillLevel", averageSkillLevel.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "averageExperienceRating", averageExperienceRating);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "atbModifier", atbModifier);

        // command rating
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "commanderMap");
        writeMapToXML(pw, indent, commanderMap);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "commanderMap");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commanderRating", commanderRating);

        // combat record rating
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "combatRecordMap");
        writeMapToXML(pw, indent, combatRecordMap);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "combatRecordMap");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "combatRecordRating", combatRecordRating);

        // transportation rating
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "transportationCapacities");
        writeMapToXML(pw, indent, transportationCapacities);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "transportationCapacities");

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "transportationRequirements");
        writeMapToXML(pw, indent, transportationRequirements);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "transportationRequirements");

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "transportationValues");
        writeMapToXML(pw, indent, transportationValues);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "transportationValues");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportationRating", transportationRating);

        // support rating
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "administrationRequirements");
        writeMapToXML(pw, indent, administrationRequirements);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "administrationRequirements");

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "crewRequirements");
        writeMapToXML(pw, indent, crewRequirements);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "crewRequirements");

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "technicianRequirements");
        writeMapToXML(pw, indent, technicianRequirements);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "technicianRequirements");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "supportRating", supportRating);

        // financial rating
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "financialRatingMap");
        writeMapToXML(pw, indent, financialRatingMap);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "financialRatingMap");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "supportRating", financialRating);

        // crime rating
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dateOfLastCrime", dateOfLastCrime);

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "crimeRatingMap");
        writeMapToXML(pw, indent, crimeRatingMap);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "crimeRatingMap");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "crimeRating", crimeRating);

        // other modifiers
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "otherModifiersMap");
        writeMapToXML(pw, indent, otherModifiersMap);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "otherModifiersMap");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "otherModifiers", otherModifiers);

        // total
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reputationRating", reputationRating);
    }

    /**
     * Writes a map to XML format.
     *
     * @param pw     the PrintWriter object used to write to XML
     * @param indent the number of spaces to indent the XML tags
     * @param map    the map to write to XML, where the keys are strings, and the
     *               values can be any type
     * @param <T>    the type of the values in the map
     * @return the updated value of the indent parameter after writing the map to
     *         XML
     */
    private <T> void writeMapToXML(final PrintWriter pw, final int indent, final Map<String, T> map) {
        for (String key : map.keySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, key, map.get(key).toString());
        }
    }

    public ReputationController generateInstanceFromXML(final Node workingNode) {
        NodeList newLine = workingNode.getChildNodes();

        try {
            for (int i = 0; i < newLine.getLength(); i++) {
                Node workingNode2 = newLine.item(i);

                if (workingNode2.getNodeName().equalsIgnoreCase("averageSkillLevel")) {
                    this.averageSkillLevel = SkillLevel.valueOf(workingNode2.getTextContent().toUpperCase());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("averageExperienceRating")) {
                    this.averageExperienceRating = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("atbModifier")) {
                    this.atbModifier = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("commanderMap")) {
                    this.parseSubNode(workingNode2, commanderMap, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("commanderRating")) {
                    this.commanderRating = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("combatRecordMap")) {
                    this.parseSubNode(workingNode2, combatRecordMap, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("combatRecordRating")) {
                    this.combatRecordRating = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("transportationCapacities")) {
                    this.parseSubNode(workingNode2, transportationCapacities, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("transportationRequirements")) {
                    this.parseSubNode(workingNode2, transportationRequirements, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("transportationValues")) {
                    this.parseSubNode(workingNode2, transportationValues, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("transportationRating")) {
                    this.transportationRating = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("administrationRequirements")) {
                    this.parseSubNode(workingNode2, administrationRequirements, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("crewRequirements")) {
                    this.parseSubNode(workingNode2, crewRequirements, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("technicianRequirements")) {
                    this.parseSubNode(workingNode2, null, true);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("supportRating")) {
                    this.supportRating = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("financialRatingMap")) {
                    this.parseSubNode(workingNode2, financialRatingMap, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("financialRating")) {
                    this.financialRating = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("dateOfLastCrime")) {
                    this.dateOfLastCrime = LocalDate.parse(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("crimeRatingMap")) {
                    this.parseSubNode(workingNode2, crimeRatingMap, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("crimeRating")) {
                    this.crimeRating = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("otherModifiersMap")) {
                    this.parseSubNode(workingNode2, otherModifiersMap, false);
                } else if (workingNode2.getNodeName().equalsIgnoreCase("otherModifiers")) {
                    this.otherModifiers = Integer.parseInt(workingNode2.getTextContent());
                } else if (workingNode2.getNodeName().equalsIgnoreCase("reputationRating")) {
                    this.reputationRating = Integer.parseInt(workingNode2.getTextContent());
                }
            }
        } catch (Exception ex) {
            logger.error("Could not parse Reputation: ", ex);
        }

        return this;
    }

    /**
     * Parses the sub-nodes of a given node and populates either a map or a
     * technicianRequirements list based on the boolean flag.
     *
     * @param workingNode              The node whose sub-nodes need to be parsed.
     * @param map                      The map to populate with the sub-node data
     *                                 (null if technicianRequirements).
     * @param isTechnicianRequirements Flag indicating whether to populate a
     *                                 technicianRequirements list.
     */
    private void parseSubNode(Node workingNode, @Nullable Map<String, Integer> map, boolean isTechnicianRequirements) {
        NodeList subNodeList = workingNode.getChildNodes();

        for (int i = 0; i < subNodeList.getLength(); i++) {
            Node node = subNodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (isTechnicianRequirements) {
                    try {
                        String[] numbers = node.getTextContent().substring(1, node.getTextContent().length() - 1)
                                .split(",\\s*");
                        List<Integer> list = Arrays.stream(numbers).map(Integer::parseInt).collect(Collectors.toList());
                        technicianRequirements.put(node.getNodeName(), list);
                    } catch (NumberFormatException ex) {
                        logger.error("Could not parse TechnicianRequirements: ", ex);
                    }
                } else {
                    try {
                        map.put(node.getNodeName(), Integer.parseInt(node.getTextContent()));
                    } catch (Exception ex) {
                        logger.error("Could not parse {}: ", map, ex);
                    }
                }
            }
        }
    }
}
