/*
 * Faction.java
 * 
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

package mekhq.campaign;

import java.awt.Color;
import java.io.FileInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.EquipmentType;
import mekhq.MekHQ;
import mekhq.campaign.parts.Part;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Faction {
	
	public static Hashtable<String, Faction> factions;
	public static String[] choosableFactionCodes = {"MERC","CC","DC","FS","FWL","LA","FC","ROS","CS","WOB","FRR","SIC","MOC","MH","OA","TC","CDS","CGB","CHH","CJF","CNC","CSJ","CSV","CW","TH","RWR"};
	
	//I am no longer using ints to define factions, but I am keeping 
	//this stuff for reference
	public static final int F_MERC      = 0;
	public static final int F_CAPCON    = 1;
	public static final int F_DRAC      = 2;
	public static final int F_FEDSUN    = 3;
	public static final int F_FWL       = 4;
	public static final int F_LYRAN     = 5;
	public static final int F_FEDCOM    = 6;
	public static final int F_ROS       = 7;
	public static final int F_COMSTAR   = 8;
	public static final int F_WOB       = 9;
	public static final int F_FRR       = 10;
	public static final int F_SIC       = 11;
	public static final int F_CANOPUS   = 12;
	public static final int F_OA        = 13;
	public static final int F_TC        = 14;
	public static final int F_MH        = 15;
	public static final int F_CHAOS     = 16;
	public static final int F_ARDC      = 17;
	public static final int F_TERRAN    = 18;
	public static final int F_RWR       = 19;
	public static final int F_PERIPHERY = 20;
	public static final int F_C_WOLF    = 21;
	public static final int F_C_JF      = 22;
	public static final int F_C_GB      = 23;
	public static final int F_C_SJ      = 24;
	public static final int F_C_NC      = 25;
	public static final int F_C_DS      = 26;
	public static final int F_C_SV      = 27;
	public static final int F_C_HH      = 28;
	public static final int F_C_OTHER   = 29;
	public static final int F_NUM       = 30;    
	
	private String shortname;
	private String fullname;
	private String[] altNames;
	private Color color;
	private String nameGenerator;
	private boolean clan;
	private boolean periphery;
	private String[] startingPlanet;
	private int[] eraMods;
	
	public Faction() {
		this("???", "Unknown");
	}
	
	public Faction(String sname, String fname) {
		shortname = sname;
		fullname = fname;
		nameGenerator = "General";
		clan = false;
		periphery = false;
		color = Color.LIGHT_GRAY;
		startingPlanet = new String[]{"Terra","Terra","Terra","Terra","Terra","Terra","Terra","Terra","Terra"};
		altNames = new String[]{"","","","","","","","",""};
		eraMods = new int[]{0,0,0,0,0,0,0,0,0};
	}

	public String getShortName() {
		return shortname;
	}
	
	public String getFullName(int era) {
		String alt = altNames[era];
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
		return clan;
	}
	
	public boolean isPeriphery() {
		return periphery;
	}
	
	public String getNameGenerator() {
		return nameGenerator;
	}
	
	public String getStartingPlanet(int era) {
		return startingPlanet[era];
	}
	
	public int getEraMod(int era) {
		return eraMods[era];
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
	
	public static ArrayList<String> getFactionList() {
		ArrayList<String> flist = new ArrayList<String>();
		for(String sname : factions.keySet()) {
			flist.add(sname);
		}
		return flist;
	}
	
	public static Faction getFaction(String sname) {
		return factions.get(sname);
	}
	
    public static String getFactionCode(int faction) {
        switch(faction) {
            case F_MERC:
                return "MERC";
            case F_CAPCON:
                return "CC";
            case F_DRAC:
                return "DC";
            case F_FEDSUN:
                return "FS";
            case F_FWL:
                return "FWL";
            case F_LYRAN:
                return "LA";
            case F_FEDCOM:
                return "FC"; 
            case F_ROS:
            	return "ROS";
            case F_COMSTAR:
                return "CS";
            case F_WOB:
                return "WOB";
            case F_FRR:
                return "FRR";
            case F_SIC:
                return "SIC";
            case F_CANOPUS:
                return "MOC";
            case F_OA:
                return "OA";
            case F_TC:
                return "TC";
            case F_MH:
                return "MH";
            case F_PERIPHERY:
                return "PIND";
            case F_ARDC:
                return "ARDC";          
            case F_CHAOS:
                return "IND";
            case F_TERRAN:
                return "TH";
            case F_RWR:
                return "RWR";
            case F_C_WOLF:
                return "CW";
            case F_C_JF:
                return "CJF";
            case F_C_GB:
            	return "CGB";
            case F_C_SJ:
            	return "CSJ";
            case F_C_NC:
                return "CNC";
            case F_C_DS:
                return "CDS";
            case F_C_SV:
                return "CSV";
            case F_C_HH:
                return "CHH";
            case F_C_OTHER:
            default:
                return "IND";
        }
    }
    
    public static Faction getFactionFromXML(Node wn) throws DOMException, ParseException {
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
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.clan = true;
				else
					retVal.clan = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("periphery")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.periphery = true;
				else
					retVal.periphery = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("startingPlanet")) {
				retVal.startingPlanet = wn2.getTextContent().split(",");
			} else if (wn2.getNodeName().equalsIgnoreCase("altNames")) {
				retVal.altNames = wn2.getTextContent().split(",");
			} else if (wn2.getNodeName().equalsIgnoreCase("eraMods")) {
				String[] values = wn2.getTextContent().split(",");
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
			} 
		}
		return retVal;
	}
    
    public static void generateFactions() throws DOMException, ParseException {
		MekHQ.logMessage("Starting load of faction data from XML...");
		// Initialize variables.
		factions = new Hashtable<String, Faction>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;
	
		
		try {
			FileInputStream fis = new FileInputStream("data/universe/factions.xml");
			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
	
			// Parse using builder to get DOM representation of the XML file
			xmlDoc = db.parse(fis);
		} catch (Exception ex) {
			MekHQ.logError(ex);
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
					factions.put(f.getShortName(), f);
				} else if (xn.equalsIgnoreCase("choosableFactionCodes")) {
					choosableFactionCodes = wn.getTextContent().split(",");
				}
			}
		}	
		MekHQ.logMessage("Loaded a total of " + factions.keySet().size() + " factions");
	}

    
}
