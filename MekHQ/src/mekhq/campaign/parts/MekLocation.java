/*
 * Location.java
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

import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.Modes;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLocation extends Part {
	private static final long serialVersionUID = -122291037522319765L;
	protected int loc;
    protected int structureType;
    protected boolean tsm;
    double percent;
    boolean breached;
    boolean blownOff;
    boolean forQuad;

    public MekLocation() {
    	this(0, 0, 0, false, false, null);
    }
    
    public MekLocation clone() {
    	MekLocation clone = new MekLocation(loc, getUnitTonnage(), structureType, tsm, forQuad, campaign);
        clone.copyBaseData(this);
    	clone.percent = this.percent;
    	clone.breached = this.breached;
    	clone.blownOff = this.blownOff;
    	return clone;
    }
    
    public int getLoc() {
        return loc;
    }

    public boolean isTsm() {
        return tsm;
    }

    public int getStructureType() {
        return structureType;
    }
    
    public MekLocation(int loc, int tonnage, int structureType, boolean hasTSM, boolean quad, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.tsm = hasTSM;
        this.percent = 1.0;
        this.forQuad = quad;
        this.breached = false;
        //TODO: need to account for internal structure and myomer types
        //crap, no static report for location names?
        this.name = "Mech Location";
        switch(loc) {
        case(Mech.LOC_HEAD):
            this.name = "Mech Head";
            break;
        case(Mech.LOC_CT):
            this.name = "Mech Center Torso";
            break;
        case(Mech.LOC_LT):
            this.name = "Mech Left Torso";
            break;
        case(Mech.LOC_RT):
            this.name = "Mech Right Torso";
            break;
        case(Mech.LOC_LARM):
            this.name = "Mech Left Arm";
        	if(forQuad) {
        		this.name = "Mech Front Left Leg";
        	}
            break;
        case(Mech.LOC_RARM):
            this.name = "Mech Right Arm";
        	if(forQuad) {
        		this.name = "Mech Front Right Leg";
    		}
            break;
        case(Mech.LOC_LLEG):
            this.name = "Mech Left Leg";
        	if(forQuad) {
        		this.name = "Mech Rear Left Leg";
        	}
            break;
        case(Mech.LOC_RLEG):
            this.name = "Mech Right Leg";
        	if(forQuad) {
        		this.name = "Mech Rear Right Leg";
        	}
            break;
        }
        if(structureType != EquipmentType.T_STRUCTURE_STANDARD) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType) + ")";
        }
        if(tsm) {
            this.name += " (TSM)";
        }
    }
    
    public double getTonnage() {
    	//TODO: how much should this weigh?
    	return 0;
    }
    
    @Override
    public long getStickerPrice() {
        double totalStructureCost = EquipmentType.getStructureCost(getStructureType()) * getUnitTonnage();
        int muscCost = isTsm() ? 16000 : 2000;
        double totalMuscleCost = muscCost * getUnitTonnage();
        double cost = 0.1 * (totalStructureCost + totalMuscleCost);

        if (loc == Mech.LOC_HEAD) {
        	//TODO: make potential adjustments for cockpit, life support, and sensors
        }
        return (long) Math.round(cost);
    }

    private boolean isArm() {
		return loc == Mech.LOC_RARM || loc == Mech.LOC_LARM;
	}
	
	public boolean forQuad() {
		return forQuad;
	}
    
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof MekLocation
                && getLoc() == ((MekLocation)part).getLoc()
                && getUnitTonnage() == ((MekLocation)part).getUnitTonnage()
                && isTsm() == ((MekLocation)part).isTsm()
                && getStructureType() == ((MekLocation) part).getStructureType()
                && (!isArm() || forQuad == ((MekLocation)part).forQuad);
    }
    
    @Override
    public boolean isSameStatus(Part part) {
    	return super.isSameStatus(part) && this.getPercent() == ((MekLocation)part).getPercent();
    }

    public double getPercent() {
    	return percent;
    }
    
    @Override
    public int getPartType() {
        return PART_TYPE_MEK_BODY_PART;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<structureType>"
				+structureType
				+"</structureType>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<tsm>"
				+tsm
				+"</tsm>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<percent>"
				+percent
				+"</percent>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<forQuad>"
				+forQuad
				+"</forQuad>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<breached>"
				+breached
				+"</breached>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("loc")) {
				loc = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("structureType")) {
				structureType = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("percent")) {
				percent = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("tsm")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					tsm = true;
				else
					tsm = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("forQuad")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					forQuad = true;
				else
					forQuad = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("breached")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					breached = true;
				else
					breached = false;
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		switch(structureType) {
		case EquipmentType.T_STRUCTURE_ENDO_STEEL:
		case EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_D;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_F;
			} else {
				return EquipmentType.RATING_E;
			}
		case EquipmentType.T_STRUCTURE_ENDO_COMPOSITE:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_F;
			}
		case EquipmentType.T_STRUCTURE_REINFORCED:
		case EquipmentType.T_STRUCTURE_COMPOSITE:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_E;
			}
		case EquipmentType.T_STRUCTURE_INDUSTRIAL:
		default:
			return EquipmentType.RATING_C;	
		}
	}

	@Override
	public int getTechRating() {
		switch(structureType) {
		case EquipmentType.T_STRUCTURE_ENDO_STEEL:
		case EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE:
			return EquipmentType.RATING_E;
		case EquipmentType.T_STRUCTURE_ENDO_COMPOSITE:
		case EquipmentType.T_STRUCTURE_REINFORCED:
		case EquipmentType.T_STRUCTURE_COMPOSITE:
			return EquipmentType.RATING_E;
		case EquipmentType.T_STRUCTURE_INDUSTRIAL:
			return EquipmentType.RATING_C;
		default:
			return EquipmentType.RATING_D;
		}
		
	}
	
	@Override
	public int getTechLevel() {
		switch(structureType) {
		case EquipmentType.T_STRUCTURE_ENDO_COMPOSITE:
		case EquipmentType.T_STRUCTURE_REINFORCED:
		case EquipmentType.T_STRUCTURE_COMPOSITE:
	    case EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE:
			return TechConstants.T_IS_EXPERIMENTAL;
	    case EquipmentType.T_STRUCTURE_ENDO_STEEL:
            return TechConstants.T_IS_TW_NON_BOX;
		default:
		    if(tsm) {
		        return TechConstants.T_IS_TW_NON_BOX;
		    } else {
		        return TechConstants.T_INTRO_BOXSET;
		    }
		}
	}
	
	@Override
	public int getTechBase() {
		switch(structureType) {
		case EquipmentType.T_STRUCTURE_COMPOSITE:
			return T_IS;
		default:
			return T_BOTH;
		}
		
	}

	@Override
	public void fix() {
		super.fix();
		if(isBlownOff()) {
			blownOff = false;
			unit.getEntity().setLocationBlownOff(loc, false);
			for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
	            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
	            // ignore empty & non-hittable slots
	            if (slot == null) {
	                continue;
	            }
	            slot.setMissing(false);
	            Mounted m = slot.getMount();
	            if(null != m) {
	            	m.setMissing(false);
	            }
			}
		} else if(isBreached()) {
			breached = false;
			unit.getEntity().setLocationStatus(loc, ILocationExposureStatus.NORMAL, true);
			for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
	            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
	            // ignore empty & non-hittable slots
	            if (slot == null) {
	                continue;
	            }
	            slot.setBreached(false);
	            Mounted m = slot.getMount();
	            if(null != m) {
	            	m.setBreached(false);
	            }
			}
		} else {
			percent = 1.0;
			if(null != unit) {
				unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
			}
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingMekLocation(loc, getUnitTonnage(), structureType, tsm, forQuad, campaign);
	}

	@Override
	public void remove(boolean salvage) {
		blownOff = false;
		if(null != unit) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
			unit.getEntity().setLocationBlownOff(loc, false);
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
			if(loc != Mech.LOC_CT) {
				Part missing = getMissingPart();
				unit.addPart(missing);
				campaign.addPart(missing);
			}
			unit.runDiagnostic();
		}
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
	}
	
	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			blownOff = unit.getEntity().isLocationBlownOff(loc);
			breached = unit.isLocationBreached(loc);
			percent = ((double) unit.getEntity().getInternalForReal(loc)) / ((double) unit.getEntity().getOInternal(loc));
			if(percent <= 0.0) {
				remove(false);
				return;
			} 
		}
		if(blownOff) {
			if(loc == Mech.LOC_HEAD) {
				this.time = 200;
				this.difficulty = 2;
			} else {
				this.time = 180;
				this.difficulty = 1;
			}
		} else if(breached) {
			breached = true;
			this.time = 60;
			this.difficulty = 0;
		} else if (percent < 0.25) {
			this.time = 270;
			this.difficulty = 2;
		} else if (percent < 0.5) {
			this.time = 180;
			this.difficulty = 1;
		} else if (percent < 0.75) {
			this.time = 135;
			this.difficulty = 0;
		} else {
			this.time = 90;
			this.difficulty = -1;
		}
		if(isSalvaging()) {
			if(isBlownOff()) {
				this.time = 0;
				this.difficulty = 0;
			} else {
				this.time = 240;
				this.difficulty = 3;
			}
		}		
	}

	public boolean isBreached() {
		return breached;
	}
	
	public boolean isBlownOff() {
		return blownOff;
	}
	
	@Override
	public boolean needsFixing() {
		return percent < 1.0 || breached || blownOff;
	}
	
	@Override
    public String getDetails() {
		if(null != unit) {
			String toReturn = unit.getEntity().getLocationName(loc);
			if(isBlownOff()) {
				toReturn += " (Blown Off)";
			} else if(isBreached()) {
				toReturn += " (Breached)";
			} else {
				toReturn += " (" + Math.round(100*percent) + "%)";
			}
			return toReturn;
		}
		return getUnitTonnage() + " tons" + " (" + Math.round(100*percent) + "%)";
    }

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.getEntity().setInternal((int)Math.round(percent * unit.getEntity().getOInternal(loc)), loc);
			if(loc == Mech.LOC_RARM || loc == Mech.LOC_LARM) {
				if(forQuad) {
					unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc);
				} else {
					unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc);
				}
			}
			else if(loc == Mech.LOC_RLEG || loc == Mech.LOC_LLEG) {
				unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc);
			}
			//TODO: we need to cycle through slots and remove crits on non-hittable ones
			//We shouldn't have to do this, these slots should not be hit in MM
			for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
	            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
	            if ((slot != null) && !slot.isEverHittable()) {
	                slot.setDestroyed(false);
	                slot.setHit(false);
	                slot.setRepairable(true);
	                slot.setMissing(false);
	                Mounted m = slot.getMount();
	                m.setHit(false);
	                m.setDestroyed(false);
	                m.setMissing(false);
	                m.setRepairable(true);
	            }
			}
		}
	}
	
	@Override
    public String checkFixable() {
		if(isBlownOff() && !isSalvaging()) {
			if(loc == Mech.LOC_LARM && unit.isLocationDestroyed(Mech.LOC_LT)) {
				return "must replace left torso first";
			}
			else if(loc == Mech.LOC_RARM && unit.isLocationDestroyed(Mech.LOC_RT)) {
				return "must replace right torso first";
			}
			else if(unit.isLocationDestroyed(Mech.LOC_CT)) {
				//we shouldnt get here
				return "cannot replace head on destroyed unit";
			}
		} 
		else if(isSalvaging()) {
			//dont allow salvaging of bad shoulder/hip limbs
			if(onBadHipOrShoulder()) {
				return "You cannot salvage a limb with a busted hip/shoulder. You must scrap it instead.";
			}
	         //cant salvage torsos until arms and legs are gone
			String limbName = " arm ";
			if(forQuad) {
				limbName = " front leg ";
			}
	        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_RT && !unit.getEntity().isLocationBad(Mech.LOC_RARM)) {
	            return "must salvage/scrap right" + limbName + "first";
	        }
	        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_LT && !unit.getEntity().isLocationBad(Mech.LOC_LARM)) {
	            return "must salvage/scrap left" + limbName + "first";
	        } 
	        //check for armor
	        if(unit.getEntity().getArmorForReal(loc, false) > 0
	        		|| (unit.getEntity().hasRearArmor(loc) && unit.getEntity().getArmorForReal(loc, true) > 0 )) {
	        	return "must salvage armor in this location first";
	        }
	        //you can only salvage a location that has nothing left on it
	        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
	            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
	            // ignore empty & non-hittable slots
	            if ((slot == null) || !slot.isEverHittable()) {
	                continue;
	            }
     
	            //certain other specific crits need to be left out (uggh, must be a better way to do this!)
	            if(slot.getType() == CriticalSlot.TYPE_SYSTEM 
	                    && (slot.getIndex() == Mech.ACTUATOR_HIP
	                          || slot.getIndex() == Mech.ACTUATOR_SHOULDER)) {
	                continue;
	            }
	            if (slot.isRepairable()) {
	                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
	            } 
	        }
		} else if (!isBreached() && !isBlownOff()) {
			//check for damaged hips and shoulders
			for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
	            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
	            if ((slot == null) || !slot.isEverHittable()) {
	                continue;
	            }
	            if(slot.getType() == CriticalSlot.TYPE_SYSTEM 
	            		&& slot.getIndex() == Mech.ACTUATOR_HIP
	            		&& slot.isDestroyed()) {
	            	return "You cannot repair a leg with a damaged hip. This leg must be scrapped and replaced instead.";
	            	
	            }
	            if(slot.getType() == CriticalSlot.TYPE_SYSTEM 
	            		&& slot.getIndex() == Mech.ACTUATOR_SHOULDER
	            		&& slot.isDestroyed()) {
	            	return "You cannot repair an arm with a damaged shoulder. This arm must be scrapped and replaced instead.";
	            	
	            }
			}
		}
        return null;
    }
	
	@Override
	public boolean isSalvaging() {
		//cant salvage a center torso
		if(loc ==  Mech.LOC_CT) {
			return false;
		}
		return super.isSalvaging();
	}
	
	@Override
	public String checkScrappable() {
		//cant scrap a center torso
		if(loc ==  Mech.LOC_CT) {
			return "Mech Center Torso's cannot be scrapped";
		}
		//only allow scrapping of locations with nothing on them
		//otherwise you will get weirdness where armor and actuators are 
		//still attached but everything else is scrapped
	    //cant salvage torsos until arms and legs are gone
        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_RT && !unit.getEntity().isLocationBad(Mech.LOC_RARM)) {
            return "You must first remove the right arm before you scrap the right torso";
        }
        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_LT && !unit.getEntity().isLocationBad(Mech.LOC_LARM)) {
            return "You must first remove the left arm before you scrap the left torso";
        } 
        //check for armor
        if(unit.getEntity().getArmor(loc, false) > 0
        		|| (unit.getEntity().hasRearArmor(loc) && unit.getEntity().getArmor(loc, true) > 0 )) {
            return "You must first remove the armor from this location before you scrap it";
        }
        //you can only salvage a location that has nothing left on it
        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }
 
            //certain other specific crits need to be left out (uggh, must be a better way to do this!)
            if(slot.getType() == CriticalSlot.TYPE_SYSTEM 
                    && (slot.getIndex() == Mech.SYSTEM_COCKPIT
                          || slot.getIndex() == Mech.ACTUATOR_HIP
                          || slot.getIndex() == Mech.ACTUATOR_SHOULDER)) {
                continue;
            }
            if (slot.isRepairable()) {
                return "You must first remove all equipment from this location before you scrap it";
            } 
        }
		return null;
	}
	
	@Override
	public TargetRoll getAllMods() {
		if(isBreached() && !isSalvaging()) {
			return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "fixing breach");
		}
		if(isBlownOff() && isSalvaging()) {
			return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "salvaging blown-off location");
		}
		return super.getAllMods();
	}
	
	public String getDesc() {
		if((!isBreached() && !isBlownOff()) || isSalvaging()) {
			return super.getDesc();
		}
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getAssignedTeamId() != null) {
			scheduled = " (scheduled) ";
		}
	
		toReturn += ">";
		if(isBlownOff()) {
			toReturn += "<b>Re-attach " + getName() + "</b><br/>";
		} else {
			toReturn += "<b>Seal " + getName() + "</b><br/>";
		}
		toReturn += getDetails() + "<br/>";
		toReturn += "" + getTimeLeft() + " minutes" + scheduled;
		if(isBlownOff()) {
			String bonus = getAllMods().getValueAsString();
			if (getAllMods().getValue() > -1) {
				bonus = "+" + bonus;
			}
			bonus = "(" + bonus + ")";
			toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
			toReturn += " " + bonus;
			if (getMode() != Modes.MODE_NORMAL) {
				toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
			}
		}
		toReturn += "</font></html>";
		return toReturn;
	}
	
	@Override
	public boolean onBadHipOrShoulder() {
		return null != unit && unit.hasBadHipOrShoulder(loc);
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_MECH);
	}
	
}
