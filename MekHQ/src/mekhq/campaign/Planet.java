/*
 * Planet.java
 * 
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Planet {
	
	private double x; 
	private double y;
	private int faction;
	private String name;
	
	public Planet() {
		this.x = 0;
		this.y = 0;
		this.faction = Faction.F_COMSTAR;
		this.name = "Terra";
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
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
				retVal.x = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("ycood")) {
				retVal.y = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("faction")) {
				retVal.faction = Integer.parseInt(wn2.getTextContent());
			}
		}
		return retVal;
	}
	
	@Override
	public boolean equals(Object object) {
		if(object instanceof Planet) {
			Planet planet = (Planet)object;
			if(planet.getName().equalsIgnoreCase(name) 
					&& planet.getX() == x
					&& planet.getY() == y) {
				return true;
			}
		}
		return false;
	}
}