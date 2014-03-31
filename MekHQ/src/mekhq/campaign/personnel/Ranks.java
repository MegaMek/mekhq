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

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.gui.model.RankTableModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
	public static final int RC_NUM	= 51; // Same as RO_MAX+1
	
	// Rank Profession Codes
	public static final int RPROF_MW	= 0;
	public static final int RPROF_ASF	= 1;
	public static final int RPROF_VEE	= 2;
	public static final int RPROF_NAVAL	= 3;
	public static final int RPROF_INF	= 4;
	public static final int RPROF_TECH	= 5;
	public static final int RPROF_NUM	= 6;
	
	private static Hashtable<Integer, Ranks> rankSystems;
	private static final int[] officerCut = {/*SLDF*/7,/*AFFS*/6,/*AFFC*/8,/*LCAF*/14,/*LAAF*/11,/*FWLM*/9,/*CCAF*/5,/*CCWH*/2,/*DCMD*/9,/*Clan*/3,/*COM*/2,/*WOB*/2};
	public static final int RANK_BONDSMAN = -1;
	public static final int RANK_PRISONER = -2;
	
	private int rankSystem;
	private ArrayList<Rank> ranks;
	
	public Ranks() {
		this(RS_SL);
	}
	
	public Ranks(int system) {
		rankSystem = system;
		useRankSystem(rankSystem);
	}
    
    public static void initializeRankSystems() {
        rankSystems = new Hashtable<Integer, Ranks>();
        MekHQ.logMessage("Starting load of Rank Systems from XML...");
        // Initialize variables.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document xmlDoc = null;
    
        
        try {
            FileInputStream fis = new FileInputStream("data/universe/ranks.xml");
            // Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
    
            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.logError(ex);
        }
    
        Element ranksEle = xmlDoc.getDocumentElement();
        NodeList nl = ranksEle.getChildNodes();
    
        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML.  At least this cleans it up.
        ranksEle.normalize(); 
    
        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            Ranks value;
            
            if (wn.getParentNode() != ranksEle)
                continue;
    
            int xc = wn.getNodeType();
    
            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();
    
                if (xn.equalsIgnoreCase("rankSystem")) {
                    value = generateInstanceFromXML(wn, null);
                    rankSystems.put(value.getRankSystem(), value);
                }
            }
        }   
        MekHQ.logMessage("Done loading Rank Systems");
    }
	
	public static Ranks getRanksFromSystem(int system) {
		return rankSystems.get(system);
	}
	
	public int getRankSystem() {
		return rankSystem;
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
		// If we've got an invalid rank system, default to Star League
		if(system >= rankSystems.size()) {
			if (rankSystems.isEmpty()) {
				ranks = new ArrayList<Rank>();
			} else {
				ranks = rankSystems.get(RS_SL).getAllRanks();
			}
			rankSystem = RS_SL;
			return;
		}
		ranks = rankSystems.get(system).getAllRanks();
		rankSystem = system;
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
	
	public static Ranks generateInstanceFromXML(Node wn, Version version) {
		Ranks retVal = new Ranks();
        
        // Dump the ranks ArrayList so we can re-use it.
        retVal.ranks = new ArrayList<Rank>();

        try {
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("system") || wn2.getNodeName().equalsIgnoreCase("rankSystem")) {
                	retVal.rankSystem = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
                	// If this is an older version from before the full blown rank system with professions
                	if (retVal.rankSystem != RS_CUSTOM && version != null
                			&& ((version.getMajorVersion() < 1 && (version.getMinorVersion() < 3
                        			|| (version.getMinorVersion() == 3 && version.getSnapshot() < 5)))
                        			|| (version.getRevision() != -1 && version.getRevision() < 1782))) {
                		// Translate the rank system
                		retVal.rankSystem = Ranks.translateFactions[retVal.rankSystem];
                		// Use the default ranks for this system
                		return Ranks.getRanksFromSystem(retVal.rankSystem);
                	} else if (version == null || retVal.rankSystem == RS_CUSTOM) {
                		retVal.ranks.add(Rank.generateInstanceFromXML(wn2));
                	}
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
        	String rating = "E"+i;
        	if (i > RWO_MAX) {
        		rating = "O"+(i-RWO_MAX);
        	} else if (i > RE_MAX) {
        		rating = "WO"+(i-RE_MAX);
        	}
        	array[i][RankTableModel.COL_NAME_RATE]	= rating;
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