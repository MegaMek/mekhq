/*
 * MissingBattleArmorSuit.java
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
import java.util.ArrayList;

import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.TargetRoll;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.MissingBattleArmorEquipmentPart;
import mekhq.campaign.unit.Unit;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingBattleArmorSuit extends MissingPart {

    /**
     * 
     */
    private static final long serialVersionUID = -3028121751208423160L;
    protected String chassis;
    protected String model;
    protected int trooper;
    protected boolean clan;
    protected boolean quad;
    protected int groundMP;
    protected int jumpMP;
    protected EntityMovementMode jumpType;
    protected int weightClass;

    public MissingBattleArmorSuit() {
        super(0, null);
    }
  
    
    public MissingBattleArmorSuit(String ch, String m, int ton, int t, int w, int gmp, int jmp, boolean q, boolean clan, EntityMovementMode mode, Campaign c) {
        super(ton, c);
        this.chassis = ch;
        this.model = m;
        this.trooper = t;
        this.quad = q;
        this.weightClass= w;
        this.groundMP = gmp;
        this.jumpMP = jmp;
        this.jumpType = mode;
        this.clan = clan;
        
        this.name = chassis + " " + model + " Suit";
    }
    
    @Override
    public void updateConditionFromPart() {
        unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, trooper);        
    }

    @Override
    public String checkFixable() {
        return null;
    }

    public boolean isQuad() {
        return quad;
    }
    
    public int getWeightClass() {
        return weightClass;
    }
    
    public int getGroundMP() {
        return groundMP;
    }
    
    public int getJumpMP() {
        return jumpMP;
    }
    
    public String getChassis() {
        return chassis;
    }
    
    public String getModel() {
        return model;
    }
    
    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof BattleArmorSuit
                && chassis.equals(((BattleArmorSuit)part).getChassis())
                && model.equals(((BattleArmorSuit)part).getModel());
    }

    @Override
    public Part getNewPart() {
        BattleArmorSuit suit = new BattleArmorSuit(chassis, model, getUnitTonnage(), -1, weightClass, groundMP, jumpMP, quad, clan, jumpType, campaign);
        long extraCost = 0;
        double extraTonnage = 0;
        for(Part part : unit.getParts()) {
            if(part instanceof MissingBattleArmorEquipmentPart && ((MissingBattleArmorEquipmentPart)part).getTrooper() == trooper) {
                Part newEquip = ((MissingBattleArmorEquipmentPart)part).getNewPart();
                extraCost += newEquip.getStickerPrice();
                extraTonnage += newEquip.getTonnage();
            }
            if(part instanceof BaArmor && ((BaArmor)part).getLocation() == trooper) {
            	
                extraCost += ((BaArmor)part).getAmountNeeded() * ((BaArmor)part).getPointCost();
                extraTonnage += ((BaArmor)part).getTonnageNeeded();
            }
        }
        suit.setExtraCost(extraCost);
        suit.setExtraTonnage(extraTonnage);
        suit.setArmorPoints(unit.getEntity().getOArmor(trooper));
        return suit;
    }
    
    @Override
    public TargetRoll getAllMods() {
        return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "BA suit removal");
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getAvailability(int era) {
        int chassisAvail = EquipmentType.RATING_E;
        if(weightClass > EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
            if(era == EquipmentType.ERA_SW) {
                chassisAvail = EquipmentType.RATING_F;
            }
        }
        else if(era < EquipmentType.ERA_CLAN) {
            chassisAvail = EquipmentType.RATING_X;
        }
        if(jumpType == EntityMovementMode.INF_UMU || jumpType == EntityMovementMode.VTOL) {
            chassisAvail = EquipmentType.RATING_F;
        }
        return chassisAvail;
    }

    @Override
    public int getTechRating() {
        int rating = EquipmentType.RATING_E;
        if(weightClass < EntityWeightClass.WEIGHT_LIGHT) {
            rating = EquipmentType.RATING_D;
        }
        return rating;
    }
    
    public int getTrooper() {
        return trooper;
    }
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<chassis>"
                +MekHqXmlUtil.escape(chassis)
                +"</chassis>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<model>"
                +MekHqXmlUtil.escape(model)
                +"</model>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<clan>"
                +clan
                +"</clan>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<trooper>"
                +trooper
                +"</trooper>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<quad>"
                +quad
                +"</quad>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<groundMP>"
                +groundMP
                +"</groundMP>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<jumpMP>"
                +jumpMP
                +"</jumpMP>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<weightClass>"
                +weightClass
                +"</weightClass>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<jumpType>"
                +MekHqXmlUtil.escape(EntityMovementMode.token(jumpType))
                +"</jumpType>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("trooper")) {
                trooper = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("groundMP")) {
                groundMP = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("jumpMP")) {
                jumpMP = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("weightClass")) {
                weightClass = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("quad")) {
                quad = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                clan = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("chassis")) {
                chassis = MekHqXmlUtil.unEscape(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("model")) {
                model = MekHqXmlUtil.unEscape(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("jumpType")) {
                jumpType = EntityMovementMode.type(MekHqXmlUtil.unEscape(wn2.getTextContent()));
            } 
        }
    }
    
    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if(null != replacement) {
            Unit u = unit;
            BattleArmorSuit newSuit = (BattleArmorSuit)replacement.clone();
            unit.addPart(newSuit);
            campaign.addPart(newSuit, 0);
            replacement.decrementQuantity();          
            newSuit.setTrooper(trooper);
            newSuit.updateConditionFromPart();
            //cycle through MissingBattleArmorEquipmentPart for trooper and replace
            ArrayList<MissingBattleArmorEquipmentPart> missingStuff = new ArrayList<MissingBattleArmorEquipmentPart>();
            for(Part p : unit.getParts()) {
                if(!(p instanceof MissingBattleArmorEquipmentPart)) {
                    continue;
                }
                MissingBattleArmorEquipmentPart missingBaEquip = (MissingBattleArmorEquipmentPart)p;
                if(missingBaEquip.getTrooper() != trooper) {
                    continue;
                }
                missingStuff.add(missingBaEquip);
            }
            for(MissingBattleArmorEquipmentPart p : missingStuff) {
                Part newBaEquip = p.getNewPart();
                campaign.addPart(newBaEquip, 0);
                p.fix();
            }
            unit.getEntity().setArmor(newSuit.getArmorPoints(), trooper);
            unit.getEntity().setInternal(0, trooper);
            newSuit.resetExtras();
            remove(false);
            u.runDiagnostic();
        }
    }
    
}