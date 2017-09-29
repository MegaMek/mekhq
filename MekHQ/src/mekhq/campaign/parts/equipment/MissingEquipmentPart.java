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

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

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
	protected double equipTonnage;

    public EquipmentType getType() {
        return type;
    }

    public int getEquipmentNum() {
    	return equipmentNum;
    }

    public void setEquipmentNum(int num) {
    	equipmentNum = num;
    }

    public MissingEquipmentPart() {
    	this(0, null, -1, null, 0, false);
    }
    
    public MissingEquipmentPart(int tonnage, EquipmentType et, int equipNum, Campaign c, double eTonnage) {
        this(tonnage, et, equipNum, c, eTonnage, false);
    }

    public MissingEquipmentPart(int tonnage, EquipmentType et, int equipNum, Campaign c,
            double eTonnage, boolean omniPodded) {
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
        this.equipTonnage = eTonnage;
        this.omniPodded = omniPodded;
    }

    @Override
	public int getBaseTime() {
		return isOmniPodded()? 30 : 120;
	}

	@Override
	public int getDifficulty() {
		return 0;
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
    	return equipTonnage;
    }

    @Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<typeName>"
				+MekHqXmlUtil.escape(typeName)
				+"</typeName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<equipmentNum>"
				+equipmentNum
				+"</equipmentNum>");
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
			} else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
				typeName = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
				equipTonnage = Double.parseDouble(wn2.getTextContent());
			}
		}
		restore();
	}
	
	@Override
	public TechAdvancement getTechAdvancement() {
	    return type.getTechAdvancement();
	}

	@Override
	public void fix() {
		Part replacement = findReplacement(false);
		if(null != replacement) {
			Part actualReplacement = replacement.clone();
			unit.addPart(actualReplacement);
			campaign.addPart(actualReplacement, 0);
			replacement.decrementQuantity();
			((EquipmentPart)actualReplacement).setEquipmentNum(equipmentNum);
			remove(false);
			//assign the replacement part to the unit
			actualReplacement.updateConditionFromPart();
		}
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		//According to official answer, if sticker prices are different then
		//they are not acceptable substitutes, so we need to check for that as
		//well
		//http://bg.battletech.com/forums/strategic-operations/(answered)-can-a-lance-for-a-35-ton-mech-be-used-on-a-40-ton-mech-and-so-on/
		Part newPart = getNewPart();
		newPart.setUnit(unit);
		if(part instanceof EquipmentPart) {
			EquipmentPart eqpart = (EquipmentPart)part;
			EquipmentType et = eqpart.getType();
			return type.equals(et) && getTonnage() == part.getTonnage() && part.getStickerPrice() == newPart.getStickerPrice();
		}
		return false;
	}

	@Override
    public String checkFixable() {
	    // The part is only fixable if the location is not destroyed.
        // be sure to check location and second location
        if(null != unit) {
            Mounted m = unit.getEntity().getEquipment(equipmentNum);
            if(null != m) {
                int loc = m.getLocation();
                if(loc == -1) {
                }
                if (unit.isLocationBreached(loc)) {
                    return unit.getEntity().getLocationName(loc) + " is breached.";
                }
                if (unit.isLocationDestroyed(loc)) {
                    return unit.getEntity().getLocationName(loc) + " is destroyed.";
                }
                loc = m.getSecondLocation();
                if(loc != Entity.LOC_NONE) {
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
	public boolean onBadHipOrShoulder() {
		if(null != unit) {
			for(int loc = 0; loc < unit.getEntity().locations(); loc++) {
	            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
	                CriticalSlot slot = unit.getEntity().getCritical(loc, i);

	                // ignore empty & system slots
	                if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
	                    continue;
	                }
	                Mounted equip = unit.getEntity().getEquipment(equipmentNum);
	                Mounted m1 = slot.getMount();
	                Mounted m2 = slot.getMount2();
	                if (m1 == null && m2 == null) {
	                	continue;
	                }
	                if ((equip.equals(m1)) || (equip.equals(m2))) {
	                    if (unit.hasBadHipOrShoulder(loc)) {
	                        return true;
	                    }
	                }
	            }
	        }
		}
		return false;
	}

	@Override
    public void setUnit(Unit u) {
    	super.setUnit(u);
    	if(null != unit) {
    		equipTonnage = type.getTonnage(unit.getEntity());
    	}
    }

	@Override
	public Part getNewPart() {
		EquipmentPart epart = new EquipmentPart(getUnitTonnage(), type, -1, omniPodded, campaign);
		epart.setEquipTonnage(equipTonnage);
		return epart;
	}
/*
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
*/

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
				mounted.setHit(true);
		        mounted.setDestroyed(true);
		        mounted.setRepairable(false);
		        unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
			}
		}
	}

	@Override
    public boolean isOmniPoddable() {
        if (type.isOmniFixedOnly()) {
            return false;
        }
        if (type instanceof MiscType) {
            return type.hasFlag(MiscType.F_MECH_EQUIPMENT)
                    || type.hasFlag(MiscType.F_TANK_EQUIPMENT)
                    || type.hasFlag(MiscType.F_FIGHTER_EQUIPMENT);
        } else if (type instanceof WeaponType) {
            return (type.hasFlag(WeaponType.F_MECH_WEAPON)
                    || type.hasFlag(WeaponType.F_TANK_WEAPON)
                    || type.hasFlag(WeaponType.F_AERO_WEAPON))
                    && !((WeaponType)type).isCapital();
        }
        return true;
    }

	@Override
	public String getLocationName() {
		if(null != unit) {
			Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
			if(null != mounted && mounted.getLocation() != -1) {
				return unit.getEntity().getLocationName(mounted.getLocation());
			}
    	}
		return null;
	}

	@Override
    public boolean isInLocation(String loc) {
		if(null == unit || null == unit.getEntity() || null == unit.getEntity().getEquipment(equipmentNum)) {
			return false;
		}

		Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
		if(null == mounted) {
			return false;
		}
		int location = unit.getEntity().getLocationFromAbbr(loc);
		for (int i = 0; i < unit.getEntity().getNumberOfCriticals(location); i++) {
	            CriticalSlot slot = unit.getEntity().getCritical(location, i);
	            // ignore empty & non-hittable slots
	            if ((slot == null) || !slot.isEverHittable() || slot.getType()!=CriticalSlot.TYPE_EQUIPMENT
	            		|| null == slot.getMount()) {
	                continue;
	            }
	            if(unit.getEntity().getEquipmentNum(slot.getMount()) == equipmentNum) {
	            	return true;
	            }
		}
		//if we are still here, lets just double check by the mounted's location and secondary location
		if(mounted.getLocation() == location) {
			return true;
		}
		if(mounted.getSecondLocation() == location) {
			return true;
		}
		return false;
    }
}
