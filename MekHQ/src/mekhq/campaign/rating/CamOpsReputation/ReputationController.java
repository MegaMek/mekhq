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

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
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
        int titleFontSize = UIUtil.scaleForGUI(7);
        int subtitleFontSize = UIUtil.scaleForGUI(5);
        String indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

        StringBuilder description = new StringBuilder("<html>");
        // HEADER
        description.append(String.format("<div style='text-align: center;'><font size='%d'><b>%s:</b> %d</font></div>",
            titleFontSize, campaign.getName(), reputationRating));
        description.append(String.format("<div style='text-align: center;'><i>%s</i></div><br>",
            resources.getString("refresh.text")));

        // AVERAGE EXPERIENCE RATING
        description.append(String.format("<b><font size='%d'>%s: %d</font></b><br>",
            subtitleFontSize, resources.getString("averageExperienceRating.text"), averageExperienceRating));

        description.append("<table>");
        description.append(String.format("<tr><td>%s<b>%s:</b></td> <td>%s</td></tr>",
            indent, resources.getString("experienceLevel.text"), averageSkillLevel.toString()));

        description.append("</table><br>");

        // COMMAND RATING
        description.append(String.format("<b><font size='%d'>%s: %d</font></b><br>",
            subtitleFontSize, resources.getString("commandRating.text"), commanderRating));

        description.append("<table>");
        Person commander = campaign.getFlaggedCommander();

        String commanderName = resources.getString("commanderNone.text");
        if (commander != null) {
            commanderName = commander.getFullName();
        }

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s</td></tr>",
            indent, resources.getString("commander.text"), commanderName));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td></tr>",
            indent, resources.getString("leadership.text"), commanderMap.get("leadership")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td></tr>",
            indent, resources.getString("tactics.text"), commanderMap.get("tactics")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td></tr>",
            indent, resources.getString("strategy.text"), commanderMap.get("strategy")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td></tr>",
            indent, resources.getString("negotiation.text"), commanderMap.get("negotiation")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d %s<i>%s</i></td></tr>",
            indent, resources.getString("traits.text"), commanderMap.get("traits"), indent,
            resources.getString("traitsNotImplemented.text")));

        if (campaign.getCampaignOptions().isUseRandomPersonalities()
            && (campaign.getCampaignOptions().isUseRandomPersonalityReputation())) {
            description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td></tr>",
                indent, resources.getString("personality.text"), commanderMap.get("personality")));
        }

        description.append("</table><br>");

        // COMBAT RECORD RATING
        description.append(String.format("<b><font size='%d'>%s: %d</font></b><br>",
            subtitleFontSize, resources.getString("combatRecordRating.text"), combatRecordRating));

        description.append("<table>");
        description.append(String.format("<tr><th></th><th><b>%s</b></th><th><b>%s</b></th></tr>",
            resources.getString("count.text"), resources.getString("modifier.text")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">+%d</td></tr>",
            indent, resources.getString("successes.text"), combatRecordMap.get("successes"),
            combatRecordMap.get("successes") * 5));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">+%d</td></tr>",
            indent, resources.getString("partialSuccesses.text"), combatRecordMap.get("partialSuccesses"),
            combatRecordMap.get("partialSuccesses")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">-%d</td></tr>",
            indent, resources.getString("failures.text"), combatRecordMap.get("failures"),
            combatRecordMap.get("failures") * 10));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">-%d</td></tr>",
            indent, resources.getString("contractsBreached.text"), combatRecordMap.get("contractsBreached"),
            combatRecordMap.get("contractsBreached") * 25));

        if (campaign.getRetainerStartDate() != null) {
            description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                    "%d</td> <td style=\"text-align:center;\">+%d</td></tr>",
                indent, resources.getString("retainerDuration.text"), combatRecordMap.get("retainerDuration"),
                combatRecordMap.get("retainerDuration") * 5));
        }

        description.append("</table><br>");

        // TRANSPORTATION RATING
        description.append(String.format("<b><font size='%d'>%s: %d</font></b><br>",
            subtitleFontSize, resources.getString("transportationRating.text"), transportationRating));

        description.append("<table>");
        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s</td></tr>",
            indent, resources.getString("hasJumpShipOrWarShip.text"),
            transportationCapacities.get("hasJumpShipOrWarShip") == 1 ? "+10" : "+0"));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s</td></tr>",
            indent, resources.getString("hasDropShip.text"),
            transportationRequirements.get("dropShipCount") > 0 ? "+0" : "-5"));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s%s</td></tr>",
            indent, resources.getString("capacityCombatant.text"),
            transportationCapacities.get("capacityRating") >= 0 ? "+" : "",
            transportationCapacities.get("capacityRating")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s%s</td></tr>",
            indent, resources.getString("capacityNonCombatant.text"),
            transportationValues.get("passenger") >= 0 ? "+" : "",
            transportationValues.get("passenger")));
        description.append("</table>");

        description.append("<table>");
        description.append(String.format("<tr><th></th><th><b>%s</b></th><th><b>%s</b></th></tr>",
            resources.getString("required.text"), resources.getString("available.text")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("dockingCollars.text"), transportationRequirements.get("dropShipCount"),
            transportationCapacities.get("dockingCollars")));

        description.append(String.format("<tr><td><b>%s%s:*</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("smallCraft.text"), transportationRequirements.get("smallCraftCount"),
            transportationCapacities.get("smallCraftBays")));

        description.append(String.format("<tr><td><b>%s%s:*</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("fighters.text"), transportationRequirements.get("asfCount"),
            transportationCapacities.get("asfBays")));

        // <50.01 compatibility handler
        try {
            description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                    "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
                indent, resources.getString("battleMeks.text"), transportationRequirements.get("mekCount"),
                transportationCapacities.get("mekBays")));
        } catch (Exception e) {
            description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                    "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
                indent, resources.getString("battleMeks.text"), transportationRequirements.get("mechCount"),
                transportationCapacities.get("mechBays")));
        }

        description.append(String.format("<tr><td><b>%s%s:*</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("vehicleSuperHeavy.text"), transportationRequirements.get("superHeavyVehicleCount"),
            transportationCapacities.get("superHeavyVehicleBays")));

        description.append(String.format("<tr><td><b>%s%s:*</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("vehicleHeavy.text"), transportationRequirements.get("heavyVehicleCount"),
            transportationCapacities.get("heavyVehicleBays")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("vehicleLight.text"), transportationRequirements.get("lightVehicleCount"),
            transportationCapacities.get("lightVehicleBays")));

        // <50.01 compatibility handler
        try {
            description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                    "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
                indent, resources.getString("protoMeks.text"), transportationRequirements.get("protoMekCount"),
                transportationCapacities.get("protoMekBays")));
        } catch (Exception e) {
            description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                    "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
                indent, resources.getString("protoMeks.text"), transportationRequirements.get("protoMechCount"),
                transportationCapacities.get("protoMechBays")));
        }

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("battleArmor.text"), transportationRequirements.get("battleArmorCount"),
            transportationCapacities.get("battleArmorBays")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("infantry.text"), transportationRequirements.get("infantryCount"),
            transportationCapacities.get("infantryBays")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("passengers.text"), transportationRequirements.get("passengerCount"),
            transportationCapacities.get("passengerCapacity")));

        description.append("</table>");
        description.append(String.format("<i>* %s</i><br><br>", resources.getString("asterisk.text")));

        // SUPPORT RATING
        description.append(String.format("<b><font size='%d'>%s: %d</font></b><br>",
            subtitleFontSize, resources.getString("supportRating.text"), supportRating));

        description.append("<table>");
        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s</td></tr>",
            indent, resources.getString("crewRequirements.text"),
            crewRequirements.get("crewRequirements") >= 0 ? "+0" : "-5"));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s%s</td></tr>",
            indent, resources.getString("administrationModifier.text"),
            administrationRequirements.get("total") >= 0 ? "+" : "",
            administrationRequirements.get("total")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s%s</td></tr>",
            indent, resources.getString("TechnicianModifier.text"),
            technicianRequirements.get("rating").get(0) >= 0 ? "+" : "",
            technicianRequirements.get("rating").get(0)));
        description.append("</table>");

        description.append("<table>");
        description.append(String.format("<tr><th></th><th><b>%s</b></th><th><b>%s</b></th></tr>",
            resources.getString("required.text"), resources.getString("available.text")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("administrationRequirements.text"), administrationRequirements.get("personnelCount"),
            administrationRequirements.get("administratorCount")));

        // <50.01 compatibility handler
        try {
            description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                    "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
                indent, resources.getString("battleMeksAndProtoMeks.text"), technicianRequirements.get("mek").get(0),
                technicianRequirements.get("mek").get(1)));
        } catch (Exception e) {
            description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                    "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
                indent, resources.getString("battleMeksAndProtoMeks.text"), technicianRequirements.get("mech").get(0),
                technicianRequirements.get("mech").get(1)));
        }

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("vehicles.text"), technicianRequirements.get("vehicle").get(0),
            technicianRequirements.get("vehicle").get(1)));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("fightersAndSmallCraft.text"), technicianRequirements.get("aero").get(0),
            technicianRequirements.get("aero").get(1)));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td style=\"text-align:center;\">" +
                "%d</td> <td style=\"text-align:center;\">%d</td></tr>",
            indent, resources.getString("battleArmor.text"), technicianRequirements.get("battleArmor").get(0),
            technicianRequirements.get("battleArmor").get(1)));
        description.append("</table><br>");

        // FINANCIAL RATING
        description.append(String.format("<b><font size='%d'>%s: %d</font></b><br>",
            subtitleFontSize, resources.getString("financialRating.text"), financialRating));

        description.append("<table>");
        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%s</td></tr>",
            indent, resources.getString("hasLoanOrDebt.text"),
            ((financialRatingMap.get("hasLoan") + financialRatingMap.get("inDebt")) > 0) ? "-10" : "0"));
        description.append("</table><br>");

        // CRIME RATING
        description.append(String.format("<b><font size='%d'>%s: %d</font></b><br>",
            subtitleFontSize, resources.getString("crimeRating.text"), crimeRating));

        description.append("<table>");
        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td></tr>",
            indent, resources.getString("piracy.text"), crimeRatingMap.get("piracy")));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td></tr>",
            indent, resources.getString("otherCrimes.text"), crimeRatingMap.get("other")));
        description.append("</table>");

        if (crimeRating < 0) {
            description.append(String.format("<b>%s%s:</b> %s<br>", indent,
                resources.getString("dateOfLastCrime.text"), dateOfLastCrime == null ? "" : dateOfLastCrime));
        }

        // OTHER MODIFIERS
        description.append(String.format("<b><font size='%d'>%s: %d</font></b><br>",
            subtitleFontSize, resources.getString("otherModifiers.text"), otherModifiers));

        description.append("<table>");
        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td> <td>%d</td></tr>",
            indent, resources.getString("inactiveYears.text"), otherModifiersMap.get("inactiveYears"),
            otherModifiersMap.get("inactiveYears") * -5));

        description.append(String.format("<tr><td><b>%s%s:</b></td> <td>%d</td></tr>",
            indent, resources.getString("customModifier.text"), otherModifiersMap.get("customModifier")));
        description.append("</table>");

        description.append("</html>");

        return description.toString();
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
                    this.averageExperienceRating = Integer.parseInt(
                        workingNode2.getTextContent().replaceAll("-", "_"));
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
