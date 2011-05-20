/*
 * CurrentLocation.java
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.TreeMap;

import megamek.common.EquipmentType;
import megamek.common.PlanetaryConditions;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This keeps track of a location, which includes both the planet
 * and the current position in-system. It may seem a little like
 * overkill to have a separate object here, but when we reach a point
 * where we want to let a force be in different locations, this will
 * make it easier to keep track of everything
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CurrentLocation {
	
	private Planet currentPlanet;
	//going to try tracking actual distance here
	private float distance;
	
	
	public CurrentLocation(Planet planet, float d) {
		this.currentPlanet = planet;
		this.distance = d;
	}
	
	public void setCurrentPlanet(Planet p) {
		currentPlanet = p;
	}
	
	public void setDistance(Float d) {
		distance = d;
	}
	
	public boolean isOnPlanet() {
		return distance <= 0;
	}
	
	public boolean isAtJumpPoint() {
		return distance >= currentPlanet.getDistanceToJumpPoint();
	}
	
	public boolean isInTransit() {
		return !isOnPlanet() && !isAtJumpPoint();
	}
	
	public Planet getCurrentPlanet() {
		return currentPlanet;
	}
	
	public float getDistance() {
		return distance;
	}
	
	public double getDaysOut() {
		return Math.sqrt((getDistance()*1000)/(9.8))/43200;
	}
	
	public String getReport(Date date) {
		String toReturn = "<b>Current Location</b><br>";
		toReturn += currentPlanet.getShortName() + " (" + Faction.getFactionName(currentPlanet.getCurrentFaction(date)) + ")<br>";
		if(isOnPlanet()) {
			toReturn += "<i>On Planet</i>";
		} 
		else if(isAtJumpPoint()) {
			toReturn += "<i>At Jump Point</i>";
		} else {
			toReturn += "<i>" + Math.round(100.0*getDaysOut())/100.0 + " days out </i>";
		}
		return "<html>" + toReturn + "</html>";
	}
}