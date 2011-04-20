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
    
    public Armor() {
    	this(false, 0, 0, 0);
    }
    
    public Armor(boolean salvage, int tonnage, int t, int points) {
        // Amount is used for armor quantity, not tonnage
        super(false, tonnage);
        this.type = t;
        this.amount = points;

        reCalc();
    }
    
    @Override
    public void reCalc() {
        this.name = EquipmentType.getArmorTypeName(type) + " Armor";

        // TechBase needs to be set to calculate cost more precisely
        this.cost = (long) Math.round(getArmorWeight(getAmount()) * EquipmentType.getArmorCost(getType()));
    }
    
    public int getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
        reCalc();
    }
    
    @Override
    public String getDesc() {
        return super.getDesc() + " (" + getAmount() + ")";
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

    @Override
    public ArrayList<String> getPotentialSSWNames(int faction) {
        ArrayList<String> sswNames = new ArrayList<String>();

        // The tech base of the part doesn't matter (Clan and IS can use each other's Ferro-Fibrous armor)
        // However the tech base of the faction is important : Clans get Ferro-Fibrous armor before IS
        String techBase = (Faction.isClanFaction(faction) ? "(CL)" : "(IS)");

        String sswName = getName();

        sswNames.add(techBase + " " + sswName);
        sswNames.add(sswName);

        return sswNames;
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
    public String getSaveString () {
        return getName() + ";" + getTonnage() + ";" + getType() + ";" + getAmount();
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
}
