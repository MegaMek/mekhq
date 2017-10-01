/*
 * Faction.java
 *
 * Copyright (C) 2009-2016 MegaMek team
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.universe;

import java.awt.Color;
import java.io.FileInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.joda.time.DateTime;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Faction {
    private static Map<String,Faction> factions;
    private static Map<Integer,Faction> factionIdMap;
    public static String[] choosableFactionCodes = {"MERC","CC","DC","FS","FWL","LA","FC","ROS","CS","WOB","FRR","SIC","MOC","MH","OA","TC","CDS","CGB","CHH","CJF","CNC","CSJ","CSV","CW","TH","RWR"};

    private String shortname;
    private String fullname;
    private String[] altNamesByEra;
    private String[] altNames;
    private Color color;
    private String nameGenerator;
    private String[] startingPlanet;
    private int[] eraMods;
    private Integer id;
    private Set<Tag> tags;
    // Start and end years (inclusive)
    private int start;
    private int end;

    public Faction() {
        this("???", "Unknown");
    }

    public Faction(String sname, String fname) {
        shortname = sname;
        fullname = fname;
        nameGenerator = "General";
        color = Color.LIGHT_GRAY;
        startingPlanet = new String[]{"Terra","Terra","Terra","Terra","Terra","Terra","Terra","Terra","Terra"};
        altNamesByEra = new String[]{"","","","","","","","",""};
        eraMods = new int[]{0,0,0,0,0,0,0,0,0};
        tags = EnumSet.noneOf(Faction.Tag.class);
        start = 0;
        end = 9999;
    }

    public String getShortName() {
        return shortname;
    }

    public String getFullName(int era) {
        String alt = "";
        if(altNamesByEra.length > era) {
            alt = altNamesByEra[era];
        }
        if(alt.trim().length() == 0) {
            return fullname;
        } else {
            return alt;
        }
    }

    public Color getColor() {
        return color;
    }

    public boolean isClan() {
        return is(Tag.CLAN);
    }
    
    public boolean isComstar() {
        return "CS".equals(shortname);
    }

    public boolean isPeriphery() {
        return is(Tag.PERIPHERY);
    }

    public String getNameGenerator() {
        return nameGenerator;
    }

    public String getStartingPlanet(int era) {
        if(startingPlanet.length > era) {
            return startingPlanet[era];
        } else if(startingPlanet.length > 0) {
            return startingPlanet[startingPlanet.length-1];
        }
        return "Terra";
    }

    public int getEraMod(int era) {
        if(eraMods.length > era) {
            return eraMods[era];
        }
        return 0;
    }

    public int getTechMod(Part part, Campaign campaign) {
        int currentYear = campaign.getCalendar().get(Calendar.YEAR);

        //TODO: This seems hacky - we shouldn't hardcode in universe details
        //like this
        int factionMod = 0;
        if (part.getTechBase() == Part.T_CLAN && !isClan()) {
            // Availability of clan tech for IS
            if (currentYear<3050)
                // Impossible to buy before clan invasion
                factionMod = 12;
            else if (currentYear<=3052)
                // Between begining of clan invasiuon and tukayyid, very very hard to buy
                factionMod = 5;
            else if (currentYear<=3060)
                // Between tukayyid and great refusal, very hard to buy
                factionMod = 4;
            else
                // After great refusal, hard to buy
                factionMod = 3;
        }
        if (part.getTechBase() == Part.T_IS && isPeriphery()) {
            // Availability of high tech rating equipment in low tech areas (periphery)
            switch (part.getTechRating()) {
                case(EquipmentType.RATING_E) :
                    factionMod += 1;
                    break;
                case(EquipmentType.RATING_F) :
                    factionMod += 2;
                    break;
            }
        }

        return factionMod;
    }
    
    public boolean is(Faction.Tag tag) {
        return tags.contains(tag);
    }
    
    public boolean validIn(int year) {
        return (year >= start) && (year <= end);
    }
    
    public boolean validIn(DateTime time) {
        return validIn(time.getYear());
    }

    public Integer getId() {
        return id;
    }
    
    public boolean hasName(String name) {
        if (name.equals(fullname)) {
            return true;
        } else {
            for (String altName : altNamesByEra) {
                if (name.equals(altName)) {
                    return true;
                }
            }
        }
        if (altNames != null && altNames.length > 0) {
            for (String altName : altNames) {
                if (name.equals(altName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static Collection<Faction> getFactions() {
        return factions.values();
    }
    
    public static Collection<String> getFactionList() {
        return new ArrayList<>(factions.keySet());
    }

    public static Faction getFaction(String sname) {
        return factions.get(sname);
    }

    public static Faction getFactionFromFullName(String fname, int year) {
        return getFactionFromFullNameAndEra(fname, Era.getEra(year));
    }

    public static Faction getFactionFromFullNameAndEra(String fname, int era) {
        Faction faction = null;
        for (Faction f : factions.values()) {
            if (f.getFullName(era).equals(fname)) {
                faction = f;
                break;
            }
        }
        return faction;
    }

    public static String getFactionCode(int faction) {
        Faction f = factionIdMap.get(faction);
        return (null != f) ? f.getShortName() : "IND"; //$NON-NLS-1$
    }

    public static Faction getFactionFromXML(Node wn) throws DOMException, ParseException {
        final String METHOD_NAME = "getFactionFromXML(Node)"; //$NON-NLS-1$
        
        Faction retVal = new Faction();
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("shortname")) {
                retVal.shortname = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("fullname")) {
                retVal.fullname = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("nameGenerator")) {
                retVal.nameGenerator = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.tags.add(Tag.CLAN);
                } else {
                    retVal.tags.remove(Tag.CLAN);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("periphery")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.tags.add(Tag.PERIPHERY);
                } else {
                    retVal.tags.remove(Tag.PERIPHERY);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("startingPlanet")) {
                retVal.startingPlanet = wn2.getTextContent().split(",", -2);
            } else if (wn2.getNodeName().equalsIgnoreCase("altNamesByEra")) {
                retVal.altNamesByEra = wn2.getTextContent().split(",", -2);
            } else if (wn2.getNodeName().equalsIgnoreCase("altNames")) {
                retVal.altNames = wn2.getTextContent().split(",", 0);
            } else if (wn2.getNodeName().equalsIgnoreCase("eraMods")) {
                String[] values = wn2.getTextContent().split(",", -2);
                for(int i = 0; i < values.length; i++) {
                    retVal.eraMods[i] = Integer.parseInt(values[i]);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("colorRGB")) {
                String[] values = wn2.getTextContent().split(",");
                if(values.length == 3) {
                    int colorRed = Integer.parseInt(values[0]);
                    int colorGreen = Integer.parseInt(values[1]);
                    int colorBlue = Integer.parseInt(values[2]);
                    retVal.color = new Color(colorRed, colorGreen, colorBlue);
                }
            } else if(wn2.getNodeName().equalsIgnoreCase("id")) {
                retVal.id = Integer.valueOf(wn2.getTextContent());
            } else if(wn2.getNodeName().equalsIgnoreCase("start")) {
                retVal.start = Integer.valueOf(wn2.getTextContent());
            } else if(wn2.getNodeName().equalsIgnoreCase("end")) {
                retVal.end = Integer.valueOf(wn2.getTextContent());
            } else if(wn2.getNodeName().equalsIgnoreCase("tags")) {
                Arrays.stream(wn2.getTextContent().split(",")).map(tag -> tag.toUpperCase(Locale.ROOT))
                    .map(Tag::valueOf).forEach(tag -> retVal.tags.add(tag));
            }
        }

        if(retVal.altNamesByEra.length < Era.E_NUM) {
            MekHQ.getLogger().log(Faction.class, METHOD_NAME, LogLevel.WARNING,
                    retVal.fullname + " faction did not have a long enough altNamesByEra vector"); //$NON-NLS-1$
        }
        if(retVal.eraMods.length < Era.E_NUM) {
            MekHQ.getLogger().log(Faction.class, METHOD_NAME, LogLevel.WARNING,
                    retVal.fullname + " faction did not have a long enough eraMods vector"); //$NON-NLS-1$
        }
        if(!retVal.is(Tag.PIRATE) && !retVal.is(Tag.MERC) && !retVal.is(Tag.TRADER)) {
            // Planet checks
            if(retVal.startingPlanet.length < Era.E_NUM) {
                MekHQ.getLogger().log(Faction.class, METHOD_NAME, LogLevel.WARNING,
                        retVal.fullname + " faction did not have a long enough startingPlanet vector"); //$NON-NLS-1$
            }
        }

        return retVal;
    }

    public static void generateFactions() throws DOMException, ParseException {
        final String METHOD_NAME = "generateFactions()";
        
        MekHQ.getLogger().log(Faction.class, METHOD_NAME, LogLevel.INFO,
                "Starting load of faction data from XML..."); //$NON-NLS-1$
        // Initialize variables.
        factions = new HashMap<>();
        factionIdMap = new HashMap<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document xmlDoc = null;


        try(FileInputStream fis = new FileInputStream("data/universe/factions.xml")) {
            // Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.getLogger().log(Faction.class, METHOD_NAME, ex);
        }

        Element factionEle = xmlDoc.getDocumentElement();
        NodeList nl = factionEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML.  At least this cleans it up.
        factionEle.normalize();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getParentNode() != factionEle)
                continue;

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("faction")) {
                    Faction f = getFactionFromXML(wn);
                    if(!factions.containsKey(f.getShortName())) {
                        factions.put(f.getShortName(), f);
                        if(null != f.getId()) {
                            if(!factionIdMap.containsKey(f.getId())) {
                                factionIdMap.put(f.getId(), f);
                            } else {
                                MekHQ.getLogger().log(Faction.class, METHOD_NAME, LogLevel.ERROR,
                                        String.format("Faction id \"%d\" already used for faction %s, can't re-use it for %s", //$NON-NLS-1$
                                                f.getId().intValue(), factionIdMap.get(f.getId()).getFullName(0),
                                                f.getFullName(0)));
                            }
                        }
                    } else {
                        MekHQ.getLogger().log(Faction.class, METHOD_NAME, LogLevel.ERROR,
                                String.format("Faction code \"%s\" already used for faction %s, can't re-use it for %s", //$NON-NLS-1$
                                        f.getShortName(), factions.get(f.getShortName()).getFullName(0), f.getFullName(0)));
                    }
                } else if (xn.equalsIgnoreCase("choosableFactionCodes")) {
                    choosableFactionCodes = wn.getTextContent().split(",");
                }
            }
        }
        MekHQ.getLogger().log(Faction.class, METHOD_NAME, LogLevel.INFO,
                "Loaded a total of " + factions.size() + " factions"); //$NON-NLS-1$
    }

    /** @return Sorted list of faction names as one string */
    public static String getFactionNames(Collection<Faction> factions, int era) {
        if( null == factions ) {
            return "-"; //$NON-NLS-1$
        }
        List<String> factionNames = new ArrayList<>(factions.size());
        for(Faction f : factions) {
            factionNames.add(f.getFullName(era));
        }
        Collections.sort(factionNames);
        return Utilities.combineString(factionNames, "/"); //$NON-NLS-1$
    }

    public static enum Tag {
        /** Inner sphere */
        IS, PERIPHERY, CLAN,
        /** A bunch of dirty pirates */
        PIRATE,
        /** Major mercenary bands */
        MERC,
        /** Major trading company */
        TRADER,
        /** Faction is limited to a single star system, or potentially just a part of a planet */
        MINOR,
        /** Faction is rebelling against the superior ("parent") faction */
        REBEL,
        /** Faction isn't overtly acting on the political/military scale; think ComStar before clan invasion */
        INACTIVE,
        /** Faction represents empty space */
        ABANDONED,
        /** Faction represents a lack of unified government */
        CHAOS,
        /** Faction is campaign-specific, generated on the fly */
        GENERATED,
        /** Faction is hidden from view */
        HIDDEN
    }
}
