/*
 * MissingEquipmentPart.java
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

import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.weapons.Weapon;
import mekhq.campaign.Era;
import mekhq.campaign.Faction;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.work.EquipmentRepair;
import mekhq.campaign.work.EquipmentReplacement;
import mekhq.campaign.work.EquipmentSalvage;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingEquipmentPart extends MissingPart {
	private static final long serialVersionUID = 2892728320891712304L;

	//crap equipmenttype is not serialized!
    protected transient EquipmentType type;

	private int equipmentNum = -1;

    public EquipmentType getType() {
        return type;
    }
    
    public int getEquipmentNum() {
    	return equipmentNum;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }
    
    public MissingEquipmentPart() {
    	this(false, 0, null, -1);
    }
    
    public MissingEquipmentPart(boolean salvage, int tonnage, EquipmentType et, int equipNum) {
        // TODO Memorize all entity attributes needed to calculate cost
        // As it is a part bought with one entity can be used on another entity
        // on which it would have a different price (only tonnage is taken into
        // account for compatibility)
        super(salvage, tonnage);
        this.type =et;
        if(null != type) {
        	this.name = type.getName();
        }
        this.equipmentNum = equipNum;
        this.time = 120;
        this.difficulty = 0;
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        if(task instanceof EquipmentReplacement) {
            EquipmentType et = ((EquipmentReplacement)task).getMounted().getType();
            if (et.getCost(null, false) == EquipmentType.COST_VARIABLE) {
                // In this case tonnage matters (ex. : hartchet, sword, ...
                return type.equals(et) && getTonnage() == ((EquipmentReplacement)task).getUnit().getEntity().getWeight();
            } else {
                return type.equals(et);
            }
        }
        return false;
    }
    
    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (name == null) {
            name = type.getName();
        } else {
            type = EquipmentType.get(name);
        }

        if (type == null) {
            System.err
            .println("Mounted.restore: could not restore equipment type \""
                    + name + "\"");
        }
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        boolean b =  part instanceof EquipmentPart
                        && getName().equals(part.getName())
                        && getStatus().equals(part.getStatus())
                        && getType().equals( ((EquipmentPart)part).getType() );
        if (getType().getCost(null, false) == EquipmentType.COST_VARIABLE)
            return b && getTonnage() == part.getTonnage();
        else
            return b;
    }

    @Override
    public int getPartType() {
        if (getType() instanceof Weapon)
            return PART_TYPE_WEAPON;
        else if (getType() instanceof AmmoType)
            return PART_TYPE_AMMO;
        else
            return PART_TYPE_EQUIPMENT_PART;
    }

    @Override
    public boolean isClanTechBase() {
        String techBase = TechConstants.getTechName(getType().getTechLevel());

        if (techBase.equals("Clan"))
            return true;
        else if (techBase.equals("Inner Sphere"))
            return false;
        else
            return false;
    }

    @Override
    public int getTech () {
        if (getType().getTechLevel() < 0 || getType().getTechLevel() >= TechConstants.SIZE)
            return TechConstants.T_IS_TW_NON_BOX;
        else
            return getType().getTechLevel();
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);		
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
		}
	}

	@Override
	public int getAvailability(int era) {		
		return type.getAvailability(Era.convertEra(era));
	}

	@Override
	public int getTechRating() {
		return type.getTechRating();
	}

	@Override 
	public void fix() {
		Part replacement = findReplacement();
		if(null != replacement) {
			unit.addPart(replacement);
			((EquipmentPart)replacement).setEquipmentNum(equipmentNum);
			remove(false);
			//assign the replacement part to the unit			
			replacement.updateConditionFromPart();
		}
	}
	
	@Override
	public boolean isAcceptableReplacement(Part part) {
		if(part instanceof EquipmentPart) {
			EquipmentPart eqpart = (EquipmentPart)part;
			EquipmentType et = eqpart.getType();
            if (et.getCost(null, false) == EquipmentType.COST_VARIABLE) {
                // In this case tonnage matters (ex. : hartchet, sword, ...
                return type.equals(et) && getTonnage() == part.getTonnage();
            } else {
                return type.equals(et);
            }
		}
		return false;
	}

	@Override
    public String checkFixable() {
        // The part is only fixable if the location is not destroyed.
        // We have to cycle through all locations because some equipment is spreadable.
        for(int loc = 0; loc < unit.getEntity().locations(); loc++) {
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                
                // ignore empty & system slots
                if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }
                
                if (equipmentNum == slot.getIndex()) {
                    if (unit.isLocationDestroyed(loc)) {
                        return unit.getEntity().getLocationName(loc) + " is destroyed.";
                    }
                }
            }
        }       
        return null;
    }

	@Override
	public Part getNewPart() {
		return new EquipmentPart(isSalvage(), getTonnage(), type, -1);
	}
}
