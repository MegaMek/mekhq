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

import mekhq.MekHQ;
import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * Battle Armor suits are crazy - you cant crit the equipment in them, so
 * if we remove the suit we should remove all the equipment with the same trooper and
 * track its value and tonnage in the suit object. As of 0.3.16, we are doing this differently. We are
 * now using the linked child and parent part ids from the Part java to link the suit to all of its
 * constituent equipment and armor. This stuff is then pulled off the unit and put back on with the
 * BattleArmorSuit.remove and MissingBattleArmorSuit.fix methods. This allows us to adjust for the fact
 * that modular equipment can now be removed separately. We still need to figure out how to acquire
 * new suits that come pre-packaged with all of their equipment.
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
    private Money alternateCost = Money.zero();
    private double alternateTon;
    private int introYear;
    private boolean isReplacement;

    public BattleArmorSuit() {
        super(0, null);
        this.trooper = 0;
        this.quad = false;
        this.weightClass= 0;
        this.groundMP = 0;
        this.jumpMP = 0;
        this.clan = false;
        this.introYear = EquipmentType.DATE_NONE;
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
        this.name = chassis + " " + model + " Suit";
        initializeExtraCostsAndTons();
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
        this.name = chassis + " " + model + " Suit";
        initializeExtraCostsAndTons();
    }

    public BattleArmorSuit clone() {
        BattleArmorSuit clone = new BattleArmorSuit(chassis, model, getUnitTonnage(), trooper, weightClass, groundMP, jumpMP, quad, clan, jumpType, campaign);
        clone.copyBaseData(this);
        clone.alternateCost = this.alternateCost;
        clone.alternateTon = this.alternateTon;
        return clone;
    }

    public int getTrooper() {
        return trooper;
    }

    public void setTrooper(int i) {
        trooper = i;
    }

    public double getTonnage() {
        //if there are no linked parts and the unit is null,
        //then use the pre-recorded alternate costs
        if ((null == unit) && !hasChildParts()) {
            return alternateTon;
        }
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
        //if there are no linked parts and the unit is null,
        //then use the pre-recorded extra costs
        if ((null == unit) && !hasChildParts()) {
            tons += alternateTon;
        }
        for (Part p : getChildParts()) {
            if (!(p instanceof BattleArmorSuit)) {
                tons += p.getTonnage();
            }
        }
        return tons;
    }

    @Override
    public Money getStickerPrice() {
        //if there are no linked parts and the unit is null,
        //then use the pre-recorded alternate costs
        if ((null == unit) && !hasChildParts()) {
            return (alternateCost != null) ? alternateCost : Money.zero();
        }
        Money cost = Money.zero();
        switch(weightClass) {
        case EntityWeightClass.WEIGHT_MEDIUM:
            cost = cost.plus(100000);
            if(jumpType == EntityMovementMode.VTOL) {
                cost = cost.plus(jumpMP * 100000);
            } else {
                cost = cost.plus(jumpMP * 75000);
            }
            break;
        case EntityWeightClass.WEIGHT_HEAVY:
            cost = cost.plus(200000);
            if(jumpType == EntityMovementMode.INF_UMU) {
                cost = cost.plus(jumpMP * 100000);
            } else {
                cost = cost.plus(jumpMP * 150000);
            }
            break;
        case EntityWeightClass.WEIGHT_ASSAULT:
            cost = cost.plus(400000);
            if(jumpType == EntityMovementMode.INF_UMU) {
                cost = cost.plus(jumpMP * 150000);
            } else {
                cost = cost.plus(jumpMP * 300000);
            }
            break;
        default:
            cost = cost.plus(50000 * (jumpMP + 1));
        }
        cost = cost.plus(25000 * (groundMP-1));
        for (Part p : getChildParts()) {
            if (p instanceof BaArmor) {
                cost = cost.plus(p.getCurrentValue());
            } else if (!(p instanceof BattleArmorSuit)) {
                cost = cost.plus(p.getStickerPrice());
            }
        }

        return cost;
    }

    private void initializeExtraCostsAndTons() {
        alternateCost = Money.zero();
        alternateTon = 0;
        //simplest way to do this is just get the full cost and tonnage of a new unit and divide by
        //squad size
        MechSummary summary = MechSummaryCache.getInstance().getMech(getChassis() + " " + getModel());
        if(null != summary) {
            double squadSize = summary.getArmorTypes().length - 1;
            alternateCost = Money.of(summary.getAlternateCost()).dividedBy(squadSize);
            alternateTon = summary.getSuitWeight();
            introYear = summary.getYear();
        }
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
    public boolean isSamePartType(Part part) {
        //because of the linked children parts, we always need to consider these as different
        //return false;
        return part instanceof BattleArmorSuit
                && chassis.equals(((BattleArmorSuit)part).getChassis())
                && model.equals(((BattleArmorSuit)part).getModel())
                && getStickerPrice().equals(part.getStickerPrice());
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
                +"<alternateCost>"
                +alternateCost.toXmlString()
                +"</alternateCost>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<alternateTon>"
                +alternateTon
                +"</alternateTon>");
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
            } else if (wn2.getNodeName().equalsIgnoreCase("alternateCost")) {
                alternateCost = Money.fromXmlString(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("alternateTon")) {
                alternateTon = Double.parseDouble(wn2.getTextContent());
            }
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
        ArrayList<Part> trooperParts = new ArrayList<>();
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
            for(Part part : unit.getParts()) {
                if(part instanceof BattleArmorEquipmentPart && ((BattleArmorEquipmentPart)part).getTrooper() == trooper) {
                    trooperParts.add(part);
                    addChildPart(part);
                }
                if(part instanceof BaArmor && ((BaArmor)part).getLocation() == trooper) {
                    BaArmor armorClone = (BaArmor)part.clone();
                    armorClone.setAmount(((BaArmor)part).getAmount());
                    armorClone.setParentPart(this);
                    campaign.getQuartermaster().addPart(armorClone, 0);
                    addChildPart(armorClone);
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
            campaign.getQuartermaster().addPart(missing, 0);
            trooper = 0;
            unit.removePart(this);
            //Taharqa: I am not sure why this runDiagnostic is here and I think its problematic
            //I know for certain it causes problems when we are trying to figure out damage
            //to salvage unit because it can sometimes update parts before it checks for destruction
            //so that they then appear to be the same and aren't checked. In general it seems
            //bad form. Looking through the code, I couldnt see any obvious reason for its
            //existence. I am going to remove it and see if it causes problems.
            //unit.runDiagnostic(false);
        }
        for(Part p : trooperParts) {
            p.remove(salvage);
        }
        Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
        if(!salvage) {
            campaign.getWarehouse().removePart(this);
        } else if(null != spare) {
            spare.incrementQuantity();
            campaign.getWarehouse().removePart(this);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {
            if (trooper < 0) {
                MekHQ.getLogger().error(getClass(), "updateConditionFromEntity",
                        "Trooper location -1 found on BattleArmorSuit attached to unit");
                return;
            }
            if(unit.getEntity().getInternal(trooper) == IArmorState.ARMOR_DESTROYED) {
                if(!checkForDestruction) {
                    remove(false);
                } else {
                    if(Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                        remove(false);
                    } else {
                        //it seems a little weird to change the entity here, but no other
                        //way to guarantee this happens
                        unit.getEntity().setInternal(0, trooper);
                    }
                }
            }
        }
    }

    @Override
    public int getBaseTime() {
        return 0;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if(null != unit) {
            return "Trooper " + trooper;
        } else {
            int nEquip = 0;
            int armor = 0;
            if (!hasChildParts()) {
                for (Part p : getChildParts()) {
                    if (p instanceof BaArmor) {
                        armor = ((BaArmor)p).getAmount();
                    } else {
                        nEquip++;
                    }
                }
                return nEquip + " pieces of equipment; " + armor + " armor points";
            }
        }
        return super.getDetails(includeRepairDetails);
    }

    @Override
    public void updateConditionFromPart() {
        //According to BT Forums, if a suit survives the 10+ roll, then it is fine
        //and does not need to be repaired
        //http://bg.battletech.com/forums/index.php/topic,33650.new.html#new
        //so we will never damage the part
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        if(isSalvaging()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "BA suit removal");
        }
        return super.getAllMods(tech);

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

    @Override
    public void doMaintenanceDamage(int d) {
        //not sure what the best policy is here, because we have no way to repair suits
        //and no guidance from the rules as written, but I think we should just destroy
        //the suit as the maintenance damage roll for BA in StratOps destroys suits
        remove(false);

    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return trooper;
    }

    public boolean needsMaintenance() {
        return false;
    }


    /*
     * This method  will load up a TestUnit in order to identify the parts that need to be
     * added to the suit
     */
    private void addSubParts() {
        //first get a copy of the entity so we can create a test unit
        MechSummary summary = MechSummaryCache.getInstance().getMech(getChassis() + " " + getModel());
        if(null == summary) {
            return;
        }
        Entity newEntity = null;
        try {
            newEntity = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
        } catch (EntityLoadingException e) {
            MekHQ.getLogger().error(getClass(), "addSubParts", e);
        }
        Unit newUnit = null;
        if (null != newEntity) {
            newUnit = new TestUnit(newEntity, campaign, false);
        }
        if(null != newUnit) {
            //This now works, except when GM Mode is used to procure which must not be using the
            //find method
            for(Part part : newUnit.getParts()) {
                if(part instanceof BattleArmorEquipmentPart && ((BattleArmorEquipmentPart)part).getTrooper() == BattleArmor.LOC_TROOPER_1) {
                    Part newEquip = part.clone();
                    newEquip.setParentPart(this);
                    campaign.getQuartermaster().addPart(newEquip, 0);
                    addChildPart(newEquip);
                }
                else if(part instanceof BaArmor && ((BaArmor)part).getLocation() == BattleArmor.LOC_TROOPER_1) {
                    BaArmor armorClone = (BaArmor)part.clone();
                    armorClone.setAmount(newUnit.getEntity().getOArmor(BattleArmor.LOC_TROOPER_1));
                    armorClone.setParentPart(this);
                    campaign.getQuartermaster().addPart(armorClone, 0);
                    addChildPart(armorClone);
                }
            }
        }
    }

    /**
     * Sets a value indicating whether or not this part
     * is being used as a replacement.
     */
    public void isReplacement(boolean value) {
        isReplacement = value;
    }

    @Override
    public void postProcessCampaignAddition() {
        if (!isReplacement && !hasChildParts()) {
            addSubParts();
        }
    }

    @Override
    public int getIntroductionDate() {
        return introYear;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return BattleArmor.getConstructionTechAdvancement(weightClass);
    }

    @Override
    public int getMassRepairOptionType() {
        return Part.REPAIR_PART_TYPE.ARMOR;
    }
}
