/*
 * SupportTeam.java
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

package mekhq.campaign.team;

import java.io.PrintWriter;
import java.io.Serializable;

import megamek.common.TargetRoll;
import mekhq.MekHQApp;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.Modes;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Taharqa
 * This is the code for a team (medical, technical, etc.)
 */
public abstract class SupportTeam implements Serializable, MekHqXmlSerializable {
	private static final long serialVersionUID = 2842840638600274021L;
	public static final int EXP_GREEN = 0;
    public static final int EXP_REGULAR = 1;
    public static final int EXP_VETERAN = 2;
    public static final int EXP_ELITE = 3;
    public static final int EXP_NUM = 4;
    
    protected String name;
    protected int rating; 
    protected int id;
    protected int fullSize;
    protected int currentSize;
    protected int hours;
    protected int minutesLeft;
    protected int overtimeLeft;
   
    protected Campaign campaign;
    
    public SupportTeam(String name, int rating) {
        this.name = name;
        this.rating = rating;
        this.hours = 8;
    }
    
    public abstract void reCalc();
  
    public String getName() {
        return name;
    }
    
    public void setName(String s) {
    	this.name = s;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int i) {
        this.id = i;
    } 
    
    public int getRating() {
    	return rating;
    }
  
	public abstract void writeToXml(PrintWriter pw1, int indent, int id);
	
	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<supportTeam id=\""
				+id
				+"\" type=\""
				+this.getClass().getName()
				+"\">");

		// There is a campaign object on here...
		// But instead of even trying to write it out, we'll deal with it in post-process on load.
		// Recursive saves to what's effectively a parent object in the architecture isn't worth the trouble.  :)
		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<currentSize>"
				+currentSize
				+"</currentSize>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<fullSize>"
				+fullSize
				+"</fullSize>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<hours>"
				+hours
				+"</hours>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<id>"
				+this.id
				+"</id>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<minutesLeft>"
				+minutesLeft
				+"</minutesLeft>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<name>"
				+name
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<overtimeLeft>"
				+overtimeLeft
				+"</overtimeLeft>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<rating>"
				+rating
				+"</rating>");
	}
	
	protected void writeToXmlEnd(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</supportTeam>");
	}

	public static SupportTeam generateInstanceFromXML(Node wn) {
		SupportTeam retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();
		
		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (SupportTeam) Class.forName(className).newInstance();
			retVal.loadFieldsFromXmlNode(wn);
			
			// Okay, now load Part-specific fields!
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("currentSize")) {
					retVal.currentSize = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("fullSize")) {
					retVal.fullSize = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("hours")) {
					retVal.hours = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("id")) {
					retVal.id = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("minutesLeft")) {
					retVal.minutesLeft = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("name")) {
					retVal.name = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("overtimeLeft")) {
					retVal.overtimeLeft = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("rating")) {
					retVal.rating = Integer.parseInt(wn2.getTextContent());
				}
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQApp.logError(ex);
		}
		
		return retVal;
	}
	
	protected abstract void loadFieldsFromXmlNode(Node wn);
}
