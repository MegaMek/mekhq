/*
 * Armor.java
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
import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Engine;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.campaign.Faction;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.work.ArmorReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Armor extends Part {
	private static final long serialVersionUID = 5275226057484468868L;
	protected int type;
    protected int amount;
    protected int amountNeeded;
    private int location;
    private boolean rear;
    
    public Armor() {
    	this(false, 0, 0, 0);
    }
    
    public Armor(boolean salvage, int tonnage, int t, int points) {
    	this(salvage, tonnage, t, points, -1, false);
    }
    
    public Armor(boolean salvage, int tonnage, int t, int points, int loc, boolean r) {
        // Amount is used for armor quantity, not tonnage
        super(false, tonnage);
        this.type = t;
        this.amount = points;
        this.location = loc;
        this.rear = r;
        this.name = "Armor (" + EquipmentType.armorNames[type] + ")";
    }
    
    @Override
	public String getDetails() {
		if(null != unit) {
			String rearMount = "";
			if(rear) {
				rearMount = " (R)";
			}
			return unit.getEntity().getLocationName(location) + rearMount + ", " + amountNeeded + " points";
		}
		return "";
	}
    
    public int getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }
    
    public int getLocation() {
    	return location;
    }
    
    public boolean isRearMounted() {
    	return rear;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof ArmorReplacement 
                && ((ArmorReplacement)task).getUnit().getEntity().getArmorType(((ArmorReplacement)task).getLoc()) == type;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof Armor
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getType() == ((Armor)part).getType();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_ARMOR;
    }

    @Override
    public boolean isClanTechBase() {
        // Armor tech base is not used (Clan/IS can use each other's armor for now)
        // TODO Set Tech base correctly for armor
        // Clan FF and IS FF do not have the same armor points per ton
        return false;
    }

    @Override
    public int getTech () {
        // Armor tech base is not used (Clan/IS can use each other's armor for now)
        // TODO Set Tech base correctly for armor
        // Clan FF and IS FF do not have the same armor points per ton
        return TechConstants.T_INTRO_BOXSET;
    }

    public double getArmorWeight(int points) {
        // from megamek.common.Entity.getArmorWeight()
        
        // this roundabout method is actually necessary to avoid rounding
        // weirdness. Yeah, it's dumb.

        boolean isClanArmor = false;
        if (isClanTechBase())
            isClanArmor= true;

        double armorPointMultiplier = EquipmentType.getArmorPointMultiplier(getType(), isClanArmor);
        double armorPerTon = 16.0 * armorPointMultiplier;
        if (getType() == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        
        double armorWeight = points / armorPerTon;
        armorWeight = Math.ceil(armorWeight * 2.0) / 2.0;
        return armorWeight;
    }

    public int getCost (int amount) {
        return (int) Math.round(getArmorWeight(amount) * EquipmentType.getArmorCost(getType()));
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<amount>"
				+amount
				+"</amount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<location>"
				+type
				+"</location>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<rear>"
				+rear
				+"</rear>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<amountNeeded>"
				+amountNeeded
				+"</amountNeeded>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("amount")) {
				amount = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("location")) {
				location = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("amountNeeded")) {
				amountNeeded = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("rear")) {
				if(wn2.getTextContent().equalsIgnoreCase("true")) {
					rear = true;
				} else {
					rear = false;
				}
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		switch(type) {
		case EquipmentType.T_ARMOR_FERRO_FIBROUS:
		case EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_D;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_F;
			} else {
				return EquipmentType.RATING_D;
			}
		case EquipmentType.T_ARMOR_LIGHT_FERRO:
		case EquipmentType.T_ARMOR_HEAVY_FERRO:
		case EquipmentType.T_ARMOR_STEALTH:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_E;
			}
		case EquipmentType.T_ARMOR_INDUSTRIAL:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_B;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_C;
			} else {
				return EquipmentType.RATING_B;
			}
		case EquipmentType.T_ARMOR_COMMERCIAL:	
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_B;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_B;
			} else {
				return EquipmentType.RATING_A;
			}
		case EquipmentType.T_ARMOR_REACTIVE:
		case EquipmentType.T_ARMOR_REFLECTIVE:
		case EquipmentType.T_ARMOR_HARDENED:
		case EquipmentType.T_ARMOR_PATCHWORK:
		case EquipmentType.T_ARMOR_FERRO_IMP:
		case EquipmentType.T_ARMOR_FERRO_CARBIDE:
		case EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE:
		case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_F;
			}
		default:
			return EquipmentType.RATING_C;	
		}
	}

	@Override
	public int getTechRating() {
		switch(type) {
		case EquipmentType.T_ARMOR_INDUSTRIAL:
			return EquipmentType.RATING_C;
		case EquipmentType.T_ARMOR_COMMERCIAL:	
			return EquipmentType.RATING_B;
		case EquipmentType.T_ARMOR_FERRO_FIBROUS:
		case EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO:		
		case EquipmentType.T_ARMOR_LIGHT_FERRO:
		case EquipmentType.T_ARMOR_HEAVY_FERRO:
		case EquipmentType.T_ARMOR_STEALTH:
			return EquipmentType.RATING_E;	
		case EquipmentType.T_ARMOR_HARDENED:
			return EquipmentType.RATING_D;
		case EquipmentType.T_ARMOR_REACTIVE:
		case EquipmentType.T_ARMOR_REFLECTIVE:
			return EquipmentType.RATING_E;
		case EquipmentType.T_ARMOR_PATCHWORK:
		case EquipmentType.T_ARMOR_FERRO_IMP:
		case EquipmentType.T_ARMOR_FERRO_CARBIDE:
		case EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE:
		case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
			return EquipmentType.RATING_F;
		default:
			return EquipmentType.RATING_D;	
		}
	}

	@Override
	public void fix() {
		unit.getEntity().setArmor(unit.getEntity().getOArmor(location, rear), location, rear);
		amountNeeded = 0;
	}

	@Override
	public Part getReplacementPart() {
		//no such thing
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		//don't do anything here yet, because we don't actually remove armor, just set it at zero
		//in the future, we need a method for adding armor points to salvage parts
	}

	@Override
	public void updateCondition() {
		int currentArmor = unit.getEntity().getArmor(location, rear);
		if(currentArmor < 0) {
			currentArmor = 0;
		}
		amountNeeded = unit.getEntity().getOArmor(location, rear) - currentArmor;
		if(amountNeeded > 0) {
			time = 5 * amountNeeded;
			difficulty = -2;
		} else {
			time = 0;
			difficulty = 0;
		}
	}

	@Override
	public boolean needsFixing() {
		return amountNeeded > 0;
	}
}
