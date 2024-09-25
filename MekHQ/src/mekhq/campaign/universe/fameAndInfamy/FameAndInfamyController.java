package mekhq.campaign.universe.fameAndInfamy;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Factions;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import static java.lang.Math.round;

public class FameAndInfamyController {
    private Map<String, Double> trackingFactions;

    private static final ResourceBundle resources = ResourceBundle.getBundle(
        "mekhq.resources.FameAndInfamy",
        MekHQ.getMHQOptions().getLocale());

    private final static MMLogger logger = MMLogger.create(FameAndInfamyController.class);

    /**
     * Constructor for the {@link FameAndInfamyController} class.
     * Initializes the {@code trackingFactions} map with the provided map of factions.
     * If any factions are missing from the provided map, they will be added with a default fame
     * value of 3.0 (or 0.0 if the provided faction uses Batchalls).
     */
    public FameAndInfamyController() {
        this.trackingFactions = new HashMap<>();
        for (String shortName : getAllFactionShortnames()) {
            if (BatchallFactions.usesBatchalls(shortName)) {
                this.trackingFactions.put(shortName, 0.0);
            } else {
                this.trackingFactions.put(shortName, 2.0);
            }
        }

        // TODO add modifiers for inter-faction relationships
    }

    /**
     * Retrieves the shortnames of all factions from the XML file.
     *
     * @return A list of faction shortnames.
     */
    public List<String> getAllFactionShortnames() {
        List<String> shortnames = new ArrayList<>();

        try {
            File inputFile = new File("data/universe/factions.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("shortname");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) nNode;
                    shortnames.add(element.getTextContent());
                }
            }
        } catch (Exception e) {
            logger.error(String.format("FameAndInfamyController failed to parse contents of 'shortname'" +
                " in 'data/universe/factions.xml'. Last successfully parsed Faction shortname: %s",
                shortnames.get(shortnames.size() - 1)));
            return shortnames;
        }

        return shortnames;
    }

    /**
     * Retrieves the precise fame value for a given faction.
     * Normally we don't care what the exact value is,
     * so {@code getFameLevelForFaction} should be used, instead.
     *
     * @param factionCode the code of the faction
     * @return the fame value for the faction
     */
    public double getFameForFaction(String factionCode) {
        return trackingFactions.get(factionCode);
    }

    /**
     * Retrieves the fame level for a faction. This is determined by normally rounding raw fame to
     * the nearest {@link Integer}
     *
     * @param factionCode The code of the faction.
     * @return The fame level of the faction.
     */
    public int getFameLevelForFaction(String factionCode) {
        return (int) round(trackingFactions.get(factionCode));
    }

    /**
     * Retrieves the name of the fame level for a faction.
     *
     * @param factionCode The code of the faction.
     * @param isInfamy Specifies whether to retrieve the fame name for infamy or fame.
     * @return The name of the fame level for the faction.
     */
    public String getFameName(String factionCode, boolean isInfamy) {
        final int level = getFameLevelForFaction(factionCode);

        if (isInfamy) {
            return switch (level) {
                case 0 -> "Reviled";
                case 1 -> "Disgraced";
                case 2 -> "Insignificant";
                case 3 -> "Venerated";
                case 4 -> "Exalted";
                case 5 -> "Legendary";
                default -> throw new IllegalStateException("Unexpected value in getFameName, infamy: "
                    + level);
            };
        } else {
            return switch (level) {
                case 0 -> "Insignificant";
                case 1 -> "Obscure";
                case 2 -> "Noted";
                case 3 -> "Notorious";
                case 4 -> "Infamous";
                case 5 -> "Reviled";
                default -> throw new IllegalStateException("Unexpected value in getFameName, fame: "
                    + level);
            };
        }
    }

    /**
     * Sets the fame value for a specific faction.
     *
     * @param factionCode The code representing the faction.
     * @param fame The fame value to be set for the faction.
     */
    public void setFameForFaction(String factionCode, double fame) {
        fame = MathUtility.clamp(fame, 0.0, 5.0);

        trackingFactions.put(factionCode, fame);
    }

    /**
     * Updates the fame of a faction by a specified adjustment.
     *
     * @param factionCode The code representing the faction.
     * @param campaign The current campaign.
     * @param adjustment  The adjustment to be made to the faction's fame.
     */
    public void updateFameForFaction(Campaign campaign, String factionCode, double adjustment) {
        int originalFame = getFameLevelForFaction(factionCode);

        double currentFame = trackingFactions.get(factionCode);
        adjustment = currentFame + adjustment;
        adjustment = MathUtility.clamp(adjustment, 0.0, 5.0);

        trackingFactions.put(factionCode, adjustment);

        int newFame = getFameLevelForFaction(factionCode);

        if (originalFame != newFame) {
            campaign.addReport(String.format(resources.getString("fameChangeReportInfamy.text"),
                newFame, Factions.getInstance().getFaction(factionCode).getFullName(campaign.getGameYear())));
        }
    }

    /**
     * Writes the fame and infamy data to an XML file using the provided {@link PrintWriter} and indent level.
     *
     * @param printWriter The {@link PrintWriter} used to write to the XML file.
     * @param indent      The indent level for formatting the XML file.
     */
    public void writeToXml(PrintWriter printWriter, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(printWriter, indent++, "fameAndInfamy");
        for (String trackedFaction : trackingFactions.keySet()) {
            double factionFame = trackingFactions.get(trackedFaction);
            boolean shouldWrite = factionFame != (BatchallFactions.usesBatchalls(trackedFaction) ? 0 : 2);

            if (shouldWrite) {
                MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, trackedFaction, getFameForFaction(trackedFaction));
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(printWriter, --indent, "fameAndInfamy");
    }

    /**
     * Parses the XML {@link NodeList} and updates the fame values for factions in a {@link Campaign}.
     *
     * @param nodeList  The XML {@link NodeList} containing the faction fame values.
     * @param campaign  The {@link Campaign} object to update with the parsed fame values.
     */
    public static void parseFromXML(final NodeList nodeList, Campaign campaign) {
        FameAndInfamyController fameAndInfamy = campaign.getFameAndInfamy();
        try {
            for (int x = 0; x < nodeList.getLength(); x++) {
                final Node workingNode = nodeList.item(x);
                if (workingNode.getNodeType() == Node.ELEMENT_NODE) {
                    double value = Double.parseDouble(workingNode.getTextContent());
                    fameAndInfamy.setFameForFaction(workingNode.getNodeName(), value);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
