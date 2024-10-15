/*
 * MissingPart.java
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

import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.WorkTime;

import java.io.PrintWriter;

/**
 * A missing part is a placeholder on a unit to indicate that a replacement
 * task needs to be performed
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public abstract class MissingPart extends Part implements IAcquisitionWork {
    public MissingPart(int tonnage, Campaign c) {
        super(tonnage, false, c);
    }

    public MissingPart(int tonnage, boolean isOmniPodded, Campaign c) {
        super(tonnage, isOmniPodded, c);
    }

    @Override
    public MissingPart clone() {
        //should never be called
        return null;
    }

    @Override
    public Money getStickerPrice() {
        // missing parts aren't worth a thing
        return Money.zero();
    }

    @Override
    public Money getBuyCost() {
        return getNewPart().getActualValue();
    }

    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    public String getStatus() {
        return "Destroyed";
    }

    @Override
    public boolean isSamePartType(Part part) {
        //missing parts should always return false
        return false;
    }

    @Override
    public String getDesc() {
        String bonus = getAllMods(null).getValueAsString();
        if (getAllMods(null).getValue() > -1) {
            bonus = '+' + bonus;
        }
        String toReturn = "<html><font";
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>Replace " + getName();

        if (getSkillMin() > SkillType.EXP_ELITE) {
            toReturn += " - <span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>Impossible</b></span>";
        } else {
            toReturn += " - <span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>"
                    + SkillType.getExperienceLevelName(getSkillMin()) + '+'
                    + "</span></b></b><br/>";
        }

        toReturn += getDetails() + "<br/>";
        if (getSkillMin() <= SkillType.EXP_ELITE) {
            toReturn += getTimeLeft() + " minutes" + scheduled;
            toReturn += " <b>TN:</b> " + bonus;
            if (getMode() != WorkTime.NORMAL) {
                toReturn += " <i>" + getCurrentModeName() + "</i>";
            }
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public String succeed() {
        fix();
        return " <font color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'><b> replaced.</b></font>";
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (replacement != null) {
            Part actualReplacement = replacement.clone();

            // Assign the replacement part to the unit
            unit.addPart(actualReplacement);

            // Add the replacement part to the campaign (after adding to the unit)
            campaign.getQuartermaster().addPart(actualReplacement, 0);

            replacement.decrementQuantity();

            remove(false);

            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public void remove(boolean salvage) {
        final Unit unit = getUnit();

        campaign.getWarehouse().removePart(this);
        if (unit != null) {
            unit.removePart(this);
        }

        setUnit(null);

        // Grab a reference to our parent part so that we don't accidentally NRE
        // when we remove the parent part reference.
        Part parentPart = getParentPart();
        if (parentPart != null) {
            parentPart.removeChildPart(this);
        }
    }

    public abstract boolean isAcceptableReplacement(Part part, boolean refit);

    public Part findReplacement(boolean refit) {
        //check to see if we already have a replacement assigned
        if (hasReplacementPart()) {
            return getReplacementPart();
        }

        // don't just return with the first part if it is damaged
        return campaign.getWarehouse().streamSpareParts()
            .filter(MissingPart::isAvailableAsReplacement)
            .reduce(null, (bestPart, part) -> {
                if (isAcceptableReplacement(part, refit)) {
                    if (bestPart == null) {
                        return part;
                    } else if (bestPart.needsFixing() && !part.needsFixing()) {
                        return part;
                    } else if (bestPart.getQuality().toNumeric() < part.getQuality().toNumeric()) {
                        return part;
                    }
                }
                return bestPart;
            });
    }

    /**
     * Gets a value indicating whether or not a part is available
     * as a replacement.
     * @param part The part being considered as a replacement.
     */
    public static boolean isAvailableAsReplacement(Part part) {
        return !(part.isReservedForRefit() || part.isBeingWorkedOn() || part.isReservedForReplacement() || !part.isPresent() || part.hasParentPart());
    }

    public boolean isReplacementAvailable() {
        return null != findReplacement(false);
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (isReplacementAvailable()) {
            return "Replacement part available";
        } else {
            PartInventory inventories = campaign.getPartInventory(getNewPart());
            return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>No replacement (" + inventories.getTransitOrderedDetails() + ")</font>";
        }
    }

    @Override
    public boolean needsFixing() {
        //missing parts always need fixing
        if (null != unit) {
            return (!unit.isSalvage() || null != getTech()) && unit.isRepairable();
        }
        return false;
    }

    @Override
    public MissingPart getMissingPart() {
        //do nothing - this should never be accessed
        return null;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        //do nothing
    }

    @Override
    public String fail(int rating) {
        skillMin = ++rating;
        timeSpent = 0;
        shorthandedMod = 0;
        if (skillMin > SkillType.EXP_ELITE) {
            Part part = findReplacement(false);
            if (null != part) {
                part.decrementQuantity();
                skillMin = SkillType.EXP_GREEN;
            }
            return " <font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'><b> failed and part destroyed.</b></font>";
        } else {
            return " <font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'><b> failed.</b></font>";
        }
    }

    @Override
    public boolean canChangeWorkMode() {
        return !isOmniPodded();
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        if (getTechBase() == T_CLAN && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        } else if (getTechBase() == T_IS && campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        } else if (getTechBase() == T_BOTH) {
            int penalty = Math.min(campaign.getCampaignOptions().getClanAcquisitionPenalty(), campaign.getCampaignOptions().getIsAcquisitionPenalty());
            if (penalty > 0) {
                target.addModifier(penalty, "tech limit");
            }
        }
        //availability mod
        int avail = getAvailability();
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + ITechnology.getRatingName(avail) + ')');

        return target;
    }

    @Override
    public String getAcquisitionDesc() {
        String toReturn = "<html><font";

        toReturn += ">";
        toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
        PartInventory inventories = campaign.getPartInventory(getNewPart());
        toReturn += inventories.getTransitOrderedDetails();
        if (!isOmniPodded()) {
            Part newPart = getAcquisitionPart();
            newPart.setOmniPodded(true);
            inventories = campaign.getPartInventory(newPart);
            if (inventories.getSupply() > 0) {
                toReturn += ", " + inventories.supplyAsString() + " OmniPod";
            }
        }
        toReturn += "<br/>";
        toReturn += getBuyCost().toAmountAndSymbolString() + "<br/>";
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public String getAcquisitionDisplayName() {
        return getAcquisitionName();
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return "";
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
    public String find(int transitDays) {
        Part newPart = getNewPart();
        newPart.setBrandNew(true);
        newPart.setDaysToArrival(transitDays);
        if (campaign.getQuartermaster().buyPart(newPart, transitDays)) {
            return "<font color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'><b> part found</b>.</font> It will be delivered in " + transitDays + " days.";
        } else {
            return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'><b> You cannot afford this part. Transaction cancelled</b>.</font>";
        }
    }

    @Override
    public Object getNewEquipment() {
        return getNewPart();
    }

    public abstract Part getNewPart();

    @Override
    public String failToFind() {
        return "<font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'><b> part not found</b>.</font>";
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    public @Nullable String checkScrappable() {
        if (!isReplacementAvailable()) {
            return "Nothing to scrap";
        }
        return null;
    }

    @Override
    public String scrap() {
        Part replace = findReplacement(false);
        if (null != replace) {
            replace.decrementQuantity();
            return replace.getName() + " scrapped.";
        }

        skillMin = SkillType.EXP_GREEN;

        return getName() + " scrapped.";
    }

    @Override
    public String getAcquisitionName() {
        String details = getNewPart().getDetails();
        details = details.replaceFirst("\\d+\\shit\\(s\\)", "");
        return getPartName() + ' ' + details;
    }

    @Override
    public int getTechLevel() {
        return getNewPart().getTechLevel();
    }

    @Override
    public void reservePart() {
        // this is being set as an overnight repair, so
        // we also need to reserve the replacement. If the
        // quantity of the replacement is more than one, we will
        // also need to split off a separate one
        // shouldn't be null, but it never hurts to check
        Part replacement = findReplacement(false);
        if ((null != replacement) && (null != getTech())) {
            if (replacement.getQuantity() > 1) {
                Part actualReplacement = replacement.clone();
                actualReplacement.setReservedBy(getTech());
                campaign.getQuartermaster().addPart(actualReplacement, 0);
                setReplacementPart(actualReplacement);
                replacement.decrementQuantity();
            } else {
                replacement.setReservedBy(getTech());
                setReplacementPart(replacement);
            }
        }
    }

    @Override
    public void cancelReservation() {
        if (hasReplacementPart()) {
            Part replacement = getReplacementPart();
            if (replacement != null) {
                replacement.setReservedBy(null);

                // Only return the replacement part to the campaign if we have one
                if (replacement.getQuantity() > 0) {
                    campaign.getQuartermaster().addPart(replacement, 0);
                }
            }
        }

        setReplacementPart(null);
    }

    @Override
    public boolean needsMaintenance() {
        return false;
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
