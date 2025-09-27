/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.missing;

import java.io.PrintWriter;
import java.text.MessageFormat;

import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Availability;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.WorkTime;
import mekhq.utilities.ReportingUtilities;

/**
 * A missing part is a placeholder on a unit to indicate that a replacement task needs to be performed
 *
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
        StringBuilder toReturn = new StringBuilder();
        toReturn.append("<html><b>Replace ").append(getName());
        if (isUnitTonnageMatters()) {
            toReturn.append(" (").append(getUnitTonnage()).append(" ton)");
        }
        toReturn.append(" - ")
              .append(ReportingUtilities.messageSurroundedBySpanWithColor(SkillType.getExperienceLevelColor(getSkillMin()),
                    SkillType.getExperienceLevelName(getSkillMin()) + "+"))
              .append("</b><br/>")
              .append(getDetails())
              .append("<br/>");

        if (getSkillMin() <= SkillType.EXP_LEGENDARY) {
            toReturn.append(getTimeLeft())
                  .append(" minutes")
                  .append(null != getTech() ? " (scheduled)" : "")
                  .append(" <b>TN:</b> ")
                  .append(getAllMods(null).getValue() > -1 ? "+" : "")
                  .append(getAllMods(null).getValueAsString());
            if (getMode() != WorkTime.NORMAL) {
                toReturn.append(" <i>").append(getCurrentModeName()).append("</i>");
            }
        }
        toReturn.append("</html>");
        return toReturn.toString();
    }

    @Override
    public String succeed() {
        fix();
        return ReportingUtilities.messageSurroundedBySpanWithColor(ReportingUtilities.getPositiveColor(),
              " <b>replaced</b>.");
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (replacement != null) {
            Part actualReplacement = replacement.clone();

            // Assign the replacement part to the unit
            unit.addPart(actualReplacement);

            // Add the replacement part to the campaign (after adding to the unit)
            campaign.getQuartermaster().addPart(actualReplacement, 0, false);

            replacement.changeQuantity(-1);

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
        return campaign.getWarehouse()
                     .streamSpareParts()
                     .filter(MissingPart::isAvailableAsReplacement)
                     .filter(p -> !p.isUsedForRefitPlanning() || !refit)
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
     * Gets a value indicating whether a part is available as a replacement.
     *
     * @param part The part being considered as a replacement.
     */
    public static boolean isAvailableAsReplacement(Part part) {
        return !(part.isReservedForRefit() ||
                       part.isBeingWorkedOn() ||
                       part.isReservedForReplacement() ||
                       !part.isPresent() ||
                       part.hasParentPart());
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
        PartInventory inventories = campaign.getPartInventory(getNewPart());
        StringBuilder toReturn = new StringBuilder();

        String superDetails = super.getDetails(includeRepairDetails);
        toReturn.append(superDetails);

        if (!(this instanceof MissingAmmoBin)) {
            // Ammo bins don't require/have stock replacements.
            if (!superDetails.isEmpty()) {
                toReturn.append(", ");
            }
            if (isReplacementAvailable()) {
                toReturn.append(inventories.getSupply()).append(" in stock");
            } else {
                toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(
                      ReportingUtilities.getNegativeColor(), "None in stock"));
            }

            String incoming = inventories.getTransitOrderedDetails();
            if (!incoming.isEmpty()) {

                toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(
                      ReportingUtilities.getWarningColor(), " (" + incoming + ")"));
            }
        }
        return toReturn.toString();
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
        if (skillMin > SkillType.EXP_LEGENDARY) {
            Part part = findReplacement(false);
            if (null != part) {
                part.changeQuantity(-1);
                skillMin = SkillType.EXP_GREEN;
            }
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getNegativeColor(),
                  "<b> failed and part destroyed</b>") + '.';
        } else {
            return ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getNegativeColor(),
                  "<b> failed</b>") + '.';
        }
    }

    @Override
    public boolean canChangeWorkMode() {
        return !isOmniPodded();
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        if (getTechBase() == TechBase.CLAN && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        } else if (getTechBase() == TechBase.IS && campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        } else if (getTechBase() == TechBase.ALL) {
            int penalty = Math.min(campaign.getCampaignOptions().getClanAcquisitionPenalty(),
                  campaign.getCampaignOptions().getIsAcquisitionPenalty());
            if (penalty > 0) {
                target.addModifier(penalty, "tech limit");
            }
        }
        //availability mod
        AvailabilityValue avail = getAvailability();
        if (avail == null) {
            target.addModifier(
                  TargetRoll.IMPOSSIBLE,
                  MessageFormat.format(
                        "Attempting to get availability modifier for null availability: {0}",
                        getPartName()
                  )
            );
            return target;
        }
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + avail.getName() + ')');

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
        // TODO: Move me to live with procurement functions?
        // Which shopping method is this used for?
        Part newPart = getNewPart();
        newPart.setBrandNew(true);
        newPart.setDaysToArrival(transitDays);
        StringBuilder toReturn = new StringBuilder();
        if (campaign.getQuartermaster().buyPart(newPart, transitDays)) {
            toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(
                        ReportingUtilities.getPositiveColor(), "<b> part found</b>"))
                  .append(". It will be delivered in ")
                  .append(transitDays)
                  .append(" days.");
        } else {
            toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(
                  ReportingUtilities.getNegativeColor(),
                  "<b> You cannot afford this part. Transaction cancelled</b>"));
        }
        return toReturn.toString();
    }

    @Override
    public Object getNewEquipment() {
        return getNewPart();
    }

    public abstract Part getNewPart();

    @Override
    public String failToFind() {
        // TODO: Move me to live with procurement functions?
        return ReportingUtilities.messageSurroundedBySpanWithColor(
              ReportingUtilities.getNegativeColor(), "<b> part not found</b>") + ".";
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
            replace.changeQuantity(-1);
            return replace.getName() + " scrapped.";
        }

        skillMin = SkillType.EXP_GREEN;

        return getName() + " scrapped.";
    }

    @Override
    public String getAcquisitionName() {
        // TODO: Unify shopping system to use these everywhere instead of only some places?
        StringBuilder toReturn = new StringBuilder();
        toReturn.append(getPartName());

        String details = getNewPart().getDetails();
        if (!details.isEmpty()) {
            toReturn.append(" (").append(details).append(')');
        }
        return toReturn.toString();
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
                campaign.getQuartermaster().addPart(actualReplacement, 0, false);
                setReplacementPart(actualReplacement);
                replacement.changeQuantity(-1);
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
                    campaign.getQuartermaster().addPart(replacement, 0, false);
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
    public boolean isIntroducedBy(int year, boolean clan, Faction techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, Faction techFaction) {
        return isExtinct(year, clan, techFaction);
    }
}
