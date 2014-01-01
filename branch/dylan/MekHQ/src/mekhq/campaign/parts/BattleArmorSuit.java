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
import java.util.ArrayList;

import megamek.common.BattleArmor;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Battle Armor suits are crazy - you cant crit the equipment in them, so 
 * if we remove the suit we should remove all the equipment with the same trooper and
 * track its value and tonnage in the suit object. Then when this suit is replaced somewhere else, 
 * we add back in the missing BA equipment parts for that trooper and change the armor as appropriate.
 * At some point, we should figure out how to handle modularity through customization
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class BattleArmorSuit extends Part {
    private static final long serialVersionUID = -122291037522319765L;
    
    
    
    protected String chassis;
    protected String model;
    protected boolean clan;
    protected int trooper;
    protected boolean quad;
    protected int groundMP;
    protected int jumpMP;
    protected EntityMovementMode jumpType;
    protected int weightClass;
    
    /*
     * When individual suits are removed from the BA unit, then we want their equipment and 
     * armor to go with them. We will replace those parts with missing parts (and zero out armor)
     * in the remove method, but we also need to adjust stuff for this part in the warehouse
     * to reflect the added equipment. Rather than actually bringing those parts with us.
     * we should just keep a record of the added value and weight and the armor points
     */
    //just keep an integer record of the armor remaining since BaArmor should not get removed
    long extraCost;
    double extraTonnage;
    int armorPoints;
    
    public BattleArmorSuit() {
        super(0, null);
        this.trooper = 0;
        this.quad = false;
        this.weightClass= 0;
        this.groundMP = 0;
        this.jumpMP = 0;
        this.armorPoints = 0;
        this.extraCost = 0;
        this.extraTonnage = 0;
        this.clan = false;
        this.jumpType = EntityMovementMode.NONE;
        this.name = "BattleArmor Suit";
    }
    
    public BattleArmorSuit(BattleArmor ba, int loc, Campaign c) {
        super((int)ba.getWeight(), c);
        this.trooper = loc;
        this.quad = ba.getChassisType() == BattleArmor.CHASSIS_TYPE_QUAD;
        this.weightClass= ba.getWeightClass();
        this.groundMP = ba.getOriginalWalkMP();
        this.jumpMP = ba.getOriginalJumpMP();
        this.clan = ba.isClan();
        this.chassis = ba.getChassis();
        this.model = ba.getModel();
        this.jumpType = ba.getMovementMode();
        this.extraCost = 0;
        this.extraTonnage = 0;
        this.armorPoints = 0;       
        this.name = chassis + " " + model + " Suit";
    }
    
    public BattleArmorSuit(String ch, String m, int ton, int t, int w, int gmp, int jmp, boolean q, boolean clan, EntityMovementMode mode, Campaign c) {
        super(ton, c);
        this.trooper = t;
        this.quad = q;
        this.weightClass= w;
        this.groundMP = gmp;
        this.jumpMP = jmp;
        this.clan = clan;
        this.chassis = ch;
        this.model = m;
        this.jumpType = mode;
        this.armorPoints = 0;
        this.extraCost = 0;
        this.extraTonnage = 0;
        this.name = chassis + " " + model + " Suit";
    }
    
    public BattleArmorSuit clone() {
        BattleArmorSuit clone = new BattleArmorSuit(chassis, model, getUnitTonnage(), trooper, weightClass, groundMP, jumpMP, quad, clan, jumpType, campaign);
        clone.armorPoints = armorPoints;
        clone.extraCost = extraCost;
        clone.extraTonnage = extraTonnage;
        clone.copyBaseData(this);
        return clone;
    }
    
    public int getTrooper() {
        return trooper;
    }
    
    public void setTrooper(int i) {
        trooper = i;
    }
    
    public double getTonnage() {
        double tons = 0;
        switch(weightClass) {
        case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
            if(clan) {
                tons += 0.13;
            } else {
                    tons += 0.08;
            }
            tons += groundMP * .025;
            if(jumpType == EntityMovementMode.INF_UMU) {
                tons += jumpMP * .045;
            }
            else if(jumpType == EntityMovementMode.VTOL) {
                tons += jumpMP * .03;
            } else {
                tons += jumpMP * .025;
            }
            break;
        case EntityWeightClass.WEIGHT_LIGHT:
            if(clan) {
                tons += 0.15;
            } else {
                    tons += 0.1;
            }
            tons += groundMP * .03;
            if(jumpType == EntityMovementMode.INF_UMU) {
                tons += jumpMP * .045;
            }
            else if(jumpType == EntityMovementMode.VTOL) {
                tons += jumpMP * .04;
            } else {
                tons += jumpMP * .025;
            }
            break;
        case EntityWeightClass.WEIGHT_MEDIUM:
            if(clan) {
                tons += 0.25;
            } else {
                    tons += 0.175;
            }
            tons += groundMP * .04;
            if(jumpType == EntityMovementMode.INF_UMU) {
                tons += jumpMP * .085;
            }
            else if(jumpType == EntityMovementMode.VTOL) {
                tons += jumpMP * .06;
            } else {
                tons += jumpMP * .05;
            }
            break;
        case EntityWeightClass.WEIGHT_HEAVY:
            if(clan) {
                tons += 0.4;
            } else {
                    tons += 0.3;
            }
            tons += groundMP * .08;
            if(jumpType == EntityMovementMode.INF_UMU) {
                tons += jumpMP * .16;
            }
            else {
                tons += jumpMP * .125;
            }
            break;
        case EntityWeightClass.WEIGHT_ASSAULT:
            if(clan) {
                tons += 0.7;
            } else {
                    tons += 0.55;
            }
            tons += groundMP * .16;       
            tons += jumpMP * .25;
            break;
        }
        tons += extraTonnage;
        return tons;
    }
    
    @Override
    public long getStickerPrice() {
        long cost = 0;
        switch(weightClass) {
        case EntityWeightClass.WEIGHT_MEDIUM:
            cost += 100000;
            if(jumpType == EntityMovementMode.VTOL) {
                cost += jumpMP * 100000;
            } else {
                cost += jumpMP * 75000;
            }
            break;
        case EntityWeightClass.WEIGHT_HEAVY:
            cost += 200000;
            if(jumpType == EntityMovementMode.INF_UMU) {
                cost += jumpMP * 100000;
            } else {
                cost += jumpMP * 150000;
            }
            break;
        case EntityWeightClass.WEIGHT_ASSAULT:
            cost += 400000;
            if(jumpType == EntityMovementMode.INF_UMU) {
                cost += jumpMP * 150000;
            } else {
                cost += jumpMP * 300000;
            }
            break;
        default:
            cost += 50000;
            cost += 50000 * jumpMP;
        }
        cost += 25000 * (groundMP-1);
        cost += extraCost;
        return cost;
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
    
    public long getExtraCost() {
        return extraCost;
    }
    
    public void setExtraCost(long c) {
        extraCost = c;
    }
    
    public double getExtraTonnage() {
        return extraTonnage;
    }
    
    public void setExtraTonnage(double d) {
        extraTonnage = d;
    }
    
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof BattleArmorSuit
                && chassis.equals(((BattleArmorSuit)part).getChassis())
                && model.equals(((BattleArmorSuit)part).getModel());
    }
    
    @Override
    public boolean isSameStatus(Part part) {
        return super.isSameStatus(part) 
                && getArmorPoints() == ((BattleArmorSuit)part).getArmorPoints()
                && getExtraCost() == ((BattleArmorSuit)part).getExtraCost()
                && getExtraTonnage() == ((BattleArmorSuit)part).getExtraTonnage();
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
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<extraCost>"
                +extraCost
                +"</extraCost>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<extraTonnage>"
                +extraTonnage
                +"</extraTonnage>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<armorPoints>"
                +armorPoints
                +"</armorPoints>");
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
            } else if (wn2.getNodeName().equalsIgnoreCase("extraCost")) {
                extraCost = Long.parseLong(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("extraTonnage")) {
                extraTonnage = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("armorPoints")) {
                armorPoints = Integer.parseInt(wn2.getTextContent());
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
    
    @Override
    public int getTechLevel() {
        if(clan) {
            return TechConstants.T_CLAN_TW;           
        } else {
            return TechConstants.T_IS_TW_NON_BOX;
        }
    }
    
    @Override
    public int getTechBase() {
        if(clan) {
            return T_CLAN;
        } else {
            return T_IS;
        }
    }

    @Override
    public void fix() {
        super.fix();
        if(null != unit) {
            unit.getEntity().setInternal(unit.getEntity().getOInternal(trooper), trooper);
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingBattleArmorSuit(chassis, model, getUnitTonnage(), trooper, weightClass, groundMP, jumpMP, quad, clan, jumpType, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            Person trooperToRemove = null;
            if(unit.getEntity().getInternal(trooper) > 0) {
                //then there is a trooper here, so remove a crewmember
                if(unit.getCrew().size() > 0) {
                    trooperToRemove = unit.getCrew().get(unit.getCrew().size()-1);
                    //dont remove yet - we need to first set the internal to 
                    //destroyed so, this slot gets skipped over when we reset the pilot
                }
            }          
            armorPoints = unit.getEntity().getArmorForReal(trooper, false);
            extraCost = 0;
            extraTonnage = 0;
            ArrayList<BattleArmorEquipmentPart> trooperParts = new ArrayList<BattleArmorEquipmentPart>();
            for(Part part : unit.getParts()) {
                if(part instanceof BattleArmorEquipmentPart && ((BattleArmorEquipmentPart)part).getTrooper() == trooper) {
                    trooperParts.add((BattleArmorEquipmentPart)part);
                }
                if(part instanceof BaArmor && ((BaArmor)part).getLocation() == trooper) {
                    extraCost += part.getCurrentValue();
                    extraTonnage += part.getTonnage();
                }
            }
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, trooper);
            if(null != trooperToRemove) {
                unit.remove(trooperToRemove, true);
            }
            unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, trooper);
            unit.getEntity().setLocationBlownOff(trooper, false);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.addPart(missing, 0);
            trooper = -1;          
            for(BattleArmorEquipmentPart p : trooperParts) {
                extraCost += p.getStickerPrice();
                extraTonnage += p.getTonnage();
                p.remove(false);
            }
            Part spare = campaign.checkForExistingSparePart(this);
            if(!salvage) {
                campaign.removePart(this);
            } else if(null != spare) {
                spare.incrementQuantity();
                campaign.removePart(this);
            }
            unit.removePart(this);        
            unit.runDiagnostic();
        }
        setSalvaging(false);
        setUnit(null);
        updateConditionFromEntity();
    }
    
    @Override
    public void updateConditionFromEntity() {
        if(null != unit) {
            if(unit.getEntity().getInternal(trooper) == IArmorState.ARMOR_DESTROYED) {
                remove(false);
                return;
            }
        }
        if(isSalvaging()) {
            this.time = 0;
            this.difficulty = 0;
        }     
    }
    
    @Override
    public String getDetails() {
        if(null != unit) {
            return "Trooper " + trooper;
        } else {
            //TODO: information about equipment?
            return "";
        }
    }

    @Override
    public void updateConditionFromPart() {
        //According to BT Forums, if a suit survives the 10+ roll, then it is fine
        //and does not need to be repaired
        //http://bg.battletech.com/forums/index.php/topic,33650.new.html#new
        //so we will never damage the part
    }
    
    @Override
    public TargetRoll getAllMods() {
        if(isSalvaging()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "BA suit removal");
        }
        return super.getAllMods();

    }
    
    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_BA);
    }

    @Override
    public boolean needsFixing() {
        return false;
    }

    @Override
    public String checkFixable() {
        return null;
    }
    
    public void resetExtras() {
        extraCost = 0;
        extraTonnage = 0;
        armorPoints = 0;
    }
    
    public int getArmorPoints() {
        return armorPoints;
    }
    
    public void setArmorPoints(int p) {
        armorPoints = p;
    }
    
    @Override
    public void doMaintenanceDamage(int d) {
        //not sure what the best policy is here, because we have no way to repair suits
        //and no guidance from the rules as written, but I think we should just destroy
        //the suit as the maintenance damage roll for BA in StratOps destroys suits
        remove(false);
        
    }
    
    
}
