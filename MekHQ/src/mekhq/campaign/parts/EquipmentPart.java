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
public class EquipmentPart extends Part {
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
    
    public void setEquipmentNum(int n) {
    	this.equipmentNum = n;
    }
    
    public EquipmentPart() {
    	this(false, 0, null, -1);
    }
    
    public EquipmentPart(boolean salvage, int tonnage, EquipmentType et, int equipNum) {
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
    }

    /**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer detail, boolean ignoreAmmo)
     *
     * @param entity The entity the Equipment comes from / is added to
     */
    private void computeCost (Entity entity) {
        if (entity == null)
            return;

        EquipmentType type = getType();
        float weight = entity.getWeight();
        int cost = 0;

        if (type instanceof MiscType
                && type.hasFlag(MiscType.F_LASER_HEAT_SINK)) {
            // TODO Laser heat sink cost ?
            cost = 6000;
        } else if (type instanceof MiscType
                && type.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
            cost = 6000;
        } else if (type instanceof MiscType
                && type.hasFlag(MiscType.F_HEAT_SINK)) {
            cost = 2000;
        } else if (entity instanceof megamek.common.Mech
                && type instanceof MiscType
                && type.hasFlag(MiscType.F_JUMP_JET)) {
            megamek.common.Mech mech = (megamek.common.Mech) entity;

            double jumpBaseCost = 200;
            // You cannot have JJ's and UMU's on the same unit.
            double c = 0;
            
            if (mech.hasUMU()) {
                c = Math.pow(mech.getAllUMUCount(), 2.0) * weight * jumpBaseCost;
            } else {
                if (mech.getJumpType() == megamek.common.Mech.JUMP_BOOSTER) {
                    jumpBaseCost = 150;
                } else if (mech.getJumpType() == megamek.common.Mech.JUMP_IMPROVED) {
                    jumpBaseCost = 500;
                }
                c = Math.pow(mech.getOriginalJumpMP(), 2.0) * weight * jumpBaseCost;
            }

            cost = (int) c;
        } else {
            // TODO take isArmored into account
            boolean isArmored = false;

            // TODO set isWeaponGroup correctly
            boolean isWeaponGroup = false;

            if (isWeaponGroup) {
                this.cost = 2;
                return ;
            }

            int itemCost = (int) type.getCost(entity, isArmored);
            if (itemCost == EquipmentType.COST_VARIABLE) {
                itemCost = type.resolveVariableCost(entity, isArmored);
            }

            cost = itemCost;
        }

        if (cost > 100000000 || cost < 0) {
            cost = 0;
        }

        if (cost == 0) {
            // Some equipments do not have a price set in megamek
            // Check if ssw has the price
        	//NO - NO SSW IN MHQ - lets figure out what items are missing in MM and fix them
        	/*
            abPlaceable placeable = null;
            
            for (String sswName : getPotentialSSWNames(Faction.F_FEDSUN)) {
                placeable = SSWLibHelper.getAbPlaceableByName(Campaign.getSswEquipmentFactory(), Campaign.getSswMech(), sswName);
                if (placeable != null)
                    break;
            }
            
            if (placeable != null)
                cost = (int) placeable.GetCost();
           */
        }

        if (cost > 100000000 || cost < 0) {
            cost = 0;
        }

        this.cost = cost;
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
		hits = 0;
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
		return new MissingEquipmentPart(isSalvage(), getTonnage(), type, equipmentNum);
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
				unit.campaign.removePart(this);
			}
	        unit.removePart(this);
	        Part missing = getMissingPart();
			unit.campaign.addPart(missing);
			unit.addPart(missing);
		}
		unit = null;
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
				difficulty = +2;
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
}
