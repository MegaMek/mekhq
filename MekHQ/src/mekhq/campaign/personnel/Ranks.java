/*
 * Ranks.java
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

package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.gui.model.RankTableModel;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This object will keep track of rank information. It will keep information
 * on a set of pre-fab rank structures and will hold info on the one chosen by the user.
 * It will also allow for the input of a user-created rank structure from a comma-delimited
 * set of names
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Ranks {
 	
	// Rank Faction Codes
	// TODO: Major periphery realms?
	public static final int RS_SL		= 0;
	public static final int RS_FS		= 1;
	public static final int RS_FC		= 2;
	public static final int RS_LC		= 3;
	public static final int RS_LA		= 4;
	public static final int RS_FWL		= 5;
	public static final int RS_CC		= 6;
	public static final int RS_CCWH		= 7;
	public static final int RS_DC		= 8;
	public static final int RS_CL		= 9;
	public static final int RS_COM		= 10;
	public static final int RS_WOB		= 11;
	public static final int RS_CUSTOM	= 12;
	public static final int RS_NUM		= 13;
	
	public static final int[] translateFactions = { /* 0 */ 0, /* 1 */ 1, /* 2 */ 4, /* 3 */ 5, /* 4 */ 6, /* 5 */ 8, /* 6 */ 9, /* 7 */ 12 };
	
	// Rank Size Codes
	// Enlisted
	public static final int RE_MIN	= 0; // Rank "None"
	public static final int RE_MAX	= 20;
	public static final int RE_NUM	= 21;
	// Warrant Officers
	public static final int RWO_MIN	= 21;
	public static final int RWO_MAX	= 30;
	public static final int RWO_NUM	= 31; // Number that comes after RWO_MAX
	// Officers
	public static final int RO_MIN	= 31;
	public static final int RO_MAX	= 50;
	public static final int RO_NUM	= 51; // Number that comes after RO_MAX
	// Total
	public static final int RC_NUM	= 51; // Same as RO_MAX
	
	// Rank Profession Codes
	public static final int RPROF_MW	= 0;
	public static final int RPROF_ASF	= 1;
	public static final int RPROF_VEE	= 2;
	public static final int RPROF_NAVAL	= 3;
	public static final int RPROF_INF	= 4;
	public static final int RPROF_TECH	= 5;
	public static final int RPROF_NUM	= 6;
	
	/*
	 * Stock Ranks by Profession & Faction
	 * Each profession must have either a 0 length entry for a faction, or the same length as the same faction under MW
	 * Hyphen entries ("-") are allowed, and will be filled in with the same position from the MW table
	 * Empty string (as in "") entries are also allowed, and will be skipped
	 */
	private static final String[][][] rankSystems = {
		{ /* SLDF */
			/* MW    */ {"None","Recruit","Private","Corporal","Sergeant","Master Sergeant","Lieutenant JG","Lieutenant SG","Captain","Major","Colonel","Lt. General","Major General","General","Commanding General"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Spaceman Recruit","Spaceman","Petty Officer","Chief PO","Master Chief PO","Warrant Officer","Lieutenant","Commander","Captain","Commodore","Rear Admiral","Vice Admiral","Admiral","Commanding Admiral"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* AFFS */
			/* MW    */ {"None","Recruit","Private","Corporal","Sergeant","Sergeant-Major","Subaltern","Leftenant","Captain","Major","Leftenant Colonel","Colonel","Leftenant General","Major General","General","Marshal","Field Marshal","Marshal of the Armies","Prince"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Recruit","Private","Corporal","Sergeant","Sergeant-Major","Subaltern","Leftenant","Captain","Major","Light Commodore","Commodore","Rear Admiral","Vice Admiral","Admiral","Fleet Admiral","-","-","-"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* AFFC */
			/* MW    */ {"None","Recruit","Private","Private, FC","Corporal","Sergeant","Sergeant-Major","Command Sergeant-Major","Cadet","Subaltern","Leftenant","Captain","Major","Leftenant Colonel","Colonel","Leftenant General","Major General","General","Marshal","Field Marshal","Marshal of the Armies","Prince"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Recruit","Private","Private, FC","Corporal","Sergeant","Sergeant-Major","Command Sergeant-Major","Cadet","Subaltern","Leftenant","Captain","Major","Light Commodore","Commodore","Rear Admiral","Vice Admiral","Admiral","Fleet Admiral","-","-","-"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* LCAF */
			/* MW    */ {"None","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Warrant Officer","Warrant Officer, FC","Senior Warrant Officer","Chief Warrant Officer","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Colonel","Colonel","Leutnant-General","Hauptmann-General","Kommandant-General","General","General of the Armies","Archon"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Warrant Officer","Warrant Officer, FC","Senior Warrant Officer","Chief Warrant Officer","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Kaptain","Kaptain","Leutnant-Kommodore","Kommodore","Hauptmann-Kommodore","Admiral","Fleet Admiral","-"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* LAAF */
			/* MW    */ {"None","Recruit","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Cadet","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Colonel","Colonel","Leutnant-General","Hauptmann-General","Kommandant-General","General","General of the Armies","Archon"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Recruit","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Cadet","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Kaptain","Kaptain","Leutnant-Kommodore","Kommodore","Hauptmann-Kommodore","Admiral","Fleet Admiral","-"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* FWLM */
			/* MW    */ {"None","Recruit","Private","Private, FC","Corporal","Sergeant","Staff Sergeant","Master Sergeant","Sergeant Major","Lieutenant JG","Lieutenant SG","Captain","Force Commander","Lieutenant Colonel","Colonel","General","Marshal","Captain-General"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Spaceman Recruit","Spaceman","Able Spaceman","Petty Officer 2nd","Petty Officer 1st","Chief Petty Officer","Senior Chief PO","Master Chief PO", "Ensign","Lieutenant","Lt. Commander","Commander","Captain","Commodore","Vice Admiral","Admiral","Fleet Admiral","-"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* CCAF */
			/* MW    */ {"None","Shia-ben-bing","San-ben-bing","Si-ben-bing","Yi-si-ben-bing","Sao-wei","Sang-wei","Sao-shao","Zhong-shao","Sang-shao","Jiang-jun","Sang-jiang-jun","Chancellor"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Shia-ben-bing","San-ben-bing","Si-ben-bing","Yi-si-ben-bing","Kong-sao-wei","Kong-sang-wei","Kong-sao-shao","Kong-zhong-shao","Kong-sang-shao","Kong-jiang-jun","","-"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* CCWH */
			/* MW    */ {"None","Zhang-si","Ban-zhang","Pai-zhang","Lien-zhang","Ying-zhang","Shiao-zhang","Gao-shiao-zhang"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* DCMS */
			/* MW    */ {"None","Hojuhei","Heishi","Gunjin","Go-cho","Gunsho","Shujin","Kashira","Sho-ko","Chu-i","Tai-i","Sho-sa","Chu-sa","Tai-sa","Sho-sho","Tai-sho","Tai-shu","Gunji-no-Kanrei","Coordinator"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Hojuhei","Heishi","Gunjin","Go-cho","Gunsho","Shujin","Kashira","Sho-ko","Sho-i","Chu-i","Dai-i","Sho-sa","Captain","Cho-sho","Tai-sho","Tai-shu","-","-"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* CLAN */
			/* MW    */ {"None","Point","Point Commander","Star Commander","Nova Commander","Star Captain","Nova Captain","Star Colonel","Galaxy Commander","saKhan","Khan","ilKhan"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {"None","Point","Point Commander","Star Commander","Nova Commander","Star Captain","Nova Captain","Star Commodore","Star Admiral","saKhan","Khan","ilKhan"},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* ComStar */
			/* MW    */ {"None","Acolyte","Adept","Demi-Precentor","Precentor","Precentor Martial","Primus"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* WoB */
			/* MW    */ {"None","Acolyte","Adept","Demi-Precentor","Precentor","Precentor Martial"},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		}
	};
	private static final int[] officerCut = {/*SLDF*/7,/*AFFS*/6,/*AFFC*/8,/*LCAF*/14,/*LAAF*/11,/*FWLM*/9,/*CCAF*/5,/*CCWH*/2,/*DCMD*/9,/*Clan*/3,/*COM*/2,/*WOB*/2};
	public static final int RANK_BONDSMAN = -1;
	public static final int RANK_PRISONER = -2;
	
	private int rankSystem;
	private ArrayList<Rank> ranks;
	
	public static String[][] getRankSystem(int system) {
		return rankSystems[system];
	}
	
	public static String getRankSystemName(int system) {
		switch(system) {
		case RS_CUSTOM:
			return "Custom";
		case RS_SL:
			return "Star League";
		case RS_FS:
			return "Federated Suns";
		case RS_FC:
			return "Federated Commonwealth";
		case RS_LC:
			return "Lyran Commonwealth";
		case RS_LA:
			return "Lyran Alliance";
		case RS_FWL:
			return "Free Worlds League";
		case RS_CC:
			return "Capellan Confederation";
		case RS_CCWH:
			return "Capellan Confederation Warrior House";
		case RS_DC:
			return "Draconis Combine";
		case RS_CL:
			return "Clan";
		case RS_COM:
			return "Comstar";
		case RS_WOB:
			return "Word of Blake";
		default:
			return "?";
		}
	}
	
	public Ranks() {
		this(RS_SL);
	}
	
	public Ranks(int system) {
		rankSystem = system;
		useRankSystem(rankSystem);
	}
    
    public boolean isEmptyProfession(int profession) {
    	// MechWarrior profession cannot be empty
    	if (profession == RPROF_MW)
    		return false;
    	
    	// Check the profession
    	for (int i = 0; i < RC_NUM; i++) {
    		// If our first Rank is an indicator of an alternate system, skip it.
    		if (i == 0 && ranks.get(0).getName(profession).startsWith("--")) {
    			continue;
    		}
    		
    		if (!ranks.get(i).getName(profession).equals("-")) {
    			return false;
    		}
    	}
    	
    	// It's empty...
    	return true;
    }
    
    public boolean useAlternateProfession(int profession) {
    	if (ranks.get(0).getName(profession).startsWith("--")) {
    		return true;
    	}
    	return false;
    }
    
    public int getAlternateProfession(int profession) {
    	return getAlternateProfession(ranks.get(0).getName(profession));
    }
    
    public int getAlternateProfession(String name) {
    	switch (name.replaceAll("--", "")) {
    		case "MW":
    			return RPROF_MW;
    		case "ASF":
    			return RPROF_ASF;
    		case "VEE":
    			return RPROF_VEE;
    		case "NAVAL":
    			return RPROF_NAVAL;
    		case "INF":
    			return RPROF_INF;
    		case "TECH":
    			return RPROF_TECH;
    		default:
    			return RPROF_MW;
    	}
    }
	
	public ArrayList<Rank> getAllRanks() {
		return ranks;
	}
	
	public void useRankSystem(int system) {
		ranks = new ArrayList<Rank>();
		if(system >= rankSystems.length) {
			ranks.add(new Rank());
			return;
		}
		for (int i = 0; i < rankSystems[system].length; i++) {
			ranks.add(new Rank(rankSystems[system][i], officerCut[system] <= i,  1.0));
		}
	}
	
	public void setCustomRanks(ArrayList<ArrayList<String>> customRanks, int offCut) {
	    ranks = new ArrayList<Rank>();
        for (int i = 0; i < customRanks.size(); i++) {
            ranks.add(new Rank(customRanks.get(i), offCut <= i,  1.0));
        }
		rankSystem = RS_CUSTOM;
	}
	
	public Rank getRank(int r) {
		if(r >= ranks.size()) {
		    //assign the highest rank
		    r = ranks.size() - 1;
		}
		if (r == RANK_BONDSMAN) { // Bondsman
			String[] bondsmen = { "Bondsman", "Bondsman", "Bondsman", "Bondsman", "Bondsman", "Bondsman" };
			return new Rank(bondsmen, false, 0.0);
		}
		if (r == RANK_PRISONER) { // Prisoners
			String [] prisoner = { "Prisoner", "Prisoner", "Prisoner", "Prisoner", "Prisoner", "Prisoner" };
            return new Rank(prisoner, false, 0.0);
		}
		return ranks.get(r);
	}
	
	public int getOfficerCut() {
	    for(int i = 0; i < ranks.size(); i++) {
            if(ranks.get(i).isOfficer()) {
                return i;
            }
        }
        return ranks.size() - 1;
	}
	
	public int getRankOrder(String rank, int profession) {
		if (rank.equals("Prisoner")) {
			return -2;
		}
		if (rank.equals("Bondsman")) {
			return -1;
		}
		for(int i = 0; i < ranks.size(); i++) {
		    if(ranks.get(i).getName(profession).equals(rank)) {
		        return i;
		    }
		}
		return 0;
	}
	
	public int getRankSystem() {
		return rankSystem;
	}
	
	public void setRankSystem(int system) {
		if(system < RS_NUM) {
			rankSystem = system;
			useRankSystem(rankSystem);
		}	
	}
	
	public String getRankNameList(int profession) {
		String rankNames = "";
		int i = 0;
		for(Rank rank : getAllRanks()) {
			rankNames += rank.getName(profession);
			i++;
			if(i < getAllRanks().size()) {
				rankNames += ",";
			}
		}
		return rankNames;
	}
	
	//Keep this for reverse compatability in loading campaigns
	public void setRanksFromList(String names, int officerCut) {
		ArrayList<ArrayList<String>> rankNames = new ArrayList<ArrayList<String>>();
		String[] rnames = names.split(",");
		for(String rname : rnames) {
			ArrayList<String> temp = new ArrayList<String>();
			for (int i = 0; i < RPROF_NUM; i++) {
				temp.add(rname);
			}
			rankNames.add(temp);
		}
		setCustomRanks(rankNames, officerCut);
	}
	
	private String getRankPostTag(int rankNum) {
		if (rankNum == 0)
			return " <!-- E0 \"None\" -->";
		if (rankNum < RE_NUM)
			return " <!-- E"+rankNum+" -->";
		if (rankNum < RWO_NUM)
			return " <!-- WO"+(rankNum-RE_MAX)+" -->";
		if (rankNum < RO_NUM)
			return " <!-- O"+(rankNum-RWO_MAX)+" -->";
		
		// Yuck, we've got nada!
		return "";
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<rankSystem>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)+"<!-- "+getRankSystemName(rankSystem)+" -->");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<system>"
                +rankSystem
                +"</system>");
        for(int i = 0; i < ranks.size(); i++) {
        	Rank r = ranks.get(i);
            r.writeToXml(pw1, indent+1);
            pw1.println(getRankPostTag(i));
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</rankSystem>");
    }
	
	public static Ranks generateInstanceFromXML(Node wn) {
        Ranks retVal = new Ranks();
        
        // Dump the ranks ArrayList so we can re-use it.
        retVal.ranks.clear();
        
        try {  
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("system")) {
                	retVal.rankSystem = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
                	retVal.ranks.add(Rank.generateInstanceFromXML(wn2));
                } 
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.logError(ex);
        }
        
        return retVal;
    }
	
	public Object[][] getRanksForModel() {
        Object[][] array = new Object[ranks.size()][RankTableModel.COL_NUM];
        int i = 0;
        for(Rank rank : ranks) {
            array[i][RankTableModel.COL_NAME_MW]	= rank.getName(RPROF_MW);
            array[i][RankTableModel.COL_NAME_ASF]	= rank.getName(RPROF_ASF);
            array[i][RankTableModel.COL_NAME_VEE]	= rank.getName(RPROF_VEE);
            array[i][RankTableModel.COL_NAME_NAVAL]	= rank.getName(RPROF_NAVAL);
            array[i][RankTableModel.COL_NAME_INF]	= rank.getName(RPROF_INF);
            array[i][RankTableModel.COL_NAME_TECH]	= rank.getName(RPROF_TECH);
            array[i][RankTableModel.COL_OFFICER] = rank.isOfficer();
            array[i][RankTableModel.COL_PAYMULT] = rank.getPayMultiplier();
            i++;
        }
        return array;
    }
	
	public void setRanksFromModel(RankTableModel model) {
        ranks = new ArrayList<Rank>();
	    Vector<Vector> vectors = model.getDataVector();
	    for(Vector<Object> row : vectors) {
	        String[] names = { (String)row.get(RankTableModel.COL_NAME_MW), (String)row.get(RankTableModel.COL_NAME_ASF),
	        		(String)row.get(RankTableModel.COL_NAME_VEE), (String)row.get(RankTableModel.COL_NAME_NAVAL),
	        		(String)row.get(RankTableModel.COL_NAME_INF), (String)row.get(RankTableModel.COL_NAME_TECH) };
	        Boolean officer = (Boolean)row.get(RankTableModel.COL_OFFICER);
            double payMult = (Double)row.get(RankTableModel.COL_PAYMULT);
	        ranks.add(new Rank(names, officer, payMult));
	    }
	}
}