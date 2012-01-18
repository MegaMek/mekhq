/*
 * IFaction.java
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Faction {
	
	public static Hashtable<String, Faction> factions;
	public static final String[] choosableFactionCodes = {"MERC","CC","DC","FS","FWL","LA","FC","ROS","CS","WOB","FRR","SIC","MOC","MH","OA","TC","CDS","CGB","CHH","CJF","CNC","CSJ","CSV","CW","TH","RWR"};
	
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
		eraMods = new int[]{0,0,0,0,0,0,0,0,0};
	}

	public String getShortName() {
		return shortname;
	}
	
	public String getFullName() {
		return fullname;
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
        
        // Change to reflect current location
        // NO, NO, NO - we are not ready to assign a location to the campaign
        // and assuming faction=location is not an acceptable substitute
        //int currentLocation = currentFaction;

        int factionMod = 0;
        if (part.isClanTechBase() && !isClan()) {
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
        
        /*
        if (!part.isClanTechBase()) {
            // Availability of high tech rating equipment in low tech areas (periphery)
            switch (techRating) {
                case(EquipmentType.RATING_E) :
                    if (Faction.isPeripheryFaction(currentLocation))
                        factionMod += 1;
                    break;
                case(EquipmentType.RATING_F) :
                    if (Faction.isPeripheryFaction(currentLocation))
                        factionMod += 2;
                    break;
            }
        }
        */

        return factionMod;
	}
	
	public static void generateFactions() {
		//TODO: I should put factions in an editable XML file
		factions = new Hashtable<String, Faction>();
		Faction faction;
		faction = new Faction("MERC", "Mercenary");
		faction.startingPlanet = new String[]{"Galatea","Solaris VII","Galatea","Galatea","Galatea","Galatea","Outreach","Outreach","Outreach"};
		faction.eraMods = new int[]{1,1,1,1,2,3,2,1,0};
		addFaction(faction);
		faction = new Faction("CC", "Cappellan Confederation");
		faction.color = Color.GREEN;
		faction.nameGenerator = "CC";
		faction.startingPlanet = new String[]{"Sian","Sian","Sian","Sian","Sian","Sian","Sian","Sian","Sian"};
		faction.eraMods = new int[]{1,0,0,1,2,3,2,1,0};
		addFaction(faction);
		faction = new Faction("DC", "Draconis Combine");
		faction.color = Color.red;
		faction.nameGenerator = "DC";
		faction.startingPlanet = new String[]{"New Samarkand","Luthien","Luthien","Luthien","Luthien","Luthien","Luthien","Luthien","Luthien"};
		faction.eraMods = new int[]{0,0,0,1,2,2,1,0,0};
		addFaction(faction);
		faction = new Faction("FS", "Federated Suns");
		faction.color = Color.yellow;
		faction.nameGenerator = "FS";
		faction.startingPlanet = new String[]{"New Avalon","New Avalon","New Avalon","New Avalon","New Avalon","New Avalon","New Avalon","New Avalon","New Avalon"};
		faction.eraMods = new int[]{0,0,0,1,2,2,1,0,0};
		addFaction(faction);
		faction = new Faction("FWL", "Free Worlds League");
		faction.color = new Color(160,32,240);
		faction.nameGenerator = "FWL";
		faction.startingPlanet = new String[]{"Atreus (Free Worlds League)","Atreus (Free Worlds League)","Atreus (Free Worlds League)","Atreus (Free Worlds League)","Atreus (Free Worlds League)","Atreus (Free Worlds League)","Atreus (Free Worlds League)","Atreus (Free Worlds League)","Atreus (Free Worlds League)"};
		faction.eraMods = new int[]{0,0,0,1,2,3,1,1,0};
		addFaction(faction);
		faction = new Faction("LA", "Lyran Alliance/Commonwealth");
		faction.color = Color.blue;
		faction.nameGenerator = "LA";
		faction.startingPlanet = new String[]{"Tharkad","Tharkad","Tharkad","Tharkad","Tharkad","Tharkad","Tharkad","Tharkad","Tharkad"};
		faction.eraMods = new int[]{0,0,0,1,2,3,2,0,0};
		addFaction(faction);
		faction = new Faction("FC", "Federated Commonwealth");
		faction.color = new Color(255,215,0);
		faction.startingPlanet = new String[]{"New Avalon","New Avalon","New Avalon","New Avalon","New Avalon","New Avalon","New Avalon","New Avalon","New Avalon"};
		faction.eraMods = new int[]{0,0,0,1,2,2,1,0,0};
		addFaction(faction);
		faction = new Faction("ROS", "Republic of the Sphere");
		faction.color = Color.ORANGE;
		addFaction(faction);
		faction = new Faction("CS", "Comstar");
		faction.color = Color.WHITE;
		faction.startingPlanet = new String[]{"Terra","Terra","Terra","Terra","Terra","Terra","Terra","Tukayyid","Tukayyid"};
		addFaction(faction);
		faction = new Faction("WOB", "Word of Blake");
		faction.color = new Color(205,192,176);
		addFaction(faction);
		faction = new Faction("FRR", "Free Rasalhague Republic");
		faction.color = new Color(148,0,11);
		faction.nameGenerator = "FRR";
		faction.startingPlanet = new String[]{"Rasalhague","Rasalhague","Rasalhague","Rasalhague","Rasalhague","Rasalhague","Rasalhague","Tukayyid","Tukayyid"};
		faction.eraMods = new int[]{0,0,0,0,0,0,2,1,0};
		addFaction(faction);
		faction = new Faction("SIC", "St. Ives Compact");
		faction.color = new Color(0,250,154);
		faction.startingPlanet = new String[]{"St. Ives","St. Ives","St. Ives","St. Ives","St. Ives","St. Ives","St. Ives","St. Ives","St. Ives"};
		faction.eraMods = new int[]{1,0,0,1,2,3,2,1,0};
		addFaction(faction);
		faction = new Faction("MOC", "Magistracy of Canopus");
		faction.color = new Color(34,139,34);
		faction.periphery = true;
		faction.startingPlanet = new String[]{"Canopus IV","Canopus IV","Canopus IV","Canopus IV","Canopus IV","Canopus IV","Canopus IV","Canopus IV","Canopus IV"};
		faction.eraMods = new int[]{1,1,1,1,2,3,2,1,1};
		addFaction(faction);
		faction = new Faction("OA", "Outworlds Alliance");
		faction.color = Color.CYAN;
		faction.periphery = true;
		faction.startingPlanet = new String[]{"Alpheratz","Alpheratz","Alpheratz","Alpheratz","Alpheratz","Alpheratz","Alpheratz","Alpheratz","Alpheratz"};
		faction.eraMods = new int[]{1,1,1,1,2,3,2,1,0};
		addFaction(faction);
		faction = new Faction("TC", "Taurian Concordat");
		faction.color = new Color(205,133,63);
		faction.startingPlanet = new String[]{"Taurus","Taurus","Taurus","Taurus","Taurus","Taurus","Taurus","Taurus","Taurus"};
		faction.periphery = true;
		faction.eraMods = new int[]{1,1,1,1,2,3,2,1,0};
		addFaction(faction);
		faction = new Faction("MH", "Marian Hegemony");
		faction.color = new Color(0,206,209);
		faction.periphery = true;
		faction.startingPlanet = new String[]{"Alphard (Independent)","Alphard (Independent)","Alphard (Independent)","Alphard (Independent)","Alphard (Independent)","Alphard (Independent)","Alphard (Independent)","Alphard (Independent)","Alphard (Independent)"};
		faction.eraMods = new int[]{1,1,1,1,2,3,2,2,1};
		addFaction(faction);
		faction = new Faction("PIND", "Independent (Periphery)");
		faction.color = Color.GRAY;
		faction.periphery = true;
		faction.eraMods = new int[]{1,1,1,1,2,3,2,2,1};
		addFaction(faction);
		faction = new Faction("ARDC", "Arc-Royal Defense Cordon");
		faction.color = new Color(218,165,32);
		faction.startingPlanet = new String[]{"Arc-Royal","Arc-Royal","Arc-Royal","Arc-Royal","Arc-Royal","Arc-Royal","Arc-Royal","Arc-Royal","Arc-Royal"};
		faction.eraMods = new int[]{0,0,0,1,2,3,2,0,0};
		addFaction(faction);
		faction = new Faction("IND", "Independent");
		faction.color = Color.GRAY;
		faction.eraMods = new int[]{1,1,1,1,2,3,2,2,1};
		addFaction(faction);
		faction = new Faction("TH", "Terran Hegemony");
		faction.color = Color.WHITE;
		faction.eraMods = new int[]{-1,-1,-1,0,0,0,0,0,0};
		addFaction(faction);
		faction = new Faction("RWR", "Rim Worlds Republic");
		faction.color = new Color(205,192,176);
		faction.eraMods = new int[]{1,1,1,0,0,0,0,0,0};
		addFaction(faction);
		faction = new Faction("CW", "Clan Wolf");
		faction.color = new Color(139,69,19);
		faction.clan = true;
		faction.nameGenerator = "Clan";
		faction.startingPlanet = new String[]{"Tamar","Tamar","Tamar","Tamar","Tamar","Tamar","Tamar","Tamar","Tamar"};
		addFaction(faction);
		faction = new Faction("CJF", "Clan Jade Falcon");
		faction.color = new Color(154, 205, 50);
		faction.clan = true;
		faction.nameGenerator = "Clan";
		faction.startingPlanet = new String[]{"Sudeten","Sudeten","Sudeten","Sudeten","Sudeten","Sudeten","Sudeten","Sudeten","Sudeten"};
		addFaction(faction);
		faction = new Faction("CGB", "Clan Ghost Bear");
		faction.color = new Color(135,206,250);
		faction.clan = true;
		faction.nameGenerator = "Clan";
		faction.startingPlanet = new String[]{"Alshain","Alshain","Alshain","Alshain","Alshain","Alshain","Alshain","Alshain","Alshain"};
		addFaction(faction);
		faction = new Faction("CSJ", "Clan Smoke Jaguar");
		faction.color = new Color(119,136,153);
		faction.clan = true;
		faction.nameGenerator = "Clan";
		addFaction(faction);
		faction = new Faction("CNC", "Clan Nova Cat");
		faction.color = new Color(238,221,130);
		faction.clan = true;
		faction.nameGenerator = "Clan";
		faction.startingPlanet = new String[]{"Irece","Irece","Irece","Irece","Irece","Irece","Irece","Irece","Irece"};
		addFaction(faction);
		faction = new Faction("CDS", "Clan Diamond Shark");
		faction.color = new Color(250,128,114);
		faction.clan = true;
		faction.nameGenerator = "Clan";
		faction.startingPlanet = new String[]{"Twycross","Twycross","Twycross","Twycross","Twycross","Twycross","Twycross","Twycross","Twycross"};
		addFaction(faction);
		faction = new Faction("CSV", "Clan Steel Viper");
		faction.color = new Color(188,143,143);
		faction.clan = true;
		faction.nameGenerator = "Clan";
		addFaction(faction);
		faction = new Faction("CHH", "Clan Hells Horses");
		faction.color = new Color(178,34,34);
		faction.clan = true;
		faction.nameGenerator = "Clan";
		faction.startingPlanet = new String[]{"Csesztreg","Csesztreg","Csesztreg","Csesztreg","Csesztreg","Csesztreg","Csesztreg","Csesztreg","Csesztreg"};
		addFaction(faction);
	}
	
	
	public static void addFaction(Faction f) {
		factions.put(f.getShortName(), f);
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
            case F_C_OTHER:
                return "CHH";
            default:
                return "IND";
        }
    }
    

    
}
