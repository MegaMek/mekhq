/*
 * ProtomekLocation.java
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
import megamek.common.Mounted;
import megamek.common.Protomech;
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
public class ProtomekLocation extends Part {
    private static final long serialVersionUID = -122291037522319765L;
    //some of these aren't used but may be later for advanced designs (i.e. WoR)
    protected int loc;
    protected int structureType;
    protected boolean booster;
    double percent;
    boolean breached;
    boolean blownOff;
    boolean forQuad;
    
    //system components for head
    //protected boolean sensors;
    //protected boolean lifeSupport;

    public ProtomekLocation() {
        this(0, 0, 0, false, false, null);
    }
    
    public ProtomekLocation(int loc, int tonnage, int structureType, boolean hasBooster, boolean quad, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.booster = hasBooster;
        this.percent = 1.0;
        this.forQuad = quad;
        this.breached = false;
        this.name = "Protomech Location";
        switch(loc) {
        case(Protomech.LOC_HEAD):
            this.name = "Protomech Head";
            break;
        case(Protomech.LOC_TORSO):
            this.name = "Protomech Torso";
            break;
        case(Protomech.LOC_LARM):
            this.name = "Protomech Left Arm";
            break;
        case(Protomech.LOC_RARM):
            this.name = "Protomech Right Arm";
            break;
        case(Protomech.LOC_LEG):
            this.name = "Protomech Legs";
            if(forQuad) {
                this.name = "Protomech Legs (Quad)";
            }
            break;
        case(Protomech.LOC_MAINGUN):
            this.name = "Protomech Main Gun";
            break;
        }
        if(booster) {
            this.name += " (Myomer Booster)";
        }
    }
    
    public ProtomekLocation clone() {
        ProtomekLocation clone = new ProtomekLocation(loc, getUnitTonnage(), structureType, booster, forQuad, campaign);
        clone.copyBaseData(this);
        clone.percent = this.percent;
        clone.breached = this.breached;
        clone.blownOff = this.blownOff;
        return clone;
    }
    
    public int getLoc() {
        return loc;
    }

    public boolean hasBooster() {
        return booster;
    }

    public int getStructureType() {
        return structureType;
    }
    
   
    
    public double getTonnage() {
        return 0;
    }
    
    @Override
    public long getStickerPrice() {
        double nloc = 7.0;
        if(null != unit) {
            nloc = unit.getEntity().locations();
        }
        double totalStructureCost = 2400 * getUnitTonnage();
        if(booster) {
            if(null != unit) {
                totalStructureCost += Math.round(unit.getEntity().getEngine().getRating() * 1000 * unit.getEntity().getWeight() * 0.025f);
            } else {
                //FIXME: uggh different costs by engine rating and weight, use a fake rating
                totalStructureCost += Math.round(75000 * getUnitTonnage() * 0.025f);
            }
        }
        double cost = totalStructureCost/nloc;
        if (loc == Protomech.LOC_TORSO) {
            cost += 575000;
        }
        return (long) Math.round(cost);
    }
    
    public boolean forQuad() {
        return forQuad;
    }
    
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof ProtomekLocation
                && getLoc() == ((ProtomekLocation)part).getLoc()
                && getUnitTonnage() == ((ProtomekLocation)part).getUnitTonnage()
                && hasBooster() == ((ProtomekLocation)part).hasBooster()
                && (!isLegs() || forQuad == ((MekLocation)part).forQuad);
               // && getStructureType() == ((ProtomekLocation) part).getStructureType();
    }
    
    private boolean isLegs() {
        return loc == Protomech.LOC_LEG;
    }
    
    @Override
    public boolean isSameStatus(Part part) {
        return super.isSameStatus(part) && this.getPercent() == ((ProtomekLocation)part).getPercent();
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
                +"<booster>"
                +booster
                +"</booster>");
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
            } else if (wn2.getNodeName().equalsIgnoreCase("booster")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    booster = true;
                else
                    booster = false;
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
        if(era == EquipmentType.ERA_CLAN) {
            return EquipmentType.RATING_E;
        } else {
            return EquipmentType.RATING_X;
        }
    }

    @Override
    public int getTechRating() {
       return EquipmentType.RATING_E;      
    }
    
    @Override
    public int getTechLevel() {
        return TechConstants.T_CLAN_TW;
    }
    
    @Override
    public int getTechBase() {
        return T_CLAN;        
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
    public MissingPart getMissingPart() {
        return new MissingProtomekLocation(loc, getUnitTonnage(), structureType, booster, forQuad, campaign);
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
            if(loc != Protomech.LOC_TORSO) {
                Part missing = getMissingPart();
                unit.addPart(missing);
                campaign.addPart(missing, 0);
            }
            //According to StratOps, this always destroys all equipment in that location as well
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                final CriticalSlot cs = unit.getEntity().getCritical(loc, i);
                if(null == cs || !cs.isEverHittable()) {
                    continue;
                }        
                cs.setHit(true);
                cs.setDestroyed(true);
                cs.setRepairable(false);
                Mounted m = cs.getMount();
                if(null != m) {
                    m.setHit(true);
                    m.setDestroyed(true);
                    m.setRepairable(false);
                }
            }
            for(Mounted m : unit.getEntity().getEquipment()) {
                if(m.getLocation() == loc || m.getSecondLocation() == loc) {
                    m.setHit(true);
                    m.setDestroyed(true);
                    m.setRepairable(false);
                }
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
            this.time = 200;
            this.difficulty = 3;
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
        String toReturn = "";
        if(null != unit) {
            toReturn = unit.getEntity().getLocationName(loc);
            if(isBlownOff()) {
                toReturn += " (Blown Off)";
            } else if(isBreached()) {
                toReturn += " (Breached)";
            } else {
                toReturn += " (" + Math.round(100*percent) + "%)";
            }
            return toReturn;
        }
        toReturn += getUnitTonnage() + " tons" + " (" + Math.round(100*percent) + "%)";
        return toReturn;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            unit.getEntity().setInternal((int)Math.round(percent * unit.getEntity().getOInternal(loc)), loc);
            //Because the last crit for protomechs is always location destruction we need to 
            //clear the first system crit we find
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                if ((slot != null) && slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                    slot.setDestroyed(false);
                    slot.setHit(false);
                    slot.setRepairable(true);
                    slot.setMissing(false);
                    break;
                }
            }
        }
    }
    
    @Override
    public String checkFixable() {
        if(isSalvaging()) {
            //check for armor
            if(unit.getEntity().getArmorForReal(loc, false) > 0
                    || (unit.getEntity().hasRearArmor(loc) && unit.getEntity().getArmorForReal(loc, true) > 0 )) {
                return "must salvage armor in this location first";
            }
            //you can only salvage a location that has nothing left on it
            int systemRepairable = 0;
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty & non-hittable slots
                if ((slot == null) || !slot.isEverHittable()) {
                    continue;
                }
                //we don't care about the final critical hit to the system
                //in locations because that just represents the location destruction
                if(slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                    if(slot.isRepairable()) {
                        if(systemRepairable > 0) {
                            return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
                        } else {
                            systemRepairable++;
                        }
                    }
                }
                else if (slot.isRepairable()) {
                    return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
                } 
            }
            //protomechs only have system stuff in the crits, so we need to also
            //check for mounted equipment separately
            for(Mounted m : unit.getEntity().getEquipment()) {
                if(m.isRepairable() && (m.getLocation() == loc || m.getSecondLocation() == loc)) {
                    return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first." + m.getName();
                }
            }
        } 
        return null;
    }
    
    @Override
    public boolean isSalvaging() {
        //cant salvage a center torso
        if(loc ==  Protomech.LOC_TORSO) {
            return false;
        }
        return super.isSalvaging();
    }
    
    @Override
    public String checkScrappable() {
        //cant scrap a center torso
        if(loc ==  Protomech.LOC_TORSO) {
            return "Protomech's Torso cannot be scrapped";
        }
        //check for armor
        if(unit.getEntity().getArmor(loc, false) > 0
                || (unit.getEntity().hasRearArmor(loc) && unit.getEntity().getArmor(loc, true) > 0 )) {
            return "You must first remove the armor from this location before you scrap it";
        }
        //you can only salvage a location that has nothing left on it
        int systemRepairable = 0;
        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }
            //we don't care about the final critical hit to the system
            //in locations because that just represents the location destruction
            if(slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                if(slot.isRepairable()) {
                    if(systemRepairable > 0) {
                        return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
                    } else {
                        systemRepairable++;
                    }
                }
            }
            else if (slot.isRepairable()) {
                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
            } 
        }
        //protomechs only have system stuff in the crits, so we need to also
        //check for mounted equipment separately
        for(Mounted m : unit.getEntity().getEquipment()) {
            if(m.isRepairable() && (m.getLocation() == loc || m.getSecondLocation() == loc)) {
                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first." + m.getName();
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
        if(getSkillMin() > SkillType.EXP_ELITE) {
            toReturn += "<font color='red'>Impossible</font>";
        } else {
            toReturn += "" + getTimeLeft() + " minutes" + scheduled;
            if(isBlownOff()) {
                String bonus = getAllMods().getValueAsString();
                if (getAllMods().getValue() > -1) {
                    bonus = "+" + bonus;
                }
                bonus = "(" + bonus + ")";
                if(!getCampaign().getCampaignOptions().isDestroyByMargin()) {
                    toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
                }
                toReturn += " " + bonus;
                if (getMode() != Modes.MODE_NORMAL) {
                    toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
                }
            }
        }
        toReturn += "</font></html>";
        return toReturn;
    }
    
    @Override
    public boolean onBadHipOrShoulder() {
        return false;
    }
    
    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECH);
    }
    
    public void doMaintenanceDamage(int d) {
        int points = unit.getEntity().getInternal(loc);
        points = Math.max(points -d, 1);
        unit.getEntity().setInternal(points, loc);
        updateConditionFromEntity();
    }
}
