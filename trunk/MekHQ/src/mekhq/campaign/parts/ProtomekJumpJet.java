/*
 * ProtomekHeatSink.java
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
import megamek.common.Protomech;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ProtomekJumpJet extends Part {
    private static final long serialVersionUID = 719878556021696393L;

    public ProtomekJumpJet() {
        this(0, null);
    }
    
    public ProtomekJumpJet clone() {
        ProtomekJumpJet clone = new ProtomekJumpJet(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
    }
   
    
    public ProtomekJumpJet(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Protomech Jump Jet";
    }
   
    @Override
    public double getTonnage() {
        if(getUnitTonnage() <=5) {
            return 0.05;
        } else if (getUnitTonnage() <= 9){
            return 0.1;
        } else {
            return 0.15;
        }
    }
    
    @Override
    public long getStickerPrice() {
        return getUnitTonnage() * 400;
    }

    @Override
    public boolean isSamePartType (Part part) {
        return part instanceof ProtomekJumpJet
                && getUnitTonnage() == ((ProtomekJumpJet)part).getUnitTonnage();
    }
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
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
    public void fix() {
        super.fix();
        if(null != unit) {
            //repair depending upon how many others are still damaged
            int damageJJ = getOtherDamagedJumpJets();
            if(damageJJ == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
            }
            else if(damageJJ < (int)Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO, 2);
            }
        }
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
    public MissingPart getMissingPart() {
        return new MissingProtomekJumpJet(getUnitTonnage(), campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            int h = 1;
            int damageJJ = getOtherDamagedJumpJets() + 1;
            if(damageJJ >= (int)Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                h = 2;
            } 
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO, h);
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
        setSalvaging(false);
        setUnit(null);
        updateConditionFromEntity();
    }

    @Override
    public void updateConditionFromEntity() {
        if(null != unit) {           
            hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
            if(hits > 2) {
                remove(false);
                return;
            }
            //only ever damage the first jump jet on the unit
            int damageJJ = 0;
            if(hits == 2) {
                damageJJ = (int)Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0);
            } else if(hits==1) {
                damageJJ = 1;
            }
            damageJJ -= getOtherDamagedJumpJets();
            if(damageJJ > 0) {
                hits = 1;
            } else {
                hits = 0;
            }
        }
        //Use mech jump jet repair/salvage times
        if(hits == 0) {
            time = 0;
            difficulty = 0;
        } 
        else {
            time = 90;
            difficulty = 0;
        }
        if(isSalvaging()) {
            time = 60;
            difficulty = 0;
        }
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }
    
    @Override
    public String getDetails() {
        if(null != unit) {
            return unit.getEntity().getLocationName(Protomech.LOC_TORSO);
        }
        return getUnitTonnage() + " tons";
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            int damageJJ = getOtherDamagedJumpJets() + hits;
            if(damageJJ == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
            }
            else if(damageJJ < (int)Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_TORSOCRIT, Protomech.LOC_TORSO, 2);
            }
        }   
    }
    
    @Override
    public String checkFixable() {
        if(isSalvaging()) {
            return null;
        }
        if(unit.isLocationBreached(Protomech.LOC_TORSO)) {
            return unit.getEntity().getLocationName(Protomech.LOC_TORSO) + " is breached.";
        }
        if(isMountedOnDestroyedLocation()) {
            return unit.getEntity().getLocationName(Protomech.LOC_TORSO) + " is destroyed.";
        }
        return null;
    }
    
    @Override
    public boolean isMountedOnDestroyedLocation() {
        return null != unit && unit.isLocationDestroyed(Protomech.LOC_TORSO);
    }
    
    @Override
    public boolean onBadHipOrShoulder() {
        return false;
    }
    
    @Override
    public boolean isPartForCriticalSlot(int index, int loc) {
        return false;//index == type && loc == location;
    }
    
    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECH);
    }
    
    @Override
    public boolean isOmniPoddable() {
        return false;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // TODO Auto-generated method stub
        
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
    
}
