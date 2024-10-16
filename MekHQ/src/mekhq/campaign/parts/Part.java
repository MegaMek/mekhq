/*
 * Part.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.Version;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTime;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.*;

/**
 * Parts do the lions share of the work of repairing, salvaging, reloading,
 * refueling, etc.
 * for units. Each unit has an ArrayList of all its relevant parts. There is a
 * corresponding unit
 * variable in part but this can be null when we are dealing with a spare part,
 * so when putting in
 * calls to unit, you should always check to make sure it is not null.
 *
 * There are two kinds of parts: Part and MissingPart. The latter is used as a
 * placeholder on a unit to
 * indicate it is missing the given part. When parts are removed from a unit,
 * they should be replaced
 * with the appropriate missing part which will remind MHQ that a replacement
 * needs to be done.
 *
 * Parts implement IPartWork and MissingParts also implement IAcquisitionWork.
 * These interfaces allow for
 * most of the actual work that can be done on parts. There is a lot of
 * variability in how parts actually handle
 * this work
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public abstract class Part implements IPartWork, ITechnology {
    private static final MMLogger logger = MMLogger.create(Part.class);

    public static final int T_UNKNOWN = -1;
    public static final int T_BOTH = 0;
    public static final int T_IS = 1;
    public static final int T_CLAN = 2;

    protected static final TechAdvancement TA_POD = Entity.getOmniAdvancement();
    // Generic TechAdvancement for a number of basic components.
    protected static final TechAdvancement TA_GENERIC = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
            .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    protected String name;
    protected int id;

    // this is the unitTonnage which needs to be tracked for some parts
    // even when off the unit. actual tonnage is returned via the
    // getTonnage() method
    protected int unitTonnage;

    protected boolean omniPodded;

    // hits to this part
    protected int hits;

    // time spent on the task so far for tasks that span days
    protected int timeSpent;
    // the minimum skill level in order to attempt
    protected int skillMin;
    // current repair mode for part
    protected WorkTime mode;

    protected Person tech;
    private boolean isTeamSalvaging;

    // null is valid. It indicates parts that are not attached to units.
    protected Unit unit;

    protected PartQuality quality;

    protected boolean brandNew;

    // we need to keep track of a couple of potential mods that result from carrying
    // over a task, otherwise people can get away with working over time with no
    // consequence
    protected boolean workingOvertime;
    protected int shorthandedMod;

    /** This tracks the unit which reserved the part for a refit */
    private Unit refitUnit;
    /**
     * The unique identifier of the tech who is reserving this part for overnight
     * work
     */
    private Person reservedBy;
    // temporarily mark the part used by current refit planning
    protected transient boolean usedForRefitPlanning;

    // for delivery
    protected int daysToArrival;

    // all parts need a reference to campaign
    protected Campaign campaign;

    /*
     * This will be unusual but in some circumstances certain parts will be linked
     * to other parts.
     * These linked parts will be considered integral and subsidary to those other
     * parts and will
     * not show up independently. Currently (8/8/2015), we are only using this for
     * BA suits
     * We need a parent part id and a vector of children parts to represent this.
     */
    protected Part parentPart;
    protected ArrayList<Part> childParts;

    /**
     * The number of parts in exactly the same condition,
     * to track multiple spare parts more efficiently and also the shopping list
     */
    protected int quantity;

    // only relevant for parts that can be acquired
    protected int daysToWait;

    /** The part which will be used as a replacement */
    private Part replacementPart;

    protected final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
            MekHQ.getMHQOptions().getLocale());

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
        this.quality = PartQuality.QUALITY_D;
        this.childParts = new ArrayList<>();
        this.isTeamSalvaging = false;
    }

    public String getQualityName() {
        return quality.toName(campaign.getCampaignOptions().isReverseQualityNames());
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
     * This is the value of the part that may be affected by characteristics and
     * campaign options
     *
     * @return the part's actual value
     */
    @Override
    public Money getActualValue() {
        return adjustCostsForCampaignOptions(getStickerPrice());
    }

    @Override
    public boolean isPriceAdjustedForAmount() {
        return false;
    }

    /**
     * Adjusts the cost of a part based on one's campaign options
     *
     * @param cost the part's base cost
     * @return the part's cost adjusted for campaign options
     */
    public Money adjustCostsForCampaignOptions(@Nullable Money cost) {
        // if the part doesn't cost anything, no amount of multiplication will change it
        if ((cost == null) || cost.isZero()) {
            return Money.zero();
        }

        switch (getTechBase()) {
            case T_IS:
                cost = cost.multipliedBy(campaign.getCampaignOptions().getInnerSphereUnitPriceMultiplier());
                break;
            case T_CLAN:
                cost = cost.multipliedBy(campaign.getCampaignOptions().getClanUnitPriceMultiplier());
                break;
            case T_BOTH:
            default:
                cost = cost.multipliedBy(
                        campaign
                                .getCampaignOptions()
                                .getCommonPartPriceMultiplier());
                break;
        }

        if (!isBrandNew()) {
            cost = cost.multipliedBy(campaign.getCampaignOptions()
                    .getUsedPartPriceMultipliers()[getQuality().toNumeric()]);
        }

        if (needsFixing() && !isPriceAdjustedForAmount()) {
            cost = cost.multipliedBy((getSkillMin() > SkillType.EXP_ELITE)
                    ? campaign.getCampaignOptions().getUnrepairablePartsValueMultiplier()
                    : campaign.getCampaignOptions().getDamagedPartsValueMultiplier());
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

    @Override
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
            // toReturn = "" + getDaysToArrival() + " days to arrival";
            String dayName = "day";
            if (getDaysToArrival() > 1) {
                dayName += "s";
            }
            toReturn = "In transit (" + getDaysToArrival() + ' ' + dayName + ')';
        }
        return toReturn;
    }

    /**
     * Gets the number of hits on the part.
     */
    public int getHits() {
        return hits;
    }

    /**
     * Sets the number of hits on the part.
     *
     * NOTE: It is the caller's responsibility to update the condition
     * of the part and any attached unit.
     *
     * @param hits The number of hits on the part.
     */
    public void setHits(int hits) {
        this.hits = Math.max(hits, 0);
    }

    @Override
    public String getDesc() {
        String bonus = getAllMods(null).getValueAsString();
        if (getAllMods(null).getValue() > -1) {
            bonus = '+' + bonus;
        }
        String toReturn = "<html><font";
        String action = "Repair ";
        if (isSalvaging()) {
            action = "Salvage ";
        }
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>" + action + getName();

        if (!getCampaign().getCampaignOptions().isDestroyByMargin()) {
            toReturn += " - <b><span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>"
                    + SkillType.getExperienceLevelName(getSkillMin()) + '+'
                    + "</span></b></b><br/>";
        } else {
            toReturn += "</b><br/>";
        }

        toReturn += getDetails() + "<br/>";
        if (getSkillMin() > SkillType.EXP_ELITE) {
            toReturn += "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>Impossible</font>";
        } else {
            toReturn += getTimeLeft() + " minutes" + scheduled;
            toReturn += " <b>TN:</b> " + bonus;
            if (getMode() != WorkTime.NORMAL) {
                toReturn += " <i>" + getCurrentModeName() + "</i>";
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
                bonus = '+' + bonus;
            }

            toReturn += getTimeLeft() + " minutes" + scheduled;

            if (!getCampaign().getCampaignOptions().isDestroyByMargin()) {
                toReturn += ", <span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>"
                        + SkillType.getExperienceLevelName(getSkillMin()) + '+'
                        + ReportingUtilities.CLOSING_SPAN_TAG;
            }

            toReturn += ", TN: " + bonus;
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
     * We are going to only limit parts by year if they totally haven't been
     * produced
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
     *             The part to be compared with the current part
     */
    public boolean isSamePartTypeAndStatus(Part part) {
        return isSamePartType(part) && isSameStatus(part);
    }

    public abstract boolean isSamePartType(Part part);

    public boolean isSameStatus(Part otherPart) {
        // parts that are reserved for refit or being worked on are never the same
        // status
        if (isReservedForRefit() || isBeingWorkedOn() || isReservedForReplacement() || hasParentPart()
                || otherPart.isReservedForRefit() || otherPart.isBeingWorkedOn() 
                || otherPart.isReservedForReplacement() || otherPart.hasParentPart()) {
            return false;
        }
        return getQuality() == otherPart.getQuality()
                && getHits() == otherPart.getHits()
                && getSkillMin() == otherPart.getSkillMin() 
                && getDaysToArrival() == otherPart.getDaysToArrival();
    }

    protected boolean isClanTechBase() {
        return getTechBase() == TECH_BASE_CLAN;
    }

    public abstract void writeToXML(final PrintWriter pw, int indent);

    protected int writeToXMLBegin(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "part", "id", id, "type", getClass());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", id);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        if (omniPodded) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "omniPodded", true);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitTonnage", unitTonnage);
        if (hits > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hits", hits);
        }

        if (timeSpent > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "timeSpent", timeSpent);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mode", mode.name());
        if (tech != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techId", tech.getId());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "skillMin", skillMin);
        if (unit != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitId", unit.getId());
        }

        if (workingOvertime) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "workingOvertime", true);
        }

        if (shorthandedMod != 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shorthandedMod", shorthandedMod);
        }

        if (refitUnit != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "refitId", refitUnit.getId());
        }

        if (daysToArrival > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysToArrival", daysToArrival);
        }

        if (!brandNew) {
            // The default value for Part.brandNew is true. Only store the tag if the value
            // is false.
            // The lack of tag in the save file will ALWAYS result in TRUE.
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "brandNew", false);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "quantity", quantity);

        if (daysToWait > 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysToWait", daysToWait);
        }

        if (replacementPart != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "replacementId", replacementPart.getId());
        }

        if (reservedBy != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reserveId", reservedBy.getId());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "quality", quality.toNumeric());
        if (isTeamSalvaging) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "isTeamSalvaging", true);
        }

        if (parentPart != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "parentPartId", parentPart.getId());
        }

        for (final Part childPart : childParts) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "childPartId", childPart.getId());
        }

        return indent;
    }

    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "part");
    }

    public static Part generateInstanceFromXML(Node wn, Version version) {
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();

        // <50.01 compatibility handlers
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
        } else if (className.equalsIgnoreCase("mekhq.campaign.parts.VeeStabiliser")) {
            className = "mekhq.campaign.parts.VeeStabilizer";
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
                    retVal.omniPodded = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("quantity")) {
                    retVal.quantity = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
                    retVal.hits = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("timeSpent")) {
                    retVal.timeSpent = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("skillMin")) {
                    retVal.skillMin = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("mode")) {
                    retVal.mode = WorkTime.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                    retVal.daysToWait = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("techId")) {
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
                } else if (wn2.getNodeName().equalsIgnoreCase("replacementId")) {
                    retVal.replacementPart = new PartRef(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("quality")) {
                    retVal.quality = PartQuality.fromNumeric(Integer.parseInt(wn2.getTextContent()));
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
            logger.error("", ex);
        }

        return retVal;
    }

    protected abstract void loadFieldsFromXmlNode(Node wn);

    @Override
    public int getActualTime() {
        double time = getBaseTime() * mode.timeMultiplier;
        if ((getUnit() != null) && (getUnit().hasPrototypeTSM())) {
            time *= 2;
        }
        return (int) Math.ceil(time);
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

    @Override
    public void addTimeSpent(int m) {
        this.timeSpent += m;
    }

    @Override
    public void resetTimeSpent() {
        this.timeSpent = 0;
    }

    @Override
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

    @Override
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
    public TargetRoll getAllMods(final @Nullable Person tech) {
        int difficulty = getDifficulty();

        if (isOmniPodded() && (isSalvaging() || (this instanceof MissingPart))
                && (getUnit() != null) && !(getUnit().getEntity() instanceof Tank)) {
            difficulty -= 2;
        }

        if (getMode() == null) {
            resetModeToNormal();
        }

        final TargetRoll mods = new TargetRoll(difficulty, "difficulty");
        final int modeMod = getMode().getMod(getCampaign().getCampaignOptions().isDestroyByMargin());
        if (modeMod != 0) {
            mods.addModifier(modeMod, getCurrentModeName());
        }

        if (getUnit() != null) {
            mods.append(getUnit().getSiteMod());
            if (getUnit().getEntity().hasQuirk(OptionsConstants.QUIRK_POS_EASY_MAINTAIN)) {
                mods.addModifier(-1, "easy to maintain");
            } else if (getUnit().getEntity().hasQuirk(OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN)) {
                mods.addModifier(1, "difficult to maintain");
            }

            if (getUnit().hasPrototypeTSM() &&
                    ((this instanceof MekLocation)
                            || (this instanceof MissingMekLocation)
                            || (this instanceof MekActuator)
                            || (this instanceof MissingMekActuator))) {
                mods.addModifier(2, "prototype TSM");
            }
        }

        if (tech != null) {
            if (tech.getOptions().booleanOption(PersonnelOptions.TECH_WEAPON_SPECIALIST)
                    && ((IPartWork.findCorrectRepairType(this) == PartRepairType.WEAPON)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.PHYSICAL_WEAPON))) {
                mods.addModifier(-1, "Weapon specialist");
            }

            if (tech.getOptions().booleanOption(PersonnelOptions.TECH_ARMOR_SPECIALIST)
                    && IPartWork.findCorrectRepairType(this).isArmour()) {
                mods.addModifier(-1, "Armor specialist");
            }

            if (tech.getOptions().booleanOption(PersonnelOptions.TECH_INTERNAL_SPECIALIST)
                    && ((IPartWork.findCorrectRepairType(this) == PartRepairType.ACTUATOR)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.ELECTRONICS)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.ENGINE)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.GYRO)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.MEK_LOCATION)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.GENERAL_LOCATION))) {
                mods.addModifier(-1, "Internal specialist");
            }

            if (tech.getOptions().booleanOption(PersonnelOptions.TECH_MAINTAINER)) {
                mods.addModifier(1, "Maintainer");
            }
        }

        return getQualityMods(mods, tech);
    }

    @Override
    public TargetRoll getAllModsForMaintenance() {
        // according to StratOps you get a -1 mod when checking on individual parts
        // but we will make this user customizable
        final TargetRoll mods = new TargetRoll(campaign.getCampaignOptions().getMaintenanceBonus(), "maintenance");
        mods.addModifier(Availability.getTechModifier(getTechRating()),
                "tech rating " + ITechnology.getRatingName(getTechRating()));

        if (getUnit() == null) {
            return mods;
        }

        mods.append(getUnit().getSiteMod());
        if (getUnit().getEntity().hasQuirk(OptionsConstants.QUIRK_POS_EASY_MAINTAIN)) {
            mods.addModifier(-1, "easy to maintain");
        } else if (getUnit().getEntity().hasQuirk(OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN)) {
            mods.addModifier(1, "difficult to maintain");
        }

        if (getUnit().getTech() != null) {
            if (getUnit().getTech().getOptions().booleanOption(PersonnelOptions.TECH_WEAPON_SPECIALIST)
                    && ((IPartWork.findCorrectRepairType(this) == PartRepairType.WEAPON)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.PHYSICAL_WEAPON))) {
                mods.addModifier(-1, "Weapon specialist");
            }

            if (getUnit().getTech().getOptions().booleanOption(PersonnelOptions.TECH_ARMOR_SPECIALIST)
                    && IPartWork.findCorrectRepairType(this).isArmour()) {
                mods.addModifier(-1, "Armor specialist");
            }

            if (getUnit().getTech().getOptions().booleanOption(PersonnelOptions.TECH_INTERNAL_SPECIALIST)
                    && ((IPartWork.findCorrectRepairType(this) == PartRepairType.ACTUATOR)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.ELECTRONICS)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.ENGINE)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.GYRO)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.MEK_LOCATION)
                            || (IPartWork.findCorrectMRMSType(this) == PartRepairType.GENERAL_LOCATION))) {
                mods.addModifier(-1, "Internal specialist");
            }

            if (getUnit().getTech().getOptions().booleanOption(PersonnelOptions.TECH_MAINTAINER)) {
                mods.addModifier(-1, "Maintainer");
            }
        }

        if (getUnit().hasPrototypeTSM()) {
            mods.addModifier(1, "prototype TSM");
        }

        return getCampaign().getCampaignOptions().isUseQualityMaintenance()
                ? getQualityMods(mods, getUnit().getTech())
                : mods;
    }

    /**
     * adds the quality modifiers for repair and maintenance of this part to a
     * TargetRoll
     *
     * @param mods - the {@link TargetRoll} that quality modifiers should be added
     *             to
     * @param tech - the {@link Person} that will make the repair or maintenance
     *             check, may be null
     * @return the modified {@link TargetRoll}
     */
    private TargetRoll getQualityMods(TargetRoll mods, Person tech) {
        mods.addModifier(getQuality().getRepairModifier(), getQualityName());
        if ((getQuality().getRepairModifier() > 0)
                && (null != tech) && tech.getOptions().booleanOption(PersonnelOptions.TECH_FIXER)) {
            // fixers can ignore the first point of penalty for poor quality
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
        // keep track of whether this was a salvage operation
        // because the entity may change
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
     *
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
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.GENERAL;
    }

    @Override
    public PartRepairType getRepairPartType() {
        return getMRMSOptionType();
    }

    @Override
    public void fix() {
        setHits(0);
        resetRepairSettings();
    }

    /**
     * Sets minimum skill, shorthanded mod, and rush job/extra time setting to
     * defaults.
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
        return " <font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'><b> failed.</b></font>";
    }

    @Override
    public String succeed() {
        if (isSalvaging()) {
            remove(true);
            return " <font color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor()
                    + "'><b> salvaged.</b></font>";
        } else {
            fix();
            return " <font color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'><b> fixed.</b></font>";
        }
    }

    /**
     * Gets a string containing details regarding the part,
     * e.g. OmniPod or how many hits it has taken and its
     * repair cost.
     *
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
     *
     * @param includeRepairDetails {@code true} if the details
     *                             should include information such as the number of
     *                             hits or how much it would cost to repair the
     *                             part.
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
            if (campaign.getCampaignOptions().isPayForRepairs() && (hits > 0)) {
                sj.add(getActualValue().multipliedBy(0.2).toAmountAndSymbolString() + " to repair");
            }
        }
        return sj.toString();
    }

    /**
     * Converts the array of strings normally returned by a call to
     * campaign.getInventory()
     * to a string that reads like "(x in transit, y on order)"
     *
     * @param inventories The inventory array, see campaign.getInventory() for
     *                    details.
     * @return Human readable string.
     */
    public String getOrderTransitStringForDetails(PartInventory inventories) {
        String inTransitString = (inventories.getTransit() == 0) ? "" : inventories.transitAsString() + " in transit";
        String onOrderString = (inventories.getOrdered() == 0) ? "" : inventories.orderedAsString() + " on order";
        String transitOrderSeparator = !inTransitString.isBlank() && !onOrderString.isBlank() ? ", " : "";

        return (!inTransitString.isBlank() || !onOrderString.isBlank())
                ? String.format("(%s%s%s)", inTransitString, transitOrderSeparator, onOrderString)
                : "";
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
     *
     * @param unit The unit reserving this part for a refit.
     */
    public void setRefitUnit(@Nullable Unit unit) {
        refitUnit = unit;
    }

    /**
     * Gets the unit which reserved this part for a refit.
     *
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
     *
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

    @Override
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

    public int getSellableQuantity() {
        return getQuantity();
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
     *
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

    @Override
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
        String answer = String.valueOf(quantity) + ' ' + getName();
        if (quantity > 1) {
            answer += "s";
        }
        return answer;
    }

    /**
     * Get the acquisition work to acquire a new part of this type
     * For most parts this is just getMissingPart(), but some override it
     *
     * @return
     */
    public IAcquisitionWork getAcquisitionWork() {
        return getMissingPart();
    }

    public void doMaintenanceDamage(int d) {
        setHits(getHits() + d);
        updateConditionFromPart();
        updateConditionFromEntity(false);
    }

    public PartQuality getQuality() {
        return quality;
    }

    public void improveQuality() {
        quality = quality.improveQuality();
    }

    public void reduceQuality() {
        quality = quality.reduceQuality();
    }

    public void setQuality(PartQuality q) {
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
     *
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
     *
     * @param childPart The part to add as a child.
     */
    public void addChildPart(Part childPart) {
        childParts.add(Objects.requireNonNull(childPart));
        childPart.setParentPart(this);
    }

    /**
     * Removes a child part from this part.
     *
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
        // nothing goes here for real parts. Only missing parts need to reserve a
        // replacement
    }

    @Override
    public void cancelReservation() {
        // nothing goes here for real parts. Only missing parts need to reserve a
        // replacement
    }

    /**
     * Make any changes to the part needed for adding to the campaign
     */
    public void postProcessCampaignAddition() {
        // do nothing
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
        sb.append(' ');
        sb.append(getDetails());
        sb.append(", q: ");
        sb.append(quantity);
        if (null != unit) {
            sb.append(", mounted: ");
            sb.append(unit);
        }
        return sb.toString();
    }

    public static String[] findPartImage(IPartWork part) {
        String imgBase = null;
        PartRepairType repairType = IPartWork.findCorrectRepairType(part);

        switch (repairType) {
            case ARMOUR:
                imgBase = "armor";
                break;
            case AMMUNITION:
                imgBase = "ammo";
                break;
            case ACTUATOR:
                imgBase = "actuator";
                break;
            case ENGINE:
                imgBase = "engine";
                break;
            case ELECTRONICS:
                imgBase = "electronics";
                break;
            case HEAT_SINK:
                imgBase = "heatsink";
                break;
            case WEAPON:
                EquipmentType equipmentType = null;

                if (part instanceof EquipmentPart) {
                    equipmentType = ((EquipmentPart) part).getType();
                } else if (part instanceof MissingEquipmentPart) {
                    equipmentType = ((MissingEquipmentPart) part).getType();
                }

                if (equipmentType != null) {
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
            case MEK_LOCATION:
            case POD_SPACE:
                imgBase = "location_mek";
                break;
            case PHYSICAL_WEAPON:
                imgBase = "melee";
                break;
            default:
                break;
        }

        if (imgBase == null) {
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
                logger.error(
                        String.format("Part %d ('%s') references missing replacement part %d",
                                getId(), getName(), id));
            }
        }

        if (parentPart instanceof PartRef) {
            int id = parentPart.getId();
            parentPart = campaign.getWarehouse().getPart(id);
            if ((parentPart == null) && (id > 0)) {
                logger.error(String.format("Part %d ('%s') references missing parent part %d",
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
                    logger.error(String.format("Part %d ('%s') references missing child part %d",
                            getId(), getName(), childPart.getId()));
                    childParts.remove(ii);
                }
            }
        }

        if (tech instanceof PartPersonRef) {
            UUID id = tech.getId();
            tech = campaign.getPerson(id);
            if (tech == null) {
                logger.error(String.format("Part %d ('%s') references missing tech %s",
                        getId(), getName(), id));
            }
        }
        if (reservedBy instanceof PartPersonRef) {
            UUID id = reservedBy.getId();
            reservedBy = campaign.getPerson(id);
            if (reservedBy == null) {
                logger.error(String.format("Part %d ('%s') references missing tech (reservation) %s",
                        getId(), getName(), id));
            }
        }

        if (unit instanceof PartUnitRef) {
            UUID id = unit.getId();
            unit = campaign.getUnit(id);
            if (unit == null) {
                logger.error(
                        String.format("Part %d ('%s') references missing unit %s",
                                getId(), getName(), id));
            }
        }

        if (refitUnit instanceof PartUnitRef) {
            UUID id = refitUnit.getId();
            refitUnit = campaign.getUnit(id);
            if (refitUnit == null) {
                logger.error(
                        String.format("Part %d ('%s') references missing refit unit %s",
                                getId(), getName(), id));
            }
        }
    }

    public static class PartRef extends Part {
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
        public @Nullable String checkFixable() {
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
        public void writeToXML(final PrintWriter pw, int indent) {

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
