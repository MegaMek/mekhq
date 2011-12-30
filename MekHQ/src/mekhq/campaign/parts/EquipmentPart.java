/*
 * EquipmentPart.java
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
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.weapons.Weapon;
import mekhq.campaign.Campaign;
import mekhq.campaign.Era;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentPart extends Part {
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
    
    public void setEquipmentNum(int n) {
    	this.equipmentNum = n;
    }
    
    public EquipmentPart() {
    	this(0, null, -1, null);
    }
    
    public EquipmentPart(int tonnage, EquipmentType et, int equipNum, Campaign c) {
        // TODO Memorize all entity attributes needed to calculate cost
        // As it is a part bought with one entity can be used on another entity
        // on which it would have a different price (only tonnage is taken into
        // account for compatibility)
        super(tonnage, c);
        this.type =et;
        if(null != type) {
        	this.name = type.getName();
        	this.typeName = type.getInternalName();
        }
        this.equipmentNum = equipNum;
    }

    public EquipmentPart clone() {
    	return new EquipmentPart(getUnitTonnage(), type, equipmentNum, campaign);
    }
    
    @Override
    public double getTonnage() {
    	//TODO: we need to copy the code from EquipmentType to generate item tonnage and
    	//calculate it from the entity or feed it in from the parts store
    	Entity en = null;
    	if(null != unit) {
    		en = unit.getEntity();
    	}
    	double ton = 0;
    	try {
    		ton = type.getTonnage(en);
    	} catch(NullPointerException ex) {
        	System.out.println("Found a null entity while calculating weight for " + name);
        }
    	return ton;
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
                    + typeName + "\"");
        }
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
    	if(needsFixing() || part.needsFixing()) {
    		return false;
    	}
        boolean b =  part instanceof EquipmentPart
                        && getType().equals( ((EquipmentPart)part).getType() );
        if (getType().getCost(null, false) == EquipmentType.COST_VARIABLE) {
        	//TODO: this needs a lot of work. There are other potential conditions here, I think
            return b && getUnitTonnage() == part.getUnitTonnage();
        }
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
    public int getTechLevel() {
        if (getType().getTechLevel() < 0 || getType().getTechLevel() >= TechConstants.SIZE)
            return TechConstants.T_IS_TW_NON_BOX;
        else
            return getType().getTechLevel();
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipmentNum>"
				+equipmentNum
				+"</equipmentNum>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+type.getInternalName()
				+"</typeName>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
				equipmentNum = Integer.parseInt(wn2.getTextContent());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			}
		}
		restore();
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
		super.fix();
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				mounted.setHit(false);
		        mounted.setDestroyed(false);
		        unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
			}
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingEquipmentPart(getUnitTonnage(), type, equipmentNum, campaign);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				mounted.setHit(true);
		        mounted.setDestroyed(true);
		        unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));	
			}
	        if(!salvage) {
				campaign.removePart(this);
			}
	        unit.removePart(this);
	        Part missing = getMissingPart();
			campaign.addPart(missing);
			unit.addPart(missing);
		}
		setUnit(null);
		equipmentNum = -1;
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(!mounted.isRepairable()) {
					remove(false);
					return;
				} else if(mounted.isDestroyed()) {
					//TODO: calculate actual hits
					hits = 1;
				} else {
					hits = 0;
				}
			}
			if(hits == 0) {
				time = 0;
				difficulty = 0;
			} else if(hits == 1) {
				time = 100;
				difficulty = -3;
			} else if(hits == 2) {
				time = 150;
				difficulty = -2;
			} else if(hits == 3) {
				time = 200;
				difficulty = 0;
			} else if(hits > 3) {
				time = 250;
				difficulty = 2;
			}
			if(isSalvaging()) {
				this.time = 120;
				this.difficulty = 0;
			}
		}
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}
	

    @Override
    public String getDetails() {
    	if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted && mounted.getLocation() != -1) {
				return unit.getEntity().getLocationName(mounted.getLocation()) + ", " + super.getDetails();
			}
    	}
    	return super.getDetails();
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

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				if(hits >= 1) {
					mounted.setDestroyed(true);
					mounted.setHit(true);
					mounted.setRepairable(true);
			        unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));	
				} else {
					mounted.setHit(false);
			        mounted.setDestroyed(false);
			        mounted.setRepairable(true);
			        unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
				}
			}
		}
	}
	
	@Override
    public String checkFixable() {
		if(isSalvaging()) {
			return null;
		}
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
	
	/**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     *
     */
    @Override
    public long getStickerPrice() {
    	//OK, we cant use the resolveVariableCost methods from megamek, because they
    	//rely on entity which may be null if this is a spare part. So we use our 
    	//own resolveVariableCost method
    	//TODO: we need a static method that returns whether this equipment type depends upon
    	// - unit tonnage
    	// - item tonnage
    	// - engine
    	// use that to determine how to add things to the parts store and to 
    	// determine whether what can be used as a replacement
    	//why does all the proto ammo have no cost?
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
        		itemCost = resolveVariableCost(isArmored);
        	}
        } catch(NullPointerException ex) {
        	System.out.println("Found a null entity while calculating cost for " + name);
        }
        return itemCost;
    }
    
    private int resolveVariableCost(boolean isArmored) {
    	int varCost = 0;
        if (type instanceof MiscType) {
            if (type.hasFlag(MiscType.F_MASC)) {
            	//TODO: account for protomechs
               /* if (entity instanceof Protomech) {
                    varCost = Math.round(entity.getEngine().getRating() * 1000 * entity.getWeight() * 0.025f);
                } else */
            	if (type.hasSubType(MiscType.S_SUPERCHARGER)) {
            		//TODO: need an engine rating
                    //Engine e = entity.getEngine();
            		Engine e = null;
                    if (e == null) {
                        varCost = 0;
                    } else {
                        varCost = e.getRating() * 10000;
                    }
                } else {
                    int mascTonnage = 0;
                    if (type.getInternalName().equals("ISMASC")) {
                        mascTonnage = Math.round(getUnitTonnage() / 20.0f);
                    } else if (type.getInternalName().equals("CLMASC")) {
                        mascTonnage = Math.round(getUnitTonnage() / 25.0f);
                    }
                    //varCost = entity.getEngine().getRating() * mascTonnage * 1000;
                }
            } else if (type.hasFlag(MiscType.F_TARGCOMP)) {
                int tCompTons = 0;
                //TODO: need to track equipment tonnage
                /*
                for (Mounted mo : entity.getWeaponList()) {
                    WeaponType wt = (WeaponType) mo.getType();
                    if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                        fTons += wt.getTonnage(entity);
                    }
                }
                */
                if (type.getInternalName().equals("ISTargeting Computer")) {
                    tCompTons = (int) Math.ceil(getTonnage() / 4.0f);
                } else if (type.getInternalName().equals("CLTargeting Computer")) {
                    tCompTons = (int) Math.ceil(getTonnage() / 5.0f);
                }
                varCost = tCompTons * 10000;
            } else if (type.hasFlag(MiscType.F_CLUB) && (type.hasSubType(MiscType.S_HATCHET) || type.hasSubType(MiscType.S_MACE_THB))) {
                int hatchetTons = (int) Math.ceil(getUnitTonnage() / 15.0);
                varCost = hatchetTons * 5000;
            } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_SWORD)) {
                int swordTons = (int) Math.ceil(getUnitTonnage() / 15.0);
                varCost = swordTons * 10000;
            } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                int bladeTons = (int) Math.ceil(0.5f + Math.ceil(getUnitTonnage() / 20.0));
                varCost = (1 + bladeTons) * 10000;
            } else if (type.hasFlag(MiscType.F_TRACKS)) {
            	//TODO: need engine
                //varCost = (int) Math.ceil((500 * entity.getEngine().getRating() * entity.getWeight()) / 75);
            } else if (type.hasFlag(MiscType.F_TALON)) {
                varCost = (int) Math.ceil(getUnitTonnage() * 300);
            }

        } else {
            if (varCost == 0) {
                // if we don't know what it is...
                System.out.println("I don't know how much " + name + " costs.");
            }
        }

        if (isArmored) {
        	//need a getCriticals command - but how does this work?
            //varCost += 150000 * getCriticals(entity);
        }
        return varCost;
    }
}
