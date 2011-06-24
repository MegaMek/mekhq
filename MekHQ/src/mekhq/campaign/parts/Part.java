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

import megamek.common.EquipmentType;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.MekHQApp;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.work.IPartWork;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parts do the lions share of the work of repairing, salvaging, reloading, refueling, etc. 
 * for units. Each unit has an ArrayList of all its relevant parts. There is a corresponding unit
 * variable in part but this can be null when we are dealing with a spare part, so when putting in 
 * calls to unit, you should always check to make sure it is not null. 
 * 
 * There are two kinds of parts: Part and MissingPart. The latter is used as a placeholder on a unit to 
 * indicate it is missing the given part. When parts are removed from a unit, they shold be replaced 
 * with the appropriate missing part which will remind MHQ that a replacement needs to be done.
 * 
 * Parts implement IPartWork and MissingParts also implement IAcquisitionWork. These interfaces allow for 
 * most of the actual work that can be done on parts. There is a lot of variability in how parts actually handle
 * this work
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class Part implements Serializable, MekHqXmlSerializable, IPartWork {
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
	
	public static final int T_BOTH = 0;
	public static final int T_IS   = 1;
	public static final int T_CLAN = 2;

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

	//this is the unitTonnage which needs to be tracked for some parts
	//even when off the unit. actual tonnage is returned via the 
	//getTonnage() method
	protected int unitTonnage;

	//hits to this part
	int hits;
	// the skill modifier for difficulty
	protected int difficulty;
	// the amount of time for the repair (this is the base time)
	protected int time;
	// time spent on the task so far for tasks that span days
	protected int timeSpent;
	// the minimum skill level in order to attempt
	protected int skillMin;
	//current repair mode for part
	protected int mode;
	protected int teamId;
	//null is valid. It indicates parts that are not attached to units.
	protected Unit unit;
	protected int unitId;
	//boolean to indicate whether the repair status on this part is set to salvage or 
	//to repair
	protected boolean salvaging;
	
	//we need to keep track of a couple of potential mods that result from carrying
	//over a task, otherwise people can get away with working over time with no consequence
	protected boolean workingOvertime;
	protected int shorthandedMod;
	
	public Part() {
		this(0);
	}
	
	public Part(int tonnage) {
		this.name = "Unknown";
		this.unitTonnage = tonnage;
		this.hits = 0;
		this.skillMin = SkillType.EXP_GREEN;
		this.mode = MODE_NORMAL;
		this.timeSpent = 0;
		this.teamId = -1;
		this.time = 0;
		this.difficulty = 0;
		this.salvaging = false;
		this.unitId = -1;
		this.workingOvertime = false;
		this.shorthandedMod = 0;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
	
	public int getUnitId() {
		return unitId;
	}

	public String getName() {
		return name;
	}

	/**
	 * Determine the current value of the part - This 
	 * can be tricky in the case of some parts that need
	 * entity information to determine price
	 * @return
	 */
	public abstract long getCurrentValue();

	public int getUnitTonnage() {
		return unitTonnage;
	}
	
	public abstract double getTonnage();
	
	public Unit getUnit() {
		return unit;
	}
	
	public void setUnit(Unit u) {
		this.unit = u;
		if(null != unit) {
			unitId = unit.getId();
		} else {
			unitId = -1;
		}
	}
	
	public String getStatus() {
		String toReturn = "Functional";
		if(needsFixing()) {
			toReturn = "Damaged";
		}
		return toReturn;
	}
	
	public int getHits() {
		return hits;
	}

	public String getDesc() {
		String bonus = getAllMods().getValueAsString();
		if (getAllMods().getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";
		String action = "Repair ";
		if(salvaging) {
			action = "Salvage ";
		}
		String scheduled = "";
		if (getAssignedTeamId() != -1) {
			scheduled = " (scheduled) ";
		}
	
		toReturn += ">";
		toReturn += "<b>" + action + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		toReturn += "" + getTimeLeft() + " minutes" + scheduled;
		toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
		toReturn += " " + bonus;
		if (getMode() != MODE_NORMAL) {
			toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
		}
		toReturn += "</font></html>";
		return toReturn;
	}
	
	//TODO: these two methods need to be abstract so that we HAVE to 
	//define them for each kind of part
	public abstract int getTechRating();
	
	public abstract int getAvailability(int era);


	public int getTechBase() {
		if(isClanTechBase()) {
			return T_CLAN;
		} else {
			return T_IS;
		}
	}
	
	public String getTechBaseName() {
		return getTechBaseName(getTechBase());
	}
	
	public static String getTechBaseName(int base) {
		switch(base) {
		case T_BOTH:
			return "IS/Clan";
		case T_CLAN:
			return "Clan";
		case T_IS:
			return "IS";
		default: 
			return "??";
		}
	}
	/**
	 * Checks if the current part is exactly the "same kind" of part as the part
	 * given in argument. It only returns true for undamaged parts. Damaged parts
	 * are never considered the same type.
	 * 
	 *  Primarily used to construct partInventory
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
        return TechConstants.isClan(getTech());
    }

	public int getTech() {
		// By default : IS intro box
		return TechConstants.T_INTRO_BOXSET;
	}

	public abstract void writeToXml(PrintWriter pw1, int indent, int id);
	
	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<part id=\""
				+id
				+"\" type=\""
				+this.getClass().getName()
				+"\">");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<id>"
				+this.id
				+"</id>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<name>"
				+name
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<unitTonnage>"
				+unitTonnage
				+"</unitTonnage>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<hits>"
				+hits
				+"</hits>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<difficulty>"
				+difficulty
				+"</difficulty>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<time>"
				+time
				+"</time>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<timeSpent>"
				+timeSpent
				+"</timeSpent>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<mode>"
				+mode
				+"</mode>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<teamId>"
				+teamId
				+"</teamId>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<skillMin>"
				+skillMin
				+"</skillMin>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<unitId>"
				+unitId
				+"</unitId>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<workingOvertime>"
				+workingOvertime
				+"</workingOvertime>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<shorthandedMod>"
				+shorthandedMod
				+"</shorthandedMod>");
	}
	
	protected void writeToXmlEnd(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</part>");
	}

	public static Part generateInstanceFromXML(Node wn) {
		Part retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();
		
		//reverse compatability checks
		if(className.equalsIgnoreCase("mekhq.campaign.parts.MekEngine")) {
			className = "mekhq.campaign.parts.EnginePart";
		} 
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.MissingMekEngine")) {
			className = "mekhq.campaign.parts.MissingEnginePart";
		}
		
		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (Part) Class.forName(className).newInstance();
			retVal.loadFieldsFromXmlNode(wn);
			
			// Okay, now load Part-specific fields!
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				 if (wn2.getNodeName().equalsIgnoreCase("id")) {
					retVal.id = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("name")) {
					retVal.name = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("unitTonnage")) {
					retVal.unitTonnage = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
					retVal.hits = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("difficulty")) {
					retVal.difficulty = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("time")) {
					retVal.time = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("timeSpent")) {
					retVal.timeSpent = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("skillMin")) {
					retVal.skillMin = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("mode")) {
					retVal.mode = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("teamId")) {
					retVal.teamId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
					retVal.unitId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("shorthandedMod")) {
					retVal.shorthandedMod = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("workingOvertime")) {
					if(wn2.getTextContent().equalsIgnoreCase("true")) {
						retVal.workingOvertime = true;
					} else {
						retVal.workingOvertime = false;
					}
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
	
	@Override
	public int getDifficulty() {
		return difficulty;
	}

	@Override
	public int getBaseTime() {
		return time;
	}

	@Override
	public int getActualTime() {
		switch (mode) {
		case MODE_EXTRA_ONE:
			return 2 * time;
		case MODE_EXTRA_TWO:
			return 4 * time;
		case MODE_RUSH_ONE:
			return (int) Math.ceil(time / 2.0);
		case MODE_RUSH_TWO:
			return (int) Math.ceil(time / 4.0);
		case MODE_RUSH_THREE:
			return (int) Math.ceil(time / 8.0);
		default:
			return time;
		}
	}

	@Override
	public int getTimeLeft() {
		return getActualTime() - getTimeSpent();
	}

	@Override
	public int getTimeSpent() {
		return timeSpent;
	}

	public void addTimeSpent(int m) {
		this.timeSpent += m;
	}

	public void resetTimeSpent() {
		this.timeSpent = 0;
	}

	@Override
	public int getSkillMin() {
		return skillMin;
	}

	public void setSkillMin(int i) {
		this.skillMin = i;
	}
	
	public int getMode() {
		return mode;
	}

	public void setMode(int i) {
		this.mode = i;
	}
	
	@Override
	public TargetRoll getAllMods() {
		TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
		if (getModeMod() != 0) {
			mods.addModifier(getModeMod(), getCurrentModeName());
		}
		if(null != unit) {
			mods.append(unit.getSiteMod());
	        if(unit.getEntity().getQuirks().booleanOption("easy_maintain")) {
	            mods.addModifier(-1, "easy to maintain");
	        }
	        else if(unit.getEntity().getQuirks().booleanOption("difficult_maintain")) {
	            mods.addModifier(1, "difficult to maintain");
	        }
		}
        mods.addModifier(Availability.getTechModifier(getTechRating()), "tech rating " + EquipmentType.getRatingName(getTechRating()));
		return mods;
	}
	

	public int getModeMod() {
		switch (mode) {
		case MODE_EXTRA_ONE:
			return -1;
		case MODE_EXTRA_TWO:
			return -2;
		default:
			return 0;
		}
	}

	public static String getModeName(int mode) {
		switch (mode) {
		case MODE_EXTRA_ONE:
			return "Extra time";
		case MODE_EXTRA_TWO:
			return "Extra time (x2)";
		case MODE_RUSH_ONE:
			return "Rush Job (1/2)";
		case MODE_RUSH_TWO:
			return "Rush Job (1/4)";
		case MODE_RUSH_THREE:
			return "Rush Job (1/8)";
		default:
			return "Normal";
		}
	}

	public String getCurrentModeName() {
		return getModeName(mode);
	}
	
	@Override
	public int getAssignedTeamId() {
		return teamId;
	}
	
	@Override
	public void setTeamId(int i) {
		this.teamId = i;
	}
	
	@Override
	public String getPartName() {
		return name;
	}
	
	@Override
	public boolean canFix(SupportTeam team) {
		return team.getRating() >= skillMin; 
	}
	
	@Override
	public String fail(int rating) {
		skillMin = ++rating;
		timeSpent = 0;
		return " <font color='red'><b> failed.</b></font>";
	}

	@Override
	public String succeed() {
		if(salvaging) {
			remove(true);
			salvaging = false;
			return " <font color='green'><b> salvaged.</b></font>";
		} else {
			fix();
			return " <font color='green'><b> fixed.</b></font>";
		}
	}
	
	@Override
    public String getDetails() {
        return hits + " hit(s)";
    }
	
	@Override 
	public boolean isSalvaging() {
		return salvaging;
	}

	public void setSalvaging(boolean b) {
		this.salvaging = b;
	}
	
	public boolean canScrap() {
		return true;
	}
	
	public String scrap() {
		remove(false);
		return getName() + " scrapped.";
	}
	
	@Override
	public boolean hasWorkedOvertime() {
		return workingOvertime;
	}
	
	@Override
	public void setWorkedOvertime(boolean b) {
		workingOvertime = b;
	}
	
	@Override
	public int getShorthandedMod() {
		return shorthandedMod;
	}
	
	@Override
	public void setShorthandedMod(int i) {
		shorthandedMod = i;
	}
}

