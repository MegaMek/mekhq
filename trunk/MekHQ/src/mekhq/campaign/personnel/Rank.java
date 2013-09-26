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
import java.io.Serializable;

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
        
    private String rankName;
    private boolean officer;
    private double payMultiplier;
    
    public Rank(String name, boolean b, double mult) {
        rankName = name;
        officer = b;
        payMultiplier = mult;
    }
    
    public Rank() {
        rankName = "Unknown";
        officer = false;
        payMultiplier = 1.0;
    }
    
    public String getName() {
        return rankName;
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
                +"<rankName>"
                +MekHqXmlUtil.escape(rankName)
                +"</rankName>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<officer>"
                +officer
                +"</officer>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<payMultiplier>"
                +payMultiplier
                +"</payMultiplier>");
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</rank>");
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
                    retVal.rankName = wn2.getTextContent();
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