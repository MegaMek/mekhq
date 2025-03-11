/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.parts;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.utilities.ReportingUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An abstraction of all the pod-mounted equipment within a single location of an omni unit. Used
 * to group them together as recipients of a single tech action.
 *
 * @author Neoancient
 */
public class PodSpace implements IPartWork {
    protected Campaign campaign;
    protected Unit unit;
    protected int location;
    protected List<Integer> childPartIds = new ArrayList<>();

    protected Person tech;
    protected int timeSpent = 0;
    protected boolean workingOvertime = false;
    protected int shorthandedMod = 0;

    protected boolean repairInPlace = false;

    public PodSpace() {
        this(Entity.LOC_NONE, null);
    }

    public PodSpace(int location, Unit unit) {
        this.location = location;
        this.unit = unit;
        if (unit != null) {
            this.campaign = unit.getCampaign();
            //We don't need a LOC_WINGS podspace, but we do need one for the fuselage equipment, which is stored at LOC_NONE.
            if ((unit.getEntity() instanceof Aero) && (location == Aero.LOC_WINGS)) {
                this.location = -1;
            }
        }
    }

    @Override
    public int getBaseTime() {
        return 30;
    }

    public List<Part> getPartList() {
        return childPartIds.stream().map(id -> campaign.getWarehouse().getPart(id))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        childPartIds.clear();
        for (Part part : getUnit().getParts()) {
            if (part.isOmniPodded() && part.getLocation() == location) {
                childPartIds.add(part.getId());
            }
        }
    }

    @Override
    public void updateConditionFromPart() {
        //nothing to do here
    }

    @Override
    public void remove(boolean salvage) {
        shorthandedMod = 0;
        //Iterate through all pod-mounted equipment in space and remove them.
        for (int pid : childPartIds) {
            final Part part = campaign.getWarehouse().getPart(pid);
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
        for (int pid : childPartIds) {
            final Part part = campaign.getWarehouse().getPart(pid);
            if (part != null && !(part instanceof MissingPart)
                    && !(part instanceof AmmoBin)
                    && part.needsFixing()
                    && !repairInPlace) {
                part.remove(true);
                MekHQ.triggerEvent(new PartChangedEvent(part));
            }
        }
        updateConditionFromEntity(false);
        for (int pid : childPartIds) {
            final Part part = campaign.getWarehouse().getPart(pid);
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
            for (int partId : childPartIds) {
                // If all remaining parts are already missing, we don't need to keep salvaging
                 if (!(campaign.getWarehouse().getPart(partId) instanceof MissingPart)) {
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
                for (int id : childPartIds) {
                    final Part p = unit.getCampaign().getWarehouse().getPart(id);
                    if (p instanceof MissingPart) {
                        return null;
                    }
                }
                return unit.getEntity().getLocationName(location) + " is not missing any pod-mounted equipment.";
            } else {
                for (int id : childPartIds) {
                    final Part p = unit.getCampaign().getWarehouse().getPart(id);
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
                return "There are no replacement parts available for "
                    + unit.getEntity().getLocationName(location) + '.';
            }
        }
        return null;
    }

    @Override
    public boolean needsFixing() {
        return childPartIds.stream()
                .map(id -> campaign.getWarehouse().getPart(id)).filter(Objects::nonNull)
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
                    MekHQ.getMHQOptions().getFontColorPositiveHexColor(), "<b> removed</b>") + ".";
        } else {
            fix();
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorPositiveHexColor(), "<b> fixed</b>") + ".";
        }
    }

    @Override
    public String fail(int rating) {
        timeSpent = 0;
        shorthandedMod = 0;
        boolean replacing = false;
        for (int id : childPartIds) {
            final Part part = campaign.getWarehouse().getPart(id);
            if (part != null && (isSalvaging() ||
                    (!(part instanceof AmmoBin) && part.needsFixing()))) {
                part.fail(rating);
                replacing |= part instanceof MissingPart;
            }
        }
        if (rating >= SkillType.EXP_ELITE && replacing) {
                return ReportingUtilities.messageSurroundedBySpanWithColor(
                        MekHQ.getMHQOptions().getFontColorNegativeHexColor(),
                        "<b> failed and part(s) destroyed</b>") + ".";
        } else {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                    MekHQ.getMHQOptions().getFontColorNegativeHexColor(),"<b> failed</b>") + ".";
        }
    }

    @Override
    public Person getTech() {
        return tech;
    }

    @Override
    public boolean isBeingWorkedOn() {
        return getTech() != null;
    }

    @Override
    public String getPartName() {
        return getLocationName() + " Pod Space";
    }

    @Override
    public int getSkillMin() {
        int minSkill = SkillType.EXP_GREEN;
        for (int id : childPartIds) {
            final Part part = campaign.getWarehouse().getPart(id);
            if (part != null) {
                if ((isSalvaging() && !(part instanceof MissingPart))
                        || (!isSalvaging() && (part instanceof MissingPart)
                                || (!(part instanceof AmmoBin) && part.needsFixing()))) {
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
        if (unit.getEntity() instanceof Mek) {
            return skillType.equals(SkillType.S_TECH_MEK);
        } else if (unit.getEntity() instanceof Aero) {
            return skillType.equals(SkillType.S_TECH_AERO);
        } else if (unit.getEntity() instanceof Tank) {
            return skillType.equals(SkillType.S_TECH_MECHANIC);
        }
        return false;
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
            .append(ReportingUtilities.messageSurroundedBySpanWithColor(
                SkillType.getExperienceLevelColor(getSkillMin()),
                SkillType.getExperienceLevelName(getSkillMin()) + "+"))
            .append("</b><br/>")
            .append(getDetails())
            .append("<br/>");

        if (getSkillMin() <= SkillType.EXP_ELITE) {
            toReturn.append(getTimeLeft())
                .append(" minutes")
                .append(getTech() != null ? " (scheduled)" : "")
                .append(" <b>TN:</b> ")
                .append(getAllMods(null).getValue() > -1 ? "+" : "")
                .append(getAllMods(null).getValueAsString());
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
        for (int id : childPartIds) {
            Part part = campaign.getWarehouse().getPart(id);
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
                        PartInventory inventories = campaign.getPartInventory(missing.getNewPart());
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
            return replacements + "/" + allParts + " available<br />"
                    + inTransit + " in transit, " + onOrder + " on order";
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

    public boolean shouldRepairInPlace() {
        return repairInPlace;
    }

    public void setRepairInPlace(boolean repairInPlace) {
        this.repairInPlace = repairInPlace;
    }

    public boolean hasSalvageableParts() {
        for (int id : childPartIds) {
            final Part p = campaign.getWarehouse().getPart(id);
            if (p != null && p.isSalvaging()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reservePart() {
        childPartIds.stream().map(id -> campaign.getWarehouse().getPart(id))
            .filter(Objects::nonNull).forEach(Part::reservePart);
    }

    @Override
    public void cancelReservation() {
        childPartIds.stream().map(id -> campaign.getWarehouse().getPart(id))
            .filter(Objects::nonNull).forEach(Part::cancelReservation);
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.GENERAL_LOCATION;
    }

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.POD_SPACE;
    }


    /**
     * Sticker price is the value of the part according to the rulebooks
     * @return the part's sticker price
     */
    @Override
    public Money getStickerPrice(){
        return Money.of(0.0);
    }

    /**
     * This is the value of the part that may be affected by characteristics and campaign options
     * (Note: Pod Space, an abstraction, does not have value or price.
     * @return the part's actual value
     */
    @Override
    public Money getActualValue() {
        return Money.of(0.0);
    }

    /**
     * This is the value of the part that may be affected by characteristics and campaign options
     * but which ignores damage
     * (Note: Pod Space, an abstraction, does not have value or price.
     * @return the part's actual value
     */
    @Override
    public Money getUndamagedValue() {
        return Money.of(0.0);
    }

    @Override
    public boolean isPriceAdjustedForAmount(){
        return false;
    }
}
