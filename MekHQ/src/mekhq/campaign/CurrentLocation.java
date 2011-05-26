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

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Vector;

import megamek.common.EquipmentType;
import megamek.common.PlanetaryConditions;
import mekhq.MekHQApp;
import mekhq.campaign.finances.Transaction;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
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
public class CurrentLocation implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4337642922571022697L;
	
	private Planet currentPlanet;
	//keep track of jump path
	private JumpPath jumpPath;
	private double rechargeTime;
	//I would like to keep track of distance, but I ain't too good with fyziks
	private double transitTime;
	
	public CurrentLocation() {
		this(null,0);
	}
	
	public CurrentLocation(Planet planet, double time) {
		this.currentPlanet = planet;
		this.transitTime = time;
		this.rechargeTime = 0.0;
		this.transitTime = 0.0;
	}
	
	public void setCurrentPlanet(Planet p) {
		currentPlanet = p;
	}
	
	public void setTransitTime(double time) {
		transitTime = time;
	}
	
	public boolean isOnPlanet() {
		return transitTime <= 0;
	}
	
	public boolean isAtJumpPoint() {
		return transitTime >= currentPlanet.getTimeToJumpPoint(1.0);
	}
	
	public boolean isInTransit() {
		return !isOnPlanet() && !isAtJumpPoint();
	}
	
	public Planet getCurrentPlanet() {
		return currentPlanet;
	}
	
	public double getTransitTime() {
		return transitTime;
	}
	
	public String getReport(Date date) {
		String toReturn = "<b>Current Location</b><br>";
		toReturn += currentPlanet.getShortName() + " (" + Faction.getFactionName(currentPlanet.getCurrentFaction(date)) + ")<br>";
		if(null != jumpPath && !jumpPath.isEmpty()) {
			toReturn += "In transit to " + jumpPath.getLastPlanet().getShortName() + "<br>";
		}
		if(isOnPlanet()) {
			toReturn += "<i>On Planet</i><br>";
		} 
		else if(isAtJumpPoint()) {
			toReturn += "<i>At Jump Point</i><br>";
		} else {
			toReturn += "<i>" + Math.round(100.0*getTransitTime())/100.0 + " days out </i><br>";
		}
		toReturn += "<i>" + Math.round(100.0*rechargeTime/currentPlanet.getRechargeTime()) + "% charged</i>";
		return "<html>" + toReturn + "</html>";
	}
	
	public JumpPath getJumpPath() {
		return jumpPath;
	}
	
	public void setJumpPath(JumpPath path) {
		jumpPath = path;
	}
	
	/**
	 * Check for a jump path and if found, do whatever needs to be done to move 
	 * forward
	 */
	public void newDay(Campaign campaign) {
		//recharge even if there is no jump path
		//because jumpships don't go anywhere
		double hours = 24.0;
		double usedRechargeTime = Math.min(hours, currentPlanet.getRechargeTime() - rechargeTime);
		if(usedRechargeTime > 0) {
			campaign.addReport("Jumpships spent " + Math.round(100.0 * usedRechargeTime)/100.0 + " hours recharging drives");
			rechargeTime += usedRechargeTime;
			if(rechargeTime >= currentPlanet.getRechargeTime()) {
				campaign.addReport("Jumpship drives full charged");
			}
		}
		if(null == jumpPath || jumpPath.isEmpty()) {
			return;
		}
		//if we are not at the final jump point, then check to see if we are transiting
		//or if we can jump
		if(jumpPath.size() > 1) {
			//first check to see if we are transiting
			double usedTransitTime = Math.min(hours, 24.0 * (currentPlanet.getTimeToJumpPoint(1.0) - transitTime));
			if(usedTransitTime > 0) {
				transitTime += usedTransitTime/24.0;
				campaign.addReport("Dropships spent " + Math.round(100.0 * usedTransitTime)/100.0 + " hours in transit to jump point");
				if(isAtJumpPoint()) {
					campaign.addReport("Jump point reached");
				}
			}
			if(isAtJumpPoint() && rechargeTime >= currentPlanet.getRechargeTime()) {
				//jump
				campaign.addReport("Jumping to " + jumpPath.get(1).getShortName());
				if(campaign.getCampaignOptions().payForTransport()) {
					campaign.getFinances().debit(campaign.calculateCostPerJump(true), Transaction.C_TRANSPORT, "jump from " + currentPlanet.getName() + " to " + jumpPath.get(1).getName(), campaign.getCalendar().getTime());
				}
				currentPlanet = jumpPath.get(1);
				jumpPath.removeFirstPlanet();
				//reduce remaining hours by usedRechargeTime or usedTransitTime, whichever is greater
				hours -= Math.max(usedRechargeTime, usedTransitTime);
				rechargeTime = hours;
				transitTime = currentPlanet.getTimeToJumpPoint(1.0);
				//if there are hours remaining, then begin recharging jump drive
				usedRechargeTime = Math.min(hours, currentPlanet.getRechargeTime() - rechargeTime);
				if(usedRechargeTime > 0) {
					campaign.addReport("Jumpships spent " + Math.round(100.0 * usedRechargeTime)/100.0 + " hours recharging drives");
					rechargeTime += usedRechargeTime;
					if(rechargeTime >= currentPlanet.getRechargeTime()) {
						campaign.addReport("Jumpship drives full charged");
					}
				}
			}
		}
		//if we are now at the final jump point, then lets begin in-system transit
		if(jumpPath.size() == 1) {
			double usedTransitTime = Math.min(hours, 24.0 * transitTime);
			campaign.addReport("Dropships spent " + Math.round(100.0 * usedTransitTime)/100.0 + " hours transiting into system");
			transitTime -= usedTransitTime/24.0;
			if(transitTime <= 0) {
				campaign.addReport(jumpPath.getLastPlanet().getShortName() + " reached.");
				//we are here!
				transitTime = 0;
				jumpPath = null;
			}
		}
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<location>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<currentPlanetName>"
				+currentPlanet.getName()
				+"</currentPlanetName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<transitTime>"
				+transitTime
				+"</transitTime>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<rechargeTime>"
				+rechargeTime
				+"</rechargeTime>");
		if(null != jumpPath) {
			jumpPath.writeToXml(pw1, indent+1);
		}
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</location>");
		
	}
	
	public static CurrentLocation generateInstanceFromXML(Node wn, Campaign c) {
		CurrentLocation retVal = null;
		
		try {		
			retVal = new CurrentLocation();
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				if (wn2.getNodeName().equalsIgnoreCase("currentPlanetName")) {
					retVal.currentPlanet = c.getPlanet(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("transitTime")) {
					retVal.transitTime = Double.parseDouble(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("rechargeTime")) {
					retVal.rechargeTime = Double.parseDouble(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("jumpPath")) {
					retVal.jumpPath = JumpPath.generateInstanceFromXML(wn2, c);
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
}