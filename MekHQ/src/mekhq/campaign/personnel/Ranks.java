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
import mekhq.gui.dialog.CampaignOptionsDialog.RankTableModel;

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
	
	//pre-fab rank systems
	public static final int RS_SL =  0;
	public static final int RS_FS =  1;
	public static final int RS_LA =  2;
	public static final int RS_FWL = 3;
	public static final int RS_CC =  4;
	public static final int RS_DC =  5;
	public static final int RS_CL =  6;
	public static final int RS_CUSTOM = 7;
	public static final int RS_NUM = 8;
	private static final String[][] rankSystems = {
		{"None","Recruit","Private","Corporal","Sergeant","Master Sergeant","Warrant Officer","Lieutenant","Captain","Major","Colonel","Lt. General","Major General","General","Commanding General"},
		{"None","Recruit","Private","Private, FC","Corporal","Sergeant","Sergeant-Major","Command Sergeant-Major","Cadet","Subaltern","Leftenant","Captain","Major","Leftenant Colonel","Colonel","Leftenant General","Major General","General","Marshal","Field Marshal","Marshal of the Armies"},
		{"None","Recruit","Private","Private, FC","Corporal","Senior Corporal","Sergeant","Staff Sergeant","Sergeant Major","Staff Sergeant Major","Senior Sergeant Major","Warrant Officer","Warrant Officer, FC","Senior Warrant Officer","Chief Warrant Officer","Cadet","Leutnant","First Leutnant","Hauptmann","Kommandant","Hauptmann-Kommandant","Leutnant-Colonel","Colonel","Leutnant-General","Hauptmann-General","Kommandant-General","General","General of the Armies","Archon"},
		{"None","Recruit","Private","Private, FC","Corporal","Sergeant","Staff Sergeant","Master Sergeant","Sergeant Major","Lieutenant","Captain","Force Commander","Lieutenant Colonel","Colonel","General","Marshal","Captain-General"},
		{"None","Shia-ben-bing","San-ben-bing","Si-ben-bing","Yi-si-ben-bing","Sao-wei","Sang-wei","Sao-shao","Zhong-shao","Sang-shao","Jiang-jun","Sang-jiang-jun"},
		{"None","Hojuhei","Heishi","Gunjin","Go-cho","Gunsho","Shujin","Kashira","Sho-ko","Chu-i","Tai-i","Sho-sa","Chu-sa","Tai-sa","Sho-sho","Tai-sho","Tai-shu","Gunji-no-Kanrei"},
		{"None","Point","Point Commander","Star Commander","Star Captain","Star Colonel","Galaxy Commander","Khan","ilKhan"}
	};
	private static final int[] officerCut = {7,8,11,9,5,9,3};
	public static final int RANK_BONDSMAN = -1;
	public static final int RANK_PRISONER = -2;
	
	private int rankSystem;
	private ArrayList<Rank> ranks;
	
	public static String[] getRankSystem(int system) {
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
		case RS_LA:
			return "Lyran Alliance";
		case RS_FWL:
			return "Free Worlds League";
		case RS_CC:
			return "Capellan Confederation";
		case RS_DC:
			return "Draconis Combine";
		case RS_CL:
			return "Clan";
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
	
	public ArrayList<Rank> getAllRanks() {
		return ranks;
	}
	
	public void useRankSystem(int system) {
		ranks = new ArrayList<Rank>();
		if(system >= rankSystems.length) {
			ranks.add(new Rank("Unknown", false, 1.0));
			return;
		}
		for (int i = 0; i < rankSystems[system].length; i++) {
			ranks.add(new Rank(rankSystems[system][i], officerCut[system] <= i,  1.0));
		}
	}
	
	public void setCustomRanks(ArrayList<String> customRanks, int offCut) {
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
			return new Rank("Bondsman", false, 0.0);
		}
		if (r == RANK_PRISONER) { // Prisoners
            return new Rank("Prisoner", false, 0.0);
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
	
	public int getRankSystem() {
		return rankSystem;
	}
	
	public void setRankSystem(int system) {
		if(system < RS_NUM) {
			rankSystem = system;
			useRankSystem(rankSystem);
		}	
	}
	
	public String getRankNameList() {
		String rankNames = "";
		int i = 0;
		for(Rank rank : getAllRanks()) {
			rankNames += rank.getName();
			i++;
			if(i < getAllRanks().size()) {
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
	
	
	public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<ranks>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<rankSystem>"
                +rankSystem
                +"</rankSystem>");
        for(Rank r : ranks) {
            r.writeToXml(pw1, indent+1);
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</ranks>");
    }
	
	public static Ranks generateInstanceFromXML(Node wn) {
        Ranks retVal = new Ranks();
        
        ArrayList<Rank> ranks = new ArrayList<Rank>();
        int rankSystem = RS_SL;
        
        try {  
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("rankSystem")) {
                    rankSystem = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
                    ranks.add(Rank.generateInstanceFromXML(wn2));
                } 
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.logError(ex);
        }
        
        retVal.rankSystem = rankSystem;
        retVal.ranks = ranks;
        
        return retVal;
    }
	
	public Object[][] getRanksArray() {
        Object[][] array = new Object[ranks.size()][3];
        int i = 0;
        for(Rank rank : ranks) {
            array[i][0] = rank.getName();
            array[i][1] = rank.isOfficer();
            array[i][2] = rank.getPayMultiplier();
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
	        ranks.add(new Rank(name, officer, payMult));
	    }
	}
	
}