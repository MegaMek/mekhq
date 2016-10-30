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

import java.awt.Color;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTime;

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
	public static final int PART_TYPE_MEK_COCKPIT = 12;

	public static final int T_UNKNOWN = -1;
	public static final int T_BOTH = 0;
	public static final int T_IS   = 1;
	public static final int T_CLAN = 2;

	public static final int QUALITY_A = 0;
    public static final int QUALITY_B = 1;
    public static final int QUALITY_C = 2;
    public static final int QUALITY_D = 3;
    public static final int QUALITY_E = 4;
    public static final int QUALITY_F = 5;

	public interface REPAIR_PART_TYPE {
		public static final int ARMOR = 0;
		public static final int AMMO = 1;
		public static final int WEAPON = 2;
		public static final int GENERAL_LOCATION = 3;
		public static final int ENGINE = 4;
		public static final int GYRO = 5;
		public static final int ACTUATOR = 6;
		public static final int ELECTRONICS = 7;
		public static final int GENERAL = 8;
		public static final int HEATSINK = 9;
		public static final int MEK_LOCATION = 10;		
		public static final int PHYSICAL_WEAPON = 11;		
	}
    
	private static final String[] partTypeLabels = { "Armor", "Weapon", "Ammo",
			"Equipment Part", "Mek Actuator", "Mek Engine", "Mek Gyro",
			"Mek Life Support", "Mek Body Part", "Mek Sensor",
			"Generic Spare Part", "Other" };

	public static String[] getPartTypeLabels() {
		return partTypeLabels;
	}

	protected String name;
	protected int id;

	//this is the unitTonnage which needs to be tracked for some parts
	//even when off the unit. actual tonnage is returned via the
	//getTonnage() method
	protected int unitTonnage;

	
	//hits to this part
	protected int hits;
	
	//Taharqa: as of 8/12/2015, we are no longer going to track difficulty and time
	//as hard coded numbers but rather use abstract methods that get them from each part
	//depending on the dynamic characteristics of the part
	// the skill modifier for difficulty
	//protected int difficulty;
	// the amount of time for the repair (this is the base time)
	//protected int time;
	
	
	// time spent on the task so far for tasks that span days
	protected int timeSpent;
	// the minimum skill level in order to attempt
	protected int skillMin;
	//current repair mode for part
	protected WorkTime mode;
	
	protected UUID teamId;
	private boolean isTeamSalvaging;

	//null is valid. It indicates parts that are not attached to units.
	protected Unit unit;
	protected UUID unitId;

	protected int quality;

	protected boolean brandNew;

	//we need to keep track of a couple of potential mods that result from carrying
	//over a task, otherwise people can get away with working over time with no consequence
	protected boolean workingOvertime;
	protected int shorthandedMod;

	//this tracks whether the part is reserved for a refit
	protected UUID refitId;
	protected UUID reserveId;

	//for delivery
	protected int daysToArrival;

	//all parts need a reference to campaign
	protected Campaign campaign;
	
	/*
	 * This will be unusual but in some circumstances certain parts will be linked to other parts.
	 * These linked parts will be considered integral and subsidary to those other parts and will
	 * not show up independently. Currently (8/8/2015), we are only using this for BA suits 
	 * We need a parent part id and a vector of children parts to represent this.
	 */
	protected int parentPartId; 
	protected ArrayList<Integer> childPartIds;

	/**
	 * The number of parts in exactly the same condition,
	 * to track multiple spare parts more efficiently and also the shopping list
	 */
	protected int quantity;

	//reverse-compatability
	protected int oldUnitId = -1;
	protected int oldTeamId = -1;
	protected int oldRefitId = -1;

	//only relevant for acquisitionable parts
	protected int daysToWait;
	protected int replacementId;

	public Part() {
		this(0, null);
	}

	public Part(int tonnage, Campaign c) {
		this.name = "Unknown";
		this.unitTonnage = tonnage;
		this.hits = 0;
		this.skillMin = SkillType.EXP_GREEN;
		this.mode = WorkTime.NORMAL;
		this.timeSpent = 0;
		this.unitId = null;
		this.workingOvertime = false;
		this.shorthandedMod = 0;
		this.refitId = null;
		this.daysToArrival = 0;
		this.campaign = c;
		this.brandNew = true;
		this.quantity = 1;
		this.replacementId = -1;
		this.quality = QUALITY_D;
		this.parentPartId = -1;
		this.childPartIds = new ArrayList<Integer>();
		this.isTeamSalvaging = false;
	}

	public static String getQualityName(int quality, boolean reverse) {
        switch(quality) {
        case QUALITY_A:
        	if(reverse) {
        		return "F";
        	}
            return "A";
        case QUALITY_B:
        	if(reverse) {
        		return "E";
        	}
            return "B";
        case QUALITY_C:
        	if(reverse) {
        		return "D";
        	}
            return "C";
        case QUALITY_D:
        	if(reverse) {
        		return "C";
        	}
            return "D";
        case QUALITY_E:
        	if(reverse) {
        		return "B";
        	}
            return "E";
        case QUALITY_F:
        	if(reverse) {
        		return "A";
        	}
            return "F";
        default:
            return "?";
        }
    }

    public String getQualityName() {
        return getQualityName(getQuality(), campaign.getCampaignOptions().reverseQualityNames());
    }

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public UUID getUnitId() {
		return unitId;
	}

	public void setCampaign(Campaign c) {
		this.campaign = c;
	}

	public Campaign getCampaign() {
	    return campaign;
	}

	public String getName() {
		return name;
	}

	/**
	 * Sticker price is the value of the part according to the rulebooks
	 * @return
	 */
	public abstract long getStickerPrice();

	/**
	 * This is the actual value of the part as affected by any characteristics
	 * of the part itself
	 * @return
	 */
	public long getCurrentValue() {
		return getStickerPrice();
	}

	/**
	 * This is the value of the part that may be affected by campaign options
	 * @return
	 */
	public long getActualValue() {
		return adjustCostsForCampaignOptions(getCurrentValue());
	}

	public boolean isPriceAdustedForAmount() {
	    return false;
	}

	protected long adjustCostsForCampaignOptions(long cost) {
		if(getTechBase() == T_CLAN) {
			cost *= campaign.getCampaignOptions().getClanPriceModifier();
		}
		if(needsFixing() && !isPriceAdustedForAmount()) {
			cost *= campaign.getCampaignOptions().getDamagedPartsValue();
			//TODO: parts that cant be fixed should also be further reduced in price
		} else if(!isBrandNew()) {
			cost *= campaign.getCampaignOptions().getUsedPartsValue(getQuality());
		}
		return cost;
	}

	public boolean isBrandNew() {
		return brandNew;
	}

	public void setBrandNew(boolean b) {
		this.brandNew = b;
	}

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
			unitTonnage = (int) u.getEntity().getWeight();
		} else {
			unitId = null;
		}
	}

	public String getStatus() {
		String toReturn = "Functional";
		if(needsFixing()) {
			toReturn = "Damaged";
		}
		if(isReservedForRefit()) {
			toReturn = "Reserved for Refit";
		}
		if(isReservedForReplacement()) {
			toReturn = "Reserved for Repair";
		}
		if(isBeingWorkedOn()) {
			toReturn = "Being worked on";
		}
		if(!isPresent()) {
            //toReturn = "" + getDaysToArrival() + " days to arrival";
		    String dayName = "day";
		    if(getDaysToArrival() > 1) {
		        dayName += "s";
		    }
            toReturn = "In transit (" + getDaysToArrival() + " " + dayName + ")";
        }
		return toReturn;
	}

	public int getHits() {
		return hits;
	}

	public String getDesc() {
		String bonus = getAllMods(null).getValueAsString();
		if (getAllMods(null).getValue() > -1) {
			bonus = "+" + bonus;
		}
		bonus = "(" + bonus + ")";
		String toReturn = "<html><font size='2'";
		String action = "Repair ";
		if(isSalvaging()) {
			action = "Salvage ";
		}
		String scheduled = "";
		if (getTeamId() != null) {
			scheduled = " (scheduled) ";
		}

		toReturn += ">";
		toReturn += "<b>" + action + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		if(getSkillMin() > SkillType.EXP_ELITE) {
		    toReturn += "<font color='red'>Impossible</font>";
		} else {
    		toReturn += "" + getTimeLeft() + " minutes" + scheduled;
    		if(!getCampaign().getCampaignOptions().isDestroyByMargin()) {
    		    toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
    		}
    		toReturn += " " + bonus;
    		if (getMode() != WorkTime.NORMAL) {
    			toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
    		}
		}
		toReturn += "</font></html>";
		return toReturn;
	}

	public String getRepairDesc() {
		String toReturn = "";
		if(needsFixing()) {
			String scheduled = "";
			if (getTeamId() != null) {
				scheduled = " (scheduled) ";
			}
			String bonus = getAllMods(null).getValueAsString();
			if (getAllMods(null).getValue() > -1) {
				bonus = "+" + bonus;
			}
			bonus = "(" + bonus + ")";
			toReturn += getTimeLeft() + " minutes" + scheduled;
			toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
			toReturn += " " + bonus;
			if (getMode() != WorkTime.NORMAL) {
				toReturn += ", " + getCurrentModeName();
			}
		}
		return toReturn;
	}

	public abstract int getTechRating();

	public abstract int getAvailability(int era);


	public int getTechBase() {
	    if(getTechLevel() == TechConstants.T_ALLOWED_ALL) {
	        return T_BOTH;
	    }
	    if (getTechLevel() == TechConstants.T_TECH_UNKNOWN) {
	        return T_UNKNOWN;
	    }
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
		case T_UNKNOWN:
		    return "UNKNOWN";
		default:
			return "??";
		}
	}

	abstract public int getTechLevel();
	
	abstract public int getIntroDate();
	
	abstract public int getExtinctDate();
	
	abstract public int getReIntroDate();
	
	/**
	 * We are going to only limit parts by year if they totally haven't been produced
	 * otherwise, we will just replace the existing availability code with X
	 */
	public boolean isIntroducedBy(int year) {
        if (year < getIntroDate()) {
            return false;
        }
        
        return true;
    }

	public boolean isExtinctIn(int year) {
		if ((getExtinctDate() == EquipmentType.DATE_NONE)) {
			return false;
		}
		if (year >= getExtinctDate() && year < getReIntroDate()) {
            return true;
        }
        return false;
	}
	
	
	/**
	 * Checks if the current part is exactly the "same kind" of part as the part
	 * given in argument. This is used to determine whether we need to add new spare
	 * parts, or increment existing ones.
	 *
	 * @param part
	 *            The part to be compared with the current part
	 */
	public boolean isSamePartTypeAndStatus(Part part) {
		return isSamePartType(part) && isSameStatus(part);
	}

	public abstract boolean isSamePartType(Part part);

	public boolean isSameStatus(Part part) {
		//parts that are reserved for refit or being worked on are never the same status
		if(isReservedForRefit() || isBeingWorkedOn() || isReservedForReplacement() || hasParentPart()
				|| part.isReservedForRefit() || part.isBeingWorkedOn() || part.isReservedForReplacement() || part.hasParentPart()) {
    		return false;
    	}
		return quality == part.getQuality() && hits == part.getHits() && part.getSkillMin() == this.getSkillMin() && this.getDaysToArrival() == part.getDaysToArrival();
	}

    protected boolean isClanTechBase() {
        return TechConstants.isClan(getTechLevel());
    }

	public abstract void writeToXml(PrintWriter pw1, int indent);

	protected void writeToXmlBegin(PrintWriter pw1, int indent) {
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
				+MekHqXmlUtil.escape(name)
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
				+"<timeSpent>"
				+timeSpent
				+"</timeSpent>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<mode>"
				+mode
				+"</mode>");
		if(null != teamId) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<teamId>"
					+teamId.toString()
					+"</teamId>");
		}
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<skillMin>"
				+skillMin
				+"</skillMin>");
		if(null != unitId) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<unitId>"
					+unitId.toString()
					+"</unitId>");
		}
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<workingOvertime>"
				+workingOvertime
				+"</workingOvertime>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<shorthandedMod>"
				+shorthandedMod
				+"</shorthandedMod>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<refitId>"
				+refitId
				+"</refitId>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<daysToArrival>"
				+daysToArrival
				+"</daysToArrival>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<brandNew>"
				+brandNew
				+"</brandNew>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<quantity>"
				+quantity
				+"</quantity>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<daysToWait>"
                +daysToWait
                +"</daysToWait>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<replacementId>"
                +replacementId
                +"</replacementId>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<quality>"
                +quality
                +"</quality>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<isTeamSalvaging>"
                +isTeamSalvaging
                +"</isTeamSalvaging>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<parentPartId>"
                +parentPartId
                +"</parentPartId>");
        for(int childId : childPartIds) {
			pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<childPartId>"
					+ childId + "</childPartId>");
		}
	}

	protected void writeToXmlEnd(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</part>");
	}

	public static Part generateInstanceFromXML(Node wn, Version version) {
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
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.EquipmentPart")) {
			className = "mekhq.campaign.parts.equipment.EquipmentPart";
		}
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.MissingEquipmentPart")) {
			className = "mekhq.campaign.parts.equipment.MissingEquipmentPart";
		}
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.AmmoBin")) {
			className = "mekhq.campaign.parts.equipment.AmmoBin";
		}
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.MissingAmmoBin")) {
			className = "mekhq.campaign.parts.equipment.MissingAmmoBin";
		}
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.JumpJet")) {
			className = "mekhq.campaign.parts.equipment.JumpJet";
		}
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.MissingJumpJet")) {
			className = "mekhq.campaign.parts.equipment.MissingJumpJet";
		}
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.HeatSink")) {
			className = "mekhq.campaign.parts.equipment.HeatSink";
		}
		else if(className.equalsIgnoreCase("mekhq.campaign.parts.MissingHeatSink")) {
			className = "mekhq.campaign.parts.equipment.MissingHeatSink";
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
				} else if (wn2.getNodeName().equalsIgnoreCase("quantity")) {
					retVal.quantity = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
					retVal.hits = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("timeSpent")) {
					retVal.timeSpent = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("skillMin")) {
					retVal.skillMin = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("mode")) {
					retVal.mode = WorkTime.of(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                    retVal.daysToWait = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("teamId")) {
					if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
						retVal.oldTeamId = Integer.parseInt(wn2.getTextContent());
					} else {
						if(!wn2.getTextContent().equals("null")) {
							retVal.teamId = UUID.fromString(wn2.getTextContent());
						}
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
					if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
						retVal.oldUnitId = Integer.parseInt(wn2.getTextContent());
					} else {
						if(!wn2.getTextContent().equals("null")) {
							retVal.unitId = UUID.fromString(wn2.getTextContent());
						}
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("shorthandedMod")) {
					retVal.shorthandedMod = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("refitId")) {
					if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
						retVal.oldRefitId = Integer.parseInt(wn2.getTextContent());
					} else {
						if(!wn2.getTextContent().equals("null")) {
							retVal.refitId = UUID.fromString(wn2.getTextContent());
						}
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("daysToArrival")) {
					retVal.daysToArrival = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("workingOvertime")) {
					if(wn2.getTextContent().equalsIgnoreCase("true")) {
						retVal.workingOvertime = true;
					} else {
						retVal.workingOvertime = false;
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("isTeamSalvaging")) {
					if(wn2.getTextContent().equalsIgnoreCase("true")) {
						retVal.isTeamSalvaging = true;
					} else {
						retVal.isTeamSalvaging = false;
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("brandNew")) {
					if(wn2.getTextContent().equalsIgnoreCase("true")) {
						retVal.brandNew = true;
					} else {
						retVal.brandNew = false;
					}
				}
				else if (wn2.getNodeName().equalsIgnoreCase("replacementId")) {
					retVal.replacementId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("quality")) {
                    retVal.quality = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("parentPartId")) {
                    retVal.parentPartId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("childPartId")) {
                    retVal.childPartIds.add(Integer.parseInt(wn2.getTextContent()));
                }
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQ.logError(ex);
		}

		// Refit protection of unit id
		if (retVal.unitId != null && retVal.refitId != null) {
		    retVal.setUnit(null);
		}

		return retVal;
	}

	protected abstract void loadFieldsFromXmlNode(Node wn);

	@Override
	public int getActualTime() {
	    return (int) Math.ceil(getBaseTime() * mode.timeMultiplier);
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

	public void resetOvertime() {
		this.workingOvertime = false;
	}

	@Override
	public int getSkillMin() {
		return skillMin;
	}

	public void setSkillMin(int i) {
		this.skillMin = i;
	}

	public WorkTime getMode() {
		return mode;
	}

	public void setMode(WorkTime wt) {
		this.mode = wt;
	}

	@Override
	public TargetRoll getAllMods(Person tech) {
		TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
		int modeMod = mode.getMod(campaign.getCampaignOptions().isDestroyByMargin());
		if (modeMod != 0) {
			mods.addModifier(modeMod, getCurrentModeName());
		}
		if(null != unit) {
			mods.append(unit.getSiteMod());
	        if(unit.getEntity().hasQuirk("easy_maintain")) {
	            mods.addModifier(-1, "easy to maintain");
	        }
	        else if(unit.getEntity().hasQuirk("difficult_maintain")) {
	            mods.addModifier(1, "difficult to maintain");
	        }
		}
		if(isClanTechBase() || (this instanceof MekLocation && this.getUnit() != null && this.getUnit().getEntity().isClan())) {
			if (null != tech && !tech.isClanner()) {
				mods.addModifier(2, "clan tech");
			}
		}
		String qualityName = getQualityName(quality, campaign.getCampaignOptions().reverseQualityNames());
		switch(quality) {
		case QUALITY_A:
            mods.addModifier(3, qualityName);
            break;
		case QUALITY_B:
            mods.addModifier(2, qualityName);
            break;
		case QUALITY_C:
            mods.addModifier(1, qualityName);
            break;
		case QUALITY_D:
            mods.addModifier(0, qualityName);
            break;
		case QUALITY_E:
            mods.addModifier(-1, qualityName);
            break;
		case QUALITY_F:
            mods.addModifier(-2, qualityName);
            break;
		}

        return mods;
	}

	public TargetRoll getAllModsForMaintenance() {
	    //according to StratOps you get a -1 mod when checking on individual parts
	    //but we will make this user customizable
	    TargetRoll mods = new TargetRoll(campaign.getCampaignOptions().getMaintenanceBonus(), "maintenance");
        mods.addModifier(Availability.getTechModifier(getTechRating()), "tech rating " + EquipmentType.getRatingName(getTechRating()));

	    if(null != unit) {
	        mods.append(unit.getSiteMod());
	        if(unit.getEntity().hasQuirk("easy_maintain")) {
	            mods.addModifier(-1, "easy to maintain");
	        }
	        else if(unit.getEntity().hasQuirk("difficult_maintain")) {
	            mods.addModifier(1, "difficult to maintain");
	        }
	    }
	    if(isClanTechBase() || (this instanceof MekLocation && this.getUnit() != null && this.getUnit().getEntity().isClan())) {
	        if (campaign.getPerson(getTeamId()) == null) {
	            mods.addModifier(2, "clan tech");
	        } else if (!campaign.getPerson(getTeamId()).isClanner()) {
	            mods.addModifier(2, "clan tech");
	        }
	    }

	    if(campaign.getCampaignOptions().useQualityMaintenance()) {
    	    switch(quality) {
    	    case QUALITY_A:
    	        mods.addModifier(3, "Quality A");
    	        break;
    	    case QUALITY_B:
    	        mods.addModifier(2, "Quality B");
    	        break;
    	    case QUALITY_C:
    	        mods.addModifier(1, "Quality C");
    	        break;
    	    case QUALITY_D:
    	        mods.addModifier(0, "Quality D");
    	        break;
    	    case QUALITY_E:
    	        mods.addModifier(-1, "Quality E");
    	        break;
    	    case QUALITY_F:
    	        mods.addModifier(-2, "Quality F");
    	        break;
    	    }
	    }

	    return mods;
	}

	public String getCurrentModeName() {
		return mode.name;
	}

	@Override
	public UUID getTeamId() {
		return teamId;
	}

	@Override
	public void setTeamId(UUID i) {
		//keep track of whether this was a salvage operation
		//because the entity may change
		if(null == i) {
			this.isTeamSalvaging = false;
		} else if(null == teamId) {
			this.isTeamSalvaging = isSalvaging();
		}
		this.teamId = i;		
	}
	
	public boolean isTeamSalvaging() {
		return null != getTeamId() && isTeamSalvaging;
	}

	public void setReserveId(UUID i) {
		this.reserveId = i;
	}

	@Override
	public String getPartName() {
		return name;
	}

	@Override
    public int getMassRepairOptionType() {
    	return REPAIR_PART_TYPE.GENERAL;
    }

	@Override
    public int getRepairPartType() {
    	return getMassRepairOptionType();
    }
	
	@Override
	public void fix() {
		hits = 0;
		skillMin = SkillType.EXP_GREEN;
		shorthandedMod = 0;
		mode = WorkTime.NORMAL;
	}

	@Override
	public String fail(int rating) {
		skillMin = ++rating;
		timeSpent = 0;
		shorthandedMod = 0;
		return " <font color='red'><b> failed.</b></font>";
	}

	@Override
	public String succeed() {
		if(isSalvaging()) {
			remove(true);
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
		if(null != unit) {
			return unit.isSalvage() || isMountedOnDestroyedLocation() || isTeamSalvaging();
		}
		return false;
	}

	public String checkScrappable() {
		return null;
	}

	public boolean canNeverScrap() {
		return false;
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

	@Override
	public abstract Part clone();

    protected void copyBaseData(Part part) {
        this.mode = part.mode;
        this.hits = part.hits;
        this.brandNew = part.brandNew;
    }

	public void setRefitId(UUID rid) {
		refitId = rid;
	}

	public UUID getRefitId() {
		return refitId;
	}

	public boolean isReservedForRefit() {
		return refitId != null;
	}

	public boolean isReservedForReplacement() {
		return reserveId != null;
	}

	public void setDaysToArrival(int days) {
		daysToArrival = days;
	}

	public int getDaysToArrival() {
		return daysToArrival;
	}

	public boolean checkArrival() {
		if(daysToArrival > 0) {
			daysToArrival--;
			return (daysToArrival == 0);
		}
		return false;
	}

	public boolean isPresent() {
		return daysToArrival == 0;
	}

	public boolean isBeingWorkedOn() {
		return teamId != null;
	}

	public void fixIdReferences(Hashtable<Integer, UUID> uHash, Hashtable<Integer, UUID> pHash) {
    	unitId = uHash.get(oldUnitId);
    	refitId = uHash.get(oldRefitId);
    	teamId = pHash.get(oldTeamId);
    }

	/*
	public void resetRepairStatus() {
		if(null != unit) {
			setSalvaging(unit.isSalvage());
			updateConditionFromEntity(false);
		}
	}
	*/

	public boolean onBadHipOrShoulder() {
		return false;
	}

	public boolean isMountedOnDestroyedLocation() {
		return false;
	}

	public boolean isPartForEquipmentNum(int index, int loc) {
		return false;
	}

    public boolean isInSupply() {
        return true;
    }

    public int getQuantity() {
    	return quantity;
    }

    public void incrementQuantity() {
    	quantity++;
    }

    public void decrementQuantity() {
    	quantity--;
    	if(quantity <= 0) {
    		for(int childId : childPartIds) {
    			Part p = campaign.getPart(childId);
    			if(null != p) {
    				campaign.removePart(p);
    			}
    		}
    		campaign.removePart(this);
    	}
    }

    public boolean isSpare() {
    	return null == unitId && parentPartId == -1;
    }

    public boolean isRightTechType(String skillType) {
    	return true;
    }

    public boolean isOmniPoddable() {
    	return false;
    }

    public int getDaysToWait() {
        return daysToWait;
    }

    public void resetDaysToWait() {
        this.daysToWait = campaign.getCampaignOptions().getWaitingPeriod();
    }

    public void decrementDaysToWait() {
        if(daysToWait > 0) {
            daysToWait--;
        }
    }

    public String getShoppingListReport(int quan) {
        return getQuantityName(quan) + ((quan > 1) ? " have " : " has ") + "been added to the procurement list.";
    }

    public String getArrivalReport() {
        return getQuantityName(quantity) + ((quantity > 1) ? " have " : " has ") + "arrived";
    }

    public String getQuantityName(int quantity) {
        String answer = "" + quantity + " " + getName();
        if(quantity > 1) {
            answer += "s";
        }
        return answer;
    }

    /** Get the acquisition work to acquire a new part of this type
     * For most parts this is just getMissingPart(), but some override it
     * @return
     */
    public IAcquisitionWork getAcquisitionWork() {
        return getMissingPart();
    }

    public void doMaintenanceDamage(int d) {
        hits += d;
        updateConditionFromPart();
        updateConditionFromEntity(false);
    }

    public int getQuality() {
        return quality;
    }

    public void improveQuality() {
        quality += 1;
    }

    public void decreaseQuality() {
        quality -= 1;
    }

    public void setQuality(int q) {
    	quality = q;
    }

    public boolean needsMaintenance() {
        return true;
    }

    public void cancelAssignment() {
        setTeamId(null);
        resetOvertime();
        resetTimeSpent();
        setShorthandedMod(0);
    }

    public abstract String getLocationName();

    public abstract int getLocation();
    
    public void setParentPartId(int id) {
    	parentPartId = id;
    }
    
    public int getParentPartId() {
    	return parentPartId;
    }
    
    public boolean hasParentPart() {
    	return parentPartId != -1;
    }
    
    public ArrayList<Integer> getChildPartIds() {
    	return childPartIds;
    }
    
    public void addChildPart(Part child) {
    	childPartIds.add(child.getId());
    	child.setParentPartId(id);
    }

    public void removeChildPart(int childId) {
    	ArrayList<Integer> tempArray = new ArrayList<Integer>();
    	for(int cid : childPartIds) {
    		if(cid == childId) {
    			Part part = campaign.getPart(childId);
        		if(null != part) {
        			part.setParentPartId(-1);
        		}
    		} else {
    			tempArray.add(cid);
    		}
    	}
    	childPartIds = tempArray;
    }
     
    public void removeAllChildParts() {
    	for(int childId : childPartIds) {
    		Part part = campaign.getPart(childId);
    		if(null != part) {
    			part.setParentPartId(-1);
    		}
    	}
    	childPartIds = new ArrayList<Integer>();
    }
    
    /**
     * Reserve a part for overnight work
     */
    @Override
    public void reservePart() {
    	//nothing goes here for real parts. Only missing parts need to reserve a replacement
    }
    
    @Override
    public void cancelReservation() {
    	//nothing goes here for real parts. Only missing parts need to reserve a replacement
    }
    
    /**
     * Make any changes to the part needed for adding to the campaign
     */
    public void postProcessCampaignAddition() {
    	//do nothing
    }
    
    public boolean isInLocation(String loc) {
    	return null != unit 
    		&& null != unit.getEntity() 
    		&& getLocation() == getUnit().getEntity().getLocationFromAbbr(loc);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        sb.append(" "); //$NON-NLS-1$
        sb.append(getDetails());
        sb.append(", q: "); //$NON-NLS-1$
        sb.append(quantity);
        if(null != unit) {
            sb.append(", mounted: "); //$NON-NLS-1$
            sb.append(unit);
        }
        return sb.toString();
    }

	public static String getRepairTypeShortName(int type) {
		switch (type) {
    		case Part.REPAIR_PART_TYPE.ARMOR:
    			return "Armor";
    			
    		case Part.REPAIR_PART_TYPE.AMMO:
    			return "Ammo";
    			
    		case Part.REPAIR_PART_TYPE.WEAPON:
    			return "Weapons";
    			
    		case Part.REPAIR_PART_TYPE.GENERAL_LOCATION:
    			return "Locations";
    			
    		case Part.REPAIR_PART_TYPE.ENGINE:
    			return "Engines";
    			
    		case Part.REPAIR_PART_TYPE.GYRO:
    			return "Gyros";
    			
    		case Part.REPAIR_PART_TYPE.ACTUATOR:
    			return "Actuators";
    			
    		case Part.REPAIR_PART_TYPE.ELECTRONICS:
    			return "Cockpit/Life Support/Sensors";
    			
    		default:
    			return "Other Items";
		}
	}
	
	public static int findCorrectMassRepairType(Part part) {
		if (part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof WeaponType) {
			return Part.REPAIR_PART_TYPE.WEAPON;
		} else {			
			return part.getMassRepairOptionType();
		}
	}
	
	public static int findCorrectRepairType(Part part) {
		if ((part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof WeaponType) ||
				(part instanceof MissingEquipmentPart && ((MissingEquipmentPart)part).getType() instanceof WeaponType)) {
			return Part.REPAIR_PART_TYPE.WEAPON;
		} else {
			if (part instanceof EquipmentPart && ((EquipmentPart)part).getType().hasFlag(MiscType.F_CLUB)) {
				return Part.REPAIR_PART_TYPE.PHYSICAL_WEAPON;
			}
			
			return part.getRepairPartType();
		}
	}

	public static String[] findPartImage(Part part) {
		String imgBase = null;
        int repairType = Part.findCorrectRepairType(part);
        
        switch (repairType) {
        	case Part.REPAIR_PART_TYPE.ARMOR:
        		imgBase = "armor";
        		break;
        	case Part.REPAIR_PART_TYPE.AMMO:
        		imgBase = "ammo";
        		break;
        	case Part.REPAIR_PART_TYPE.ACTUATOR:
        		imgBase = "actuator";
        		break;
        	case Part.REPAIR_PART_TYPE.ENGINE:
        		imgBase = "engine";
        		break;
        	case Part.REPAIR_PART_TYPE.ELECTRONICS:
        		imgBase = "electronics";
        		break;
        	case Part.REPAIR_PART_TYPE.HEATSINK:
        		imgBase = "heatsink";
        		break;
        	case Part.REPAIR_PART_TYPE.WEAPON:
        		EquipmentType equipmentType = null;
        		
        		if (part instanceof EquipmentPart) {
        			equipmentType = ((EquipmentPart)part).getType();
        		} else if (part instanceof MissingEquipmentPart) {
        			equipmentType = ((MissingEquipmentPart)part).getType();
        		}

        		if (null != equipmentType) {
	        		if (equipmentType.hasFlag(WeaponType.F_LASER)) {
	        			imgBase = "laser";	
	        		} else if (equipmentType.hasFlag(WeaponType.F_MISSILE)) {
	        			imgBase = "missile";	
	        		} else if (equipmentType.hasFlag(WeaponType.F_BALLISTIC)) {
	        			imgBase = "ballistic";	
	        		} else if (equipmentType.hasFlag(WeaponType.F_ARTILLERY)) {
	        			imgBase = "artillery";	
	        		}	        		
        		}
        		
        		break;
        	case Part.REPAIR_PART_TYPE.MEK_LOCATION:
        		imgBase = "location_mek";
        		break;
        	case Part.REPAIR_PART_TYPE.PHYSICAL_WEAPON:
        		imgBase = "melee";
        		break;
        }

        if (null == imgBase) {
        	imgBase = "equipment";
        }


        String[] imgData = new String[2];
        imgData[0] = "data/images/misc/repair/";
        imgData[1] = imgBase;

        return imgData;
	}
}

