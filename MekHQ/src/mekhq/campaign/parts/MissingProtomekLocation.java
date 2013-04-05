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
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingProtomekLocation extends MissingPart {
    private static final long serialVersionUID = -122291037522319765L;
    protected int loc;
    protected int structureType;
    protected boolean booster;
    protected double percent;
    protected boolean forQuad;

    public MissingProtomekLocation() {
        this(0, 0, 0, false, false, null);
    }
    
    
    public MissingProtomekLocation(int loc, int tonnage, int structureType, boolean hasBooster, boolean quad, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.booster = hasBooster;
        this.percent = 1.0;
        this.forQuad = quad;
        //TODO: need to account for internal structure and myomer types
        //crap, no static report for location names?
        this.name = "Mech Location";
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
            break;
        case(Protomech.LOC_MAINGUN):
            this.name = "Protomech Main Gun";
            break;
        }
        if(booster) {
            this.name += " (Myomer Booster)";
        }
        this.time = 240;
        this.difficulty = 3;
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
        //TODO: how much should this weigh?
        return 0;
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

    public boolean forQuad() {
        return forQuad;
    }
    
    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if(loc == Protomech.LOC_TORSO && !refit) {
            //you can't replace a center torso
            return false;
        }
        if(part instanceof ProtomekLocation) {
            ProtomekLocation mekLoc = (ProtomekLocation)part;
            return mekLoc.getLoc() == loc
                && mekLoc.getUnitTonnage() == getUnitTonnage()
                && mekLoc.hasBooster() == booster
                && mekLoc.getStructureType() == structureType;
        }
        return false;
    }
    
    @Override
    public String checkFixable() {
      
        //there must be no usable equipment currently in the location
        //you can only salvage a location that has nothing left on it
        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
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
        return new ProtomekLocation(loc, getUnitTonnage(), structureType, booster, forQuad, campaign);
    }
    
    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
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
            remove(false);
            actualReplacement.updateConditionFromPart();
            //TODO: we need to remove some of the critical damage
            u.runDiagnostic();
        }
    }
}
