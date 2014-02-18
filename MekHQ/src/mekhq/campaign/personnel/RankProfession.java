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
 * Ranks by Profession
 * @author dmyers
 */
public class RankProfession {
	// Rank Profession Codes
	public static final int RPROF_MW		= 0;
	public static final int RPROF_ASF		= 1;
	public static final int RPROF_VEE		= 2;
	public static final int RPROF_NAVAL	= 3;
	public static final int RPROF_INF		= 4;
	public static final int RPROF_TECH	= 5;
	public static final int RPROF_NUM		= 6;
	
	private int profession;
	private ArrayList<Rank> ranks;
	
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
	
	private static final int[][][] rankLevels = {
		{ /* SLDF */
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* AFFS */ 
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* AFFC */ 
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* LCAF */
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* LAAF */
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* FWLM */
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* CCAF */
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* CCWH */
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* DCMS */
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* CLAN */
			/* MW    */ {},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* ComStar */
			/* MW    */ {0,10,10,10,10,0,0},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		},
		{ /* WoB */
			/* MW    */ {0,10,10,10,10,0},
			/* ASF   */ {},
			/* VEE   */ {},
			/* NAVAL */ {},
			/* INF   */ {},
			/* TECH  */ {}
		}
	};
	
	public RankProfession() {
		this(0,0);
	}
	
	public RankProfession(int system, int profession) {
		this.profession = profession;
		buildRanksForProfession(system, this.profession);
	}
	
	public int getProfession() {
		return profession;
	}
	
	public void setProfession(int s, int p) {
		profession = p;
	}
	
	public String getRankProfessionName() {
		return getRankProfessionName(profession);
	}
	
	public static String getRankProfessionName(int profession) {
		switch(profession) {
			case RPROF_MW:
				return "MechWarriors";
			case RPROF_ASF:
				return "Aerospace Pilots";
			case RPROF_VEE:
				return "Vehicle Crewmembers";
			case RPROF_NAVAL:
				return "Naval Personnel";
			case RPROF_INF:
				return "Infantry Personnel";
			case RPROF_TECH:
				return "Technical and Support Staff";
			default:
				return "Unknown Profession Type";
		}
	}
	
	public void buildRanksForProfession(int system, int profession) {
		ranks = new ArrayList<Rank>();
		if(profession >= rankSystems[system].length) {
			ranks.add(new Rank());
			return;
		}
		// We don't build for zero length professions...
		// getRank() will return from the MW table
		if (rankSystems[system][profession].length == 0) {
			return;
		}
		// Always based off length of this system's MechWarrior array
		for (int i = 0; i < rankSystems[system][profession].length; i++) {
			int level = rankLevels[system][profession].length == 0 ? 0 : rankLevels[system][profession][i];
			ranks.add(new Rank(rankSystems[system][profession][i], officerCut[system] <= i,  1.0, level));
		}
	}
	
	public static String[][][] getRankSystems() {
		return rankSystems;
	}
	
	public static String[][] getRankProfessionsForSystem(int system) {
		return rankSystems[system];
	}
	
	public void setCustomRanks(ArrayList<String> customRanks, int offCut) {
	    ranks = new ArrayList<Rank>();
        for (int i = 0; i < customRanks.size(); i++) {
            ranks.add(new Rank(customRanks.get(i), offCut <= i,  1.0));
        }
	}
	
	public void setCustomRanks(ArrayList<Rank> customRanks) {
	    ranks = new ArrayList<Rank>();
        for (int i = 0; i < customRanks.size(); i++) {
            ranks.add(customRanks.get(i));
        }
	}
	
	public Rank getRank(int r) {
		// Assign the highest rank
		if(r >= ranks.size()) {
			r = ranks.size() - 1;
		}
		
		// If we don't have a rank now, we'll return a fake "-" entry
		if (r == -1) {
			return new Rank("-", false, 1.0);
		}
		
		// Hey! We've actually got a rank!
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
	
	public int getRankOrder(String rank) {
		if (rank.equals("Prisoner")) {
			return -2;
		}
		if (rank.equals("Bondsman")) {
			return -1;
		}
		for(int i = 0; i < ranks.size(); i++) {
		    if(ranks.get(i).getName().equals(rank)) {
		        return i;
		    }
		}
		return 0;
	}
	
	public ArrayList<Rank> getAllRanksForProfession() {
		return ranks;
	}
	
	public String getRankNameList() {
		String rankNames = "";
		int i = 0;
		for(Rank rank : getAllRanksForProfession()) {
			rankNames += rank.getName();
			i++;
			if(i < getAllRanksForProfession().size()) {
				rankNames += ",";
			}
		}
		return rankNames;
	}
	
	//Keep this for reverse compatability in loading campaigns
	public void setRanksFromList(String names, int officerCut) {
		ArrayList<String> rankNames = new ArrayList<String>();
		String[] rnames = names.split(",");
		for(String rname : rnames) {
			rankNames.add(rname);
		}
		setCustomRanks(rankNames, officerCut);
	}
	
	
	public void writeToXml(PrintWriter pw1, int indent, boolean custom) {
        pw1.println(MekHqXmlUtil.indentStr(indent)+"<rankProfessions>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<rankProfession>"
                +profession
                +"</rankProfession>");
        if (custom) {
	        for(Rank r : ranks) {
	            r.writeToXml(pw1, indent+1);
	        }
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</rankProfessions>");
    }
	
	public static RankProfession generateInstanceFromXML(Node wn) {
        RankProfession retVal = new RankProfession();
        retVal.ranks = new ArrayList<Rank>();
        retVal.profession = RPROF_MW;
        
        try {  
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("profession")) {
                	retVal.profession = Integer.parseInt(wn2.getTextContent());
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
	
	public Object[][] getRanksArray() {
        Object[][] array = new Object[ranks.size()][4];
        int i = 0;
        for(Rank rank : ranks) {
            array[i][0] = rank.getName();
            array[i][1] = rank.isOfficer();
            array[i][2] = rank.getPayMultiplier();
            array[i][3] = rank.getLevels();
            i++;
        }
        return array;
    }
	
	public void setRanksFromModel(RankTableModel model) {
        ranks = new ArrayList<Rank>();
	    Vector<Vector> vectors = model.getDataVector();
	    for(Vector<Object> row : vectors) {
	        String name = (String)row.get(0);
	        Boolean officer = (Boolean)row.get(1);
            double payMult = (Double)row.get(2);
            int levels = (Integer)row.get(3);
	        ranks.add(new Rank(name, officer, payMult, levels));
	    }
	}
}
