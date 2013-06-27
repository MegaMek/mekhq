/*
 * Injury.java
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

package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.util.UUID;

import mekhq.MekHQ;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.personnel.Person;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Injury {
	private String fluff;
	private int days;
	private int originalDays;
	private int hits;
	private int location;
	private int type;
	private boolean permanent;
	private boolean workedOn;
	private boolean extended;
	protected UUID id;
	
	// Do not reorder these for backwards compatibility!
 	public static final int INJ_CUT = 0;
 	public static final int INJ_BRUISE = 1;
 	public static final int INJ_LACERATION = 2;
 	public static final int INJ_SPRAIN = 3;
 	public static final int INJ_CONCUSSION = 4;
 	public static final int INJ_BROKEN_RIB = 5;
 	public static final int INJ_BRUISED_KIDNEY = 6;
 	public static final int INJ_BROKEN_LIMB = 7;
 	public static final int INJ_BROKEN_COLLAR_BONE = 8;
 	public static final int INJ_INTERNAL_BLEEDING = 9;
 	public static final int INJ_LOST_LIMB = 10;
 	public static final int INJ_CEREBRAL_CONTUSION = 11;
 	public static final int INJ_PUNCTURED_LUNG = 12;
 	public static final int INJ_CTE = 13;
 	public static final int INJ_BROKEN_BACK = 14;
 	public static final int INJ_NUM = 15;
 	
 	public Injury() {
		fluff = "";
		days = 0;
		originalDays = 0;
		hits = 0;
		location = 0;
		type = 0;
		permanent = false;
		workedOn = false;
		extended = false;
	}
	
	public Injury(int time, String text, int loc, int type, int num, boolean workedOn) {
		this(time, text, loc, type, num, workedOn, false);
	}

	public Injury(int time, String text, int loc, int type, int num, boolean workedOn, boolean perm) {
		this(time, text, loc, type, num, workedOn, false, false);
	}
	
	public Injury(int time, String text, int loc, int type, int num, boolean workedOn, boolean perm, boolean extended) {
		setTime(time);
		setOriginalTime(time);
		setFluff(text);
		setLocation(loc);
		setType(type);
		setHits(num);
		setPermanent(perm);
		setWorkedOn(workedOn);
		setExtended(extended);
		id = UUID.randomUUID();
	}
	
	public UUID getUUID() {
		return id;
	}
	
	public String getUUIDAsString() {
		return id.toString();
	}
	
	public int getTime() {
		return days;
	}
	
	public void setTime(int time) {
		days = time;
	}
	
	public int getOriginalTime() {
		return originalDays;
	}
	
	public void setOriginalTime(int time) {
		originalDays = time;
	}
	
	public String getFluff() {
		return fluff;
	}
	
	public void setFluff(String text) {
		fluff = text;
	}
	
	public int getLocation() {
		return location;
	}
	
	public void setLocation(int loc) {
		location = loc;
	}
	
	public int getHits() {
		return hits;
	}
	
	public void setHits(int num) {
		hits = num;
	}
	
	public boolean getPermanent() {
		return permanent;
	}
	
	public void setPermanent(boolean perm) {
		permanent = perm;
	}
	
	public boolean getExtended() {
		return extended;
	}
	
	public void setExtended(boolean ext) {
		extended = ext;
	}
	
	public boolean getWorkedOn() {
		return workedOn;
	}
	
	public void setWorkedOn(boolean wo) {
		workedOn = wo;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public String getName() {
		String buffer = "";
		
		switch (location) {
		case Person.BODY_HEAD:
			buffer = "Head";
			break;
	 	case Person.BODY_LEFT_LEG:
			buffer = "Left Leg";
			break;
	 	case Person.BODY_LEFT_ARM:
			buffer = "Left Arm";
			break;
	 	case Person.BODY_CHEST:
			buffer = "Chest";
			break;
	 	case Person.BODY_ABDOMEN:
			buffer = "Abdomen";
			break;
	 	case Person.BODY_RIGHT_ARM:
			buffer = "Right Arm";
			break;
	 	case Person.BODY_RIGHT_LEG:
			buffer = "Right Leg";
			break;
		}
		
		switch (type) {
		case INJ_CUT:
			buffer = "Cut "+buffer;
			break;
	 	case INJ_BRUISE:
			buffer = "Bruised "+buffer;
			break;
	 	case INJ_LACERATION:
			buffer = "Lacerated "+buffer;
			break;
	 	case INJ_SPRAIN:
			buffer = "Sprained "+buffer;
			break;
	 	case INJ_CONCUSSION:
			buffer = "Concussion";
			break;
	 	case INJ_BROKEN_RIB:
			buffer = "Broken Rib";
			break;
	 	case INJ_BRUISED_KIDNEY:
			buffer = "Bruised Kidney";
			break;
	 	case INJ_BROKEN_LIMB:
			buffer = "Broken "+buffer;
			break;
	 	case INJ_BROKEN_COLLAR_BONE:
			buffer = "Broken Collarbone";
			break;
	 	case INJ_INTERNAL_BLEEDING:
			buffer = "Internal Bleeding";
			break;
	 	case INJ_LOST_LIMB:
			buffer = "Missing "+buffer;
			break;
	 	case INJ_CEREBRAL_CONTUSION:
			buffer = "Cerebral Contusion";
			break;
	 	case INJ_PUNCTURED_LUNG:
			buffer = "Punctured Lung";
			break;
	 	case INJ_CTE:
			buffer = "Chronic Traumatic Encephalopathy";
			break;
	 	case INJ_BROKEN_BACK:
			buffer = "Broken Back";
			break;
		}
		
		return buffer;
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<injury>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<fluff>"
				+MekHqXmlUtil.escape(fluff)
				+"</fluff>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<days>"
				+days
				+"</days>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<originalDays>"
				+originalDays
				+"</originalDays>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<hits>"
				+hits
				+"</hits>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<location>"
				+location
				+"</location>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<permanent>"
				+permanent
				+"</permanent>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<extended>"
				+extended
				+"</extended>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<workedOn>"
				+workedOn
				+"</workedOn>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<InjuryUUID>"
				+id.toString()
				+"</InjuryUUID>");
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</injury>");
	}
	
	public static Injury generateInstanceFromXML(Node wn) {
		Injury retVal = new Injury();
		
		try {	
			// Okay, now load fields!
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("fluff")) {
					retVal.fluff = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("days")) {
					retVal.days = Integer.parseInt(wn2.getTextContent().trim());
				} else if (wn2.getNodeName().equalsIgnoreCase("originalDays")) {
					retVal.originalDays = Integer.parseInt(wn2.getTextContent().trim());
				} else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
					retVal.hits = Integer.parseInt(wn2.getTextContent().trim());
				} else if (wn2.getNodeName().equalsIgnoreCase("location")) {
					retVal.location = Integer.parseInt(wn2.getTextContent().trim());
				} else if (wn2.getNodeName().equalsIgnoreCase("type")) {
					retVal.type = Integer.parseInt(wn2.getTextContent().trim());
				} else if (wn2.getNodeName().equalsIgnoreCase("permanent")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.permanent = true;
					else
						retVal.permanent = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("extended")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.extended = true;
					else
						retVal.extended = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("workedOn")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.workedOn = true;
					else
						retVal.workedOn = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("InjuryUUID")) {
					retVal.id = UUID.fromString(wn2.getTextContent());
				}
			}
			if (retVal.id == null) { // We didn't have an ID, so let's generate one!
				retVal.id = UUID.randomUUID();
			}
		} catch (Exception ex) {
			// Doh!
			MekHQ.logError(ex);
		}
		
		return retVal;
	}
}