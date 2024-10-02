/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Factions {
    private static final MMLogger logger = MMLogger.create(Factions.class);

    // region Variable Declarations
    private static Factions instance;

    private Map<String, Faction> factions = new HashMap<>();

    private RATGenerator ratGenerator;
    // endregion Variable Declarations

    // region Constructors
    private Factions() {
        this(RATGenerator.getInstance());
    }

    private Factions(final RATGenerator ratGenerator) {
        this.ratGenerator = Objects.requireNonNull(ratGenerator);
    }
    // endregion Constructors

    public static Factions getInstance() {
        if (instance == null) {
            instance = new Factions();
        }

        return instance;
    }

    public static void setInstance(@Nullable Factions instance) {
        Factions.instance = instance;
    }

    public Faction getDefaultFaction() {
        return getFaction("MERC");
    }

    public RATGenerator getRATGenerator() {
        return ratGenerator;
    }

    public void setRATGenerator(RATGenerator ratGenerator) {
        this.ratGenerator = Objects.requireNonNull(ratGenerator);
    }

    public List<Faction> getChoosableFactions() {
        return getFactions().stream().filter(Faction::isPlayable).collect(Collectors.toList());
    }

    public Collection<Faction> getFactions() {
        return factions.values();
    }

    /**
     * Returns a list of all factions active in a specific year.
     * @param date
     * @return
     */
    public Collection<Faction> getActiveFactions(LocalDate date) {
        return getFactions().stream().filter(f ->
            f.validIn(date) && !f.isInactive())
            .collect(Collectors.toList());
    }

    public Collection<String> getFactionList() {
        return new ArrayList<>(factions.keySet());
    }

    public Faction getFaction(String sname) {
        Faction defaultFaction = new Faction();
        if (factions == null) {
            return defaultFaction;
        } else {
            return factions.getOrDefault(sname, defaultFaction);
        }
    }

    public Faction getFactionFromFullNameAndYear(final String factionName, final int year) {
        return factions.values().stream()
                .filter(faction -> faction.getFullName(year).equals(factionName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Helper function that gets the faction record for the specified faction, or a
     * fallback general faction record. Useful for RAT generator activity.
     *
     * @param faction The faction whose MegaMek faction record to retrieve.
     * @return Found faction record or null.
     */
    public FactionRecord getFactionRecordOrFallback(String faction) {
        FactionRecord fRec = getRATGenerator().getFaction(faction);
        if (fRec == null) {
            Faction f = getFaction(faction);
            if (f != null) {
                if (f.isPeriphery()) {
                    fRec = getRATGenerator().getFaction("Periphery");
                } else if (f.isClan()) {
                    fRec = getRATGenerator().getFaction("CLAN");
                } else {
                    fRec = getRATGenerator().getFaction("IS");
                }
            }

            if (fRec == null) {
                String message = String.format("Could not locate faction record for %s", faction);
                logger.error(message);
            }
        }

        return fRec;
    }

    /**
     * Loads the default Factions data.
     *
     * @throws DOMException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Factions loadDefault()
            throws DOMException, SAXException, IOException, ParserConfigurationException {
        logger.info("Starting load of faction data from XML...");

        Factions factions = load("data/universe/factions.xml");

        logger.info("Loaded a total of %d factions", factions.factions.size());

        return factions;
    }

    /**
     * Loads Factions data from a file.
     *
     * @param factionsPath The path to the XML file containing Factions data.
     *
     * @throws DOMException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Factions load(String factionsPath)
            throws DOMException, SAXException, IOException, ParserConfigurationException {
        Factions retVal = new Factions();

        Document xmlDoc;

        try (FileInputStream fis = new FileInputStream(factionsPath)) {
            // Using factory get an instance of document builder
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        }

        Element factionEle = xmlDoc.getDocumentElement();
        NodeList nl = factionEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        factionEle.normalize();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (!wn.getParentNode().equals(factionEle)) {
                continue;
            }

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("faction")) {
                    Faction faction = Faction.getFactionFromXML(wn);
                    if (!retVal.factions.containsKey(faction.getShortName())) {
                        retVal.factions.put(faction.getShortName(), faction);
                    } else {
                        String message = String.format(
                                "Faction code \"%s\" already used for faction %s, can't re-use it for %s",
                                faction.getShortName(),
                                retVal.factions.get(faction.getShortName()).getFullName(0),
                                faction.getFullName(0));
                        logger.error(message);
                    }
                }
            }
        }

        return retVal;
    }

    /**
     * Gets the logo image for the provided faction code, and fallback image date dependency.
     *
     * @param campaign The campaign.
     * @param factionCode The faction code.
     * @param fallbackDateDependent Whether to use a fallback image that may depend on the campaign's date.
     * @return The faction logo image.
     */
    public static ImageIcon getFactionLogo(Campaign campaign, String factionCode,
                                           boolean fallbackDateDependent) {
        final String PORTRAIT_DIRECTORY = "data/images/force/Pieces/Logos/";
        final String DIRECTORY_CLAN = "Clan/";
        final String DIRECTORY_INNER_SPHERE = "Inner Sphere/";
        final String DIRECTORY_PERIPHERY = "Periphery/";
        final String PORTRAIT_FILE_TYPE = ".png";

        return switch (factionCode) {
            // Each case sets the icon to the corresponding image
            case "CC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Capellan Confederation" + PORTRAIT_FILE_TYPE);
            case "CS" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "ComStar" + PORTRAIT_FILE_TYPE);
            case "DC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Draconis Combine" + PORTRAIT_FILE_TYPE);
            case "DA" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Duchy of Andurien" + PORTRAIT_FILE_TYPE);
            case "DTA" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Duchy of Tamarind-Abbey" + PORTRAIT_FILE_TYPE);
            case "FC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Federated Commonwealth (Alternate)" + PORTRAIT_FILE_TYPE);
            case "FS" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Federated Suns" + PORTRAIT_FILE_TYPE);
            case "FRR" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Free Rasalhague Republic" + PORTRAIT_FILE_TYPE);
            case "FWL" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Free Worlds League" + PORTRAIT_FILE_TYPE);
            case "FWLR" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Free Worlds League - Nonaligned" + PORTRAIT_FILE_TYPE);
            case "LA" -> {
                if (campaign.getGameYear() >= 3058 && campaign.getGameYear() < 3085) {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                        + "Lyran Alliance" + PORTRAIT_FILE_TYPE);
                } else {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                        + "Lyran Commonwealth" + PORTRAIT_FILE_TYPE);
                }
            }
            case "MSC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Marik-Stewart Commonwealth" + PORTRAIT_FILE_TYPE);
            case "OP" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Oriente Protectorate" + PORTRAIT_FILE_TYPE);
            case "RF" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Regulan Fiefs" + PORTRAIT_FILE_TYPE);
            case "ROS" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Republic of the Sphere" + PORTRAIT_FILE_TYPE);
            case "RCM" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Rim Commonality" + PORTRAIT_FILE_TYPE);
            case "SL" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Star League" + PORTRAIT_FILE_TYPE);
            case "TH" -> {
                if (campaign.getLocalDate().isAfter(LocalDate.of(2340, 1, 17))) {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                        + "Terran Hegemony (Alternate, House Cameron)" + PORTRAIT_FILE_TYPE);
                } else {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                        + "Terran Hegemony" + PORTRAIT_FILE_TYPE);
                }
            }
            case "WOB" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_INNER_SPHERE
                + "Word of Blake" + PORTRAIT_FILE_TYPE);
            case "CBS" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Blood Spirit" + PORTRAIT_FILE_TYPE);
            case "CB" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                +  "Clan Burrock" + PORTRAIT_FILE_TYPE);
            case "CCC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                +  "Clan Cloud Cobra" + PORTRAIT_FILE_TYPE);
            case "CCO" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                +  "Clan Coyote" + PORTRAIT_FILE_TYPE);
            case "CDS" -> {
                if (campaign.getGameYear() >= 3100) {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                        +  "Clan Sea Fox" + PORTRAIT_FILE_TYPE);
                } else {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                        +  "Clan Diamond Shark" + PORTRAIT_FILE_TYPE);
                }
            }
            case "CFM" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Fire Mandrill" + PORTRAIT_FILE_TYPE);
            case "CGB" -> {
                if (campaign.getGameYear() >= 3060) {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                        + "Ghost Bear Dominion" + PORTRAIT_FILE_TYPE);
                } else {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                        + "Clan Ghost Bear" + PORTRAIT_FILE_TYPE);
                }
            }
            case "CGS" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Goliath Scorpion" + PORTRAIT_FILE_TYPE);
            case "CHH" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Hell's Horses" + PORTRAIT_FILE_TYPE);
            case "CIH" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Ice Hellion" + PORTRAIT_FILE_TYPE);
            case "CJF" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Jade Falcon" + PORTRAIT_FILE_TYPE);
            case "CMG" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Mongoose" + PORTRAIT_FILE_TYPE);
            case "CNC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Nova Cat" + PORTRAIT_FILE_TYPE);
            case "CSJ" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Smoke Jaguar" + PORTRAIT_FILE_TYPE);
            case "CSR" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Snow Raven" + PORTRAIT_FILE_TYPE);
            case "CSA" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Star Adder" + PORTRAIT_FILE_TYPE);
            case "CSV" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Steel Viper" + PORTRAIT_FILE_TYPE);
            case "CSL" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Stone Lion" + PORTRAIT_FILE_TYPE);
            case "CWI" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Widowmaker" + PORTRAIT_FILE_TYPE);
            case "CW", "CWE" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Wolf" + PORTRAIT_FILE_TYPE);
            case "CWIE" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Clan Wolf-in-Exile" + PORTRAIT_FILE_TYPE);
            case "CEI" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Scorpion Empire" + PORTRAIT_FILE_TYPE);
            case "RD" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Rasalhague Dominion" + PORTRAIT_FILE_TYPE);
            case "RA" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_CLAN
                + "Raven Alliance" + PORTRAIT_FILE_TYPE);
            case "ARC" -> {
                if (campaign.getGameYear() >= 3022 && campaign.getGameYear() < 3026) {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                        + "Arano Restoration" + PORTRAIT_FILE_TYPE);
                } else {
                    yield new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                        + "Aurigan Coalition" + PORTRAIT_FILE_TYPE);
                }
            }
            case "ARD" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Aurigan Directorate" + PORTRAIT_FILE_TYPE);
            case "AXP" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Axumite Providence" + PORTRAIT_FILE_TYPE);
            case "CDP" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Calderon Protectorate" + PORTRAIT_FILE_TYPE);
            case "CI" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Chainelane Isles" + PORTRAIT_FILE_TYPE);
            case "CIR" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Circinus Federation" + PORTRAIT_FILE_TYPE);
            case "EF" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Elysian Fields" + PORTRAIT_FILE_TYPE);
            case "FOR" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Fiefdom of Randis" + PORTRAIT_FILE_TYPE);
            case "FVC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Filtvelt Coalition" + PORTRAIT_FILE_TYPE);
            case "FR" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Fronc Reaches" + PORTRAIT_FILE_TYPE);
            case "GV" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Greater Valkyrate" + PORTRAIT_FILE_TYPE);
            case "HL" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Hanseatic League" + PORTRAIT_FILE_TYPE);
            case "IP" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Illyrian Palatinate" + PORTRAIT_FILE_TYPE);
            case "JF" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "JarnFolk" + PORTRAIT_FILE_TYPE);
            case "LL" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Lothian League" + PORTRAIT_FILE_TYPE);
            case "MOC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Magistracy of Canopus" + PORTRAIT_FILE_TYPE);
            case "MH" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Marian Hegemony" + PORTRAIT_FILE_TYPE);
            case "NDC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "New Delphi Compact" + PORTRAIT_FILE_TYPE);
            case "NIOPS" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Niops Association" + PORTRAIT_FILE_TYPE);
            case "NC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Nueva Castile" + PORTRAIT_FILE_TYPE);
            case "OC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Oberon Confederation" + PORTRAIT_FILE_TYPE);
            case "OA" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Outworlds Alliance" + PORTRAIT_FILE_TYPE);
            case "RWR" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Rim Worlds Republic" + PORTRAIT_FILE_TYPE);
            case "SIC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "St. Ives Compact" + PORTRAIT_FILE_TYPE);
            case "TC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Taurian Concordat" + PORTRAIT_FILE_TYPE);
            case "TD" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Tortuga Dominions" + PORTRAIT_FILE_TYPE);
            case "UC" -> new ImageIcon(PORTRAIT_DIRECTORY + DIRECTORY_PERIPHERY
                + "Umayyad Caliphate" + PORTRAIT_FILE_TYPE);
            case "PIR" -> new ImageIcon("data/images/universe/factions/logo_pirates.png");
            default -> {
                if (!fallbackDateDependent) {
                    yield new ImageIcon("data/images/universe/factions/logo_star_league.png");
                }

                int currentYear = campaign.getGameYear();

                if (currentYear > 3149 || currentYear < 2630) {
                    yield new ImageIcon("data/images/universe/factions/logo_mercenaries.png");
                } else if (currentYear > 2788) {
                    yield new ImageIcon("data/images/universe/factions/logo_comstar.png");
                } else {
                    yield new ImageIcon("data/images/universe/factions/logo_star_league.png");
                }
            }
        };
    }
}
