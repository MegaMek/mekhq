/*
 * Rank.java
 *
 * Copyright (c) 2013 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
import java.util.Arrays;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A specific rank with information about officer status and payment multipliers
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */

public class Rank implements MekHqXmlSerializable {

    private ArrayList<String> rankNames;
    private boolean officer;
    private double payMultiplier;
    private ArrayList<Integer> rankLevels;

    // Manei Domini Ranks
    // These should be safe as completely static items
	public static final int MD_RANK_NONE	= -1;
	public static final int MD_RANK_ALPHA	= 0;
	public static final int MD_RANK_BETA	= 1;
	public static final int MD_RANK_OMEGA	= 2;
	public static final int MD_RANK_TAU		= 3;
	public static final int MD_RANK_DELTA	= 4;
	public static final int MD_RANK_SIGMA	= 5;
	public static final int MD_RANK_OMICRON	= 6;
	public static final int MD_RANK_NUM	= 7;

    public Rank() {
        this(new ArrayList<String>(), false, 1.0);
    }

    public Rank(String[] names) {
        this(names, false, 1.0);
    }

    public Rank(ArrayList<String> names) {
        this(names, false, 1.0);
    }

    public Rank(String[] name, boolean b, double mult) {
        this(new ArrayList<String>(Arrays.asList(name)), b, mult);
    }

    public Rank(ArrayList<String> names, boolean b, double mult) {
    	rankNames = names;
        officer = b;
        payMultiplier = mult;
        rankLevels = new ArrayList<Integer>();
        for (int i = 0; i < rankNames.size(); i++) {
        	rankLevels.add(0);
        	if (rankNames.get(i).contains(":")) {
        		String[] temp = rankNames.get(i).split(":");
        		rankNames.set(i, temp[0].trim());
        		rankLevels.set(i, Integer.parseInt(temp[1].trim()));
        	}
        }
    }

	public static String getManeiDominiRankName(int rank) {
		switch (rank) {
			case MD_RANK_ALPHA: return "Alpha";
			case MD_RANK_BETA: return "Beta";
			case MD_RANK_OMEGA: return "Omega";
			case MD_RANK_TAU: return "Tau";
			case MD_RANK_DELTA: return "Delta";
			case MD_RANK_SIGMA: return "Sigma";
			case MD_RANK_OMICRON: return "Omicron";
			default: return "";
		}
	}

    public String getName(int profession) {
    	if (profession >= rankNames.size()) {
    		return "Profession Out of Bounds";
    	}
    	return rankNames.get(profession);
    }

    public String getNameWithLevels(int profession) {
    	if (profession >= rankNames.size()) {
    		return "Profession Out of Bounds";
    	}
    	return rankNames.get(profession) + (rankLevels.get(profession) > 0 ? ":" + rankLevels.get(profession) : "");
    }

    public boolean isOfficer() {
        return officer;
    }

    public void setOfficer(boolean b) {
        officer = b;
    }

    public double getPayMultiplier() {
        return payMultiplier;
    }

    public void setPayMultiplier(double d) {
        payMultiplier = d;
    }

    public int getRankLevels(int profession) {
    	return rankLevels.get(profession);
    }

    public String getRankNamesAsString() {
    	String names = "";
    	String sep = "";
    	for (String name : rankNames) {
    		names += sep+name;
    		if (rankLevels.size() > 0 && rankLevels.get(rankNames.indexOf(name)) > 0) {
    			names += rankLevels.get(rankNames.indexOf(name)).toString();
    		}
    		sep = ",";
    	}
    	return names;
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<rank>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<rankNames>"
                +MekHqXmlUtil.escape(getRankNamesAsString())
                +"</rankNames>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<officer>"
                +officer
                +"</officer>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<payMultiplier>"
                +payMultiplier
                +"</payMultiplier>");
        pw1.print(MekHqXmlUtil.indentStr(indent) + "</rank>");
    }

    public static Rank generateInstanceFromXML(Node wn) {
        Rank retVal = null;

        try {
            retVal = new Rank();

            // Okay, now load Skill-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("rankName")) {
                	String[] rNames = { wn2.getTextContent(), "--MW", "--MW", "--MW", "--MW", "--MW" };
                    retVal.rankNames = new ArrayList<String>(Arrays.asList(rNames));
                } else if (wn2.getNodeName().equalsIgnoreCase("rankNames")) {
                    retVal.rankNames = new ArrayList<String>(Arrays.asList(wn2.getTextContent().split(",")));
                    for (int i = 0; i < retVal.rankNames.size(); i++) {
                    	retVal.rankLevels.add(0);
                    	if (retVal.rankNames.get(i).contains(":")) {
                    		String[] temp = retVal.rankNames.get(i).split(":");
                    		retVal.rankNames.set(i, temp[0].trim());
                    		retVal.rankLevels.set(i, Integer.parseInt(temp[1].trim()));
                    	}
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("officer")) {
                    retVal.officer = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("payMultiplier")) {
                    retVal.payMultiplier = Double.parseDouble(wn2.getTextContent());
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

}