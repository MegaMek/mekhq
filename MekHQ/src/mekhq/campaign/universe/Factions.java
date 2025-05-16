/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe;

import static java.awt.Color.BLACK;
import static megamek.utilities.ImageUtilities.addTintToImageIcon;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

        logger.info(String.format("Loaded a total of %d factions", factions.factions.size()));

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
     * @deprecated use {@link #getFactionLogo(int, String)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public static ImageIcon getFactionLogo(Campaign campaign, String factionCode,
                                           boolean fallbackDateDependent) {
        return getFactionLogo(campaign.getGameYear(), factionCode);
    }

    /**
     * Returns the logo ImageIcon for a specific faction and game year.
     *
     * <p>This method resolves the appropriate logo file for the given {@code factionCode} and {@code gameYear},
     * accounting for historical changes in faction logos over time where applicable.</p>
     *
     * <p>The resulting image is loaded from the predefined directory as a PNG file and tinted black.
     * For unknown or missing faction codes, a default logo is used.</p>
     *
     * @param gameYear    the year in the game context, potentially affecting logo selection for some factions
     * @param factionCode the code identifying the faction (e.g., "FS" for Federated Suns)
     *
     * @return an {@link ImageIcon} object for the specified faction, tinted black
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static ImageIcon getFactionLogo(int gameYear, String factionCode) {
        final String IMAGE_DIRECTORY = "data/images/universe/factions/";
        final String FILE_TYPE = ".png";

        String key = switch (factionCode) {
            case "ARC", "ARD" -> "logo_aurigan_coalition";
            case "CDP" -> "logo_calderon_protectorate";
            case "CC" -> "logo_capellan_confederation";
            case "CIR" -> "logo_circinus_federation";
            case "CBS" -> "logo_clan_blood_spirit";
            case "CB" -> "logo_clan_burrock";
            case "CCC" -> "logo_clan_cloud_cobra";
            case "CCO" -> "logo_clan_coyote";
            case "CFM" -> "logo_clan_fire_mandrills";
            case "CGB" -> {
                if (gameYear >= 3060) {
                    yield "logo_rasalhague_dominion";
                } else {
                    yield "logo_clan_ghost_bear";
                }
            }
            case "CGS" -> "logo_clan_goliath_scorpion";
            case "CHH" -> "logo_clan_hells_horses";
            case "CIH" -> "logo_clan_ice_hellion";
            case "CJF" -> "logo_clan_jade_falcon";
            case "CMG" -> "logo_clan_mongoose";
            case "CNC" -> "logo_clan_nova_cat";
            case "CDS" -> {
                if (gameYear <= 2984 || gameYear >= 3100) {
                    yield "logo_clan_sea_fox";
                } else {
                    yield "logo_clan_diamond_sharks";
                }
            }
            case "CSJ" -> "logo_clan_smoke_jaguar";
            case "CSR" -> "logo_clan_snow_raven";
            case "CSA" -> "logo_clan_star_adder";
            case "CSV" -> "logo_clan_steel_viper";
            case "CSL" -> "logo_clan_stone_lion";
            case "CW", "CWE", "CWIE" -> "logo_clan_wolf";
            case "CWOV" -> "logo_clan_wolverine";
            case "CS" -> "logo_comstar";
            case "DC" -> "logo_draconis_combine";
            case "DA" -> "logo_duchy_of_andurien";
            case "DTA" -> "logo_duchy_of_tamarind_abbey";
            case "CEI" -> "logo_escorpion_imperio";
            case "FC" -> "logo_federated_commonwealth";
            case "FS" -> "logo_federated_suns";
            case "FOR" -> "logo_fiefdom_of_randis";
            case "FVC" -> "logo_filtvelt_coalition";
            case "FRR" -> "logo_free_rasalhague_republic";
            case "FWL" -> "logo_free_worlds_league";
            case "FR" -> "logo_fronc_reaches";
            case "HL" -> "logo_hanseatic_league";
            case "IP" -> "logo_illyrian_palatinate";
            case "LL" -> "logo_lothian_league";
            case "LA" -> "logo_lyran_alliance";
            case "MOC" -> "logo_magistracy_of_canopus";
            case "MH" -> "logo_marian_hegemony";
            case "MERC" -> "logo_mercenaries";
            case "MV" -> "logo_morgrains_valkyrate";
            case "NC" -> "logo_nueva_castile";
            case "OC" -> "logo_oberon_confederation";
            case "OA" -> "logo_outworld_alliance";
            case "PIR" -> "logo_pirates";
            case "RD" -> "logo_rasalhague_dominion";
            case "RF" -> "logo_regulan_fiefs";
            case "ROS" -> "logo_republic_of_the_sphere";
            case "RWR" -> "logo_rim_worlds_republic";
            case "IND" -> "logo_security_forces";
            case "SIC" -> "logo_st_ives_compact";
            case "SL" -> "logo_star_league";
            case "TC" -> "logo_taurian_concordat";
            case "TD" -> "logo_tortuga_dominions";
            case "UC" -> "logo_umayyad_caliphate";
            case "WOB" -> "logo_word_of_blake";
            case "TH" -> "logo_terran_hegemony";
            case "CI" -> "logo_chainelane_isles";
            case "SOC" -> "logo_the_society";
            case "CWI" -> "logo_clan_widowmaker";
            case "EF" -> "logo_elysian_fields";
            case "GV" -> "logo_greater_valkyrate";
            case "JF" -> "logo_jarnfolk";
            case "MSC" -> "logo_marik_stewart_commonwealth";
            case "OP" -> "logo_oriente_protectorate";
            case "RA" -> "logo_raven_alliance";
            case "RCM" -> "logo_raven_alliance";
            case "NIOPS" -> "logo_niops_association";
            case "AXP" -> "logo_axumite_providence";
            case "NDC" -> "logo_new_delphi_compact";
            case "REB" -> "logo_rebels";
            // Fallbacks
            default -> {
                Faction faction = Factions.getInstance().getFaction(factionCode);

                if (faction != null && faction.isClan()) {
                    yield "logo_clan_generic";
                } else {
                    yield "logo_mercenaries";
                }
            }
        };

        ImageIcon icon = new ImageIcon(IMAGE_DIRECTORY + key + FILE_TYPE);
        icon = addTintToImageIcon(icon.getImage(), BLACK);

        return icon;
    }
}
