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
import java.util.HashMap;

import javax.swing.JTable;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
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
	
	// Prisoners and Bondsmen, for sorting
	public static final int RANK_BONDSMAN = -1;
	public static final int RANK_PRISONER = -2;
	
	private int rankSystem;
	private ArrayList<RankProfession> rankProfessions;
	
	public static String[] getRankSystem(int profession, int system) {
		// Default to MechWarrior if there is no system built for this
		if (RankProfession.getRankProfessionsForSystem(system)[profession].length == 0) {
			return RankProfession.getRankProfessionsForSystem(system)[RankProfession.RPROF_MW];
		}
		return RankProfession.getRankProfessionsForSystem(system)[profession];
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
	
	public ArrayList<RankProfession> getAllRankProfessions() {
		return rankProfessions;
	}
	
	public void useRankSystem(int system) {
		rankProfessions = new ArrayList<RankProfession>();
		if(system >= RankProfession.getRankSystems().length) {
			rankProfessions.add(new RankProfession());
			return;
		}
		for (int i = 0; i < RankProfession.getRankProfessionsForSystem(system).length; i++) {
			rankProfessions.add(new RankProfession(system, i));
		}
	}
	
	public void setCustomRanks(ArrayList<ArrayList> customRanks, int offCut) {
	    rankProfessions = new ArrayList<RankProfession>();
	    for (int i = 0; i < RankProfession.RPROF_NUM; i++) {
	    	RankProfession profession = new RankProfession();
	    	profession.setProfession(rankSystem, i);
	    	if (!customRanks.get(i).isEmpty() && customRanks.get(i).get(0) instanceof Rank) {
	    		profession.setCustomRanks(customRanks.get(i));
	    	} else {
	    		profession.setCustomRanks(customRanks.get(i), offCut);
	    	}
	    	rankProfessions.add(profession);
	    }
		rankSystem = RS_CUSTOM;
	}
	
	public Rank getRank(int rankNum, int profession) {
		if(profession >= rankProfessions.size()) {
		    // build off MW table instead...
		    profession = RankProfession.RPROF_MW;
		}
		if (rankNum == RANK_BONDSMAN) { // Bondsman
			return new Rank("Bondsman", false, 0.0);
		}
		if (rankNum == RANK_PRISONER) { // Prisoners
            return new Rank("Prisoner", false, 0.0);
		}
		Rank rank = rankProfessions.get(profession).getRank(rankNum);
		if (rank.getName().equals("-")) {
			rank = getRank(rankNum, RankProfession.RPROF_MW);
		} else if (rank.getName().equals("")) {
			rank = getRank(rankNum-1, profession);
		}
		return rank;
	}
	
	public int getOfficerCut() {
	    return rankProfessions.get(0).getOfficerCut();
	}
	
	// Get rank order from a known profession
	public int getRankOrder(String rank, int profession) {
		if (rank.equals("Prisoner")) {
			return -2;
		}
		if (rank.equals("Bondsman")) {
			return -1;
		}
		
		return rankProfessions.get(profession).getRankOrder(rank);
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
		return rankProfessions.get(profession).getRankNameList();
	}
	
	//Keep this for reverse compatability in loading campaigns
	public void setRanksFromList(String names, int officerCut) {
		ArrayList<String> rankNames = new ArrayList<String>();
		ArrayList<String> emptyRankNames = new ArrayList<String>(); // Hack for the profession ranks
		String[] rnames = names.split(",");
		for(String rname : rnames) {
			rankNames.add(rname);
		}
		
		rankProfessions = new ArrayList<RankProfession>();
		for (int i = 0; i < RankProfession.RPROF_NUM; i++) {
			RankProfession p = new RankProfession();
			p.setProfession(rankSystem, i);
			if (i == RankProfession.RPROF_MW) {
				p.setCustomRanks(rankNames, officerCut);
			} else {
				p.setCustomRanks(emptyRankNames, officerCut);
			}
		}
	}
	
	
	public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<ranks>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<rankSystem>"
                +rankSystem
                +"</rankSystem>");
        for(RankProfession p : rankProfessions) {
            p.writeToXml(pw1, indent+1, rankSystem == RS_CUSTOM);
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</ranks>");
    }
	
	public static Ranks generateInstanceFromXML(Node wn, Version version) {
        Ranks retVal = new Ranks();
        retVal.rankProfessions = new ArrayList<RankProfession>();
        retVal.rankSystem = RS_SL;
        ArrayList<Rank> ranks = new ArrayList<Rank>();
        
        try {  
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("rankSystem")) {
                	retVal.rankSystem = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("rankProfessions")) {
                	retVal.rankProfessions.add(RankProfession.generateInstanceFromXML(wn2));
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
        
        if ((version.getMajorVersion() < 1 && version.getMinorVersion() < 4 && version.getSnapshot() < 4)
        			|| (version.getRevision() != -1 && version.getRevision() < 1761)) {
        	if (retVal.rankSystem > RS_FS)
        		retVal.rankSystem += 2;
        	if (retVal.rankSystem > RS_CC)
        		retVal.rankSystem += 1;
        	if (retVal.rankSystem > RS_CL)
        		retVal.rankSystem += 2;
        }
        
        if (retVal.rankSystem == RS_CUSTOM && !ranks.isEmpty()) {
        	int offCut = 0;
        	for (int c = 0; c < ranks.size(); c++) {
        		if (ranks.get(c).isOfficer()) {
        			offCut = c;
        			break;
        		}
        	}
        	ArrayList customRanks = new ArrayList<ArrayList>();
        	for (int i = 0; i < RankProfession.RPROF_NUM; i++) {
        		if (i == RankProfession.RPROF_MW) {
        			customRanks.add(ranks);
        		} else {
        			customRanks.add(new ArrayList<Rank>());
        		}
        	}
        	retVal.setCustomRanks(customRanks, offCut);
        }
        
        // Empty rank system protection...
        if (retVal.rankProfessions.isEmpty()) {
        	retVal.useRankSystem(retVal.rankSystem);
        }
        
        return retVal;
    }
	
	public Object[][] getRanksArray(int profession) {
        return rankProfessions.get(profession).getRanksArray();
    }
	
	public void setRanksFromTableMap(HashMap<Integer,JTable> rankTableMap) {
        rankProfessions = new ArrayList<RankProfession>();
	    for(int i = 0; i < RankProfession.RPROF_NUM; i++) {
	    	RankTableModel model = (RankTableModel) rankTableMap.get(i).getModel();
	        RankProfession p = new RankProfession();
	        p.setRanksFromModel(model);
	        p.setProfession(rankSystem, i);
	        rankProfessions.add(p);
	    }
	}
	
}