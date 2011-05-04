/*
 * Part.java
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

package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Engine;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.MekHQApp;
import mekhq.campaign.Faction;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.work.ReplacementItem;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class Part implements Serializable, MekHqXmlSerializable {
	private static final long serialVersionUID = 6185232893259168810L;
	public static final int PART_TYPE_ARMOR = 0;
	public static final int PART_TYPE_WEAPON = 1;
	public static final int PART_TYPE_AMMO = 2;
	public static final int PART_TYPE_EQUIPMENT_PART = 3;
	public static final int PART_TYPE_MEK_ACTUATOR = 4;
	public static final int PART_TYPE_MEK_ENGINE = 5;
	public static final int PART_TYPE_MEK_GYRO = 6;
	public static final int PART_TYPE_MEK_LIFE_SUPPORT = 7;
	public static final int PART_TYPE_MEK_BODY_PART = 8;
	public static final int PART_TYPE_MEK_SENSOR = 9;
	public static final int PART_TYPE_GENERIC_SPARE_PART = 10;
	public static final int PART_TYPE_OTHER = 11;

	private static final String[] partTypeLabels = { "Armor", "Weapon", "Ammo",
			"Equipment Part", "Mek Actuator", "Mek Engine", "Mek Gyro",
			"Mek Life Support", "Mek Body Part", "Mek Sensor",
			"Generic Spare Part", "Other" };

	public static String[] getPartTypeLabels() {
		return partTypeLabels;
	}

	// TODO: how to track clan vs. inner sphere
	protected String name;
	protected int id;
	protected boolean salvage;
	protected long cost;
	protected int tonnage;

	public Part() {
		this(false, 0);
	}
	
	public Part(boolean salvage, int tonnage) {
		this.name = "Unknown";
		this.salvage = salvage;
		this.tonnage = tonnage;
		this.cost = 0;
	}

	public abstract void reCalc();
	
	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public boolean isSalvage() {
		return salvage;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return name + " (" + getCostString() + ")";
	}

	public long getCost() {
		return cost;
	}

	public int getTonnage() {
		return tonnage;
	}

	public String getStatus() {
		String toReturn = "Mint";
		if (isSalvage()) {
			toReturn = "Salvage";
		}
		return toReturn;
	}

	public String getDescHTML() {
		String toReturn = "<font size='2'>";
		toReturn += "<b>" + getDesc() + "</b><br/>";
		toReturn += getStatus() + "<br/>";
		toReturn += "</font>";
		return toReturn;
	}
	
	//TODO: these two methods need to be abstract so that we HAVE to 
	//define them for each kind of part
	public abstract int getTechRating();
	
	public abstract int getAvailability(int era);

	public abstract boolean canBeUsedBy(ReplacementItem task);

	/**
	 * Checks if the current part is exactly the "same kind" of part as the part
	 * given in argument. It also takes into account the status {@link
	 * getStatus()} of the part
	 * 
	 * @param part
	 *            The part to be compared with the current part
	 */
	public abstract boolean isSamePartTypeAndStatus(Part part);

	/**
	 * Returns the type of the part. Used for parts filtering
	 * 
	 * @return The type of the part
	 */
	public int getPartType() {
		return PART_TYPE_OTHER;
	}

	public boolean isClanTechBase() {
		// By default : IS tech base
		return false;
	}

	public int getTech() {
		// By default : IS intro box
		return TechConstants.T_INTRO_BOXSET;
	}

	/**
	 * Gets potential SSW names which can be used by
	 * SSWLibHelper.getAbPlaceableByName
	 * 
	 * @return
	 */
	public ArrayList<String> getPotentialSSWNames(int faction) {
		ArrayList<String> sswNames = new ArrayList<String>();

		String sswName = getName();
		sswNames.add(sswName);

		return sswNames;
	}

	public String getCostString() {
		NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
		String text = numberFormat.format(getCost()) + " "
				+ (getCost() != 0 ? "CBills" : "CBill");
		return text;
	}

	public String getSaveString() {
		return getName() + ";" + getTonnage();
	}

	public static Part getPartByName(String saveString) {
		String name = saveString.split(";")[0];
		int tonnage = Integer.parseInt(saveString.split(";")[1]);

		if (saveString.contains("Armor")) {
			String typeString = saveString.split(";")[2];
			int type = Integer.parseInt(typeString);
			String amountString = saveString.split(";")[3];
			int amount = Integer.parseInt(amountString);

			Armor armor = new Armor(false, tonnage, type, amount);
			return armor;

		} else if (saveString.contains("Actuator")) {
			String typeString = saveString.split(";")[2];
			int type = Integer.parseInt(typeString);

			MekActuator mekActuator = new MekActuator(false, tonnage, type);
			return mekActuator;

		} else if (saveString.contains("Gyro")) {
			String typeString = saveString.split(";")[2];
			int type = Integer.parseInt(typeString);
			String walkMpString = saveString.split(";")[3];
			int walkMp = Integer.parseInt(walkMpString);

			MekGyro mekGyro = new MekGyro(false, tonnage, type, walkMp);
			return mekGyro;

		} else if (saveString.contains("Mech Life Support System")) {
			MekLifeSupport mekLifeSupport = new MekLifeSupport(false, tonnage);

			return mekLifeSupport;
		} else if (saveString.contains("Mech Head")
				|| saveString.contains("Mech Center Torso")
				|| saveString.contains("Mech Left Torso")
				|| saveString.contains("Mech Right Torso")
				|| saveString.contains("Mech Left Arm")
				|| saveString.contains("Mech Right Arm")
				|| saveString.contains("Mech Left Leg")
				|| saveString.contains("Mech Right Leg")) {
			String locString = saveString.split(";")[2];
			int loc = Integer.parseInt(locString);
			String structureTypeString = saveString.split(";")[3];
			int structureType = Integer.parseInt(structureTypeString);
			String tsmString = saveString.split(";")[4];
			boolean tsm = Boolean.parseBoolean(tsmString);

			MekLocation mekLocation = new MekLocation(false, loc, tonnage,
					structureType, tsm);
			
			return mekLocation;
		} else if (saveString.contains("Mech Sensors")) {
			MekSensor mekSensor = new MekSensor(false, tonnage);
			return mekSensor;
		} else if (saveString.contains("Engine")) {
			String ratingString = saveString.split(";")[2];
			int rating = Integer.parseInt(ratingString);
			String typeString = saveString.split(";")[3];
			int type = Integer.parseInt(typeString);
			boolean clanEngine = Boolean.parseBoolean(saveString.split(";")[4]);
			boolean tankEngine = Boolean.parseBoolean(saveString.split(";")[5]);
			boolean largeEngine = Boolean
					.parseBoolean(saveString.split(";")[6]);
			int flags = (clanEngine ? Engine.CLAN_ENGINE : 0)
					+ (tankEngine ? Engine.TANK_ENGINE : 0)
					+ (largeEngine ? Engine.LARGE_ENGINE : 0);
			int faction = (clanEngine ? Faction.F_C_OTHER : Faction.F_FEDSUN);

			Engine engine = new Engine(rating, type, flags);
			MekEngine mekEngine = new MekEngine(false, tonnage, faction,
					engine, 1.0);

			return mekEngine;
		} else if (name.contains("EquipmentPart")) {
			String typeName = saveString.split(";")[2];
			EquipmentType type = EquipmentType.get(typeName);
			int cost = Integer.parseInt(saveString.split(";")[3]);

			EquipmentPart equipmentPart = new EquipmentPart(false, tonnage, -1,
					type, null);
			equipmentPart.setCost(cost);

			return equipmentPart;
		} else if (name.contains("Spare Part")) {
			String techString = saveString.split(";")[2];
			int tech = Integer.parseInt(techString);
			String amountString = saveString.split(";")[3];
			int amount = Integer.parseInt(amountString);
			GenericSparePart genericSparePart = new GenericSparePart(tech,
					amount);

			return genericSparePart;
		} else {
			return null;
		}
	}

	public abstract void writeToXml(PrintWriter pw1, int indent, int id);
	
	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<part id=\""
				+id
				+"\" type=\""
				+this.getClass().getName()
				+"\">");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<cost>"
				+cost
				+"</cost>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<id>"
				+this.id
				+"</id>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<name>"
				+name
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<salvage>"
				+salvage
				+"</salvage>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<tonnage>"
				+tonnage
				+"</tonnage>");
	}
	
	protected void writeToXmlEnd(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</part>");
	}

	public static Part generateInstanceFromXML(Node wn) {
		Part retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();
		
		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (Part) Class.forName(className).newInstance();
			retVal.loadFieldsFromXmlNode(wn);
			
			// Okay, now load Part-specific fields!
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("cost")) {
					retVal.cost = Long.parseLong(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("id")) {
					retVal.id = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("name")) {
					retVal.name = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("salvage")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.salvage = true;
					else
						retVal.salvage = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("tonnage")) {
					retVal.tonnage = Integer.parseInt(wn2.getTextContent());
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

