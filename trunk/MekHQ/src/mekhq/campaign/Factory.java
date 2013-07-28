/*
 * Factory.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Written by Dylan Myers <ralgith@gmail.com>
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

package mekhq.campaign;

import java.text.ParseException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Factory {
	private String shortname;
	private String fullname;
	private String nameGenerator;
	private String owner; // Faction shortname that owns it
	private String planet;
	private boolean clan;
	
	// TODO: Need some more variables here for holding units and components produced
	
	public Factory() {
		this("GF", "Generic Factory");
	}
	
	public Factory(String sname, String fname) {
		shortname = sname;
		fullname = fname;
		nameGenerator = "General";
		owner = "IND";
		planet = "";
		clan = false;
	}

	public String getShortName() {
		return shortname;
	}

	public String getFullName() {
		return fullname;
	}
	
	public String getOwner() {
		return owner;
	}
	
	public String getPlanet() {
		return planet;
	}
	
	public boolean isClan() {
		return clan;
	}
	
	public void setClan(boolean tf) {
		clan = tf;
	}
	
	public String getNameGenerator() {
		return nameGenerator;
	}
    
    public static Factory getFactionFromXML(Node wn) throws DOMException, ParseException {
		Factory retVal = new Factory();
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("shortname")) {
				retVal.shortname = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("fullname")) {
				retVal.fullname = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("nameGenerator")) {
				retVal.nameGenerator = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.clan = true;
				else
					retVal.clan = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("planet")) {
				retVal.planet = wn2.getTextContent();
			} 
		}
		return retVal;
	}
}