/*
 * Armor.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.ITechnology;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.ArmorType;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.WorkTime;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Armor extends Part implements IAcquisitionWork {
    private static final MMLogger logger = MMLogger.create(Armor.class);

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
        if (type > -1) {
            this.name += " (" + ArmorType.of(type, clan).getName() + ')';
        }
    }

    @Override
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
    public Money getActualValue() {
        return adjustCostsForCampaignOptions(Money.of(getTonnage() * ArmorType.of(type, clan).getCost()));
    }

    public double getTonnageNeeded() {
        double armorPerTon = ArmorType.of(type, isClanTechBase()).getPointsPerTon();
        if (type == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }
        return amountNeeded / armorPerTon;
    }

    public Money getValueNeeded() {
        return adjustCostsForCampaignOptions(Money.of(getTonnageNeeded() * ArmorType.of(type, clan).getCost()));
    }

    @Override
    public Money getStickerPrice() {
        // always in 5-ton increments
        return Money.of(5 * ArmorType.of(type, clan).getCost());
    }

    @Override
    public Money getBuyCost() {
        return getActualValue();
    }

    @Override
    public String getDesc() {
        if (isSalvaging()) {
            return super.getDesc();
        }
        StringBuilder toReturn = new StringBuilder();
        toReturn.append("<html><b>Replace ")
            .append(getName());
        if (!getCampaign().getCampaignOptions().isDestroyByMargin()) {
            toReturn.append(" - ")
            .append(ReportingUtilities.messageSurroundedBySpanWithColor(
                SkillType.getExperienceLevelColor(getSkillMin()),
                SkillType.getExperienceLevelName(getSkillMin()) + "+"));
        }
        toReturn.append("</b><br/>")
            .append(getDetails())
            .append("<br/>");

        if (getSkillMin() <= SkillType.EXP_ELITE) {
            toReturn.append(getTimeLeft())
                .append(" minutes")
                .append(null != getTech() ? " (scheduled)" : "")
                .append(" <b>TN:</b> ")
                .append(getAllMods(null).getValue() > -1 ? "+" : "")
                .append(getAllMods(null).getValueAsString());
            if (getMode() != WorkTime.NORMAL) {
                toReturn.append(" <i>")
                    .append(getCurrentModeName())
                    .append( "</i>");
            }
        }
        toReturn.append("</html>");
        return toReturn.toString();
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringBuilder toReturn = new StringBuilder();
        if (null != unit) {
            if (isSalvaging()) {
                toReturn.append(unit.getEntity().getLocationName(location))
                .append(rear ? " (Rear)" : "")
                .append(", ")
                .append(amount)
                .append(amount == 1 ? " point" : " points");
            } else {
                toReturn.append(unit.getEntity().getLocationName(location))
                    .append(rear ? " (Rear)" : "")
                    .append(", ")
                    .append(amountNeeded)
                    .append(amountNeeded == 1 ? " point" : " points")
                    .append("<br/>");

                int amountAvailable = getAmountAvailable();
                if(amountAvailable == 0) {
                    toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(
                        MekHQ.getMHQOptions().getFontColorNegativeHexColor(), "None in stock"));
                } else if (amountAvailable < amountNeeded) {
                    toReturn.append(ReportingUtilities.spanOpeningWithCustomColor(
                            MekHQ.getMHQOptions().getFontColorNegativeHexColor()))
                        .append("Only ")
                        .append(amountAvailable)
                        .append(" in stock")
                        .append(ReportingUtilities.CLOSING_SPAN_TAG);
                } else {
                    toReturn.append(ReportingUtilities.spanOpeningWithCustomColor(
                            MekHQ.getMHQOptions().getFontColorPositiveHexColor()))
                        .append(amountAvailable)
                        .append(" in stock")
                        .append(ReportingUtilities.CLOSING_SPAN_TAG);
                }
    
                PartInventory inventories = campaign.getPartInventory(getNewPart());
                String orderTransitString = inventories.getTransitOrderedDetails();
                if (!orderTransitString.isEmpty()) {
                    toReturn.append(ReportingUtilities.spanOpeningWithCustomColor(
                            MekHQ.getMHQOptions().getFontColorWarningHexColor()))
                        .append(" (")
                        .append(orderTransitString)
                        .append(")")
                        .append(ReportingUtilities.CLOSING_SPAN_TAG);
                }
            }
        
        } else {
            toReturn.append(amount)
                .append(" points");
        }
        return toReturn.toString();
    }

    public int getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public void addAmount(final int amount) {
        this.amount += amount;
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
        if (getType() == EquipmentType.T_ARMOR_STANDARD
                && armor.getType() == EquipmentType.T_ARMOR_STANDARD) {
            // standard armor is compatible between clan and IS
            return true;
        }
        return getType() == armor.getType() && isClanTechBase() == armor.isClanTechBase();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (getClass() == part.getClass())
                && Objects.equals(getRefitUnit(), part.getRefitUnit())
                && isSameType((Armor) part);
    }

    @Override
    public boolean isSameStatus(Part part) {
        return this.getDaysToArrival() == part.getDaysToArrival();
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ArmorType.of(type, clan).getTechAdvancement();
    }

    public double getArmorWeight(int points) {
        // from megamek.common.Entity.getArmorWeight()

        // this roundabout method is actually necessary to avoid rounding
        // weirdness. Yeah, it's dumb.
        double armorPerTon = ArmorType.of(getType(), isClan()).getPointsPerTon();
        if (getType() == EquipmentType.T_ARMOR_HARDENED) {
            armorPerTon = 8.0;
        }

        double armorWeight = points / armorPerTon;
        armorWeight = Math.ceil(armorWeight * 2.0) / 2.0;
        return armorWeight;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "amount", amount);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "location", location);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rear", rear);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "amountNeeded", amountNeeded);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clan", clan);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
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
            } catch (Exception e) {
                logger.error("", e);
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
        // Make sure that we handle the capital scale conversion when setting the fix
        // amount
                (unit.getEntity().isCapitalScale() ? (amountFound / 10) : amountFound),
                unit.getEntity().getOArmor(location, rear));
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
        if (campaign.getQuartermaster().buyPart(newPart, transitDays)) {
            return "<font color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor()
                    + "'><b> part found</b>.</font> It will be delivered in " + transitDays + " days.";
        } else {
            return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor()
                    + "'><b> You cannot afford this part. Transaction cancelled</b>.</font>";
        }
    }

    @Override
    public Object getNewEquipment() {
        return getNewPart();
    }

    @Override
    public String failToFind() {
        return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor()
                + "'><b> part not found</b>.</font>";
    }

    @Override
    public MissingPart getMissingPart() {
        // no such thing
        return null;
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return new Armor(0, type, (int) Math.round(5 * getArmorPointsPerTon()), -1, false, clan, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        unit.getEntity().setArmor(IArmorState.ARMOR_DESTROYED, location, rear);
        if (salvage) {
            // Account for capital-scale units when warehouse armor is stored at standard
            // scale.
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
            } else if (entity instanceof Dropship) {
                return 15;
            } else if (entity.isCapitalScale()) {
                return 120;
            }   
        }
        // Meks, protomeks, battle armor, and normal aerospace
        return 5;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (isReservedForRefit()) {
            return;
        }
        if (null == unit) {
            return;
        }
        amount = unit.getEntity().getArmorForReal(location, rear);
        if (amount < 0) {
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
        if (null != unit) {
            int armor = Math.min(amount, unit.getEntity().getOArmor(location, rear));
            if (armor == 0) {
                armor = IArmorState.ARMOR_DESTROYED;
            }
            unit.getEntity().setArmor(armor, location, rear);
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (isSalvaging()) {
            return null;
        }
        if (getAmountAvailable() == 0) {
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
        String toReturn = "<html><font";

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
        return ((int) Math.round(getArmorPointsPerTon())) * 5 + " points (5 tons)";
    }

    @Override
    public String getAcquisitionName() {
        return getName();
    }

    @Override
    public String getAcquisitionBonus() {
        String bonus = getAllAcquisitionMods().getValueAsString();
        if (getAllAcquisitionMods().getValue() > -1) {
            bonus = '+' + bonus;
        }
        return '(' + bonus + ')';
    }

    @Override
    public Part getAcquisitionPart() {
        return getNewPart();
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        if (isClanTechBase() && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        } else if (campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        // availability mod
        int avail = getAvailability();
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ')');
        return target;
    }

    @Override
    public int getSellableQuantity() {
        return amount;
    }

    public double getArmorPointsPerTon() {
        return ArmorType.of(type, clan).getPointsPerTon();
    }

    public Part getNewPart() {
        return new Armor(0, type, (int) Math.round(5 * getArmorPointsPerTon()), -1, false, clan, campaign);
    }

    public boolean isEnoughSpareArmorAvailable() {
        return getAmountAvailable() >= amountNeeded;
    }

    public int getAmountAvailable() {
        Armor a = (Armor) campaign.getWarehouse().findSparePart(part -> (part instanceof Armor)
                && part.isPresent()
                && !part.isReservedForRefit()
                && isSameType((Armor) part));

        return (a == null) ? 0 : a.getAmount();
    }

    public void changeAmountAvailable(int amount) {
        Armor a = (Armor) campaign.getWarehouse().findSparePart(part -> (part instanceof Armor)
                && part.isPresent()
                && Objects.equals(getRefitUnit(), part.getRefitUnit())
                && isSameType((Armor) part));

        if (null != a) {
            a.setAmount(a.getAmount() + amount);
            if (a.getAmount() <= 0) {
                campaign.getWarehouse().removePart(a);
            }
        } else if (amount > 0) {
            campaign.getQuartermaster()
                    .addPart(new Armor(getUnitTonnage(), type, amount, -1, false, isClanTechBase(), campaign), 0);
        }
    }

    @Override
    public String fail(int rating) {
        skillMin = ++rating;
        timeSpent = 0;
        shorthandedMod = 0;
        // if we are impossible to fix now, we should scrap this amount of armor
        // from spares and start over
        String scrap = "";
        if (skillMin > SkillType.EXP_ELITE) {
            scrap = " Armor supplies lost!";
            if (isSalvaging()) {
                remove(false);
            } else {
                skillMin = SkillType.EXP_GREEN;
                changeAmountAvailable(
                        -1 * Math.min((unit.getEntity().isCapitalScale() ? (amountNeeded * 10) : amountNeeded),
                                getAmountAvailable()));
            }
        }
        return " <font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'><b> failed." + scrap
                + "</b></font>";
    }

    @Override
    public String scrap() {
        remove(false);
        skillMin = SkillType.EXP_GREEN;
        return ArmorType.of(type, clan).getName() + " armor scrapped.";
    }

    @Override
    public boolean isInSupply() {
        return amountNeeded <= getAmountAvailable();
    }

    @Override
    public String getQuantityName(int quan) {
        double totalTon = quan * getTonnage();
        String report = DecimalFormat.getInstance().format(totalTon) + " tons of " + getName();
        if (totalTon == 1.0) {
            report = DecimalFormat.getInstance().format(totalTon) + " ton of " + getName();
        }
        return report;
    }

    @Override
    public String getArrivalReport() {
        double totalTon = quantity * getTonnage();
        String report = getQuantityName(quantity);
        if (totalTon == 1.0) {
            report += " has arrived";
        } else {
            report += " have arrived";
        }
        return report;
    }

    @Override
    public void doMaintenanceDamage(int d) {
        int current = unit.getEntity().getArmor(location, rear);
        if (d >= current) {
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
        if (type > -1) {
            this.name += " (" + ArmorType.of(type, clan).getName() + ')';
        }
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ARMOUR;
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
