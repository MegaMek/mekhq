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
    }
    
    public String getName(int profession) {
    	if (profession >= rankNames.size()) {
    		return "Profession Out of Bounds";
    	}
    	return rankNames.get(profession);
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
    
    public String getRankNamesAsString() {
    	String names = "";
    	String sep = "";
    	for (String name : rankNames) {
    		names += sep+name;
    		sep = ",";
    	}
    	return names;
    }
    
    public static Rank generateInstanceFromXML(Node wn) {
        Rank retVal = null;
        
        try {
            retVal = new Rank();
            
            // Okay, now load Skill-specific fields!
            NodeList nl = wn.getChildNodes();
            
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("rankNames")) {
                    retVal.rankNames = new ArrayList<String>(Arrays.asList(wn2.getTextContent().split(",")));
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