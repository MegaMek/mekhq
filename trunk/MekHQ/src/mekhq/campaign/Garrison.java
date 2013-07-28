/*
 * Garrison.java
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

public class Garrison {
	private String shortname;
	private String fullname;
	private String nameGenerator;
	private boolean clan;
	
	public Garrison() {
		this("SGU", "Some Garrison Unit");
	}
	
	public Garrison(String sname, String fname) {
		shortname = sname;
		fullname = fname;
		nameGenerator = "General";
		clan = false;
	}

	public String getShortName() {
		return shortname;
	}

	public String getFullName() {
		return fullname;
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
    
    public static Garrison getFactionFromXML(Node wn) throws DOMException, ParseException {
    	Garrison retVal = new Garrison();
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
			} 
		}
		return retVal;
	}
}