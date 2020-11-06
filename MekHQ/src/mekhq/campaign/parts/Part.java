/*
 * Part.java
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

import mekhq.campaign.finances.Money;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.SimpleTechLevel;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTime;

/**
 * Parts do the lions share of the work of repairing, salvaging, reloading, refueling, etc.
 * for units. Each unit has an ArrayList of all its relevant parts. There is a corresponding unit
 * variable in part but this can be null when we are dealing with a spare part, so when putting in
 * calls to unit, you should always check to make sure it is not null.
 *
 * There are two kinds of parts: Part and MissingPart. The latter is used as a placeholder on a unit to
 * indicate it is missing the given part. When parts are removed from a unit, they shold be replaced
 * with the appropriate missing part which will remind MHQ that a replacement needs to be done.
 *
 * Parts implement IPartWork and MissingParts also implement IAcquisitionWork. These interfaces allow for
 * most of the actual work that can be done on parts. There is a lot of variability in how parts actually handle
 * this work
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class Part implements Serializable, MekHqXmlSerializable, IPartWork, ITechnology {
    private static final long serialVersionUID = 6185232893259168810L;
    public static final int PART_TYPE_ARMOR = 0;
    public static final int PART_TYPE_WEAPON = 1;
    public static final int PART_TYPE_AMMO = 2;
    public static final int PART_TYPE_EQUIPMENT_PART = 3;
    public static final int PART_TYPE_MEK_ACTUATOR = 4;
    public static final int PART_TYPE_MEK_ENGINE = 5;
    public static final int PART_TYPE_MEK_GYRO = 6;
    public static final int PART_TYPE_MEK_LIFE_SUPPORT = 7;
    public static final int PART_TYPE_MEK_BODY_PART = 8;
    public static final int PART_TYPE_MEK_SENSOR = 9;
    public static final int PART_TYPE_GENERIC_SPARE_PART = 10;
    public static final int PART_TYPE_OTHER = 11;
    public static final int PART_TYPE_MEK_COCKPIT = 12;
    public static final int PART_TYPE_OMNI_SPACE = 13;

    public static final int T_UNKNOWN = -1;
    public static final int T_BOTH = 0;
    public static final int T_IS   = 1;
    public static final int T_CLAN = 2;

    public static final int QUALITY_A = 0;
    public static final int QUALITY_B = 1;
    public static final int QUALITY_C = 2;
    public static final int QUALITY_D = 3;
    public static final int QUALITY_E = 4;
    public static final int QUALITY_F = 5;

    public interface REPAIR_PART_TYPE {
        int ARMOR = 0;
        int AMMO = 1;
        int WEAPON = 2;
        int GENERAL_LOCATION = 3;
        int ENGINE = 4;
        int GYRO = 5;
        int ACTUATOR = 6;
        int ELECTRONICS = 7;
        int GENERAL = 8;
        int HEATSINK = 9;
        int MEK_LOCATION = 10;
        int PHYSICAL_WEAPON = 11;
        int POD_SPACE = 12;
    }

    protected static final String NL = System.lineSeparator();

    private static final String[] partTypeLabels = { "Armor", "Weapon", "Ammo",
            "Equipment Part", "Mek Actuator", "Mek Engine", "Mek Gyro",
            "Mek Life Support", "Mek Body Part", "Mek Sensor",
            "Generic Spare Part", "Other", "Mek Cockpit", "Pod Space" };

    protected static final TechAdvancement TA_POD = Entity.getOmniAdvancement();
    // Generic TechAdvancement for a number of basic components.
    protected static final TechAdvancement TA_GENERIC = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
            .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    public static String[] getPartTypeLabels() {
        return partTypeLabels;
    }

    protected String name;
    protected int id;

    //this is the unitTonnage which needs to be tracked for some parts
    //even when off the unit. actual tonnage is returned via the
    //getTonnage() method
    protected int unitTonnage;

    protected boolean omniPodded;

    //hits to this part
    protected int hits;

    //Taharqa: as of 8/12/2015, we are no longer going to track difficulty and time
    //as hard coded numbers but rather use abstract methods that get them from each part
    //depending on the dynamic characteristics of the part
    // the skill modifier for difficulty
    //protected int difficulty;
    // the amount of time for the repair (this is the base time)
    //protected int time;


    // time spent on the task so far for tasks that span days
    protected int timeSpent;
    // the minimum skill level in order to attempt
    protected int skillMin;
    //current repair mode for part
    protected WorkTime mode;

    protected Person tech;
    private boolean isTeamSalvaging;

    //null is valid. It indicates parts that are not attached to units.
    protected Unit unit;

    protected int quality;

    protected boolean brandNew;

    //we need to keep track of a couple of potential mods that result from carrying
    //over a task, otherwise people can get away with working over time with no consequence
    protected boolean workingOvertime;
    protected int shorthandedMod;

    /** This tracks the unit which resorved the part for a refit */
    private Unit refitUnit;
    /** The unique identifier of the tech who is reserving this part for overnight work */
    private Person reservedBy;
    //temporarily mark the part used by current refit planning
    protected transient boolean usedForRefitPlanning;

    //for delivery
    protected int daysToArrival;

    //all parts need a reference to campaign
    protected Campaign campaign;

    /*
     * This will be unusual but in some circumstances certain parts will be linked to other parts.
     * These linked parts will be considered integral and subsidary to those other parts and will
     * not show up independently. Currently (8/8/2015), we are only using this for BA suits
     * We need a parent part id and a vector of children parts to represent this.
     */
    protected Part parentPart;
    protected ArrayList<Part> childParts;

    /**
     * The number of parts in exactly the same condition,
     * to track multiple spare parts more efficiently and also the shopping list
     */
    protected int quantity;

    //reverse-compatibility
    protected int oldUnitId = -1;
    protected int oldTeamId = -1;
    protected int oldRefitId = -1;

    //only relevant for parts that can be acquired
    protected int daysToWait;

    /** The part which will be used as a replacement */
    private Part replacementPart;

    public Part() {
        this(0, false, null);
    }

    public Part(int tonnage, Campaign c) {
        this(tonnage, false, c);
    }

    public Part(int tonnage, boolean omniPodded, Campaign c) {
        this.name = "Unknown";
        this.unitTonnage = tonnage;
        this.omniPodded = omniPodded;
        this.hits = 0;
        this.skillMin = SkillType.EXP_GREEN;
        this.mode = WorkTime.NORMAL;
        this.timeSpent = 0;
        this.workingOvertime = false;
        this.shorthandedMod = 0;
        this.usedForRefitPlanning = false;
        this.daysToArrival = 0;
        this.campaign = c;
        this.brandNew = true;
        this.quantity = 1;
        this.quality = QUALITY_D;
        this.childParts = new ArrayList<>();
        this.isTeamSalvaging = false;
    }

    public static String getQualityName(int quality, boolean reverse) {
        switch (quality) {
            case QUALITY_A:
                if (reverse) {
                    return "F";
                }
                return "A";
            case QUALITY_B:
                if (reverse) {
                    return "E";
                }
                return "B";
            case QUALITY_C:
                if (reverse) {
                    return "D";
                }
                return "C";
            case QUALITY_D:
                if (reverse) {
                    return "C";
                }
                return "D";
            case QUALITY_E:
                if (reverse) {
                    return "B";
                }
                return "E";
            case QUALITY_F:
                if (reverse) {
                    return "A";
                }
                return "F";
            default:
                return "?";
        }
    }

    public String getQualityName() {
        return getQualityName(getQuality(), campaign.getCampaignOptions().reverseQualityNames());
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setCampaign(Campaign c) {
        this.campaign = c;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getName() {
        return name;
    }

    /**
     * Sticker price is the value of the part according to the rulebooks
     * @return
     */
    public abstract Money getStickerPrice();

    /**
     * This is the actual value of the part as affected by any characteristics
     * of the part itself
     * @return
     */
    public Money getCurrentValue() {
        return getStickerPrice();
    }

    /**
     * This is the value of the part that may be affected by campaign options
     * @return
     */
    public Money getActualValue() {
        return adjustCostsForCampaignOptions(getCurrentValue());
    }

    public boolean isPriceAdjustedForAmount() {
        return false;
    }

    protected Money adjustCostsForCampaignOptions(@Nullable Money cost) {
        // if the part doesn't cost anything, no amount of multiplication will change it
        if ((cost == null) || cost.isZero()) {
            return Money.zero();
        }

        if (getTechBase() == T_CLAN) {
            cost = cost.multipliedBy(campaign.getCampaignOptions().getClanPriceModifier());
        }
        if (needsFixing() && !isPriceAdjustedForAmount()) {
            cost = cost.multipliedBy(campaign.getCampaignOptions().getDamagedPartsValue());
            //TODO: parts that cant be fixed should also be further reduced in price
        } else if (!isBrandNew()) {
            cost = cost.multipliedBy(campaign.getCampaignOptions().getUsedPartsValue(getQuality()));
        }

        return cost;
    }

    public boolean isBrandNew() {
        return brandNew;
    }

    public void setBrandNew(boolean b) {
        this.brandNew = b;
    }

    /**
     * Gets a value indicating if there is a replacement
     * part assigned to this part.
     */
    public boolean hasReplacementPart() {
        return replacementPart != null;
    }

    /**
     * Gets the replacement for this part.
     */
    @Nullable
    public Part getReplacementPart() {
        return replacementPart;
    }

    /**
     * Sets the replacement part for this part.
     */
    public void setReplacementPart(@Nullable Part part) {
        replacementPart = part;
    }

    public int getUnitTonnage() {
        return unitTonnage;
    }

    public abstract double getTonnage();

    public boolean isOmniPodded() {
        return omniPodded;
    }

    public void setOmniPodded(boolean omniPod) {
        this.omniPodded = omniPod;
    }

    public @Nullable Unit getUnit() {
        return unit;
    }

    public void setUnit(@Nullable Unit u) {
        unit = u;
        if (null != unit) {
            unitTonnage = (int) unit.getEntity().getWeight();
        }
    }

    public String getStatus() {
        String toReturn = "Functional";
        if (needsFixing()) {
            toReturn = "Damaged";
        }
        if (isReservedForRefit()) {
            toReturn = "Reserved for Refit";
        }
        if (isReservedForReplacement()) {
            toReturn = "Reserved for Repair";
        }
        if (isBeingWorkedOn()) {
            toReturn = "Being worked on";
        }
        if (!isPresent()) {
            //toReturn = "" + getDaysToArrival() + " days to arrival";
            String dayName = "day";
            if (getDaysToArrival() > 1) {
                dayName += "s";
            }
            toReturn = "In transit (" + getDaysToArrival() + " " + dayName + ")";
        }
        return toReturn;
    }

    public int getHits() {
        return hits;
    }

    public String getDesc() {
        String bonus = getAllMods(null).getValueAsString();
        if (getAllMods(null).getValue() > -1) {
            bonus = "+" + bonus;
        }
        bonus = "(" + bonus + ")";
        String toReturn = "<html><font size='2'";
        String action = "Repair ";
        if (isSalvaging()) {
            action = "Salvage ";
        }
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>" + action + getName() + "</b><br/>";
        toReturn += getDetails() + "<br/>";
        if (getSkillMin() > SkillType.EXP_ELITE) {
            toReturn += "<font color='red'>Impossible</font>";
        } else {
            toReturn += "" + getTimeLeft() + " minutes" + scheduled;
            if (!getCampaign().getCampaignOptions().isDestroyByMargin()) {
                toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
            }
            toReturn += " " + bonus;
            if (getMode() != WorkTime.NORMAL) {
                toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
            }
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    public String getRepairDesc() {
        String toReturn = "";
        if (needsFixing()) {
            String scheduled = "";
            if (getTech() != null) {
                scheduled = " (scheduled) ";
            }
            String bonus = getAllMods(null).getValueAsString();
            if (getAllMods(null).getValue() > -1) {
                bonus = "+" + bonus;
            }
            bonus = "(" + bonus + ")";
            toReturn += getTimeLeft() + " minutes" + scheduled;
            toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
            toReturn += " " + bonus;
            if (getMode() != WorkTime.NORMAL) {
                toReturn += ", " + getCurrentModeName();
            }
        }
        return toReturn;
    }

    public String getTechBaseName() {
        return getTechBaseName(getTechBase());
    }

    public static String getTechBaseName(int base) {
        switch (base) {
            case T_BOTH:
                return "IS/Clan";
            case T_CLAN:
                return "Clan";
            case T_IS:
                return "IS";
            case T_UNKNOWN:
                return "UNKNOWN";
            default:
                return "??";
        }
    }

    /**
     * @return TechConstants tech level
     */
    public int getTechLevel() {
        return getSimpleTechLevel().getCompoundTechLevel(campaign.getFaction().isClan());
    }

    public SimpleTechLevel getSimpleTechLevel() {
        if (campaign.useVariableTechLevel()) {
            return getSimpleLevel(campaign.getGameYear());
        } else {
            return getStaticTechLevel();
        }
    }

    public SimpleTechLevel getSimpleTechLevel(int year) {
        if (campaign.useVariableTechLevel()) {
            return getSimpleLevel(year);
        } else {
            return getStaticTechLevel();
        }
    }

    public SimpleTechLevel getSimpleTechLevel(int year, boolean clan, int faction) {
        if (campaign.useVariableTechLevel()) {
            return getSimpleLevel(year, clan, faction);
        } else {
            return getStaticTechLevel();
        }
    }

    /**
     * We are going to only limit parts by year if they totally haven't been produced
     * otherwise, we will just replace the existing availability code with X
     */
    public boolean isIntroducedBy(int year) {
        return year >= getIntroductionDate();
    }

    /**
     * Checks if the current part is exactly the "same kind" of part as the part
     * given in argument. This is used to determine whether we need to add new spare
     * parts, or increment existing ones.
     *
     * @param part
     *            The part to be compared with the current part
     */
    public boolean isSamePartTypeAndStatus(Part part) {
        return isSamePartType(part) && isSameStatus(part);
    }

    public abstract boolean isSamePartType(Part part);

    public boolean isSameStatus(Part part) {
        //parts that are reserved for refit or being worked on are never the same status
        if (isReservedForRefit() || isBeingWorkedOn() || isReservedForReplacement() || hasParentPart()
                || part.isReservedForRefit() || part.isBeingWorkedOn() || part.isReservedForReplacement() || part.hasParentPart()) {
            return false;
        }
        return quality == part.getQuality() && hits == part.getHits() && part.getSkillMin() == this.getSkillMin() && this.getDaysToArrival() == part.getDaysToArrival();
    }

    protected boolean isClanTechBase() {
        return getTechBase() == TECH_BASE_CLAN;
    }

    @Override
    public abstract void writeToXml(PrintWriter pw1, int indent);

    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        String level = MekHqXmlUtil.indentStr(indent),
            level1 = MekHqXmlUtil.indentStr(indent + 1);

        StringBuilder builder = new StringBuilder(256);
        builder.append(level)
            .append("<part id=\"")
            .append(id)
            .append("\" type=\"")
            .append(this.getClass().getName())
            .append("\">")
            .append(NL)
            .append(level1)
            .append("<id>")
            .append(id)
            .append("</id>")
            .append(NL)
            .append(level1)
            .append("<name>")
            .append(MekHqXmlUtil.escape(name))
            .append("</name>")
            .append(NL);
        if (omniPodded) {
            builder.append(level1)
                .append("<omniPodded/>")
                .append(NL);
        }
        builder.append(level1)
            .append("<unitTonnage>")
            .append(unitTonnage)
            .append("</unitTonnage>")
            .append(NL);
        if (hits > 0) {
            builder.append(level1)
                .append("<hits>")
                .append(hits)
                .append("</hits>")
                .append(NL);
        }
        if (timeSpent > 0) {
            builder.append(level1)
                .append("<timeSpent>")
                .append(timeSpent)
                .append("</timeSpent>")
                .append(NL);
        }
        builder.append(level1)
            .append("<mode>")
            .append(mode)
            .append("</mode>")
            .append(NL);
        if (null != tech) {
            builder.append(level1)
                .append("<teamId>")
                .append(tech.getId())
                .append("</teamId>")
                .append(NL);
        }
        builder.append(level1)
            .append("<skillMin>")
            .append(skillMin)
            .append("</skillMin>")
            .append(NL);
        if (null != unit) {
            builder.append(level1)
                .append("<unitId>")
                .append(unit.getId())
                .append("</unitId>")
                .append(NL);
        }
        if (workingOvertime) {
            builder.append(level1)
                .append("<workingOvertime>")
                .append(workingOvertime)
                .append("</workingOvertime>")
                .append(NL);
        }
        if (shorthandedMod != 0) {
            builder.append(level1)
                .append("<shorthandedMod>")
                .append(shorthandedMod)
                .append("</shorthandedMod>")
                .append(NL);
        }
        if (refitUnit != null) {
            builder.append(level1)
                .append("<refitId>")
                .append(refitUnit.getId())
                .append("</refitId>")
                .append(NL);
        }
        if (daysToArrival > 0) {
            builder.append(level1)
                .append("<daysToArrival>")
                .append(daysToArrival)
                .append("</daysToArrival>")
                .append(NL);
        }
        if (brandNew) {
            builder.append(level1)
                .append("<brandNew>")
                .append(brandNew)
                .append("</brandNew>")
                .append(NL);
        }
        builder.append(level1)
            .append("<quantity>")
            .append(quantity)
            .append("</quantity>")
            .append(NL);
        if (daysToWait > 0) {
            builder.append(level1)
                .append("<daysToWait>")
                .append(daysToWait)
                .append("</daysToWait>")
                .append(NL);
        }
        if (replacementPart != null) {
            builder.append(level1)
                .append("<replacementId>")
                .append(replacementPart.getId())
                .append("</replacementId>")
                .append(NL);
        }
        if (reservedBy != null) {
            builder.append(level1)
                .append("<reserveId>")
                .append(reservedBy.getId())
                .append("</reserveId>")
                .append(NL);
        }
        builder.append(level1)
            .append("<quality>")
            .append(quality)
            .append("</quality>")
            .append(NL);
        if (isTeamSalvaging) {
            builder.append(level1)
                .append("<isTeamSalvaging>")
                .append(isTeamSalvaging)
                .append("</isTeamSalvaging>")
                .append(NL);
        }
        if (parentPart != null) {
            builder.append(level1)
                .append("<parentPartId>")
                .append(parentPart.getId())
                .append("</parentPartId>")
                .append(NL);
        }
        for (Part childPart : childParts) {
            builder.append(level1)
                .append("<childPartId>")
                .append(childPart.getId())
                .append("</childPartId>")
                .append(NL);
        }
        pw1.print(builder.toString());
    }

    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</part>");
    }

    public static Part generateInstanceFromXML(Node wn, Version version) {
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();

        //reverse compatibility checks
        if (className.equalsIgnoreCase("mekhq.campaign.parts.MekEngine")) {
            className = "mekhq.campaign.parts.EnginePart";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.MissingMekEngine")) {
            className = "mekhq.campaign.parts.MissingEnginePart";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.EquipmentPart")) {
            className = "mekhq.campaign.parts.equipment.EquipmentPart";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.MissingEquipmentPart")) {
            className = "mekhq.campaign.parts.equipment.MissingEquipmentPart";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.AmmoBin")) {
            className = "mekhq.campaign.parts.equipment.AmmoBin";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.MissingAmmoBin")) {
            className = "mekhq.campaign.parts.equipment.MissingAmmoBin";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.JumpJet")) {
            className = "mekhq.campaign.parts.equipment.JumpJet";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.MissingJumpJet")) {
            className = "mekhq.campaign.parts.equipment.MissingJumpJet";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.HeatSink")) {
            className = "mekhq.campaign.parts.equipment.HeatSink";
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.MissingHeatSink")) {
            className = "mekhq.campaign.parts.equipment.MissingHeatSink";
        }

        Part retVal = null;
        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = (Part) Class.forName(className).getDeclaredConstructor().newInstance();
            retVal.loadFieldsFromXmlNode(wn);

            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.id = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("unitTonnage")) {
                    retVal.unitTonnage = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("omniPodded")) {
                    retVal.omniPodded = true;
                } else if (wn2.getNodeName().equalsIgnoreCase("quantity")) {
                    retVal.quantity = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
                    retVal.hits = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("timeSpent")) {
                    retVal.timeSpent = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("skillMin")) {
                    retVal.skillMin = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("mode")) {
                    retVal.mode = WorkTime.of(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                    retVal.daysToWait = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("teamId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        retVal.tech = new PartPersonRef(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        retVal.unit = new PartUnitRef(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("shorthandedMod")) {
                    retVal.shorthandedMod = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("refitId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        retVal.refitUnit = new PartUnitRef(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToArrival")) {
                    retVal.daysToArrival = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("workingOvertime")) {
                     retVal.workingOvertime = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("isTeamSalvaging")) {
                     retVal.isTeamSalvaging = wn2.getTextContent().equalsIgnoreCase("true");
                } else if (wn2.getNodeName().equalsIgnoreCase("brandNew")) {
                     retVal.brandNew = wn2.getTextContent().equalsIgnoreCase("true");
                }
                else if (wn2.getNodeName().equalsIgnoreCase("replacementId")) {
                    retVal.replacementPart = new PartRef(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("quality")) {
                    retVal.quality = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("parentPartId")) {
                    retVal.parentPart = new PartRef(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("childPartId")) {
                    int childPartId = Integer.parseInt(wn2.getTextContent());
                    if (childPartId > 0) {
                        retVal.childParts.add(new PartRef(childPartId));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("reserveId")) {
                    retVal.reservedBy = new PartPersonRef(UUID.fromString(wn2.getTextContent()));
                }
            }

            // Refit protection of unit id
            if (retVal.unit != null && retVal.refitUnit != null) {
                retVal.setUnit(null);
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().error(ex);
        }

        return retVal;
    }

    protected abstract void loadFieldsFromXmlNode(Node wn);

    @Override
    public int getActualTime() {
        return (int) Math.ceil(getBaseTime() * mode.timeMultiplier);
    }

    @Override
    public int getTimeLeft() {
        // Cannot be less than 0 time left.
        return Math.max(0, getActualTime() - getTimeSpent());
    }

    @Override
    public int getTimeSpent() {
        return timeSpent;
    }

    public void addTimeSpent(int m) {
        this.timeSpent += m;
    }

    public void resetTimeSpent() {
        this.timeSpent = 0;
    }

    public void resetOvertime() {
        this.workingOvertime = false;
    }

    @Override
    public int getSkillMin() {
        return skillMin;
    }

    public void setSkillMin(int i) {
        this.skillMin = i;
    }

    public WorkTime getMode() {
        return mode;
    }

    public void setMode(WorkTime wt) {
        if (canChangeWorkMode()) {
            this.mode = wt;
        } else {
            this.mode = WorkTime.NORMAL;
        }
    }

    /*
     * Reset our WorkTime back to normal so that we can adjust as
     * necessary
     */
    public void resetModeToNormal() {
        setMode(WorkTime.NORMAL);
    }

    @Override
    public boolean canChangeWorkMode() {
        return !(isOmniPodded() && isSalvaging());
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        int difficulty = getDifficulty();

        if (isOmniPodded() && (isSalvaging() || this instanceof MissingPart)
                && (null != unit) && !(unit.getEntity() instanceof Tank)) {
            difficulty -= 2;
        }

        if (null == mode) {
            mode = WorkTime.NORMAL;
        }

        TargetRoll mods = new TargetRoll(difficulty, "difficulty");
        int modeMod = mode.getMod(campaign.getCampaignOptions().isDestroyByMargin());
        if (modeMod != 0) {
            mods.addModifier(modeMod, getCurrentModeName());
        }
        if (null != unit) {
            mods.append(unit.getSiteMod());
            if (unit.getEntity().hasQuirk("easy_maintain")) {
                mods.addModifier(-1, "easy to maintain");
            }
            else if (unit.getEntity().hasQuirk("difficult_maintain")) {
                mods.addModifier(1, "difficult to maintain");
            }
        }
        if (isClanTechBase() || (this instanceof MekLocation && this.getUnit() != null && this.getUnit().getEntity().isClan())) {
            if (null != tech && !tech.isClanner()
                    && !tech.getOptions().booleanOption(PersonnelOptions.TECH_CLAN_TECH_KNOWLEDGE)) {
                mods.addModifier(2, "Clan tech");
            }
        }
        if (null != tech
                && tech.getOptions().booleanOption(PersonnelOptions.TECH_WEAPON_SPECIALIST)
                && (IPartWork.findCorrectRepairType(this) == Part.REPAIR_PART_TYPE.WEAPON
                || IPartWork.findCorrectMassRepairType(this) == Part.REPAIR_PART_TYPE.PHYSICAL_WEAPON)) {
            mods.addModifier(-1, "Weapon specialist");
        }
        if (null != tech
                && tech.getOptions().booleanOption(PersonnelOptions.TECH_ARMOR_SPECIALIST)
                        && IPartWork.findCorrectRepairType(this) == Part.REPAIR_PART_TYPE.ARMOR) {
            mods.addModifier(-1, "Armor specialist");
        }
        if (null != tech
                && tech.getOptions().booleanOption(PersonnelOptions.TECH_INTERNAL_SPECIALIST)
                && (IPartWork.findCorrectRepairType(this) == Part.REPAIR_PART_TYPE.ACTUATOR
                || IPartWork.findCorrectMassRepairType(this) == Part.REPAIR_PART_TYPE.ELECTRONICS
                || IPartWork.findCorrectMassRepairType(this) == Part.REPAIR_PART_TYPE.ENGINE
                || IPartWork.findCorrectMassRepairType(this) == Part.REPAIR_PART_TYPE.GYRO
                || IPartWork.findCorrectMassRepairType(this) == Part.REPAIR_PART_TYPE.MEK_LOCATION
                || IPartWork.findCorrectMassRepairType(this) == Part.REPAIR_PART_TYPE.GENERAL_LOCATION)) {
            mods.addModifier(-1, "Internal specialist");
        }

        mods = getQualityMods(mods, tech);

        return mods;
    }

    public TargetRoll getAllModsForMaintenance() {
        //according to StratOps you get a -1 mod when checking on individual parts
        //but we will make this user customizable
        TargetRoll mods = new TargetRoll(campaign.getCampaignOptions().getMaintenanceBonus(), "maintenance");
        mods.addModifier(Availability.getTechModifier(getTechRating()), "tech rating " + ITechnology.getRatingName(getTechRating()));

        if (null != getUnit()) {
            mods.append(getUnit().getSiteMod());
            if (getUnit().getEntity().hasQuirk("easy_maintain")) {
                mods.addModifier(-1, "easy to maintain");
            }
            else if (getUnit().getEntity().hasQuirk("difficult_maintain")) {
                mods.addModifier(1, "difficult to maintain");
            }

            if (isClanTechBase() || ((this instanceof MekLocation) && getUnit().getEntity().isClan())) {
                if (getUnit().getTech() == null) {
                    mods.addModifier(2, "Clan tech");
                } else if (!getUnit().getTech().isClanner()
                        && !getUnit().getTech().getOptions().booleanOption(PersonnelOptions.TECH_CLAN_TECH_KNOWLEDGE)) {
                    mods.addModifier(2, "Clan tech");
                }
            }
        }

        if (campaign.getCampaignOptions().useQualityMaintenance()) {
            mods = getQualityMods(mods, getUnit().getTech());
        }
        return mods;
    }

    /**
     * adds the quality modifiers for repair and maintenance of this part to a TargetRoll
     * @param mods - the {@link TargetRoll} that quality modifiers should be added to
     * @param tech - the {@link Person} that will make the repair or maintenance check, may be null
     * @return the modified {@link TargetRoll}
     */
    private TargetRoll getQualityMods(TargetRoll mods, Person tech) {
        int qualityMod = 0;
        switch (quality) {
            case QUALITY_A:
                qualityMod = 3;
                break;
            case QUALITY_B:
                qualityMod = 2;
                break;
            case QUALITY_C:
                qualityMod = 1;
                break;
            case QUALITY_D:
                qualityMod = 0;
                break;
            case QUALITY_E:
                qualityMod = -1;
                break;
            case QUALITY_F:
                qualityMod = -2;
                break;
        }
        mods.addModifier(qualityMod, getQualityName(quality, campaign.getCampaignOptions().reverseQualityNames()));
        if ((qualityMod > 0) &&
                (null != tech) &&
                tech.getOptions().booleanOption(PersonnelOptions.TECH_FIXER)) {
            //fixers can ignore the first point of penalty for poor quality
            mods.addModifier(-1, "Mr/Ms Fix-it");
        }
        return mods;
    }

    public String getCurrentModeName() {
        return mode.name;
    }

    @Override
    public @Nullable Person getTech() {
        return tech;
    }

    @Override
    public void setTech(@Nullable Person tech) {
        //keep track of whether this was a salvage operation
        //because the entity may change
        if (null == tech) {
            this.isTeamSalvaging = false;
        } else if (null == getTech()) {
            this.isTeamSalvaging = isSalvaging();
        }
        this.tech = tech;
    }

    public boolean isTeamSalvaging() {
        return null != getTech() && isTeamSalvaging;
    }

    /**
     * Sets the the team member who has reserved this part for work they are
     * performing overnight.
     * @param tech The team member.
     */
    public void setReservedBy(@Nullable Person tech) {
        this.reservedBy = tech;
    }

    @Override
    public String getPartName() {
        return name;
    }

    @Override
    public int getMassRepairOptionType() {
        return REPAIR_PART_TYPE.GENERAL;
    }

    @Override
    public int getRepairPartType() {
        return getMassRepairOptionType();
    }

    @Override
    public void fix() {
        hits = 0;
        resetRepairSettings();
    }

    /**
     * Sets minimum skill, shorthanded mod, and rush job/extra time setting to defaults.
     */
    public void resetRepairSettings() {
        skillMin = SkillType.EXP_GREEN;
        shorthandedMod = 0;
        mode = WorkTime.NORMAL;
    }

    @Override
    public String fail(int rating) {
        skillMin = ++rating;
        timeSpent = 0;
        shorthandedMod = 0;
        return " <font color='red'><b> failed.</b></font>";
    }

    @Override
    public String succeed() {
        if (isSalvaging()) {
            remove(true);
            return " <font color='green'><b> salvaged.</b></font>";
        } else {
            fix();
            return " <font color='green'><b> fixed.</b></font>";
        }
    }

    /**
     * Gets a string containing details regarding the part,
     * e.g. OmniPod or how many hits it has taken and its
     * repair cost.
     * @return A string containing details regarding the part.
     */
    @Override
    public String getDetails() {
        return getDetails(true);
    }

    /**
     * Gets a string containing details regarding the part,
     * and optionally include information on its repair
     * status.
     * @param includeRepairDetails {@code true} if the details
     *        should include information such as the number of
     *        hits or how much it would cost to repair the
     *        part.
     * @return A string containing details regarding the part.
     */
    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringJoiner sj = new StringJoiner(", ");
        if (!StringUtils.isEmpty(getLocationName())) {
            sj.add(getLocationName());
        }
        if (isOmniPodded()) {
            sj.add("OmniPod");
        }
        if (includeRepairDetails) {
            sj.add(hits + " hit(s)");
            if (campaign.getCampaignOptions().payForRepairs() && (hits > 0)) {
                Money repairCost = getStickerPrice().multipliedBy(0.2);
                sj.add(repairCost.toAmountAndSymbolString() + " to repair");
            }
        }
        return sj.toString();
    }

    /**
     * Converts the array of strings normally returned by a call to campaign.getInventory()
     * to a string that reads like "(x in transit, y on order)"
     * @param inventories The inventory array, see campaign.getInventory() for details.
     * @return Human readable string.
     */
    public String getOrderTransitStringForDetails(PartInventory inventories) {
        String inTransitString = inventories.getTransit() == 0 ? "" : inventories.transitAsString() + " in transit";
        String onOrderString = inventories.getOrdered() == 0 ? "" : inventories.orderedAsString() + " on order";
        String transitOrderSeparator = inTransitString.length() > 0 && onOrderString.length() > 0 ? ", " : "";

        return (inTransitString.length() > 0 || onOrderString.length() > 0) ?
                String.format("(%s%s%s)", inTransitString, transitOrderSeparator, onOrderString) : "";
    }

    @Override
    public boolean isSalvaging() {
        if (null != unit) {
            return unit.isSalvage() || isMountedOnDestroyedLocation() || isTeamSalvaging();
        }
        return false;
    }

    public String checkScrappable() {
        return null;
    }

    public boolean canNeverScrap() {
        return false;
    }

    public String scrap() {
        String msg;

        if (null == getUnit()) {
            msg = getName() + " scrapped.";
        } else {
            msg = getName() + " on " + unit.getName() + " scrapped.";
        }

        remove(false);
        return msg;
    }

    @Override
    public boolean hasWorkedOvertime() {
        return workingOvertime;
    }

    @Override
    public void setWorkedOvertime(boolean b) {
        workingOvertime = b;
    }

    @Override
    public int getShorthandedMod() {
        return shorthandedMod;
    }

    @Override
    public void setShorthandedMod(int i) {
        shorthandedMod = i;
    }

    @Override
    public abstract Part clone();

    protected void copyBaseData(Part part) {
        this.mode = part.mode;
        this.hits = part.hits;
        this.brandNew = part.brandNew;
        this.omniPodded = part.omniPodded;
        this.quality = part.quality;
    }

    /**
     * Sets the unit which has reserved this part for a refit.
     * @param unit The unit reserving this part for a refit.
     */
    public void setRefitUnit(@Nullable Unit unit) {
        refitUnit = unit;
    }

    /**
     * Gets the unit which reserved this part for a refit.
     * @return The unit reserving this part.
     */
    public @Nullable Unit getRefitUnit() {
        return refitUnit;
    }

    /**
     * Gets a value indicating if the part is reserved for a refit.
     */
    public boolean isReservedForRefit() {
        return refitUnit != null;
    }

    /**
     * Gets a value indicating if the part is reserved for an
     * overnight replacement task.
     */
    public boolean isReservedForReplacement() {
        return reservedBy != null;
    }

    public boolean isUsedForRefitPlanning() {
        return usedForRefitPlanning;
    }

    public void setUsedForRefitPlanning(boolean flag) {
        usedForRefitPlanning = flag;
    }

    /**
     * Sets the number of days until the part arrives.
     * @param days The number of days until the part arrives.
     */
    public void setDaysToArrival(int days) {
        daysToArrival = Math.max(days, 0);
    }

    /**
     * Gets the number of days until the part arrives.
     */
    public int getDaysToArrival() {
        return daysToArrival;
    }

    /**
     * Gets a value indicating whether or not the part is present.
     */
    public boolean isPresent() {
        return daysToArrival == 0;
    }

    public boolean isBeingWorkedOn() {
        return getTech() != null;
    }

    public boolean onBadHipOrShoulder() {
        return false;
    }

    public boolean isMountedOnDestroyedLocation() {
        return false;
    }

    public boolean isPartForEquipmentNum(int index, int loc) {
        return false;
    }

    public boolean isInSupply() {
        return true;
    }

    /**
     * Gets the number of parts on-hand.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Increments the number of parts in stock by one.
     */
    public void incrementQuantity() {
        setQuantity(getQuantity() + 1);
    }

    /**
     * Decrements the number of parts in stock by one,
     * and removes the part from the campaign if it
     * reaches zero.
     */
    public void decrementQuantity() {
        setQuantity(getQuantity() - 1);
    }

    /**
     * A method to set the number of parts en masse.
     * @param number The new number of spares in the pile.
     */
    public void setQuantity(int number) {
        quantity = Math.max(number, 0);
        if (quantity == 0) {
            for (Part childPart : childParts) {
                campaign.getWarehouse().removePart(childPart);
            }
            campaign.getWarehouse().removePart(this);
        }
    }

    /**
     * Gets a value indicating whether or not this is a spare part.
     */
    public boolean isSpare() {
        return (unit == null)
            && (parentPart == null)
            && (refitUnit == null)
            && (reservedBy == null);
    }

    public boolean isRightTechType(String skillType) {
        return true;
    }

    public boolean isOmniPoddable() {
        return false;
    }

    public int getDaysToWait() {
        return daysToWait;
    }

    public void resetDaysToWait() {
        this.daysToWait = campaign.getCampaignOptions().getWaitingPeriod();
    }

    public void decrementDaysToWait() {
        if (daysToWait > 0) {
            daysToWait--;
        }
    }

    public String getShoppingListReport(int quan) {
        return getQuantityName(quan) + ((quan > 1) ? " have " : " has ") + "been added to the procurement list.";
    }

    public String getArrivalReport() {
        return getQuantityName(quantity) + ((quantity > 1) ? " have " : " has ") + "arrived";
    }

    public String getQuantityName(int quantity) {
        String answer = "" + quantity + " " + getName();
        if (quantity > 1) {
            answer += "s";
        }
        return answer;
    }

    /** Get the acquisition work to acquire a new part of this type
     * For most parts this is just getMissingPart(), but some override it
     * @return
     */
    public IAcquisitionWork getAcquisitionWork() {
        return getMissingPart();
    }

    public void doMaintenanceDamage(int d) {
        hits += d;
        updateConditionFromPart();
        updateConditionFromEntity(false);
    }

    public int getQuality() {
        return quality;
    }

    public void improveQuality() {
        quality += 1;
    }

    public void decreaseQuality() {
        quality -= 1;
    }

    public void setQuality(int q) {
        quality = q;
    }

    public boolean needsMaintenance() {
        return true;
    }

    public void cancelAssignment() {
        setTech(null);
        resetOvertime();
        resetTimeSpent();
        setShorthandedMod(0);
    }

    public abstract String getLocationName();

    /**
     * Sets the parent part.
     * @param part The parent part.
     */
    public void setParentPart(@Nullable Part part) {
        parentPart = part;
    }

    /**
     * Gets the parent part, or null if none exists.
     */
    public @Nullable Part getParentPart() {
        return parentPart;
    }

    /**
     * Gets a value indicating whether or not this part
     * has a parent part.
     */
    public boolean hasParentPart() {
        return parentPart != null;
    }

    /**
     * Gets a value indicating whether or not this part has child parts.
     */
    public boolean hasChildParts() {
        return !childParts.isEmpty();
    }

    /**
     * Gets a list of child parts for this part.
     */
    public List<Part> getChildParts() {
        return Collections.unmodifiableList(childParts);
    }

    /**
     * Adds a child part to this part.
     * @param childPart The part to add as a child.
     */
    public void addChildPart(Part childPart) {
        childParts.add(Objects.requireNonNull(childPart));
        childPart.setParentPart(this);
    }

    /**
     * Removes a child part from this part.
     * @param childPart The child part to remove.
     */
    public void removeChildPart(Part childPart) {
        Objects.requireNonNull(childPart);

        if (childParts.remove(childPart)) {
            childPart.setParentPart(null);
        }
    }

    /**
     * Removes all child parts from this part.
     */
    public void removeAllChildParts() {
        for (Part childPart : childParts) {
            childPart.setParentPart(null);
        }
        childParts = new ArrayList<>();
    }

    /**
     * Reserve a part for overnight work
     */
    @Override
    public void reservePart() {
        //nothing goes here for real parts. Only missing parts need to reserve a replacement
    }

    @Override
    public void cancelReservation() {
        //nothing goes here for real parts. Only missing parts need to reserve a replacement
    }

    /**
     * Make any changes to the part needed for adding to the campaign
     */
    public void postProcessCampaignAddition() {
        //do nothing
    }

    public boolean isInLocation(String loc) {
        if (null == unit || null == unit.getEntity()) {
            return false;
        }
        return getLocation() == getUnit().getEntity().getLocationFromAbbr(loc);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        sb.append(" "); //$NON-NLS-1$
        sb.append(getDetails());
        sb.append(", q: "); //$NON-NLS-1$
        sb.append(quantity);
        if (null != unit) {
            sb.append(", mounted: "); //$NON-NLS-1$
            sb.append(unit);
        }
        return sb.toString();
    }

    public static String getRepairTypeShortName(int type) {
        switch (type) {
            case Part.REPAIR_PART_TYPE.ARMOR:
                return "Armor";
            case Part.REPAIR_PART_TYPE.AMMO:
                return "Ammo";
            case Part.REPAIR_PART_TYPE.WEAPON:
                return "Weapons";
            case Part.REPAIR_PART_TYPE.GENERAL_LOCATION:
                return "Locations";
            case Part.REPAIR_PART_TYPE.ENGINE:
                return "Engines";
            case Part.REPAIR_PART_TYPE.GYRO:
                return "Gyros";
            case Part.REPAIR_PART_TYPE.ACTUATOR:
                return "Actuators";
            case Part.REPAIR_PART_TYPE.ELECTRONICS:
                return "Cockpit/Life Support/Sensors";
            default:
                return "Other Items";
        }
    }

    public static String[] findPartImage(IPartWork part) {
        String imgBase = null;
        int repairType = IPartWork.findCorrectRepairType(part);

        switch (repairType) {
            case Part.REPAIR_PART_TYPE.ARMOR:
                imgBase = "armor";
                break;
            case Part.REPAIR_PART_TYPE.AMMO:
                imgBase = "ammo";
                break;
            case Part.REPAIR_PART_TYPE.ACTUATOR:
                imgBase = "actuator";
                break;
            case Part.REPAIR_PART_TYPE.ENGINE:
                imgBase = "engine";
                break;
            case Part.REPAIR_PART_TYPE.ELECTRONICS:
                imgBase = "electronics";
                break;
            case Part.REPAIR_PART_TYPE.HEATSINK:
                imgBase = "heatsink";
                break;
            case Part.REPAIR_PART_TYPE.WEAPON:
                EquipmentType equipmentType = null;

                if (part instanceof EquipmentPart) {
                    equipmentType = ((EquipmentPart)part).getType();
                } else if (part instanceof MissingEquipmentPart) {
                    equipmentType = ((MissingEquipmentPart)part).getType();
                }

                if (null != equipmentType) {
                    if (equipmentType.hasFlag(WeaponType.F_LASER)) {
                        imgBase = "laser";
                    } else if (equipmentType.hasFlag(WeaponType.F_MISSILE)) {
                        imgBase = "missile";
                    } else if (equipmentType.hasFlag(WeaponType.F_BALLISTIC)) {
                        imgBase = "ballistic";
                    } else if (equipmentType.hasFlag(WeaponType.F_ARTILLERY)) {
                        imgBase = "artillery";
                    }
                }

                break;
            case Part.REPAIR_PART_TYPE.MEK_LOCATION:
            case Part.REPAIR_PART_TYPE.POD_SPACE:
                imgBase = "location_mek";
                break;
            case Part.REPAIR_PART_TYPE.PHYSICAL_WEAPON:
                imgBase = "melee";
                break;
        }

        if (null == imgBase) {
            imgBase = "equipment";
        }


        String[] imgData = new String[2];
        imgData[0] = "data/images/misc/repair/";
        imgData[1] = imgBase;

        return imgData;
    }

    public abstract ITechnology getTechAdvancement();

    @Override
    public boolean isClan() {
        return getTechAdvancement().isClan();
    }

    @Override
    public boolean isMixedTech() {
        return false;
    }

    @Override
    public int getTechBase() {
        return getTechAdvancement().getTechBase();
    }

    @Override
    public int getTechRating() {
        return getTechAdvancement().getTechRating();
    }

    @Override
    public int getIntroductionDate() {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getIntroductionDate(), TA_POD.getIntroductionDate());
        }
        return getTechAdvancement().getIntroductionDate();
    }

    @Override
    public int getIntroductionDate(boolean clan) {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getIntroductionDate(clan), TA_POD.getIntroductionDate(clan));
        }
        return getTechAdvancement().getIntroductionDate(clan);
    }

    @Override
    public int getPrototypeDate() {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getPrototypeDate(), TA_POD.getPrototypeDate());
        }
        return getTechAdvancement().getPrototypeDate();
    }

    @Override
    public int getPrototypeDate(boolean clan) {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getPrototypeDate(clan), TA_POD.getPrototypeDate(clan));
        }
        return getTechAdvancement().getPrototypeDate(clan);
    }

    @Override
    public int getProductionDate() {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getProductionDate(), TA_POD.getProductionDate());
        }
        return getTechAdvancement().getProductionDate();
    }

    @Override
    public int getProductionDate(boolean clan) {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getProductionDate(clan), TA_POD.getProductionDate(clan));
        }
        return getTechAdvancement().getProductionDate(clan);
    }

    @Override
    public int getCommonDate() {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getCommonDate(), TA_POD.getCommonDate());
        }
        return getTechAdvancement().getCommonDate();
    }

    @Override
    public int getCommonDate(boolean clan) {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getCommonDate(clan), TA_POD.getCommonDate(clan));
        }
        return getTechAdvancement().getCommonDate(clan);
    }

    @Override
    public int getExtinctionDate() {
        return getTechAdvancement().getExtinctionDate();
    }

    @Override
    public int getExtinctionDate(boolean clan) {
        return getTechAdvancement().getExtinctionDate(clan);
    }

    @Override
    public int getReintroductionDate() {
        return getTechAdvancement().getReintroductionDate();
    }

    @Override
    public int getReintroductionDate(boolean clan) {
        return getTechAdvancement().getReintroductionDate(clan);
    }

    @Override
    public int getBaseAvailability(int era) {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getBaseAvailability(era), TA_POD.getBaseAvailability(era));
        }
        return getTechAdvancement().getBaseAvailability(era);
    }

    public int getAvailability() {
        return calcYearAvailability(campaign.getGameYear(),
                campaign.useClanTechBase(),
                campaign.getTechFaction());
    }

    @Override
    public int calcYearAvailability(int year, boolean clan) {
        int av = getTechAdvancement().calcYearAvailability(campaign.getGameYear(),
                campaign.getFaction().isClan());
        if (omniPodded) {
            av = Math.max(av, TA_POD.calcYearAvailability(campaign.getGameYear(),
                campaign.getFaction().isClan()));
        }
        return av;
    }

    @Override
    public int getIntroductionDate(boolean clan, int faction) {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getIntroductionDate(clan, faction),
                    TA_POD.getIntroductionDate(clan, faction));
        }
        return getTechAdvancement().getIntroductionDate(clan, faction);
    }

    @Override
    public int getPrototypeDate(boolean clan, int faction) {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getPrototypeDate(clan, faction),
                    TA_POD.getPrototypeDate(clan, faction));
        }
        return getTechAdvancement().getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate(boolean clan, int faction) {
        if (omniPodded) {
            return Math.max(getTechAdvancement().getProductionDate(clan, faction),
                    TA_POD.getProductionDate(clan, faction));
        }
        return getTechAdvancement().getProductionDate(clan, faction);
    }

    @Override
    public int getExtinctionDate(boolean clan, int faction) {
        return getTechAdvancement().getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate(boolean clan, int faction) {
        return getTechAdvancement().getReintroductionDate(clan, faction);
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        if (omniPodded) {
            return SimpleTechLevel.max(getTechAdvancement().getStaticTechLevel(),
                    SimpleTechLevel.STANDARD);
        }
        return getTechAdvancement().getStaticTechLevel();
    }

    public void fixReferences(Campaign campaign) {
        if (replacementPart instanceof PartRef) {
            int id = replacementPart.getId();
            replacementPart = campaign.getWarehouse().getPart(id);
            if ((replacementPart == null) && (id > 0)) {
                MekHQ.getLogger().error(
                    String.format("Part %d ('%s') references missing replacement part %d",
                        getId(), getName(), id));
            }
        }

        if (parentPart instanceof PartRef) {
            int id = parentPart.getId();
            parentPart = campaign.getWarehouse().getPart(id);
            if ((parentPart == null) && (id > 0)) {
                MekHQ.getLogger().error(
                    String.format("Part %d ('%s') references missing parent part %d",
                        getId(), getName(), id));
            }
        }

        for (int ii = childParts.size() - 1; ii >= 0; --ii) {
            Part childPart = childParts.get(ii);
            if (childPart instanceof PartRef) {
                Part realPart = campaign.getWarehouse().getPart(childPart.getId());
                if (realPart != null) {
                    childParts.set(ii, realPart);
                } else if (childPart.getId() > 0) {
                    MekHQ.getLogger().error(
                        String.format("Part %d ('%s') references missing child part %d",
                            getId(), getName(), childPart.getId()));
                    childParts.remove(ii);
                }
            }
        }

        if (tech instanceof PartPersonRef) {
            UUID id = tech.getId();
            tech = campaign.getPerson(id);
            if (tech == null) {
                MekHQ.getLogger().error(
                    String.format("Part %d ('%s') references missing tech %s",
                        getId(), getName(), id));
            }
        }
        if (reservedBy instanceof PartPersonRef) {
            UUID id = reservedBy.getId();
            reservedBy = campaign.getPerson(id);
            if (reservedBy == null) {
                MekHQ.getLogger().error(
                    String.format("Part %d ('%s') references missing tech (reservation) %s",
                        getId(), getName(), id));
            }
        }

        if (unit instanceof PartUnitRef) {
            UUID id = unit.getId();
            unit = campaign.getUnit(id);
            if (unit == null) {
                MekHQ.getLogger().error(
                    String.format("Part %d ('%s') references missing unit %s",
                        getId(), getName(), id));
            }
        }

        if (refitUnit instanceof PartUnitRef) {
            UUID id = refitUnit.getId();
            refitUnit = campaign.getUnit(id);
            if (refitUnit == null) {
                MekHQ.getLogger().error(
                    String.format("Part %d ('%s') references missing refit unit %s",
                        getId(), getName(), id));
            }
        }
    }

    public static class PartRef extends Part {
        private static final long serialVersionUID = 1L;

        public PartRef(int id) {
            this.id = id;
        }

        @Override
        public int getBaseTime() {
            return 0;
        }

        @Override
        public void updateConditionFromEntity(boolean checkForDestruction) {
        }

        @Override
        public void updateConditionFromPart() {
        }

        @Override
        public void remove(boolean salvage) {
        }

        @Override
        public MissingPart getMissingPart() {
            return null;
        }

        @Override
        public int getLocation() {
            return 0;
        }

        @Override
        public String checkFixable() {
            return null;
        }

        @Override
        public boolean needsFixing() {
            return false;
        }

        @Override
        public int getDifficulty() {
            return 0;
        }

        @Override
        public Money getStickerPrice() {
            return null;
        }

        @Override
        public double getTonnage() {
            return 0;
        }

        @Override
        public boolean isSamePartType(Part part) {
            return false;
        }

        @Override
        public void writeToXml(PrintWriter pw1, int indent) {
        }

        @Override
        protected void loadFieldsFromXmlNode(Node wn) {
        }

        @Override
        public Part clone() {
            return null;
        }

        @Override
        public String getLocationName() {
            return null;
        }

        @Override
        public ITechnology getTechAdvancement() {
            return null;
        }
    }

    public static class PartPersonRef extends Person {

        private static final long serialVersionUID = 1L;

        private PartPersonRef(UUID id) {
            super(id);
        }
    }

    public static class PartUnitRef extends Unit {

        private PartUnitRef(UUID id) {
            setId(id);
        }
    }
}
