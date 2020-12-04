/*
 * Armor.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Objects;

import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.ITechnology;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.WorkTime;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Armor extends Part implements IAcquisitionWork {
    private static final long serialVersionUID = 5275226057484468868L;
    protected int type;
    protected int amount;
    protected int amountNeeded;
    protected int location;
    private boolean rear;
    protected boolean clan;

    public Armor() {
        this(0, 0, 0, -1, false, false, null);
    }

    public Armor(int tonnage, int t, int points, int loc, boolean r, boolean clan, Campaign c) {
        // Amount is used for armor quantity, not tonnage
        super(tonnage, c);
        this.type = t;
        this.amount = points;
        this.location = loc;
        this.rear = r;
        this.clan = clan;
        this.name = "Armor";
        if(type > -1) {
            this.name += " (" + EquipmentType.armorNames[type] + ")";
        }
    }

    public Armor clone() {
        Armor clone = new Armor(0, type, amount, -1, false, clan, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        return amount / getArmorPointsPerTon();
    }

    @Override
    public Money getCurrentValue() {
        return Money.of(getTonnage() * EquipmentType.getArmorCost(type));
    }

    public double getTonnageNeeded() {
        double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(type, isClanTechBase());
        if (type == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        return amountNeeded / armorPerTon;
    }

    public Money getValueNeeded() {
        return adjustCostsForCampaignOptions(Money.of(getTonnageNeeded() * EquipmentType.getArmorCost(type)));
    }

    @Override
    public Money getStickerPrice() {
        //always in 5-ton increments
        return Money.of(5 * EquipmentType.getArmorCost(type));
    }

    @Override
    public Money getBuyCost() {
        return getStickerPrice();
    }

    public String getDesc() {
        if(isSalvaging()) {
            return super.getDesc();
        }
        String bonus = getAllMods(null).getValueAsString();
        if (getAllMods(null).getValue() > -1) {
            bonus = "+" + bonus;
        }
        bonus = "(" + bonus + ")";
        String toReturn = "<html><font size='2'";

        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>Replace " + getName() + "</b><br/>";
        toReturn += getDetails() + "<br/>";
        if(getAmountAvailable() > 0) {
            toReturn += "" + getTimeLeft() + " minutes" + scheduled;
            if(!getCampaign().getCampaignOptions().isDestroyByMargin()) {
                toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
            }
            toReturn += " " + bonus;
        }
        if (getMode() != WorkTime.NORMAL) {
            toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if(null != unit) {
            String rearMount = "";
            if(rear) {
                rearMount = " (R)";
            }
            if(!isSalvaging()) {
                String availability;
                int amountAvailable = getAmountAvailable();
                PartInventory inventories = campaign.getPartInventory(getNewPart());

                String orderTransitString = getOrderTransitStringForDetails(inventories);

                if(amountAvailable == 0) {
                    availability = "<br><font color='red'>No armor " + orderTransitString + "</font>";
                } else if(amountAvailable < amountNeeded) {
                    availability = "<br><font color='red'>Only " + amountAvailable + " available " + orderTransitString + "</font>";
                } else {
                    availability = "<br><font color='green'>" + amountAvailable + " available " + orderTransitString + "</font>";
                }

                return unit.getEntity().getLocationName(location) + rearMount + ", " + amountNeeded + " points" + availability;
            }
            return unit.getEntity().getLocationName(location) + rearMount + ", " + amount + " points";
        }
        return amount + " points";
    }

    public int getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public int getAmountNeeded() {
        return amountNeeded;
    }

    public int getTotalAmount() {
        return amount + amountNeeded;
    }

    @Override
    public int getLocation() {
        return location;
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(location) : null;
    }

    public boolean isRearMounted() {
        return rear;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setAmountNeeded(int needed) {
        this.amountNeeded = needed;
    }

    public boolean isSameType(Armor armor) {
        if(getType() == EquipmentType.T_ARMOR_STANDARD
                && armor.getType() == EquipmentType.T_ARMOR_STANDARD) {
            //standard armor is compatible between clan and IS
            return true;
        }
        return getType() == armor.getType()  && isClanTechBase() == armor.isClanTechBase();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof Armor)
                && Objects.equals(getRefitUnit(), part.getRefitUnit())
                && isSameType((Armor)part);
    }

    @Override
    public boolean isSameStatus(Part part) {
        return this.getDaysToArrival() == part.getDaysToArrival();
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return EquipmentType.getArmorTechAdvancement(type, clan);
    }

    public double getArmorWeight(int points) {
        // from megamek.common.Entity.getArmorWeight()

        // this roundabout method is actually necessary to avoid rounding
        // weirdness. Yeah, it's dumb.
        double armorPointMultiplier = EquipmentType.getArmorPointMultiplier(getType(), isClanTechBase());
        double armorPerTon = 16.0 * armorPointMultiplier;
        if (getType() == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }

        double armorWeight = points / armorPerTon;
        armorWeight = Math.ceil(armorWeight * 2.0) / 2.0;
        return armorWeight;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        String level1 = MekHqXmlUtil.indentStr(indent+1);
        StringBuilder builder = new StringBuilder(128);
        builder.append(level1)
            .append("<amount>")
                .append(amount)
                .append("</amount>")
                .append(NL);
        builder.append(level1)
                .append("<type>")
                .append(type)
                .append("</type>")
                .append(NL);
        builder.append(level1)
                .append("<location>")
                .append(location)
                .append("</location>")
                .append(NL);
        builder.append(level1)
                .append("<rear>")
                .append(rear)
                .append("</rear>")
                .append(NL);
        builder.append(level1)
                .append("<amountNeeded>")
                .append(amountNeeded)
                .append("</amountNeeded>")
                .append(NL);
        builder.append(level1)
                .append("<clan>")
                .append(clan)
                .append("</clan>")
                .append(NL);
        pw1.print(builder.toString());
        writeAdditionalFields(pw1, indent + 1);
        writeToXmlEnd(pw1, indent);
    }

    /**
     * This should be overridden by subclasses that need to write additional fields
     *
     * @param pw      The writer instance
     * @param indent  The amount to indent the xml output
     */
    protected void writeAdditionalFields(PrintWriter pw, int indent) {
        // do nothing
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("amount")) {
                amount = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("type")) {
                type = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("location")) {
                location = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("amountNeeded")) {
                amountNeeded = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("rear")) {
                rear = wn2.getTextContent().equalsIgnoreCase("true");
            } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                clan = wn2.getTextContent().equalsIgnoreCase("true");
            }
        }
    }

    @Override
    public void fix() {
        if (unit.getEntity().isCapitalScale()) {
            amountNeeded *= 10;
        }
        int amountFound = Math.min(getAmountAvailable(), amountNeeded);
        int fixAmount = Math.min(amount +
                // Make sure that we handle the capital scale conversion when setting the fix amount
                (unit.getEntity().isCapitalScale() ? (amountFound / 10) : amountFound), unit.getEntity().getOArmor(location, rear));
        unit.getEntity().setArmor(fixAmount, location, rear);
        changeAmountAvailable(-1 * amountFound);
        updateConditionFromEntity(false);
        skillMin = SkillType.EXP_GREEN;
        shorthandedMod = 0;
    }

    @Override
    public String find(int transitDays) {
        Part newPart = getNewPart();
        newPart.setBrandNew(true);
        newPart.setDaysToArrival(transitDays);
        if(campaign.getQuartermaster().buyPart(newPart, transitDays)) {
            return "<font color='green'><b> part found</b>.</font> It will be delivered in " + transitDays + " days.";
        } else {
            return "<font color='red'><b> You cannot afford this part. Transaction cancelled</b>.</font>";
        }
    }

    @Override
    public Object getNewEquipment() {
        return getNewPart();
    }

    @Override
    public String failToFind() {
        return "<font color='red'><b> part not found</b>.</font>";
    }

    @Override
    public MissingPart getMissingPart() {
        //no such thing
        return null;
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return new Armor(0, type, (int)Math.round(5 * getArmorPointsPerTon()), -1, false, clan, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, location, rear);
        if(salvage) {
            // Account for capital-scale units when warehouse armor is stored at standard scale.
            if (unit.getEntity().isCapitalScale()) {
                amount *= 10;
            }
            changeAmountAvailable(amount);
        }
        updateConditionFromEntity(false);
    }

    public int getBaseTimeFor(Entity entity) {
        if (null != entity) {
            if (entity instanceof Tank) {
                return 3;
            }
            //December 2017 errata, only large craft should return 15m/point.
            else if (entity.hasETypeFlag(Entity.ETYPE_DROPSHIP) || entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                return 15;
            }
        }
        return 5;
    }


    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(isReservedForRefit()) {
            return;
        }
        if(null == unit) {
            return;
        }
        amount = unit.getEntity().getArmorForReal(location, rear);
        if(amount < 0) {
            amount = 0;
        }
        amountNeeded = unit.getEntity().getOArmor(location, rear) - amount;
    }

    @Override
    public int getBaseTime() {
        Entity entity = unit != null ? unit.getEntity() : null;
        if (isSalvaging()) {
            return getBaseTimeFor(entity) * amount;
        }
        return getBaseTimeFor(entity) * Math.min(amountNeeded, getAmountAvailable());
    }

    @Override
    public int getDifficulty() {
        return -2;
    }

    @Override
    public boolean isSalvaging() {
        return super.isSalvaging() && amount > 0;
    }

    @Override
    public boolean needsFixing() {
        return amountNeeded > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            int armor = Math.min(amount, unit.getEntity().getOArmor(location, rear));
            if(armor == 0) {
                armor = IArmorState.ARMOR_DESTROYED;
            }
            unit.getEntity().setArmor(armor, location, rear);
        }
    }

    @Override
    public String checkFixable() {
        if(isSalvaging()) {
            return null;
        }
        if(getAmountAvailable() == 0) {
            return "No spare armor available";
        }
        if (isMountedOnDestroyedLocation()) {
            return unit.getEntity().getLocationName(location) + " is destroyed.";
        }
        return null;
    }

    @Override
    public boolean isMountedOnDestroyedLocation() {
        return null != unit && unit.isLocationDestroyed(location);
    }

    @Override
    public boolean onBadHipOrShoulder() {
        return null != unit && unit.hasBadHipOrShoulder(location);
    }

    @Override
    public String getAcquisitionDesc() {
        String toReturn = "<html><font size='2'";

        toReturn += ">";
        toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
        toReturn += getAcquisitionExtraDesc() + "<br/>";
        PartInventory inventories = campaign.getPartInventory(getAcquisitionPart());
        toReturn += inventories.getTransitOrderedDetails() + "<br/>";
        toReturn += adjustCostsForCampaignOptions(getStickerPrice()).toAmountAndSymbolString() + "<br/>";
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public String getAcquisitionDisplayName() {
        return getName();
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return ((int)Math.round(getArmorPointsPerTon())) * 5 + " points (5 tons)";
    }

    @Override
    public String getAcquisitionName() {
        return getName();
    }

    @Override
    public String getAcquisitionBonus() {
        String bonus = getAllAcquisitionMods().getValueAsString();
        if(getAllAcquisitionMods().getValue() > -1) {
            bonus = "+" + bonus;
        }

        return "(" + bonus + ")";
    }

    @Override
    public Part getAcquisitionPart() {
        return getNewPart();
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        if(isClanTechBase() && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        }
        else if(campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        //availability mod
        int avail = getAvailability();
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ")");
        return target;
    }

    public double getArmorPointsPerTon() {
        //if(null != unit) {
            // armor is checked for in 5-ton increments
            //int armorType = unit.getEntity().getArmorType(location);
        double armorPerTon = 16.0 * EquipmentType.getArmorPointMultiplier(type, clan);
        if (type == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        return armorPerTon;
        //}
        //return 0.0;
    }

    public Part getNewPart() {
        return new Armor(0, type, (int)Math.round(5 * getArmorPointsPerTon()), -1, false, clan, campaign);
    }

    public boolean isEnoughSpareArmorAvailable() {
        return getAmountAvailable() >= amountNeeded;
    }

    public int getAmountAvailable() {
        Armor a = (Armor) campaign.getWarehouse().findSparePart(part -> {
            return part instanceof Armor
                && part.isPresent()
                && !part.isReservedForRefit()
                && isSameType((Armor)part);
        });

        return a != null ? a.getAmount() : 0;
    }

    public void changeAmountAvailable(int amount) {
        Armor a = (Armor) campaign.getWarehouse().findSparePart(part -> {
            return (part instanceof Armor)
                && part.isPresent()
                && Objects.equals(getRefitUnit(), part.getRefitUnit())
                && isSameType((Armor)part);
        });

        if (null != a) {
            a.setAmount(a.getAmount() + amount);
            if (a.getAmount() <= 0) {
                campaign.getWarehouse().removePart(a);
            }
        } else if (amount > 0) {
            campaign.getQuartermaster().addPart(new Armor(getUnitTonnage(), type, amount, -1, false, isClanTechBase(), campaign), 0);
        }
    }

    @Override
    public String fail(int rating) {
        skillMin = ++rating;
        timeSpent = 0;
        shorthandedMod = 0;
        //if we are impossible to fix now, we should scrap this amount of armor
        //from spares and start over
        String scrap = "";
        if(skillMin > SkillType.EXP_ELITE) {
            scrap = " Armor supplies lost!";
            if(isSalvaging()) {
                remove(false);
            } else {
                skillMin = SkillType.EXP_GREEN;
                changeAmountAvailable(-1 * Math.min((unit.getEntity().isCapitalScale() ? (amountNeeded * 10) : amountNeeded), getAmountAvailable()));
            }
        }
        return " <font color='red'><b> failed." + scrap + "</b></font>";
    }

    @Override
    public String scrap() {
        remove(false);
        skillMin = SkillType.EXP_GREEN;
        return EquipmentType.armorNames[type] + " armor scrapped.";
    }

    @Override
    public boolean isInSupply() {
        //int currentArmor = Math.max(0, unit.getEntity().getArmorForReal(location, rear));
        //int fullArmor = unit.getEntity().getOArmor(location, rear);
        //int neededArmor = fullArmor - currentArmor;
        return amountNeeded <= getAmountAvailable();
    }

    @Override
    public String getQuantityName(int quan) {
        double totalTon = quan * getTonnage();
        String report = "" + DecimalFormat.getInstance().format(totalTon) + " tons of " + getName();
        if(totalTon == 1.0) {
            report = "" + DecimalFormat.getInstance().format(totalTon) + " ton of " + getName();
        }
        return report;
    }

    @Override
    public String getArrivalReport() {
        double totalTon = quantity * getTonnage();
        String report = getQuantityName(quantity);
        if(totalTon == 1.0) {
            report += " has arrived";
        } else {
            report += " have arrived";
        }
        return report;
    }

    @Override
    public void doMaintenanceDamage(int d) {
        int current = unit.getEntity().getArmor(location, rear);
        if(d >= current) {
            unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, location, rear);
        } else {
            unit.getEntity().setArmor(current - d, location, rear);

       }
       updateConditionFromEntity(false);
    }

    @Override
    public boolean isPriceAdjustedForAmount() {
        return true;
    }

    public void changeType(int ty, boolean cl) {
        this.type = ty;
        this.clan = cl;
        this.name = "Armor";
        if(type > -1) {
            this.name += " (" + EquipmentType.armorNames[type] + ")";
        }
    }

    @Override
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.ARMOR;
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return isExtinct(year, clan, techFaction);
    }
}
