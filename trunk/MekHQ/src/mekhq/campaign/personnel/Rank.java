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
    private int levels;
    
    // A couple of arrays for use in the getLevelName() method
    private static int[]    numbers = { 1000,  900,  500,  400,  100,   90,  
        50,   40,   10,    9,    5,    4,    1 };
    private static String[] letters = { "M",  "CM",  "D",  "CD", "C",  "XC",
      "L",  "XL",  "X",  "IX", "V",  "IV", "I" };
    
    public Rank() {
    	this("Unknown", false, 1.0);
    }
    
    public Rank(String name, boolean b, double mult) {
    	this(name, b, mult, 0);
    }
    
    public Rank(String name, boolean b, double mult, int lev) {
        rankName = name;
        officer = b;
        payMultiplier = mult;
        levels = lev;
    }
    
    public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
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
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<levels>"
                +levels
                +"</levels>");
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
                } else if (wn2.getNodeName().equalsIgnoreCase("levels")) {
                    retVal.levels = Integer.parseInt(wn2.getTextContent());
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
    
    // TODO: Optionize this to allow user to choose roman or arabic numerals
    public static String getLevelName(int level) {
    	return getLevelName(level, false);
    }
    
    public static String getLevelName(int level, boolean checkZero) {
    	// If we're 0, then we just return an empty string
    	if (checkZero && level == 0) {
    		return "";
    	}
    	
    	// Roman numeral, prepended with a space for display purposes
    	String roman = " ";
        int num = level+1;

        for (int i = 0; i < numbers.length; i++) {
			while (num >= numbers[i]) {
				roman += letters[i];
				num -= numbers[i];
			}
        }
        
        return roman;
    }
    
    // TODO: Optionize this to allow user to choose roman or arabic numerals
    public static int getLevelFromName(String name) {
    	// If we're 0, then we just return an empty string
    	if (name.equals("")) {
    		return 0;
    	}
    	
    	// Roman numeral, prepended with a space for display purposes
    	int arabic = 0;
        String roman = name;

        for (int i = 0; i < roman.length(); i++) {
        	int num = letters.toString().indexOf(roman.charAt(i));
        	if (i < roman.length()) {
        		int temp = letters.toString().indexOf(roman.charAt(i+1));
        		// If this is a larger number, then we need to combine them
        		if (temp > num) {
        			num = temp - num;
        			i++;
        		}
        	}
        	
        	arabic += num;
        }
        
        return arabic-1;
    }
}