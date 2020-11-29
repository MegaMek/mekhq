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
import java.util.StringJoiner;

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.WorkTime;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLocation extends Part {
	private static final long serialVersionUID = -122291037522319765L;
	protected int loc;
    protected int structureType;
    protected boolean clan; // Only need for Endo-Steel
    protected boolean tsm;
    double percent;
    boolean breached;
    boolean blownOff;
    boolean forQuad;

    //system components for head
    protected boolean sensors;
    protected boolean lifeSupport;

    public MekLocation() {
    	this(0, 0, 0, false, false, false, false, false, null);
    }

    public MekLocation clone() {
    	MekLocation clone = new MekLocation(loc, getUnitTonnage(), structureType, clan,
    	        tsm, forQuad, sensors, lifeSupport, campaign);
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

    public void setClan(boolean clan) {
        this.clan = clan;
    }

    public MekLocation(int loc, int tonnage, int structureType, boolean clan,
            boolean hasTSM, boolean quad, boolean sensors, boolean lifeSupport, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.clan = clan;
        this.tsm = hasTSM;
        this.percent = 1.0;
        this.forQuad = quad;
        this.sensors = sensors;
        this.lifeSupport = lifeSupport;
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
        if (EquipmentType.T_STRUCTURE_ENDO_STEEL == structureType) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType, isClan()) + ")";
        } else if(structureType != EquipmentType.T_STRUCTURE_STANDARD) {
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
    public Money getStickerPrice() {
        double totalStructureCost = EquipmentType.getStructureCost(getStructureType()) * getUnitTonnage();
        int muscCost = isTsm() ? 16000 : 2000;
        double totalMuscleCost = muscCost * getUnitTonnage();
        double cost = 0.1 * (totalStructureCost + totalMuscleCost);

        if (loc == Mech.LOC_HEAD) {
        	if(sensors) {
        	    cost += 2000 * getUnitTonnage();
        	}
        	if(lifeSupport) {
        	    cost += 50000;
        	}
        }
        return Money.of(cost);
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
                && ((getStructureType() != EquipmentType.T_STRUCTURE_ENDO_STEEL)
                        || (isClan() == part.isClan()))
                && (!isArm() || forQuad == ((MekLocation)part).forQuad)
                // Sensors and life support only matter if we're comparing two parts in the warehouse.
                && ((getUnit() != null) || (part.getUnit() != null)
                        || (hasSensors() == ((MekLocation)part).hasSensors()
                        && hasLifeSupport() == ((MekLocation)part).hasLifeSupport()));
    }

    @Override
    public boolean isSameStatus(Part part) {
    	return super.isSameStatus(part) && this.getPercent() == ((MekLocation)part).getPercent();
    }

    public double getPercent() {
    	return percent;
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
                +"<clan>"
                +clan
                +"</clan>");
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
                +"<sensors>"
                +sensors
                +"</sensors>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<lifeSupport>"
                +lifeSupport
                +"</lifeSupport>");
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
            } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                clan = Boolean.parseBoolean(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("percent")) {
				percent = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                clan = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("tsm")) {
                tsm = Boolean.parseBoolean(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("forQuad")) {
			    forQuad = Boolean.parseBoolean(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("sensors")) {
			    sensors = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("lifeSupport")) {
                lifeSupport = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("breached")) {
                breached = Boolean.parseBoolean(wn2.getTextContent());
			}
		}
	}

	@Override
	public void fix() {
		super.fix();
		if(isBlownOff()) {
            blownOff = false;
            if (null != unit) {
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
            }
		} else if(isBreached()) {
            breached = false;
            if (null != unit) {
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
            }
		} else {
			percent = 1.0;
			if(null != unit) {
				unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
			}
		}
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingMekLocation(loc, getUnitTonnage(), structureType, clan, tsm, forQuad, campaign);
	}

	@Override
	public void remove(boolean salvage) {
		blownOff = false;
		breached = false;
		if(null != unit) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
			unit.getEntity().setLocationBlownOff(loc, false);
			unit.getEntity().setLocationStatus(loc, ILocationExposureStatus.NORMAL, true);
			Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
			if(!salvage) {
				campaign.getWarehouse().removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.getWarehouse().removePart(this);
			}
			//if this is a head. check for life support and sensors
			if(loc == Mech.LOC_HEAD) {
			    removeHeadComponents();
			}
			unit.removePart(this);
			if(loc != Mech.LOC_CT) {
				Part missing = getMissingPart();
				unit.addPart(missing);
				campaign.getQuartermaster().addPart(missing, 0);
			}
		}
		setUnit(null);
		updateConditionFromEntity(false);
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		if(null != unit) {
			blownOff = unit.getEntity().isLocationBlownOff(loc);
			breached = unit.isLocationBreached(loc);
			percent = ((double) unit.getEntity().getInternalForReal(loc)) / ((double) unit.getEntity().getOInternal(loc));
			if(percent <= 0.0) {
				remove(false);
			}
		}
	}

	@Override
	public int getBaseTime() {
        // StratOps p191 / p183-185
		if (isSalvaging()) {
			if (isBlownOff()) {
                // it takes no time to salvage a blown off limb...
                // ...because it isn't there.
				return 0;
			} else {
                return getRepairOrSalvageTime();
			}
        }

        // StratOps p183 Master Repair Table
		if (isBlownOff()) {
			if (loc == Mech.LOC_HEAD) {
                // 200 minutes for blown off head
				return 200;
			} else {
                // 180 minutes for a blown off limb
				return 180;
			}
        }

		if (isBreached()) {
            // StratOps p177 - 'Mech Hull Breach
            // "Once this time is spent [fixing the breach], the components
            // in the damaged location work normally. Other damage suffered
            // by that location prior to and after the breach must be
            // repaired normally."
			return 60;
        }

		return getRepairOrSalvageTime();
    }

    /**
     * Gets the time (in minutes) to repair or salvage this location.
     * @return The time (in minutes) to repair or salvage this location.
     */
    private int getRepairOrSalvageTime() {
        // StratOps p184 Master Repair Table
        // NOTE: MissingMekLocation handles destroyed locations
        if (percent < 0.25) {
			return 270;
		} else if (percent < 0.5) {
			return 180;
		} else if (percent < 0.75) {
			return 135;
		} else {
            // <25% damage
            return 90;
        }
    }

	@Override
	public int getDifficulty() {
        // StratOps p191 / p183-185
		if (isSalvaging()) {
			if (isBlownOff()) {
                // The difficulty in removing a location which
                // does not exist is philosophical...
				return 0;
			} else {
                // ...otherwise use the difficulty table
				return getRepairOrSalvageDifficulty();
			}
        }

        // StratOps p183 Master Repair Table
		if (isBlownOff()) {
			if(loc == Mech.LOC_HEAD) {
                // +2 for re-attaching a head
				return 2;
			} else {
                // +1 for re-attaching anything else
				return 1;
			}
        }

		if (isBreached()) {
            // 'Mech hull breach is +0
			return 0;
		}

		return getRepairOrSalvageDifficulty();
    }

    /**
     * Gets the repair or salvage difficulty for this location.
     * @return The difficulty modifier for repair or salvage of this location.
     */
    private int getRepairOrSalvageDifficulty() {
        // StrapOps p184 Master Repair Table
        // NOTE: MissingMekLocation handles destroyed locations
        if (percent < 0.25) {
			return 2;
		} else if (percent < 0.5) {
			return 1;
		} else if (percent < 0.75) {
			return 0;
        } else {
            // <25% damage
            return -1;
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
		return percent < 1.0 || breached || blownOff
				|| (unit != null && unit.hasBadHipOrShoulder(loc));
	}

	@Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
	    String toReturn = "";
		if(null != unit) {
            toReturn = unit.getEntity().getLocationName(loc);
            if (includeRepairDetails) {
                if(isBlownOff()) {
                    toReturn += " (Blown Off)";
                } else if(isBreached()) {
                    toReturn += " (Breached)";
                } else {
                    toReturn += " (" + Math.round(100*percent) + "%)";
                }
            }
			return toReturn;
		}
        toReturn += getUnitTonnage() + " tons";
        if (includeRepairDetails) {
            toReturn += " (" + Math.round(100*percent) + "%)";
        }
		if(loc == Mech.LOC_HEAD) {
		    StringJoiner components = new StringJoiner(", ");
    		if(hasSensors()) {
                components.add("Sensors");
            }
            if(hasLifeSupport()) {
                components.add("Life Support");
            }
            if(components.length() > 0) {
                toReturn += " [" + components.toString() + "]";
            }
		}
		return toReturn;
    }

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.getEntity().setInternal((int)Math.round(percent * unit.getEntity().getOInternal(loc)), loc);
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
		if(null == unit) {
			return null;
		}
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
	                          || slot.getIndex() == Mech.ACTUATOR_SHOULDER
	                          || slot.getIndex() == Mech.SYSTEM_LIFE_SUPPORT
	                          || slot.getIndex() == Mech.SYSTEM_SENSORS)) {
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
	public TargetRoll getAllMods(Person tech) {
		if(isBreached() && !isSalvaging()) {
			return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "fixing breach");
		}
		if(isBlownOff() && isSalvaging()) {
			return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "salvaging blown-off location");
		}
		return super.getAllMods(tech);
	}

	public String getDesc() {
		if((!isBreached() && !isBlownOff()) || isSalvaging()) {
			return super.getDesc();
		}
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getTech() != null) {
			scheduled = " (scheduled) ";
		}

		toReturn += ">";
		if(isBlownOff()) {
			toReturn += "<b>Re-attach " + getName() + "</b><br/>";
		} else {
			toReturn += "<b>Seal " + getName() + "</b><br/>";
		}
		toReturn += getDetails() + "<br/>";
		if(getSkillMin() > SkillType.EXP_ELITE) {
            toReturn += "<font color='red'>Impossible</font>";
        } else {
            toReturn += "" + getTimeLeft() + " minutes" + scheduled;
    		if(isBlownOff()) {
    			String bonus = getAllMods(null).getValueAsString();
    			if (getAllMods(null).getValue() > -1) {
    				bonus = "+" + bonus;
    			}
    			bonus = "(" + bonus + ")";
    			if(!getCampaign().getCampaignOptions().isDestroyByMargin()) {
    			    toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
    			}
    			toReturn += " " + bonus;
    			if (getMode() != WorkTime.NORMAL) {
    				toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
    			}
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

	public boolean hasSensors() {
	    return sensors;
	}

	public void setSensors(boolean b) {
	    sensors = b;
	}

	public boolean hasLifeSupport() {
	    return lifeSupport;
	}

	public void setLifeSupport(boolean b) {
	    lifeSupport = b;
	}

	private void removeHeadComponents() {
        MekSensor sensor = null;
        MekLifeSupport support = null;
        for(Part p : unit.getParts()) {
            if(null == sensor && p instanceof MekSensor) {
                sensor = (MekSensor)p;
            }
            if(null == support && p instanceof MekLifeSupport) {
                support = (MekLifeSupport)p;
            }
            if(null != sensor && null != support) {
                break;
            }
        }
        if(null != sensor) {
            sensor.remove(false);
            sensors = true;
        }
        if(null != support) {
            support.remove(false);
            lifeSupport = true;
        }
    }

	 public void doMaintenanceDamage(int d) {
	     int points = unit.getEntity().getInternal(loc);
         points = Math.max(points -d, 1);
         unit.getEntity().setInternal(points, loc);
         updateConditionFromEntity(false);
	 }

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		return Entity.LOC_NONE;
	}

	@Override
	public TechAdvancement getTechAdvancement() {
	    return EquipmentType.getStructureTechAdvancement(structureType, clan);
	}

	@Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.GENERAL_LOCATION;
    }

	@Override
	public int getRepairPartType() {
    	return Part.REPAIR_PART_TYPE.MEK_LOCATION;
    }
}
