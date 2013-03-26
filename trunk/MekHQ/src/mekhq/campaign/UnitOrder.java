/*
 * Unit.java
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

package mekhq.campaign;


import java.io.PrintWriter;
import java.util.UUID;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.Version;
import mekhq.campaign.parts.Availability;
import mekhq.campaign.work.IAcquisitionWork;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * We use an extension of unit to create a unit order acquisition work
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class UnitOrder extends Unit implements IAcquisitionWork, MekHqXmlSerializable {

    int quantity;
    int daysToWait;

    public UnitOrder() {
        super(null, null);       
    }
    
    public UnitOrder(Entity en, Campaign c) {
        super(en, c);
        initializeParts(false);
        quantity = 1;
        daysToWait = 0;
    }
    
    @Override
    public boolean needsFixing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getDifficulty() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public TargetRoll getAllMods() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String succeed() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String fail(int rating) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UUID getAssignedTeamId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getMode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getAcquisitionName() {
        return getName();
    }

    @Override
    public Object getNewEquipment() {
        MechSummary summary = MechSummaryCache.getInstance().getMech(getEntity().getChassis() + " " + getEntity().getModel());
        if(null == summary) {
            //throw(new EntityLoadingException());
        }
         try {
            return new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
        } catch (EntityLoadingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getAcquisitionDesc() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Unit getUnit() {
        return this;
    }

    @Override
    public int getDaysToWait() {
        return daysToWait;
    }

    @Override
    public void resetDaysToWait() {
        this.daysToWait = campaign.getCampaignOptions().getWaitingPeriod();
    }

    @Override
    public void decrementDaysToWait() {
        if(daysToWait > 0) {
            daysToWait--;
        }       
    }

    @Override
    public String find(int transitDays) {
        //TODO: probably get a duplicate entity
        if(campaign.buyUnit((Entity)getNewEquipment(), transitDays)) {
            return "<font color='green'><b> unit found</b>.</font> It will be delivered in " + transitDays + " days.";
        } else {
            return "<font color='red'><b> You cannot afford this unit. Transaction cancelled</b>.</font>";
        }
    }

    @Override
    public String failToFind() {
        resetDaysToWait();
        return "<font color='red'><b> unit not found</b>.</font>";
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        if(!entity.isCanon()) {
            //TODO: custom job
        }
        if(entity.isClan() && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        }
        else if(campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        //TODO: Fix weight classes
        //TODO: aero large craft
        //TODO: support vehicles
        if(entity instanceof Mech) {
            if(!((Mech) entity).isIndustrial()) {
                target.addModifier(0, "BattleMech");
            } else {
                target.addModifier(-1, "IndustrialMech");
            }
            switch(entity.getWeightClass()) {
            case EntityWeightClass.WEIGHT_LIGHT:
                target.addModifier(-1, "Light");
                break;
            case EntityWeightClass.WEIGHT_MEDIUM:
                target.addModifier(0, "Medium");
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                target.addModifier(1, "Heavy");
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
            default:
                target.addModifier(3, "Assault");
            }
        }
        else if(entity instanceof BattleArmor) {
            target.addModifier(0, "BattleArmor");
        }
        else if(entity instanceof Infantry) {
            if(entity.getMovementMode() == EntityMovementMode.INF_LEG) {
                target.addModifier(-3, "Foot Infantry");
            }
            else if(entity.getMovementMode() == EntityMovementMode.INF_JUMP) {
                target.addModifier(-1, "Jump Infantry");
            }
            else if(entity.getMovementMode() == EntityMovementMode.INF_MOTORIZED) {
                target.addModifier(-2, "Motorized Infantry");
            } else {
                target.addModifier(-1, "Mechanized Infantry");
            }
        }
        else if(entity instanceof Tank) {
            target.addModifier(-1, "Vehicle");
            switch(entity.getWeightClass()) {
            case EntityWeightClass.WEIGHT_LIGHT:
                target.addModifier(-1, "Light");
                break;
            case EntityWeightClass.WEIGHT_MEDIUM:
                target.addModifier(0, "Medium");
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                target.addModifier(1, "Heavy");
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
            default:
                target.addModifier(3, "Assault");
            }
        }
        else if(entity instanceof ConvFighter) {
            target.addModifier(+0, "Conventional Fighter");
        }
        else if(entity instanceof Aero) {
            target.addModifier(0, "Aerospace Fighter");
            switch(entity.getWeightClass()) {
            case EntityWeightClass.WEIGHT_LIGHT:
                target.addModifier(-1, "Light");
                break;
            case EntityWeightClass.WEIGHT_MEDIUM:
                target.addModifier(0, "Medium");
                break;
            case EntityWeightClass.WEIGHT_HEAVY:
                target.addModifier(1, "Heavy");
                break;
            case EntityWeightClass.WEIGHT_ASSAULT:
            default:
                target.addModifier(3, "Assault");
            }
        }
        else if(entity instanceof Protomech) {
            target.addModifier(+1, "Protomech");
        }
        //parts need to be initialized for this to work
        int avail = getAvailability(campaign.getEra());
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + EquipmentType.getRatingName(avail) + ")");      
        return target;
    }

    @Override
    public int getTechBase() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getTechLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public void incrementQuantity() {
        quantity++;
    }

    @Override
    public void decrementQuantity() {
        quantity--;
    }

    @Override
    public String getShoppingListReport(int quantity) {
        return getName() + " added to procurement list.";
    }
    
    /*
     * Don't need as much info as unit to re-create
     */
    public void writeToXml(PrintWriter pw1, int indentLvl) {
        pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "<unitOrder>");

        pw1.println(MekHqXmlUtil.writeEntityToXmlString(getEntity(), indentLvl+1, campaign.getEntities()));
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<quantity>"
                +quantity
                +"</quantity>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
                +"<daysToWait>"
                +daysToWait
                +"</daysToWait>");
        pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "</unitOrder>");
    }
    
    public static UnitOrder generateInstanceFromXML(Node wn, Version version) {
        UnitOrder retVal = new UnitOrder();
        
        NodeList nl = wn.getChildNodes();

        try {
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                
                if (wn2.getNodeName().equalsIgnoreCase("quantity")) {
                    retVal.quantity = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                    retVal.daysToWait = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
                    retVal.entity = MekHqXmlUtil.getEntityFromXmlString(wn2);
                } 
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.logError(ex);
        }
        
        retVal.initializeParts(false);

        return retVal;
    }
    
}