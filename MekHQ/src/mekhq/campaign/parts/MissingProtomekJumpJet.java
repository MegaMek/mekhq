/*
 * MissingProtomekJumpJet.java
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
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingProtomekJumpJet extends MissingPart {
    private static final long serialVersionUID = 719878556021696393L;

    public MissingProtomekJumpJet() {
        this(0, null);
    }
    
    public MissingProtomekJumpJet(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Protomech Jump Jet";
    }
    
    @Override 
	public int getBaseTime() {
		return 60;
	}
	
	@Override
	public int getDifficulty() {
		return 0;
	}
   
    @Override
    public double getTonnage() {
        if(getUnitTonnage() <=5) {
            return 0.05;
        } else {
            return 0.1;
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        
    }

    @Override
    public int getAvailability(int era) {
        if(era == EquipmentType.ERA_CLAN) {
            return EquipmentType.RATING_D;
        } else {
            return EquipmentType.RATING_X;
        }
    }

    @Override
    public int getTechRating() {
        return EquipmentType.RATING_D;
    }
    
    @Override
    public int getTechBase() {
        return T_CLAN;
    }
    
    @Override
    public int getTechLevel() {
        return TechConstants.T_CLAN_TW;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            if(null != unit) {
                int damageJJ = getOtherDamagedJumpJets() + 1;
                if(damageJJ < (int)Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                    unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
                    unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO, 1);
                } else {
                    unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
                    unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO, 2);
                }
            }
        }
    }

    @Override
    public String checkFixable() {
    	if(null == unit) {
    		return null;
    	}
        if(unit.isLocationBreached(Protomech.LOC_TORSO)) {
            return unit.getEntity().getLocationName(Protomech.LOC_TORSO) + " is breached.";
        }
        if(unit.isLocationDestroyed(Protomech.LOC_TORSO)) {
            return unit.getEntity().getLocationName(Protomech.LOC_TORSO) + " is destroyed.";
        }
        return null;
    }

    @Override 
    public void fix() {
        Part replacement = findReplacement(false);
        if(null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            remove(false);
            //assign the replacement part to the unit           
            actualReplacement.updateConditionFromPart();
        }
    }
    
    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof ProtomekJumpJet
                && getUnitTonnage() == ((ProtomekJumpJet)part).getUnitTonnage();
    }

    @Override
    public Part getNewPart() {
        return new ProtomekJumpJet(getUnitTonnage(), campaign);
    }
    
    private int getOtherDamagedJumpJets() {
        int damagedJJ = 0;
        if(null != unit) {
            for(Part p : unit.getParts()) {
                if(p.getId() == this.getId()) {
                    continue;
                }
                if(p instanceof MissingProtomekJumpJet 
                        || (p instanceof ProtomekJumpJet && ((ProtomekJumpJet)p).needsFixing())) {
                    damagedJJ++;
                }
            }
        }
        return damagedJJ;
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
	public int getIntroDate() {
		return 3055;
	}

	@Override
	public int getExtinctDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getReIntroDate() {
		return EquipmentType.DATE_NONE;
	}

}
