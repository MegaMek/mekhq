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
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.universe.Planet;


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
	    DateTime now = Utilities.getDateTimeDay(date);
	    StringBuilder sb = new StringBuilder();
	    sb.append("<html><b>Current Location</b><br>");
	    sb.append(currentPlanet.getPrintableName(now)).append("<br>");
		if(null != jumpPath && !jumpPath.isEmpty()) {
		    sb.append("In transit to ").append(jumpPath.getLastPlanet().getPrintableName(now)).append(" ");
		}
		if(isOnPlanet()) {
			sb.append("<i>on planet</i>");
		} 
		else if(isAtJumpPoint()) {
		    sb.append("<i>at jump point</i>");
		} else {
		    sb.append("<i>").append(String.format(Locale.ROOT, "%.2f", getTransitTime())).append(" days out </i>");
		}
		if(!Double.isInfinite(currentPlanet.getRechargeTime(now))) {
		    sb.append(", <i>").append(String.format(Locale.ROOT, "%.0f", 100.0 * rechargeTime/currentPlanet.getRechargeTime(now))).append("% charged </i>");
		}
		return sb.append("</html>").toString();
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
	    DateTime currentDate = Utilities.getDateTimeDay(campaign.getCalendar());
		double hours = 24.0;
		double neededRechargeTime = currentPlanet.getRechargeTime(currentDate);
		double usedRechargeTime = Math.min(hours, neededRechargeTime - rechargeTime);
		if(usedRechargeTime > 0) {
			campaign.addReport("Jumpships spent " + Math.round(100.0 * usedRechargeTime)/100.0 + " hours recharging drives");
			rechargeTime += usedRechargeTime;
			if(rechargeTime >= neededRechargeTime) {
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
			if(isAtJumpPoint() && rechargeTime >= neededRechargeTime) {
				//jump
				if(campaign.getCampaignOptions().payForTransport()) {
					double days = Math.round(jumpPath.getTotalTime(currentDate, campaign.getLocation().getTransitTime())*100.0)/100.0;
					int roundedMonths = (int) Math.ceil(days / 30.0);
					if(!campaign.getFinances().debit(campaign.calculateCostPerJump(true, campaign.getCampaignOptions().useEquipmentContractBase(), jumpPath.getJumps(), roundedMonths), Transaction.C_TRANSPORT, "jump from " + currentPlanet.getName(currentDate) + " to " + jumpPath.get(1).getName(currentDate), campaign.getCalendar().getTime())) {
					    campaign.addReport("<font color='red'><b>You cannot afford to make the jump!</b></font>");
					    return;
					}
				}
                campaign.addReport("Jumping to " + jumpPath.get(1).getPrintableName(currentDate));
				currentPlanet = jumpPath.get(1);
				jumpPath.removeFirstPlanet();
				//reduce remaining hours by usedRechargeTime or usedTransitTime, whichever is greater
				hours -= Math.max(usedRechargeTime, usedTransitTime);
				transitTime = currentPlanet.getTimeToJumpPoint(1.0);
				rechargeTime = 0;
				//if there are hours remaining, then begin recharging jump drive
				usedRechargeTime = Math.min(hours, neededRechargeTime - rechargeTime);
				if(usedRechargeTime > 0) {
					campaign.addReport("Jumpships spent " + Math.round(100.0 * usedRechargeTime)/100.0 + " hours recharging drives");
					rechargeTime += usedRechargeTime;
					if(rechargeTime >= neededRechargeTime) {
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
				campaign.addReport(jumpPath.getLastPlanet().getPrintableName(currentDate) + " reached.");
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
				+MekHqXmlUtil.escape(currentPlanet.getId())
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
	    final String METHOD_NAME = "generateInstanceFromXML(Node,Campaign)"; //$NON-NLS-1$
	    
		CurrentLocation retVal = null;
		
		try {		
			retVal = new CurrentLocation();
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				if (wn2.getNodeName().equalsIgnoreCase("currentPlanetName")) {
					Planet p = c.getPlanet(wn2.getTextContent());
					if(null == p) {
						//whoops we cant find your planet man, back to Earth
					    MekHQ.getLogger().log(CurrentLocation.class, METHOD_NAME, LogLevel.ERROR,
					            "Couldn't find planet named " + wn2.getTextContent()); //$NON-NLS-1$
						p = c.getPlanet("Terra");
						if(null == p) {
							//if that doesnt work then give the first planet we have
							p = c.getPlanets().get(0);
						}
					}
					retVal.currentPlanet = p;
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
            MekHQ.getLogger().log(CurrentLocation.class, METHOD_NAME, ex);
		}

		return retVal;
	}
}