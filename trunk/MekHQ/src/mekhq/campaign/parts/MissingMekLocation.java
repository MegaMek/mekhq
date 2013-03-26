/*
 * MissingMekLocation.java
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
import megamek.common.Mech;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekLocation extends MissingPart {
	private static final long serialVersionUID = -122291037522319765L;
	protected int loc;
    protected int structureType;
    protected boolean tsm;
    protected double percent;
    protected boolean forQuad;

    public MissingMekLocation() {
    	this(0, 0, 0, false, false, null);
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
    
    public MissingMekLocation(int loc, int tonnage, int structureType, boolean hasTSM, boolean quad, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.tsm = hasTSM;
        this.percent = 1.0;
        this.forQuad = quad;
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
            		this.name = "Mech Front Left Leg";
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
        this.time = 240;
        this.difficulty = 3;
    }
    
    public double getTonnage() {
    	//TODO: how much should this weigh?
    	return 0;
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
	public int getTechBase() {
		return T_BOTH;
	}

	private boolean isArm() {
		return loc == Mech.LOC_RARM || loc == Mech.LOC_LARM;
	}
	
	public boolean forQuad() {
		return forQuad;
	}
	
	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		if(loc == Mech.LOC_CT && !refit) {
			//you can't replace a center torso
			return false;
		}
		if(part instanceof MekLocation) {
			MekLocation mekLoc = (MekLocation)part;
			/*if(mekLoc.getLoc() == Mech.LOC_HEAD && null != unit) {
			    //cockpit must either be none, or match
			    if(mekLoc.hasCockpit() && mekLoc.getCockpitType() != ((Mech)unit.getEntity()).getCockpitType()) {
			        return false;
			    }
			}*/
			return mekLoc.getLoc() == loc
				&& mekLoc.getUnitTonnage() == getUnitTonnage()
				&& mekLoc.isTsm() == tsm
				&& mekLoc.getStructureType() == structureType
				&& (!isArm() || mekLoc.forQuad() == forQuad);
		}
		return false;
	}
	
	@Override
	public String checkFixable() {
		if (unit.getEntity() instanceof Mech) {
			// cant replace appendages when corresponding torso is gone
			if (loc == Mech.LOC_LARM
					&& unit.getEntity().isLocationBad(Mech.LOC_LT)) {
				return "must replace left torso first";
			} else if (loc == Mech.LOC_RARM
					&& unit.getEntity().isLocationBad(Mech.LOC_RT)) {
				return "must replace right torso first";
			}
		}
		//there must be no usable equipment currently in the location
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
                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first. They can then be re-installed.";
            } 
        }
		return null;
	}

	@Override
	public Part getNewPart() {
	   /* int cockpitType = -1;
	    if(null != unit) {
	        cockpitType = ((Mech)unit.getEntity()).getCockpitType();
	    }*/
	    boolean lifeSupport = (loc == Mech.LOC_HEAD);
	    boolean sensors = (loc == Mech.LOC_HEAD);
		return new MekLocation(loc, getUnitTonnage(), structureType, tsm, forQuad, sensors, lifeSupport, campaign);
	}
	
	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
		}
	}
	
	@Override
	public void fix() {
		Part replacement = findReplacement(false);
		if(null != replacement) {
			Unit u = unit;
			Part actualReplacement = replacement.clone();
			unit.addPart(actualReplacement);
			campaign.addPart(actualReplacement, 0);
			replacement.decrementQuantity();
			//TODO: if this is a mech head, check to see if it had components
            if(loc == Mech.LOC_HEAD && actualReplacement instanceof MekLocation) { 
                updateHeadComponents((MekLocation)actualReplacement);
                ((MekLocation)actualReplacement).setSensors(false);
                ((MekLocation)actualReplacement).setLifeSupport(false);
            }
			remove(false);
			actualReplacement.updateConditionFromPart();
			
			u.runDiagnostic();
		}
	}
	
	private void updateHeadComponents(MekLocation part) {
	    MissingMekSensor missingSensor = null;
	    MissingMekLifeSupport missingLifeSupport = null;
	    for(Part p : unit.getParts()) {
	        if(null == missingSensor && p instanceof MissingMekSensor) {
	            missingSensor = (MissingMekSensor)p;
	        }
	        if(null == missingLifeSupport && p instanceof MissingMekLifeSupport) {
                missingLifeSupport = (MissingMekLifeSupport)p;
            }
	        if(null != missingSensor && null != missingLifeSupport) {
	            break;
	        }
	    }
	    Part newPart;
	    if(part.hasSensors() && null != missingSensor) {
	        newPart = missingSensor.getNewPart();
	        unit.addPart(newPart);
	        campaign.addPart(newPart, 0);
	        missingSensor.remove(false);
	        newPart.updateConditionFromPart(); 
	    }
	    /*if(part.hasCockpit() && null != missingCockpit) {
            newPart = missingCockpit.getNewPart();
            unit.addPart(newPart);
            campaign.addPart(newPart);
            missingCockpit.remove(false);
            newPart.updateConditionFromPart(); 
        }*/
	    if(part.hasLifeSupport() && null != missingLifeSupport) {
            newPart = missingLifeSupport.getNewPart();
            unit.addPart(newPart);
            campaign.addPart(newPart, 0);
            missingLifeSupport.remove(false);
            newPart.updateConditionFromPart(); 
        }
	}
}
