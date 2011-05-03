/*
 * Unit.java
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

package mekhq.campaign;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This is the start of a planet object that will keep lots of information about 
 * planets that can be displayed on the interstellar map. Planet objects will 
 * *NOT* be serialized. The arraylist of planets in campaign will be loaded
 * from scratch on start up.
 * 
 * At the moment, I am using the planets.xml file from MekWars to test things out.
 * I am just going to use the x,y coordinate and faction owner for starters to 
 * see if i can actually plot it all on the map, but in the future we can expand this
 * using the existing MekWars info (like terrain options for map selection), plus any we 
 * want to add (like canonical factories and such). 
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Planet {
	
	private int x; 
	private int y;
	private int faction;
	private String name;
	
	public Planet() {
		this.x = 0;
		this.y = 0;
		this.faction = Faction.F_COMSTAR;
		this.name = "Terra";
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getFaction() {
		return faction;
	}
	
	public String getName() {
		return name;
	}
	
	public static Planet getPlanetFromXML(Node wn) {
		Planet retVal = new Planet();
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("name")) {
				retVal.name = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("xcood")) {
				retVal.x = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("ycood")) {
				retVal.y = Integer.parseInt(wn2.getTextContent());
			}
			//I have to dig down two levels for faction info. I should really
			//just build up my own xml files from the ISCS files
			else if (wn2.getNodeName().equalsIgnoreCase("influence")) {
				NodeList nl2 = wn2.getChildNodes();
				for (int y=0; y<nl2.getLength(); y++) {
					Node wn3 = nl2.item(y);
					 if (wn3.getNodeName().equalsIgnoreCase("inf")) {
						 NodeList nl3 = wn3.getChildNodes();
						 for (int z=0; z<nl3.getLength(); z++) {
							 Node wn4 = nl3.item(y);
							 if (wn4.getNodeName().equalsIgnoreCase("faction")) {
								 String fname = wn4.getTextContent();
								 if(fname.equalsIgnoreCase("davion")) {
									 retVal.faction = Faction.F_FEDSUN;
								 } else if(fname.equalsIgnoreCase("liao")) {
									 retVal.faction = Faction.F_CAPCON;
								 } else if(fname.equalsIgnoreCase("steiner")) {
									 retVal.faction = Faction.F_LYRAN;
								 } else if(fname.equalsIgnoreCase("kurita")) {
									 retVal.faction = Faction.F_DRAC;
								 } else if(fname.equalsIgnoreCase("marik")) {
									 retVal.faction = Faction.F_FWL;
								 } else {
									 retVal.faction = Faction.F_COMSTAR;
								 }
							 }
						 }
					 }
				}
			}
		}
		return retVal;
	}
}