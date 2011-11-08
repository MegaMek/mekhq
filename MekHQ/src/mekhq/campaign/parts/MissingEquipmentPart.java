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

import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.weapons.Weapon;
import mekhq.campaign.Era;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingEquipmentPart extends MissingPart {
	private static final long serialVersionUID = 2892728320891712304L;

	//crap equipmenttype is not serialized!
    protected transient EquipmentType type;
    protected String typeName;
	protected int equipmentNum = -1;

    public EquipmentType getType() {
        return type;
    }
    
    public int getEquipmentNum() {
    	return equipmentNum;
    }
    
    public MissingEquipmentPart() {
    	this(0, null, -1);
    }
    
    public MissingEquipmentPart(int tonnage, EquipmentType et, int equipNum) {
        // TODO Memorize all entity attributes needed to calculate cost
        // As it is a part bought with one entity can be used on another entity
        // on which it would have a different price (only tonnage is taken into
        // account for compatibility)
        super(tonnage);
        this.type =et;
        if(null != type) {
        	this.name = type.getName();
        	this.typeName = type.getInternalName();
        }
        this.equipmentNum = equipNum;
        this.time = 120;
        this.difficulty = 0;
    }
    
    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (typeName == null) {
        	typeName = type.getName();
        } else {
            type = EquipmentType.get(typeName);
        }

        if (type == null) {
            System.err
            .println("Mounted.restore: could not restore equipment type \""
                    + name + "\"");
        }
    }
    
    @Override
    public double getTonnage() {
    	if(null != unit) {
    		return type.getTonnage(unit.getEntity());
    	}
    	return 0;
    }
    
    /**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     */
    @Override
    public long getPurchasePrice() {
    	//costs are a total nightmare
        //some costs depend on entity, but we can't do it that way
        //because spare parts don't have entities. If parts start on an entity
        //thats fine, but this will become problematic when we set up a parts
        //store. For now I am just going to pass in a null entity and attempt
    	//to catch any resulting NPEs
    	Entity en = null;
    	boolean isArmored = false;
    	if (unit != null) {
            en = unit.getEntity();
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
            	isArmored = mounted.isArmored();
            }
    	}

        int itemCost = 0;      
        try {
        	itemCost = (int) type.getCost(en, isArmored);
        	if (itemCost == EquipmentType.COST_VARIABLE) {
        		itemCost = type.resolveVariableCost(en, isArmored);
        	}
        } catch(NullPointerException ex) {
        	System.out.println("Found a null entity while calculating cost for " + name);
        }
        return itemCost;
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
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+typeName
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipmentNum>"
				+equipmentNum
				+"</equipmentNum>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			}
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
                return type.equals(et) && getUnitTonnage() == part.getUnitTonnage();
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
		return new EquipmentPart(getUnitTonnage(), type, -1);
	}
	
	private boolean hasReallyCheckedToday() {
		return checkedToday;
	}
	
	@Override
	public boolean hasCheckedToday() {
		//if this unit has been checked for any other equipment of this same type
		//then return false, regardless of whether this one has been checked
		if(null != unit) {
			for(Part part : unit.getParts()) {
				if(part.getId() == getId()) {
					continue;
				}
				if(part instanceof MissingEquipmentPart 
						&& ((MissingEquipmentPart)part).getType().equals(type) 
						&& ((MissingEquipmentPart)part).hasReallyCheckedToday()) {
					return true;
				}
			}
		}
		return super.hasCheckedToday();
	}
	
	public int getLocation() {
    	if(null != unit) {
    		Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				return mounted.getLocation();
			}
    	}
    	return -1;
    }
}
