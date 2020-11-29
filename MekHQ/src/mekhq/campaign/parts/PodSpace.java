/*
 * Copyright (C) 2017 - The MegaMek Team
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;

/**
 * An abstraction of all the pod-mounted equipment within a single location of an omni unit. Used
 * to group them together as recipients of a single tech action.
 *
 * @author Neoancient
 *
 */
public class PodSpace implements Serializable, IPartWork {

    private static final long serialVersionUID = -9022671736030862210L;

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
            if (unit.getEntity() instanceof Aero && location == Aero.LOC_WINGS) {
                this.location = -1;
            }
        }
    }

    @Override
    public int getBaseTime() {
        return 30;
    }

    public List<Part> getPartList() {
        return childPartIds.stream().map(id -> campaign.getPart(id))
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
            final Part part = campaign.getPart(pid);
            if (part != null) {
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
            final Part part = campaign.getPart(pid);
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
            final Part part = campaign.getPart(pid);
            if (part != null && part instanceof MissingPart) {
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
    public String checkFixable() {
        if(isSalvaging() || location < 0) {
            return null;
        }
        // The part is only fixable if the location is not destroyed.
        // be sure to check location and second location
        if(null != unit) {
            if (unit.isLocationBreached(location)) {
                return unit.getEntity().getLocationName(location) + " is breached.";
            }
            if (unit.isLocationDestroyed(location)) {
                return unit.getEntity().getLocationName(location) + " is destroyed.";
            }
            if (repairInPlace) {
                for (int id : childPartIds) {
                    final Part p = unit.getCampaign().getPart(id);
                    if (p != null && p instanceof MissingPart) {
                        return null;
                    }
                }
                return unit.getEntity().getLocationName(location) + " is not missing any pod-mounted equipment.";
            } else {
                for (int id : childPartIds) {
                    final Part p = unit.getCampaign().getPart(id);
                    if (p == null || !p.needsFixing()) {
                        continue;
                    }
                    MissingPart missing = null;
                    if (p instanceof MissingPart) {
                        missing = (MissingPart)p;
                    } else {
                        missing = p.getMissingPart();
                    }
                    if (missing.isReplacementAvailable()) {
                        return null;
                    }
                }
                return "There are no replacement parts available for "
                    + unit.getEntity().getLocationName(location) + ".";
            }
        }
        return null;
    }

    @Override
    public boolean needsFixing() {
        return childPartIds.stream()
                .map(id -> campaign.getPart(id)).filter(Objects::nonNull)
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

    public int getLocation() {
        return location;
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
        if(null != unit) {
            mods.append(unit.getSiteMod());
            if(unit.getEntity().hasQuirk("easy_maintain")) {
                mods.addModifier(-1, "easy to maintain");
            }
            else if(unit.getEntity().hasQuirk("difficult_maintain")) {
                mods.addModifier(1, "difficult to maintain");
            }
        }
        return mods;
    }

    @Override
    public String succeed() {
        if (isSalvaging()) {
            remove(true);
            return " <font color='green'><b> removed.</b></font>";
        } else {
            fix();
            return " <font color='green'><b> fixed.</b></font>";
        }
    }

    @Override
    public String fail(int rating) {
        timeSpent = 0;
        shorthandedMod = 0;
        boolean replacing = false;
        for (int id : childPartIds) {
            final Part part = campaign.getPart(id);
            if (part != null && (isSalvaging() ||
                    (!(part instanceof AmmoBin) && part.needsFixing()))) {
                part.fail(rating);
                replacing |= part instanceof MissingPart;
            }
        }
        if(rating >= SkillType.EXP_ELITE && replacing) {
                return " <font color='red'><b> failed and part(s) destroyed.</b></font>";
        } else {
            return " <font color='red'><b> failed.</b></font>";
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
            final Part part = campaign.getPart(id);
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
        if (unit.getEntity() instanceof Mech) {
            return skillType.equals(SkillType.S_TECH_MECH);
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
        String bonus = getAllMods(null).getValueAsString();
        if (getAllMods(null).getValue() > -1) {
            bonus = "+" + bonus;
        }
        bonus = "(" + bonus + ")";
        String toReturn = "<html><font size='2'";
        String action = "Replace ";
        if(isSalvaging()) {
            action = "Salvage ";
        }
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>" + action + getPartName() + " Equipment</b><br/>";
        toReturn += getDetails() + "<br/>";
        if(getSkillMin() > SkillType.EXP_ELITE) {
            toReturn += "<font color='red'>Impossible</font>";
        } else {
            toReturn += "" + getTimeLeft() + " minutes" + scheduled;
            if(!campaign.getCampaignOptions().isDestroyByMargin()) {
                toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
            }
            toReturn += " " + bonus;
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
        int allParts = 0;
        int replacements = 0;
        int inTransit = 0;
        int onOrder = 0;
        for (int id : childPartIds) {
            Part part = campaign.getPart(id);
            if (part != null) {
                if (!isSalvaging() && !(part instanceof AmmoBin) && part.needsFixing()) {
                    allParts++;
                    MissingPart missing;
                    if (part instanceof MissingPart) {
                        missing = (MissingPart)part;
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
            final Part p = campaign.getPart(id);
            if (p != null && p.isSalvaging()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reservePart() {
        childPartIds.stream().map(id -> campaign.getPart(id))
            .filter(Objects::nonNull).forEach(Part::reservePart);
    }

    @Override
    public void cancelReservation() {
        childPartIds.stream().map(id -> campaign.getPart(id))
            .filter(Objects::nonNull).forEach(Part::cancelReservation);
    }

    @Override
    public int getMassRepairOptionType() {
        return Part.REPAIR_PART_TYPE.GENERAL_LOCATION;
    }

    @Override
    public int getRepairPartType() {
        return Part.REPAIR_PART_TYPE.POD_SPACE;
    }

}
