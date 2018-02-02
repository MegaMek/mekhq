/*
 * Avionics.java
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

import org.w3c.dom.Node;

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IAero;
import megamek.common.LandAirMech;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Avionics extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = -717866644605314883L;

	public Avionics() {
    	this(0, null);
    }
    
    public Avionics(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Avionics";
    }
    
    public Avionics clone() {
    	Avionics clone = new Avionics(0, campaign);
        clone.copyBaseData(this);
    	return clone;
    }
        
	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		int priorHits = hits;
		if(null != unit
		        && (unit.getEntity().getEntityType() & (Entity.ETYPE_AERO | Entity.ETYPE_LAND_AIR_MECH)) != 0) {
			hits = ((IAero)unit.getEntity()).getAvionicsHits();
			if(checkForDestruction 
					&& hits > priorHits
					&& (hits < 3 && !campaign.getCampaignOptions().useAeroSystemHits())
					&& Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				remove(false);
				return;
			} else if (hits >= 3) {
			    remove(false);
			    return;
			}
		}
	}
	
	@Override 
	public int getBaseTime() {
		if (campaign.getCampaignOptions().useAeroSystemHits()) {
		    //Test of proposed errata for repair times
		    Entity e = unit.getEntity();
		    if (isSalvaging()) {
		        if (e.hasETypeFlag(Entity.ETYPE_DROPSHIP) || e.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                    return 4800;
                } else {
                    return 600;
                }
		    }
		    if (hits == 1) {
		        if (e.hasETypeFlag(Entity.ETYPE_DROPSHIP) || e.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
		            return 240;
		        } else {
		            return 120;
		        }
		    } 
		    if (hits == 2) {
		        if (e.hasETypeFlag(Entity.ETYPE_DROPSHIP) || e.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                    return 480;
                } else {
                    return 240;
                }
		    }
		}
		if (isSalvaging()) {
		    return 4800;
		}
		return 480;
	}
	
	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return 1;
		}
		return 0;
	}

	@Override
	public void updateConditionFromPart() {
	    if (null == unit) {
	        return;
	    }
	    if (unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setAvionicsHits(hits);
		} else if (unit.getEntity() instanceof LandAirMech) {
		    if(hits == 0) {
		        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS);
		    } else {
		        unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS, hits);
	        }
		}
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit) {
		    if (unit.getEntity() instanceof Aero) {
		        ((Aero)unit.getEntity()).setAvionicsHits(0);
		    } else if (unit.getEntity() instanceof LandAirMech) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS);
		    }
	    }
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
		    if (unit.getEntity() instanceof Aero) {
		        ((Aero)unit.getEntity()).setAvionicsHits(3);
		    } else if (unit.getEntity() instanceof LandAirMech) {
		        unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS, 3);
		    }
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing, 0);
		}
		setUnit(null);
		updateConditionFromEntity(false);
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingAvionics(getUnitTonnage(), campaign);
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public long getStickerPrice() {
		//TODO: table in TechManual makes no sense - where are control systems for ASFs?
		return 0;
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		//go with conventional fighter avionics
		return EquipmentType.RATING_B;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof Avionics;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		//nothing to load
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
	    if (unit != null && unit.getEntity() instanceof LandAirMech) {
	        return skillType.equals(SkillType.S_TECH_MECH);
	    }
		return skillType.equals(SkillType.S_TECH_AERO);
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
	    return TA_GENERIC;
	}
	
}