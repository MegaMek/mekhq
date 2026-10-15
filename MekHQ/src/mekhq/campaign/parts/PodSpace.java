/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTimeEstimate;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An abstraction of all the pod-mounted equipment within a single location of an omni unit. Used to group them together
 * as recipients of a single tech action.
 *
 * @author Neoancient
 */
public class PodSpace implements IPartWork {
    private static final MMLogger LOGGER = MMLogger.create(PodSpace.class);

    protected Campaign campaign;
    protected Unit unit;
    protected int location;
    protected List<UUID> childPartIds = new ArrayList<>();
    private final LocationNode locationNode = new LocationNode(this);

    protected Person tech;
    protected int timeSpent = 0;
    protected boolean workingOvertime = false;
    protected int shorthandedMod = 0;

    protected boolean repairInPlace = false;

    /** The tech a saved job named, until {@link #fixReferences(Campaign)} finds them in the loaded campaign. */
    private UUID savedTechId;

    @Deprecated(since = "0.51.0", forRemoval = true)
    public PodSpace() {
        this(Entity.LOC_NONE, null);
    }

    public PodSpace(int location, Unit unit) {
        this.location = location;
        this.unit = unit;
        if (unit != null) {
            this.campaign = unit.getCampaign();
            //We don't need a LOC_WINGS Pod space, but we do need one for the fuselage equipment, which is stored at
            // LOC_NONE.
            if ((unit.getEntity() instanceof Aero) && (location == Aero.LOC_WINGS)) {
                this.location = -1;
            }
        }
    }

    /**
     * Finds one of the pod-mounted parts by its identity, wherever it is kept: with the unit, which may be at a base.
     *
     * @return the part, or {@code null} if it is no longer in the campaign
     */
    private @Nullable Part findChildPart(UUID childPartId) {
        return campaign.getCampaignLocationManager().findPartAnywhere(campaign, childPartId);
    }

    @Override
    public int getBaseTime() {
        return 30;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public List<Part> getPartList() {
        return childPartIds.stream()
                     .map(this::findChildPart)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        childPartIds.clear();
        for (Part part : getUnit().getParts()) {
            if (part.isOmniPodded() && part.getLocation() == location) {
                childPartIds.add(part.getUniqueId());
            }
        }
    }

    @Override
    public void updateConditionFromPart() {
        //nothing to do here
    }

    /**
     * @return {@code true} if this pod space holds anything a save has to keep: a tech on the job, time already spent
     *       on it, or the repair-in-place choice made for it
     */
    public boolean hasWorkInProgress() {
        return (tech != null) || (timeSpent > 0) || repairInPlace;
    }

    /**
     * Takes over the job from an earlier copy of this pod space. A unit's pod spaces are built again whenever its parts
     * are, which includes every load, so the job in progress is handed from the old copy to the new one.
     *
     * @param earlier the pod space for the same location before the rebuild, or a job read from a save
     */
    public void continueWorkFrom(PodSpace earlier) {
        tech = earlier.tech;
        timeSpent = earlier.timeSpent;
        workingOvertime = earlier.workingOvertime;
        shorthandedMod = earlier.shorthandedMod;
        repairInPlace = earlier.repairInPlace;
    }

    /**
     * Saves the job in progress on this pod space. Only called for a pod space that {@link #hasWorkInProgress()}.
     */
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "podSpace");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "location", location);
        if (tech != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techId", tech.getId());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "timeSpent", timeSpent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "workingOvertime", workingOvertime);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shorthandedMod", shorthandedMod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "repairInPlace", repairInPlace);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "podSpace");
    }

    /**
     * Reads a saved job. The result only carries the job: the unit's real pod spaces are built when its parts are
     * initialized, and take the job over with {@link #continueWorkFrom(PodSpace)}.
     *
     * @return the saved job, or {@code null} if it could not be read, in which case the unit loads without it
     */
    public static @Nullable PodSpace generateInstanceFromXML(Node podSpaceNode, Unit unit) {
        PodSpace savedJob = new PodSpace(Entity.LOC_NONE, null);
        savedJob.unit = unit;
        NodeList childNodes = podSpaceNode.getChildNodes();
        try {
            for (int index = 0; index < childNodes.getLength(); index++) {
                Node childNode = childNodes.item(index);
                String value = childNode.getTextContent().trim();
                switch (childNode.getNodeName()) {
                    case "location" -> savedJob.location = Integer.parseInt(value);
                    case "techId" -> savedJob.savedTechId = UUID.fromString(value);
                    case "timeSpent" -> savedJob.timeSpent = Integer.parseInt(value);
                    case "workingOvertime" -> savedJob.workingOvertime = Boolean.parseBoolean(value);
                    case "shorthandedMod" -> savedJob.shorthandedMod = Integer.parseInt(value);
                    case "repairInPlace" -> savedJob.repairInPlace = Boolean.parseBoolean(value);
                    default -> {
                        // whitespace between the tags
                    }
                }
            }
        } catch (IllegalArgumentException exception) {
            LOGGER.error(exception, "[PodSpace] A saved pod job on {} could not be read and was dropped",
                  unit.getName());
            return null;
        }
        return savedJob;
    }

    /**
     * Finds the tech a saved job named among the loaded personnel.
     */
    public void fixReferences(Campaign campaign) {
        if (savedTechId == null) {
            return;
        }
        tech = campaign.getPlayerForce().getHumanResources().getPerson(savedTechId);
        if (tech == null) {
            LOGGER.error("[PodSpace] The pod job at location {} on {} names missing tech {}", location,
                  unit.getName(), savedTechId);
        }
        savedTechId = null;
    }

    @Override
    public void remove(boolean salvage) {
        shorthandedMod = 0;
        //Iterate through all pod-mounted equipment in space and remove them.
        for (UUID pid : childPartIds) {
            final Part part = findChildPart(pid);
            // Don't remove missing parts! We'll need to fix them.
            if (part != null && !(part instanceof MissingPart)) {
                part.remove(salvage);
                MekHQ.triggerEvent(new PartChangedEvent(part));
            }
        }
        updateConditionFromEntity(false);
    }

    @Override
    public void fix() {
        shorthandedMod = 0;
        for (UUID pid : childPartIds) {
            final Part part = findChildPart(pid);
            if (part != null &&
                      !(part instanceof MissingPart) &&
                      !(part instanceof AmmoBin) &&
                      part.needsFixing() &&
                      !repairInPlace) {
                part.remove(true);
                MekHQ.triggerEvent(new PartChangedEvent(part));
            }
        }
        updateConditionFromEntity(false);
        for (UUID pid : childPartIds) {
            final Part part = findChildPart(pid);
            if (part instanceof MissingPart) {
                part.fix();
                MekHQ.triggerEvent(new PartChangedEvent(part));
            }
        }
        updateConditionFromEntity(false);
    }

    @Override
    public MissingPart getMissingPart() {
        return null;
    }

    @Override
    public @Nullable String checkFixable() {
        if ((isSalvaging() && !childPartIds.isEmpty()) || location < 0) {
            for (UUID partId : childPartIds) {
                // If all remaining parts are already missing, we don't need to keep salvaging
                if (!(findChildPart(partId) instanceof MissingPart)) {
                    return null;
                }
            }
        }
        // The part is only fixable if the location is not destroyed.
        // be sure to check location and second location
        if (null != unit) {
            if (unit.isLocationBreached(location)) {
                return unit.getEntity().getLocationName(location) + " is breached.";
            }
            if (unit.isLocationDestroyed(location)) {
                return unit.getEntity().getLocationName(location) + " is destroyed.";
            }
            if (repairInPlace) {
                boolean hasMissingPart = false;
                for (UUID id : childPartIds) {
                    final Part p = findChildPart(id);
                    if (p instanceof MissingPart missing) {
                        hasMissingPart = true;
                        // Only fixable if a replacement is actually in stock. This check prevents an infinite loop
                        // in MRMS
                        if (missing.isReplacementAvailable()) {
                            return null;
                        }
                    }
                }
                if (hasMissingPart) {
                    return "There are no replacement parts available for " +
                                 unit.getEntity().getLocationName(location) +
                                 '.';
                }
                return unit.getEntity().getLocationName(location) + " is not missing any pod-mounted equipment.";
            } else {
                for (UUID id : childPartIds) {
                    final Part p = findChildPart(id);
                    if (p == null || !p.needsFixing()) {
                        continue;
                    }
                    MissingPart missing;
                    if (p instanceof MissingPart) {
                        missing = (MissingPart) p;
                    } else {
                        missing = p.getMissingPart();
                    }
                    if (missing.isReplacementAvailable()) {
                        return null;
                    }
                }
                return "There are no replacement parts available for " +
                             unit.getEntity().getLocationName(location) +
                             '.';
            }
        }
        return null;
    }

    @Override
    public boolean needsFixing() {
        return childPartIds.stream()
                     .map(this::findChildPart)
                     .filter(Objects::nonNull)
                     .anyMatch(p -> !(p instanceof AmmoBin) && p.needsFixing());
    }

    @Override
    public int getDifficulty() {
        if (unit.getEntity() instanceof Tank) {
            return 0;
        }
        return -2;
    }

    public String getLocationName() {
        if (getUnit() != null) {
            if (getUnit().getEntity() instanceof Aero && location == Entity.LOC_NONE) {
                return "Fuselage";
            } else {
                return getUnit().getEntity().getLocationName(location);
            }
        }
        return null;
    }

    @Override
    public int getLocation() {
        return location;
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
        if (null != unit) {
            mods.append(unit.getSiteMod());
            if (unit.getEntity().hasQuirk("easy_maintain")) {
                mods.addModifier(-1, "easy to maintain");
            } else if (unit.getEntity().hasQuirk("difficult_maintain")) {
                mods.addModifier(1, "difficult to maintain");
            }
        }
        return mods;
    }

    @Override
    public String succeed() {
        if (isSalvaging()) {
            remove(true);
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getPositiveColor(), "<b> removed</b>") + ".";
        } else {
            fix();
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getPositiveColor(), "<b> fixed</b>") + ".";
        }
    }

    @Override
    public String fail(int rating) {
        timeSpent = 0;
        shorthandedMod = 0;
        boolean replacing = false;
        for (UUID id : childPartIds) {
            final Part part = findChildPart(id);
            if (part != null && (isSalvaging() || (!(part instanceof AmmoBin) && part.needsFixing()))) {
                part.fail(rating);
                replacing |= part instanceof MissingPart;
            }
        }
        if (rating >= SkillType.EXP_LEGENDARY && replacing) {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getNegativeColor(),
                  "<b> failed and part(s) destroyed</b>") + ".";
        } else {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getNegativeColor(), "<b> failed</b>") + ".";
        }
    }

    @Override
    public Person getTech() {
        return tech;
    }

    @Override
    public @Nonnull LocationNode getLocationNode() {
        // Pod space is part of its unit, so it is wherever the unit is
        return (unit != null) ? unit.getLocationNode() : locationNode;
    }

    /**
     * Pod work draws on the stock kept with the unit, which is a base's for a unit at a base.
     */
    @Override
    public LocalWarehouse getWarehouse() {
        LocalWarehouse unitWarehouse = (unit == null) ? null : unit.getWarehouse();
        return (unitWarehouse != null) ? unitWarehouse : campaign.getPlayerForce().getWarehouse();
    }

    @Override
    public boolean isBeingWorkedOn() {
        return getTech() != null;
    }

    /**
     * When the job ends, finished or abandoned, the repair-in-place choice made for it goes too, so the next job on
     * this pod space starts from the usual swap. A job put on hold keeps it.
     */
    @Override
    public void cancelAssignment(boolean resetTime) {
        IPartWork.super.cancelAssignment(resetTime);
        if (resetTime) {
            repairInPlace = false;
        }
    }

    @Override
    public String getPartName() {
        return getLocationName() + " Pod Space";
    }

    @Override
    public int getSkillMin() {
        int minSkill = SkillType.EXP_GREEN;
        for (UUID id : childPartIds) {
            final Part part = findChildPart(id);
            if (part != null) {
                if ((isSalvaging() && !(part instanceof MissingPart)) ||
                          (!isSalvaging() && (part instanceof MissingPart) ||
                                 (!(part instanceof AmmoBin) && part.needsFixing()))) {
                    minSkill = Math.max(minSkill, part.getSkillMin());
                }
            }
        }
        return minSkill;
    }

    @Override
    public int getActualTime() {
        return getBaseTime();
    }

    @Override
    public int getTimeSpent() {
        return timeSpent;
    }

    @Override
    public int getTimeLeft() {
        return getActualTime() - getTimeSpent();
    }

    @Override
    public void addTimeSpent(int time) {
        timeSpent += time;
    }

    @Override
    public void resetTimeSpent() {
        timeSpent = 0;
    }

    @Override
    public void resetOvertime() {
        workingOvertime = false;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECHANICAL);
    }

    @Override
    public TargetRoll getAllModsForMaintenance() {
        return null;
    }

    @Override
    public void setTech(Person tech) {
        this.tech = tech;
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
    public String getDesc() {
        StringBuilder toReturn = new StringBuilder();
        toReturn.append("<html><b>")
              .append(isSalvaging() ? "Salvage  " : "Replace ")
              .append(getPartName())
              .append(" Equipment - ")
              .append(ReportingUtilities.messageSurroundedBySpanWithColor(SkillType.getExperienceLevelColor(getSkillMin()),
                    SkillType.getExperienceLevelName(getSkillMin()) + "+"))
              .append("</b><br/>")
              .append(getDetails())
              .append("<br/>");

        if (getSkillMin() <= SkillType.EXP_LEGENDARY) {
            toReturn.append(WorkTimeEstimate.describeTimeLeft(campaign, this))
                  .append(' ')
                  .append(Part.formatTargetNumberText(getAllMods(null)));
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
        int allParts = 0;
        int replacements = 0;
        int inTransit = 0;
        int onOrder = 0;
        for (UUID id : childPartIds) {
            Part part = findChildPart(id);
            if (part != null) {
                if (!isSalvaging() && !(part instanceof AmmoBin) && part.needsFixing()) {
                    allParts++;
                    MissingPart missing;
                    if (part instanceof MissingPart) {
                        missing = (MissingPart) part;
                    } else {
                        missing = part.getMissingPart();
                    }
                    if (missing.isReplacementAvailable()) {
                        replacements++;
                    } else {
                        //FIXME: This won't work if there are multiple items of the same type that need replacing and the number on order or in transit is less than the required number
                        PartInventory inventories = missing.getPartInventory(missing.getNewPart());
                        if (inventories.getTransit() > 0) {
                            inTransit++;
                        }
                        if (inventories.getOrdered() > 0) {
                            onOrder++;
                        }
                    }
                } else if (isSalvaging() && !(part instanceof MissingPart)) {
                    allParts++;
                }
                //TODO: add string for reconfiguring
            }
        }
        if (isSalvaging()) {
            return allParts + " parts remaining";
        } else {
            return replacements +
                         "/" +
                         allParts +
                         " available<br />" +
                         inTransit +
                         " in transit, " +
                         onOrder +
                         " on order";
        }
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public boolean isSalvaging() {
        if (unit != null) {
            return unit.isSalvage() || unit.isLocationDestroyed(location);
        }
        return false;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean shouldRepairInPlace() {
        return isRepairInPlace();
    }

    /**
     * @return {@code true} if damaged pod equipment is repaired where it is, {@code false} if it is swapped for a spare
     */
    public boolean isRepairInPlace() {
        return repairInPlace;
    }

    public void setRepairInPlace(boolean repairInPlace) {
        this.repairInPlace = repairInPlace;
    }

    public boolean hasSalvageableParts() {
        for (UUID id : childPartIds) {
            final Part p = findChildPart(id);
            if (p != null && p.isSalvaging()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Holds a spare for each piece of missing pod equipment, in the name of the tech on this job, so the spare is still
     * there when the job carries over to the next day.
     */
    @Override
    public void reservePart() {
        if (tech == null) {
            return;
        }
        for (UUID childPartId : childPartIds) {
            if (findChildPart(childPartId) instanceof MissingPart missingPart) {
                missingPart.reserveReplacementFor(tech);
            }
        }
    }

    @Override
    public void cancelReservation() {
        childPartIds.stream()
              .map(this::findChildPart)
              .filter(Objects::nonNull)
              .forEach(Part::cancelReservation);
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        // The Mass Repair "Replace/Salvage OmniPod Equipment" row governs pod work
        return PartRepairType.POD_SPACE;
    }

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.POD_SPACE;
    }


    /**
     * Sticker price is the value of the part according to the rulebooks
     *
     * @return the part's sticker price
     */
    @Override
    public Money getStickerPrice() {
        return Money.of(0.0);
    }

    /**
     * This is the value of the part that may be affected by characteristics and campaign options Note: Pod Space, an
     * abstraction, does not have value or price.
     *
     * @return the part's actual value
     */
    @Override
    public Money getActualValue() {
        return Money.of(0.0);
    }

    /**
     * This is the value of the part that may be affected by characteristics and campaign options but which ignores
     * damage Note: Pod Space, an abstraction, does not have value or price.
     *
     * @return the part's actual value
     */
    @Override
    public Money getUndamagedValue() {
        return Money.of(0.0);
    }

    @Override
    public boolean isPriceAdjustedForAmount() {
        return false;
    }
}
