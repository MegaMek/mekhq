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
package mekhq.campaign.parts.equipment;

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
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This part covers most of the equipment types in WeaponType, AmmoType, and MiscType
 * It can robustly handle all equipment with static weights and costs. It can also
 * handle equipment whose only variability in terms of cost is the equipment tonnage itself.
 * More complicated variable weight/cost equipment needs to be subclassed.
 * Some examples of equipment that needs to be subclasses:
 * 	- MASC (depends on engine rating)
 *  - AES (depends on location and cost is by unit tonnage)
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentPart extends Part {
	private static final long serialVersionUID = 2892728320891712304L;

	//crap equipmenttype is not serialized!
    protected transient EquipmentType type;
    protected String typeName;
	protected int equipmentNum = -1;
	protected double equipTonnage;

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
        super(tonnage, c);
        this.type =et;
        if(null != type) {
        	this.name = type.getName();
        	this.typeName = type.getInternalName();
        }
        this.equipmentNum = equipNum;
        if(null != type) {
	        try {
	        	equipTonnage = type.getTonnage(null);
	        } catch(NullPointerException ex) {
	        	//System.out.println("Found a null entity while calculating tonnage for " + name);
	        }
        }
    }
    
    @Override
    public void setUnit(Unit u) {
    	super.setUnit(u);
    	if(null != unit) {
    		equipTonnage = type.getTonnage(unit.getEntity());
    	}
    }
    
    public void setEquipTonnage(double ton) {
    	equipTonnage = ton;
    }

    public EquipmentPart clone() {
    	EquipmentPart clone = new EquipmentPart(getUnitTonnage(), type, equipmentNum, campaign);
        clone.copyBaseData(this);
        if(hasVariableTonnage(type)) {
            clone.setEquipTonnage(equipTonnage);
        }
    	return clone;
    }
    
    @Override
    public double getTonnage() {
        return equipTonnage;
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
            return;
        }
        try {
        	equipTonnage = type.getTonnage(null);
        } catch(NullPointerException ex) {
        	//System.out.println("Found a null entity while calculating tonnage for " + name);
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof EquipmentPart
        		&& getType().equals(((EquipmentPart)part).getType())
        		&& getTonnage() == part.getTonnage();
    }

    @Override
    public boolean isSameStatus(Part part) {
    	return super.isSameStatus(part) && (this.getHits() > 0) == (part.getHits() > 0) && this.getDifficulty() == part.getDifficulty();
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
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipmentNum>"
				+equipmentNum
				+"</equipmentNum>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+MekHqXmlUtil.escape(type.getInternalName())
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipTonnage>"
				+equipTonnage
				+"</equipTonnage>");
		writeToXmlEnd(pw1, indent);
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
			else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
				equipTonnage = Double.parseDouble(wn2.getTextContent());
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
				mounted.setMissing(false);
		        mounted.setDestroyed(false);
		        unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
			}
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingEquipmentPart(getUnitTonnage(), type, equipmentNum, campaign, equipTonnage);
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
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				int number = quantity;
				while(number > 0) {
					spare.incrementQuantity();
					number--;
				}
				campaign.removePart(this);
			}
	        unit.removePart(this);
	        Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing);
		}
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
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
				}
				hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, equipmentNum, mounted.getLocation());
				if(mounted.isSplit()) {
				hits += unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, equipmentNum, mounted.getSecondLocation());
				}
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
    
    public boolean isRearFacing() {
    	if(null != unit) {
    		Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				return mounted.isRearMounted();
			}
    	}
    	return false;
    }

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted) {
				mounted.setMissing(false);
				if(hits >= 1) {
					mounted.setDestroyed(true);
					mounted.setHit(true);
					mounted.setRepairable(true);
			        unit.damageSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted), hits);	
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
                	if (unit.isLocationBreached(loc)) {
                		return unit.getEntity().getLocationName(loc) + " is breached.";
                	}
                    if (unit.isLocationDestroyed(loc)) {
                        return unit.getEntity().getLocationName(loc) + " is destroyed.";
                    }
                }
            }
        }       
        return null;
    }
	
	@Override
	public boolean isMountedOnDestroyedLocation() {
		if(null == unit) {
			return false;
		}
		for(int loc = 0; loc < unit.getEntity().locations(); loc++) {
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                
                // ignore empty & system slots
                if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }
                
                if (equipmentNum == slot.getIndex()) {
                    if (unit.isLocationDestroyed(loc)) {
                        return true;
                    }
                }             
            }
        }     
		return false;
	}
	
	@Override
	public boolean onBadHipOrShoulder() {
		if(null != unit) {
			for(int loc = 0; loc < unit.getEntity().locations(); loc++) {
	            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
	                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
	                
	                // ignore empty & system slots
	                if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
	                    continue;
	                }
	                
	                if (equipmentNum == slot.getIndex()) {
	                    if (unit.hasBadHipOrShoulder(loc)) {
	                        return true;
	                    }
	                }
	            }
	        }    
		}
		return false;
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
        double itemCost = type.getRawCost();   
        if (itemCost == EquipmentType.COST_VARIABLE) {
            itemCost = resolveVariableCost(isArmored);
        }
    	if (unit != null) {
            en = unit.getEntity();
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
            	isArmored = mounted.isArmored();
            }
            type.getCost(en, isArmored, getLocation());
    	}
    	int finalCost = (int)itemCost;
    	if (isArmored) {
            //need a getCriticals command - but how does this work?
            //finalCost += 150000 * getCriticals(entity);
        }
        return finalCost;
    }
    
    private int resolveVariableCost(boolean isArmored) {
    	double varCost = 0;
        if (type instanceof MiscType) {           
            if (type.hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)) {
                varCost = getTonnage() * 10000;
            } else if (type.hasFlag(MiscType.F_FLOTATION_HULL) || type.hasFlag(MiscType.F_VACUUM_PROTECTION) || type.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING) || type.hasFlag(MiscType.F_OFF_ROAD)) {
                //??
            } else if (type.hasFlag(MiscType.F_LIMITED_AMPHIBIOUS) || type.hasFlag((MiscType.F_FULLY_AMPHIBIOUS))) {
                varCost = getTonnage() * 10000;
            } else if (type.hasFlag(MiscType.F_DUNE_BUGGY)) {
                varCost = 10 * getTonnage() * getTonnage();
            } else if (type.hasFlag(MiscType.F_MASC) && type.hasFlag(MiscType.F_BA_EQUIPMENT)) {
                //TODO: handle this one differently
                //costValue = entity.getRunMP() * 75000;
            } else if (type.hasFlag(MiscType.F_HEAD_TURRET) || type.hasFlag(MiscType.F_SHOULDER_TURRET) || type.hasFlag(MiscType.F_QUAD_TURRET)) {
                varCost = getTonnage() * 10000;
            } else if (type.hasFlag(MiscType.F_SPONSON_TURRET)) {
                varCost = getTonnage() * 4000;
            } else if (type.hasFlag(MiscType.F_PINTLE_TURRET)) {
                varCost = getTonnage() * 1000;
            } else if (type.hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                //TODO: handle this through motive system part
                varCost = getTonnage() * 100000;
            } else if (type.hasFlag(MiscType.F_JET_BOOSTER)) {
                //TODO: Handle this one through subtyping
                //varCost = entity.getEngine().getRating() * 10000;
            } else if (type.hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                varCost = (getTonnage() * 10000) + 5000;
            } else if (type.hasFlag(MiscType.F_TARGCOMP)) {
                varCost = getTonnage() * 10000;
            } else if (type.hasFlag(MiscType.F_CLUB) && (type.hasSubType(MiscType.S_HATCHET) || type.hasSubType(MiscType.S_MACE_THB))) {
                varCost = getTonnage() * 5000;
            } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_SWORD)) {
                varCost = getTonnage() * 10000;
            } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                varCost = (1 + getTonnage()) * 10000;
            } else if (type.hasFlag(MiscType.F_TRACKS)) {
                //TODO: Handle this through subtyping
                //varCost = (int) Math.ceil((500 * entity.getEngine().getRating() * entity.getWeight()) / 75);
            } else if (type.hasFlag(MiscType.F_TALON)) {
                varCost = (int) Math.ceil(getTonnage() * 300);
            } else if (type.hasFlag(MiscType.F_SPIKES)) {
                varCost = (int) Math.ceil(getTonnage() * 50);
            } else if (type.hasFlag(MiscType.F_PARTIAL_WING)) {
                varCost = (int) Math.ceil(getTonnage() * 50000);
            } else if (type.hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                //TODO: subtype this one
                //int multiplier = entity.locationIsLeg(loc) ? 700 : 500;
                //costValue = (int) Math.ceil(entity.getWeight() * multiplier);
            } else if (type.hasFlag(MiscType.F_HAND_WEAPON) && (type.hasSubType(MiscType.S_CLAW))) {
                varCost = (int) Math.ceil(getUnitTonnage() * 200);
            } else if (type.hasFlag(MiscType.F_CLUB) && (type.hasSubType(MiscType.S_LANCE))) {
                varCost = (int) Math.ceil(getUnitTonnage() * 150);
            }

        } 
        if (varCost == 0) {
          // if we don't know what it is...
          System.out.println("I don't know how much " + name + " costs.");
        }      
        return (int) Math.ceil(varCost);
    }
    
    /*
     * The following static functions help the parts store determine how to handle 
     * variable weight equipment. If the type returns true to hasVariableTonnage
     * then the parts store will use a for loop to create equipment of the given tonnage
     * using the other helper functions. Note that this should not be used for supclassed
     * equipment parts whose "uniqueness" depends on more than the item tonnage
     */
    public static boolean hasVariableTonnage(EquipmentType type) {
    	return type.hasFlag(MiscType.F_TARGCOMP) ||
    			type.hasFlag(MiscType.F_CLUB) ||
    			type.hasFlag(MiscType.F_TALON);    			
    }
    
    public static double getStartingTonnage(EquipmentType type) {
    	return 1;
    }
    
    public static double getMaxTonnage(EquipmentType type) {
    	if (type.hasFlag(MiscType.F_TALON)|| (type.hasFlag(MiscType.F_CLUB) && (type.hasSubType(MiscType.S_HATCHET) || type.hasSubType(MiscType.S_MACE_THB)))) {
            return 7;
        } else if (type.hasFlag(MiscType.F_CLUB) && (type.hasSubType(MiscType.S_LANCE) || type.hasSubType(MiscType.S_SWORD))) {
            return 5;
        } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_MACE)) {
            return 10;
        } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
            return 5.5;
        } else if (type.hasFlag(MiscType.F_TARGCOMP)) {
        	//direct fire weapon weight divided by 4  - what is reasonably the highest - 15 tons?
        	return 15;
        }
    	return 1;
    }
    
    public static double getTonnageIncrement(EquipmentType type) {
    	if((type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_RETRACTABLE_BLADE))) {
    		return 0.5;
    	}
    	return 1;
    }
    
    @Override
    public boolean isPartForCriticalSlot(int index, int loc) {
    	return equipmentNum == index;
    }
    
    @Override
    public boolean isOmniPoddable() {
    	//TODO: is this on equipment type?
    	return true;
    }
}
