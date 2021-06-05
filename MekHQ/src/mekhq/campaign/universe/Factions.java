/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;

public class Factions {
    //region Variable Declarations
    private static Factions instance;

    private Map<String, Faction> factions = new HashMap<>();

    private RATGenerator ratGenerator;
    //endregion Variable Declarations

    //region Constructors
    private Factions() {
        this(RATGenerator.getInstance());
    }

    private Factions(final RATGenerator ratGenerator) {
        this.ratGenerator = Objects.requireNonNull(ratGenerator);
    }
    //endregion Constructors

    public static Factions getInstance() {
        if (instance == null) {
            instance = new Factions();
        }

        return instance;
    }

    public static void setInstance(@Nullable Factions instance) {
        Factions.instance = instance;
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

    public Faction getFactionFromFullNameAndYear(String fname, int year) {
        Faction faction = null;
        for (Faction f : factions.values()) {
            if (f.getFullName(year).equals(fname)) {
                faction = f;
                break;
            }
        }
        return faction;
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
                MekHQ.getLogger().error("Could not locate faction record for " + faction);
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
        MekHQ.getLogger().info("Starting load of faction data from XML...");

        Factions factions = load("data/universe/factions.xml");

        MekHQ.getLogger().info("Loaded a total of " + factions.factions.size() + " factions");

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
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        }

        Element factionEle = xmlDoc.getDocumentElement();
        NodeList nl = factionEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML.  At least this cleans it up.
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
                        MekHQ.getLogger().error(
                                String.format("Faction code \"%s\" already used for faction %s, can't re-use it for %s",
                                        faction.getShortName(), retVal.factions.get(faction.getShortName()).getFullName(0), faction.getFullName(0)));
                    }
                }
            }
        }

        return retVal;
    }
}
