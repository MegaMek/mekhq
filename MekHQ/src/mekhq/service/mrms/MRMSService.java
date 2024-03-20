/*
 * Copyright (c) 2017-2023 - The MegaMek Team. All Rights Reserved.
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
package mekhq.service.mrms;

import megamek.common.*;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.service.mrms.MRMSService.MRMSUnitAction.STATUS;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.*;

public class MRMSService {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.MRMS",
            MekHQ.getMHQOptions().getLocale());

    private MRMSService() {

    }

    public static boolean isValidMRMSUnit(Unit unit, MRMSConfiguredOptions configuredOptions) {
        if (unit.isSelfCrewed() || (!unit.isSalvage() && !configuredOptions.useRepair())
                || (unit.isSalvage() && !configuredOptions.useSalvage())) {
            return false;
        }

        return (unit.getEntity() instanceof Tank) || (unit.getEntity() instanceof Aero)
                || (unit.getEntity() instanceof Mech) || (unit.getEntity() instanceof BattleArmor);
    }

    public static MRMSPartSet performWarehouseMRMS(List<IPartWork> selectedParts,
                                                   MRMSConfiguredOptions configuredOptions,
                                                   Campaign campaign) {
        if (!configuredOptions.useRepair()) { // Warehouse only uses repair
            campaign.addReport(resources.getString("MRMS.CompleteDisabled.report"));
            return new MRMSPartSet();
        }
        campaign.addReport(resources.getString("MRMS.StartWarehouse.report"));

        List<Person> techs = campaign.getTechs(true);

        MRMSPartSet partSet = new MRMSPartSet();

        if (techs.isEmpty()) {
            campaign.addReport(resources.getString("MRMS.NoAvailableTechs.report"));
        } else {
            Map<PartRepairType, MRMSOption> mrmsOptionsByType = new HashMap<>();

            for (MRMSOption mrmsOption : configuredOptions.getMRMSOptions()) {
                mrmsOptionsByType.put(mrmsOption.getType(), mrmsOption);
            }

            /*
             * Filter our parts list to only those that aren't being worked on
             * or those that meet our criteria as defined in the campaign
             * configurations
             */
            List<IPartWork> parts = filterParts(selectedParts, mrmsOptionsByType, techs, campaign);

            if (!parts.isEmpty()) {
                for (IPartWork partWork : parts) {
                    Part part = (Part) partWork;
                    part.resetModeToNormal();

                    List<Person> validTechs = filterTechs(partWork, techs, mrmsOptionsByType, true, campaign);

                    if (validTechs.isEmpty()) {
                        continue;
                    }

                    int originalQuantity = part.getQuantity();

                    for (int i = 0; i < originalQuantity; i++) {
                        partSet.addPartAction(repairPart(campaign, part, null, validTechs,
                                mrmsOptionsByType, configuredOptions, true));
                    }
                }
            }
        }

        return partSet;
    }

    public static String performSingleUnitMRMS(Campaign campaign, Unit unit) {
        MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(campaign);
        if (!configuredOptions.isEnabled()) {
            String msg = resources.getString("MRMS.CompleteDisabled.report");
            campaign.addReport(msg);
            return msg;
        } else if ((!unit.isSalvage() && !configuredOptions.useRepair())
                || (unit.isSalvage() && !configuredOptions.useSalvage())) {
            String msg = MessageFormat.format(resources.getString("MRMS.CompleteTypeDisabled.report"),
                    unit.isSalvage() ? resources.getString("Salvage") : resources.getString("Repair"));
            campaign.addReport(msg);
            return msg;
        } else if (campaign.requiresAdditionalAstechs()) {
            String message = resources.getString("MRMS.InsufficientAstechs.report");
            campaign.addReport(message);
            return message;
        }

        List<MRMSOption> activeMRMSOptions = configuredOptions.getActiveMRMSOptions();
        MRMSUnitAction unitAction = performUnitMRMS(campaign, unit, unit.isSalvage(),
                activeMRMSOptions, configuredOptions);

        String actionDescriptor = unit.isSalvage() ? resources.getString("Salvage") : resources.getString("Repair");
        String msg = String.format("<font color='green'>Mass %s complete on %s.</font>", actionDescriptor,
                unit.getName());

        switch (unitAction.getStatus()) {
            case ACTIONS_PERFORMED:
                int count = unitAction.getPartSet().countRepairs();
                msg += String.format(" There were %s action%s performed.", count, (count == 1 ? "" : "s"));
                break;
            case NO_PARTS:
                msg += " No actions were performed because there are currently no valid parts.";
                break;
            case ALL_PARTS_IN_PROCESS:
                msg += " No actions were performed because all parts are being worked on.";
                break;
            case NO_TECHS:
                msg += " No actions were performed because there are currently no valid techs.";
                break;
            case UNFIXABLE_LIMB:
                msg += " No actions were performed because there is at least one unfixable limb and configured settings do not allow location repairs.";
                break;
            case NO_ACTIONS:
            default:
                break;
        }

        campaign.addReport(msg);

        List<Person> techs = campaign.getTechs();

        if (!techs.isEmpty()) {
            List<IPartWork> parts = unit.getPartsNeedingService(true);
            parts = filterParts(parts, null, techs, campaign);

            if (!parts.isEmpty()) {
                if (parts.size() == 1) {
                    campaign.addReport("<font color='red'>There in still 1 part that is not being worked on.</font>");
                } else {
                    campaign.addReport(String.format(
                            "<font color='red'>There are still %s parts that are not being worked on.</font>",
                            parts.size()));
                }
            }
        }

        return String.format("Mass %s complete on %s.", actionDescriptor, unit.getName());
    }

    public static void mrmsAllUnits(Campaign campaign) {
        MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(campaign);
        if (!configuredOptions.isEnabled()) {
            campaign.addReport(resources.getString("MRMS.CompleteDisabled.report"));
            return;
        } else if (campaign.requiresAdditionalAstechs()) {
            campaign.addReport(resources.getString("MRMS.InsufficientAstechs.report"));
            return;
        }

        List<Unit> units = new ArrayList<>();

        for (Unit unit : campaign.getServiceableUnits()) {
            if (!isValidMRMSUnit(unit, configuredOptions)) {
                continue;
            }

            units.add(unit);
        }

        // Sort the list status fixing the least damaged first
        units.sort((o1, o2) -> {
            int damageIdx1 = UnitStatusSorter.getDamageStateIndex(Unit.getDamageStateName(o1.getDamageState()));
            int damageIdx2 = UnitStatusSorter.getDamageStateIndex(Unit.getDamageStateName(o2.getDamageState()));

            if (damageIdx2 == damageIdx1) {
                return 0;
            } else if (damageIdx2 < damageIdx1) {
                return -1;
            }

            return 1;
        });

        mrmsUnits(campaign, units, configuredOptions);
    }

    public static void mrmsUnits(Campaign campaign, List<Unit> units,
                                 MRMSConfiguredOptions configuredOptions) {
        // This shouldn't happen but is being added preventatively
        if (!configuredOptions.isEnabled()) {
            campaign.addReport(resources.getString("MRMS.CompleteDisabled.report"));
            return;
        }
        Map<MRMSUnitAction.STATUS, List<MRMSUnitAction>> unitActionsByStatus = new HashMap<>();
        List<MRMSOption> activeMRMSOptions = configuredOptions.getActiveMRMSOptions();

        for (Unit unit : units) {
            MRMSUnitAction unitAction = performUnitMRMS(campaign, unit,
                    unit.isSalvage(), activeMRMSOptions, configuredOptions);

            List<MRMSUnitAction> list = unitActionsByStatus.computeIfAbsent(unitAction.getStatus(), k -> new ArrayList<>());

            list.add(unitAction);
        }

        if (unitActionsByStatus.isEmpty()) {
            campaign.addReport(resources.getString("MRMS.CompleteNoUnits.report"));
        } else {
            int totalCount = 0;
            int actionsPerformed = 0;

            for (MRMSUnitAction.STATUS key : unitActionsByStatus.keySet()) {
                if (key == MRMSUnitAction.STATUS.ALL_PARTS_IN_PROCESS) {
                    continue;
                }

                totalCount += unitActionsByStatus.get(key).size();
            }

            if (unitActionsByStatus.containsKey(MRMSUnitAction.STATUS.ACTIONS_PERFORMED)) {
                List<MRMSUnitAction> unitsByStatus = unitActionsByStatus
                        .get(MRMSUnitAction.STATUS.ACTIONS_PERFORMED);

                for (MRMSUnitAction mrmsUnitAction : unitsByStatus) {
                    actionsPerformed += mrmsUnitAction.getPartSet().countRepairs();
                }
            }

            StringBuilder sb = new StringBuilder(
                    String.format("<font color='green'>Mass Repair/Salvage complete for %s units.</font>", totalCount));

            if (actionsPerformed > 0) {
                sb.append(String.format(" %s repair/salvage action%s performed.", actionsPerformed,
                        (actionsPerformed == 1 ? "" : "s")));
            }

            sb.append(generateUnitRepairSummary("<br/>- %s unit%s had repairs/parts salvaged.", unitActionsByStatus,
                    MRMSUnitAction.STATUS.ACTIONS_PERFORMED));
            sb.append(generateUnitRepairSummary(
                    "<br/>- %s unit%s had no actions performed because there were no valid parts.", unitActionsByStatus,
                    MRMSUnitAction.STATUS.NO_PARTS));
            sb.append(generateUnitRepairSummary(
                    "<br/>- %s unit%s had no actions performed because there were no valid techs.", unitActionsByStatus,
                    MRMSUnitAction.STATUS.NO_TECHS));
            sb.append(generateUnitRepairSummary(
                    "<br/>- %s unit%s had no actions performed because there were unfixable limbs and configured settings do not allow location repairs.",
                    unitActionsByStatus, MRMSUnitAction.STATUS.UNFIXABLE_LIMB));

            campaign.addReport(sb.toString());
        }

        generateCampaignLogForUnitStatus(unitActionsByStatus, MRMSUnitAction.STATUS.NO_PARTS,
                "Units with no valid parts:", campaign);
        generateCampaignLogForUnitStatus(unitActionsByStatus, MRMSUnitAction.STATUS.NO_TECHS,
                "Units with no valid techs:", campaign);
        generateCampaignLogForUnitStatus(unitActionsByStatus, MRMSUnitAction.STATUS.UNFIXABLE_LIMB,
                "Units with unfixable limbs:", campaign);

        if (!unitActionsByStatus.isEmpty()) {
            List<Person> techs = campaign.getTechs();

            if (!techs.isEmpty()) {
                int count = 0;
                int unitCount = 0;

                for (List<MRMSUnitAction> mrmsUnitActions : unitActionsByStatus.values()) {
                    for (MRMSUnitAction mrmsUnitAction : mrmsUnitActions) {
                        List<IPartWork> parts = mrmsUnitAction.getUnit().getPartsNeedingService(true);
                        int tempCount = filterParts(parts, null, techs, campaign).size();

                        if (tempCount > 0) {
                            unitCount++;
                            count += tempCount;
                        }
                    }
                }

                if (count > 0) {
                    if (count == 1) {
                        campaign.addReport("<font color='red'>There in still 1 part that is not being worked on.</font>");
                    } else {
                        campaign.addReport(String.format(
                                "<font color='red'>There are still %s parts that are not being worked on %s unit%s.</font>",
                                count, unitCount, (unitCount == 1) ? "" : "s"));
                    }
                }
            }
        }

        // Remove any units which after mass repair/salvage are no longer usable.
        for (Unit unit : units) {
            if (!unit.isRepairable() && !unit.hasSalvageableParts()) {
                campaign.removeUnit(unit.getId());
            }
        }
    }

    private static String generateUnitRepairSummary(String baseDescription,
            Map<MRMSUnitAction.STATUS, List<MRMSUnitAction>> unitActionsByStatus,
            MRMSUnitAction.STATUS status) {

        if (!unitActionsByStatus.containsKey(status)) {
            return "";
        }

        int count = unitActionsByStatus.get(status).size();

        return String.format(baseDescription, count, count == 1 ? "" : "s");
    }

    private static void generateCampaignLogForUnitStatus(
            Map<MRMSUnitAction.STATUS, List<MRMSUnitAction>> unitActionsByStatus,
            MRMSUnitAction.STATUS status, String statusDesc, Campaign campaign) {
        if (!unitActionsByStatus.containsKey(status) || unitActionsByStatus.get(status).isEmpty()) {
            return;
        }

        StringBuilder sbMsg = new StringBuilder();
        sbMsg.append(statusDesc);

        List<MRMSUnitAction> unitsByStatus = unitActionsByStatus.get(status);

        for (MRMSUnitAction mrmsUnitAction : unitsByStatus) {
            sbMsg.append("<br/>- ").append(mrmsUnitAction.getUnit().getName());
        }

        campaign.addReport(sbMsg.toString());
    }

    private static MRMSUnitAction performUnitMRMS(Campaign campaign, Unit unit, boolean isSalvage,
                                                  List<MRMSOption> mrmsOptions,
                                                  MRMSConfiguredOptions configuredOptions) {
        List<Person> techs = campaign.getTechs(true);

        if (techs.isEmpty()) {
            return new MRMSUnitAction(unit, isSalvage, MRMSUnitAction.STATUS.NO_TECHS);
        }

        MRMSUnitAction unitAction = new MRMSUnitAction(unit, isSalvage, MRMSUnitAction.STATUS.NO_ACTIONS);

        // Filter our tech list to only our techs that can work on this unit
        for (int i = techs.size() - 1; i >= 0; i--) {
            Person tech = techs.get(i);

            if (!tech.canTech(unit.getEntity())) {
                techs.remove(i);
            }
        }

        Map<PartRepairType, MRMSOption> mrmsOptionsByType = new HashMap<>();

        for (MRMSOption mrmsOption : mrmsOptions) {
            mrmsOptionsByType.put(mrmsOption.getType(), mrmsOption);
        }

        // Possibly call this multiple times. Sometimes some actions are first dependent upon
        // others being finished, also failed actions can be performed again by a tech with a higher
        // skill.
        boolean performMoreRepairs = true;

        long time = System.nanoTime();

        while (performMoreRepairs) {
            MRMSUnitAction currentUnitAction = performUnitMassTechAction(campaign, unit, techs,
                    mrmsOptionsByType, isSalvage, configuredOptions);

            performMoreRepairs = currentUnitAction.getPartSet().isHasRepairs();
            unitAction.merge(currentUnitAction);

            if (unitAction.isStatusNoActions()) {
                unitAction.setStatus(currentUnitAction.getStatus());
            }
        }

        debugLog("Finished fixing %s in %s ns", "performUnitMRMS", unit.getName(), System.nanoTime() - time);

        return unitAction;
    }

    private static MRMSUnitAction performUnitMassTechAction(Campaign campaign, Unit unit,
                                                            List<Person> techs,
                                                            Map<PartRepairType, MRMSOption> mrmsOptionsByType,
                                                            boolean salvaging,
                                                            MRMSConfiguredOptions configuredOptions) {
        List<IPartWork> parts = unit.getPartsNeedingService(true);

        if (parts.isEmpty()) {
            parts = unit.getPartsNeedingService(false);

            if (!parts.isEmpty()) {
                return new MRMSUnitAction(unit, salvaging, MRMSUnitAction.STATUS.ALL_PARTS_IN_PROCESS);
            }

            return new MRMSUnitAction(unit, salvaging, MRMSUnitAction.STATUS.NO_PARTS);
        }

        for (IPartWork partWork : parts) {
            if (partWork instanceof Part) {
                ((Part) partWork).resetModeToNormal();
            }
        }

        // If we're performing an action on a unit and we allow auto-scrapping of parts that can't
        // be fixed by an elite tech, let's first get rid of those parts and start with a cleaner
        // slate
        if (configuredOptions.isScrapImpossible()) {
            boolean refreshParts = false;

            for (IPartWork partWork : parts) {
                if ((partWork instanceof Part) && (partWork.getSkillMin() > SkillType.EXP_ELITE)) {
                    campaign.addReport(((Part) partWork).scrap());
                    refreshParts = true;
                }
            }

            if (refreshParts) {
                parts = unit.getPartsNeedingService(true);
            }
        }

        if (unit.getEntity().isOmni() && !unit.isSalvage()) {
            for (PodSpace ps : unit.getPodSpace()) {
                ps.setRepairInPlace(!configuredOptions.isReplacePodParts());
            }

            // If we're replacing damaged parts, we want to remove any that have an available
            // replacement from the list since the pod space repair will cover it.
            List<IPartWork> temp = new ArrayList<>();

            for (IPartWork p : parts) {
                if ((p instanceof Part) && ((Part) p).isOmniPodded()) {
                    if (!(p instanceof AmmoBin) || salvaging) {
                        MissingPart m = p.getMissingPart();
                        if ((m != null) && m.isReplacementAvailable()) {
                            continue;
                        }
                    }
                }

                temp.add(p);
            }

            parts = temp;
        }

        if (techs.isEmpty()) {
            return new MRMSUnitAction(unit, salvaging, MRMSUnitAction.STATUS.NO_TECHS);
        }

        /*
         * If we're a mek and we have a limb with a bad shoulder/hip, we're
         * going to try to flip it to salvageable and remove all the parts so
         * that we can nuke the limb. If we do this, when we're finally done we
         * need to flip the mek back to repairable so that we don't accidentally
         * strip everything off it.
         */
        boolean scrappingLimbMode = false;

        /*
         * Pre checking for hips/shoulders on repairable meks. If we have a bad
         * hip or shoulder, we're not going to do anything until we get those
         * parts out of the location and scrap it. Once we're at a happy place,
         * we'll proceed.
         */

        if ((unit.getEntity() instanceof Mech)) {
            Map<Integer, Part> locationMap = new HashMap<>();

            for (IPartWork partWork : parts) {
                if ((partWork instanceof MekLocation) && ((MekLocation) partWork).onBadHipOrShoulder()) {
                    locationMap.put(((MekLocation) partWork).getLoc(), (MekLocation) partWork);
                } else if (partWork instanceof MissingMekLocation) {
                    locationMap.put(partWork.getLocation(), (MissingMekLocation) partWork);
                }
            }

            if (!locationMap.isEmpty()) {
                MRMSOption mrmsOption = mrmsOptionsByType.get(PartRepairType.GENERAL_LOCATION);

                if ((null == mrmsOption) || !mrmsOption.isActive()) {
                    return new MRMSUnitAction(unit, salvaging, MRMSUnitAction.STATUS.UNFIXABLE_LIMB);
                }

                /*
                 * Find our parts in our bad locations. If we don't actually
                 * have, just scrap the limbs and move on with our normal work
                 */

                scrappingLimbMode = true;

                if (!salvaging) {
                    unit.setSalvage(true);
                }

                List<IPartWork> partsTemp = unit.getPartsNeedingService(true);
                List<IPartWork> partsToBeRemoved = new ArrayList<>();
                Map<Integer, Integer> countOfPartsPerLocation = new HashMap<>();

                for (IPartWork partWork : partsTemp) {
                    if (!(partWork instanceof MekLocation) && !(partWork instanceof MissingMekLocation)
                            && locationMap.containsKey(partWork.getLocation()) && partWork.isSalvaging()) {
                        partsToBeRemoved.add(partWork);

                        int count = 0;

                        if (countOfPartsPerLocation.containsKey(partWork.getLocation())) {
                            count = countOfPartsPerLocation.get(partWork.getLocation());
                        }

                        count++;

                        countOfPartsPerLocation.put(partWork.getLocation(), count);
                    }
                }

                if (partsToBeRemoved.isEmpty()) {
                    /*
                     * We have no parts left on our unfixable locations, so
                     * we'll just scrap those locations and rebuild the parts
                     * list and reset back our normal repair mode
                     */

                    for (Part part : locationMap.values()) {
                        if (part instanceof MekLocation) {
                            campaign.addReport(part.scrap());
                        }
                    }

                    scrappingLimbMode = false;

                    if (!salvaging) {
                        unit.setSalvage(false);
                    }

                    parts = unit.getPartsNeedingService(true);
                } else {
                    for (int locId : countOfPartsPerLocation.keySet()) {
                        boolean unfixable = false;
                        Part loc = null;

                        if (locationMap.containsKey(locId)) {
                            loc = locationMap.get(locId);
                            unfixable = (loc instanceof MekLocation);
                        }

                        if (unfixable) {
                            campaign.addReport(String.format(
                                    "<font color='orange'>Found an unfixable limb (%s) on %s which contains %s parts. Going to remove all parts and scrap the limb before proceeding with other repairs.</font>",
                                    loc.getName(), unit.getName(), countOfPartsPerLocation.get(locId)));
                        } else {
                            campaign.addReport(String.format(
                                    "<font color='orange'>Found missing location (%s) on %s which contains %s parts. Going to remove all parts before proceeding with other repairs.</font>",
                                    loc != null ? loc.getName() : Integer.toString(locId), unit.getName(), countOfPartsPerLocation.get(locId)));
                        }
                    }

                    parts = partsToBeRemoved;
                }
            }
        }

        boolean originalAllowCarryover = configuredOptions.isAllowCarryover();

        /*
         * If we're scrapping limbs, we don't want salvage repairs to go into a
         * new day otherwise it can be confusing when trying to figure why a
         * unit can't be repaired because 'salvage' repairs don't show up on the
         * task list as scheduled if we're in 'repair' mode.
         */
        if (scrappingLimbMode) {
            configuredOptions.setAllowCarryover(false);
        }

        /*
         * Filter our parts list to only those that aren't being worked on or
         * those that meet our criteria as defined in the campaign
         * configurations
         */
        parts = filterParts(parts, mrmsOptionsByType, techs, campaign);

        if (parts.isEmpty()) {
            if (scrappingLimbMode) {
                unit.setSalvage(false);
            }

            return new MRMSUnitAction(unit, salvaging, MRMSUnitAction.STATUS.NO_PARTS);
        }

        MRMSUnitAction unitAction = new MRMSUnitAction(unit, salvaging,
                MRMSUnitAction.STATUS.ACTIONS_PERFORMED);

        for (IPartWork partWork : parts) {
            if (partWork instanceof Part) {
                ((Part) partWork).resetModeToNormal();
            }

            List<Person> validTechs = filterTechs(partWork, techs, mrmsOptionsByType, false, campaign);

            if (validTechs.isEmpty()) {
                unitAction.addPartAction(MRMSPartAction.createNoTechs(partWork));
                continue;
            }

            unitAction.addPartAction(repairPart(campaign, partWork, unit, validTechs,
                    mrmsOptionsByType, configuredOptions, false));
        }

        if (scrappingLimbMode) {
            unit.setSalvage(false);
            configuredOptions.setAllowCarryover(originalAllowCarryover);
        }

        if (unitAction.getPartSet().isOnlyNoTechs()) {
            unitAction.resetPartSet();
            unitAction.setStatus(STATUS.NO_TECHS);
        }

        return unitAction;
    }

    private static MRMSPartAction repairPart(Campaign campaign, IPartWork partWork, Unit unit,
                                             List<Person> techs,
                                             Map<PartRepairType, MRMSOption> mrmsOptionsByType,
                                             MRMSConfiguredOptions configuredOptions,
                                             boolean warehouseMode) {
        // We were doing this check for every tech, that's unnecessary as it
        // doesn't change from tech to tech
        MRMSOption mrmsOptions = mrmsOptionsByType.get(IPartWork.findCorrectMRMSType(partWork));

        if (mrmsOptions == null) {
            return MRMSPartAction.createOptionDisabled(partWork);
        }

        long repairPartTime = System.nanoTime();

        TechSorter sorter = new TechSorter(partWork);
        Map<String, WorkTime> techSkillToWorktimeMap = new HashMap<>();
        List<Person> sameDayTechs = new ArrayList<>();
        List<Person> overflowDayTechs = new ArrayList<>();
        List<Person> sameDayAssignedTechs = new ArrayList<>();
        List<Person> overflowDayAssignedTechs = new ArrayList<>();
        int highestAvailableTechSkill = -1;

        for (Person tech : techs) {
            Skill skill = tech.getSkillForWorkingOn(partWork);

            if (skill.getExperienceLevel() > highestAvailableTechSkill) {
                highestAvailableTechSkill = skill.getExperienceLevel();
            }

            if (highestAvailableTechSkill == SkillType.EXP_ELITE) {
                break;
            }
        }

        debugLog("Starting with %s techs on %s", "repairPart", techs.size(), partWork.getPartName());

        boolean canChangeWorkTime = (partWork instanceof Part) && partWork.canChangeWorkMode();

        for (int i = techs.size() - 1; i >= 0; i--) {
            long time = System.nanoTime();

            Person tech = techs.get(i);

            debugLog("Checking tech %s", "repairPart", tech.getFullName());

            Skill skill = tech.getSkillForWorkingOn(partWork);

            if (partWork instanceof Part) {
                ((Part) partWork).resetModeToNormal();
            }

            // We really only have to check one tech of each skill level
            if (!techSkillToWorktimeMap.containsKey(skill.getType().getName() + "-" + skill.getLevel())) {
                TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);
                WorkTime selectedWorktime = null;

                // Check if we need to increase the time to meet the min BTH
                if (targetRoll.getValue() > mrmsOptions.getBthMin()) {
                    if (!configuredOptions.isUseExtraTime()) {
                        debugLog("... can't increase time to reach BTH due to configuration", "repairPart");
                        continue;
                    } else if (!canChangeWorkTime) {
                        debugLog("... can't increase time because this part can not have it's workMode changed", "repairPart");
                        continue;
                    }

                    WorkTimeCalculation workTimeCalc = calculateNewMRMSWorktime(partWork, tech,
                            mrmsOptions, campaign, true, highestAvailableTechSkill);

                    if (workTimeCalc.getWorkTime() == null) {
                        if (workTimeCalc.isReachedMaxSkill()) {
                            debugLog("... can't increase time enough to reach BTH with max available tech", "repairPart");

                            return MRMSPartAction.createMaxSkillReached(partWork, highestAvailableTechSkill,
                                    mrmsOptions.getBthMin());
                        } else {
                            debugLog("... can't increase time enough to reach BTH", "repairPart");

                            continue;
                        }
                    }

                    selectedWorktime = workTimeCalc.getWorkTime();
                } else if (targetRoll.getValue() < mrmsOptions.getBthMax()) {
                    // Or decrease the time to meet the max BTH
                    if (configuredOptions.isUseRushJob() && canChangeWorkTime) {
                        WorkTimeCalculation workTimeCalc = calculateNewMRMSWorktime(partWork, tech, mrmsOptions, campaign,
                                false, highestAvailableTechSkill);

                        if (null == workTimeCalc.getWorkTime()) {
                            selectedWorktime = WorkTime.NORMAL;
                        } else {
                            selectedWorktime = workTimeCalc.getWorkTime();
                        }
                    }
                }

                techSkillToWorktimeMap.put(skill.getType().getName() + "-" + skill.getLevel(), selectedWorktime);

                if (partWork instanceof Part) {
                    ((Part) partWork).resetModeToNormal();
                }
            }

            boolean assigned = false;

            if ((unit != null) && configuredOptions.isUseAssignedTechsFirst()) {
                Force force = campaign.getForce(unit.getForceId());

                if ((force != null) && (force.getTechID()) != null) {
                    assigned = force.getTechID().toString().equals(tech.getId().toString());
                }

                if (!assigned && !tech.getTechUnits().isEmpty()) {
                    assigned = tech.getTechUnits().contains(unit);
                }
            }

            boolean isSameDayTech;

            if ((tech.getMinutesLeft() < partWork.getActualTime())) {
                if (!configuredOptions.isAllowCarryover()) {
                    debugLog("... would carry over day and configuration doesn't allow", "repairPart");

                    continue;
                }

                isSameDayTech = !configuredOptions.isOptimizeToCompleteToday();
            } else {
                isSameDayTech = true;
            }

            if (isSameDayTech) {
                if (assigned) {
                    sameDayAssignedTechs.add(tech);
                } else {
                    sameDayTechs.add(tech);
                }
            } else {
                if (assigned) {
                    overflowDayAssignedTechs.add(tech);
                } else {
                    overflowDayTechs.add(tech);
                }
            }

            debugLog("... time to check tech: %s ns", "repairPart", (System.nanoTime() - time));
        }

        List<Person> validTechs = new ArrayList<>();

        if (!sameDayAssignedTechs.isEmpty()) {
            sameDayAssignedTechs.sort(sorter);
            validTechs.addAll(sameDayAssignedTechs);
        }

        if (!sameDayTechs.isEmpty()) {
            sameDayTechs.sort(sorter);
            validTechs.addAll(sameDayTechs);
        }

        if (!overflowDayAssignedTechs.isEmpty()) {
            overflowDayAssignedTechs.sort(sorter);
            validTechs.addAll(overflowDayAssignedTechs);
        }

        if (!overflowDayTechs.isEmpty()) {
            overflowDayTechs.sort(sorter);
            validTechs.addAll(overflowDayTechs);
        }

        if (validTechs.isEmpty()) {
            debugLog("Ending because there are no techs", "repairPart");

            return MRMSPartAction.createNoTechs(partWork);
        }

        Person tech = validTechs.get(0);

        if (partWork instanceof Part) {
            Skill skill = tech.getSkillForWorkingOn(partWork);
            WorkTime wt = techSkillToWorktimeMap.get(skill.getType().getName() + "-" + skill.getLevel());

            if (null == wt) {
                debugLog("[ERROR] Null work-time from techToWorktimeMap for %s", "repairPart", tech.getFullName());
                wt = WorkTime.NORMAL;
            }

            ((Part) partWork).setMode(wt);
        }

        if (warehouseMode && (partWork instanceof Part)) {
            campaign.fixWarehousePart((Part) partWork, tech);
        } else {
            campaign.fixPart(partWork, tech);
        }

        // If this tech has no time left, filter them out so we don't
        // spend cycles on them in the future
        if (tech.getMinutesLeft() <= 0) {
            techs.remove(tech);
        }

        Thread.yield();

        debugLog("Ending after %s ns", "repairPart", System.nanoTime() - repairPartTime);

        return MRMSPartAction.createRepaired(partWork);
    }

    private static List<IPartWork> filterParts(List<IPartWork> parts, Map<PartRepairType, MRMSOption> mrmsOptionsByType,
                                               List<Person> techs, Campaign campaign) {
        List<IPartWork> newParts = new ArrayList<>();

        if (techs.isEmpty() || parts.isEmpty()) {
            return newParts;
        }

        Map<String, Person> techCache = new HashMap<>();

        for (IPartWork partWork : parts) {
            if (partWork.isBeingWorkedOn()) {
                continue;
            }

            if ((partWork instanceof MissingPart) && !((MissingPart) partWork).isReplacementAvailable()) {
                continue;
            }

            if (mrmsOptionsByType != null) {
                PartRepairType repairType = IPartWork.findCorrectMRMSType(partWork);

                MRMSOption mrmsOption = mrmsOptionsByType.get(repairType);

                if ((mrmsOption == null) || !mrmsOption.isActive()) {
                    continue;
                }
            }

            if (!checkArmorSupply(partWork)) {
                continue;
            }

            // See if this part is blocked or can be dealt with
            // Find an appropriate tech and get their skill then create an
            // elite tech with the same skill
            Skill partSkill = null;

            for (Person techExisting : techs) {
                partSkill = techExisting.getSkillForWorkingOn(partWork);

                if (partSkill != null) {
                    break;
                }
            }

            if (partSkill == null) {
                continue;
            }

            String skillName = partSkill.getType().getName();

            // Find a tech in our placeholder cache
            Person tech = techCache.get(skillName);

            if (null == tech) {
                // Create a dummy elite tech with the proper skill and 1
                // minute and put it in our cache for later use

                tech = new Person("Temp", String.format("Tech (%s)", skillName), campaign);
                tech.addSkill(skillName, partSkill.getType().getEliteLevel(), 1);
                tech.setMinutesLeft(1);

                techCache.put(skillName, tech);
            }

            TargetRoll roll = campaign.getTargetFor(partWork, tech);

            if ((roll.getValue() == TargetRoll.IMPOSSIBLE) || (roll.getValue() == TargetRoll.AUTOMATIC_FAIL)
                    || (roll.getValue() == TargetRoll.CHECK_FALSE)) {
                continue;
            }

            newParts.add(partWork);
        }

        return newParts;
    }

    private static List<Person> filterTechs(IPartWork partWork, List<Person> techs,
                                            Map<PartRepairType, MRMSOption> mrmsOptionsByType,
                                            boolean warehouseMode, Campaign campaign) {
        List<Person> validTechs = new ArrayList<>();

        if (techs.isEmpty()) {
            return validTechs;
        }

        MRMSOption mrmsOption = mrmsOptionsByType.get(IPartWork.findCorrectMRMSType(partWork));

        if (null == mrmsOption) {
            return validTechs;
        }

        for (int i = techs.size() - 1; i >= 0; i--) {
            Person tech = techs.get(i);

            if (tech.getMinutesLeft() <= 0) {
                continue;
            }

            if (warehouseMode && !tech.isRightTechTypeFor(partWork)) {
                continue;
            }

            Skill skill = tech.getSkillForWorkingOn(partWork);

            if (skill == null) {
                continue;
            }

            if (mrmsOption.getSkillMin() > skill.getExperienceLevel()) {
                continue;
            }

            if (mrmsOption.getSkillMax() < skill.getExperienceLevel()) {
                continue;
            }

            if (partWork.getSkillMin() > skill.getExperienceLevel()) {
                continue;
            }

            // Check if we can actually even repair this part
            TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);

            if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE)
                    || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
                    || (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
                continue;
            }

            validTechs.add(tech);
        }

        return validTechs;
    }

    private static boolean checkArmorSupply(IPartWork part) {
        if (part.isSalvaging()) {
            return true;
        }

        return (!(part instanceof Armor)) || ((Armor) part).isInSupply();
    }

    private static WorkTimeCalculation calculateNewMRMSWorktime(IPartWork partWork,
                                                                Person tech,
                                                                MRMSOption mrmsOption,
                                                                Campaign campaign,
                                                                boolean increaseTime,
                                                                int highestAvailableTechSkill) {
        long time = System.nanoTime();

        debugLog("...... starting calculateNewMRMSWorktime", "calculateNewMRMSWorktime");

        if (partWork instanceof Part) {
            ((Part) partWork).resetModeToNormal();
        }

        TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);

        if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
                || (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
            debugLog("...... ending calculateNewMRMSWorktime due to impossible role - %s ns", "calculateNewMRMSWorktime",
                    System.nanoTime() - time);

            return new WorkTimeCalculation();
        }

        WorkTime newWorkTime = partWork.getMode();
        WorkTime previousNewWorkTime;

        Skill skill = tech.getSkillForWorkingOn(partWork);

        while (null != newWorkTime) {
            previousNewWorkTime = newWorkTime;
            newWorkTime = newWorkTime.moveTimeToNextLevel(increaseTime);

            debugLog("...... looping workTime check. NewWorkTime: %s, PreviousWorkTime: %s", "calculateNewMRMSWorktime",
                    (null == newWorkTime ? "NULL" : newWorkTime.name()), previousNewWorkTime.name());

            // If we're trying to a rush a job, our effective skill goes down
            // Let's make sure we don't put it so high that we can't fix it
            // anymore
            if (!increaseTime) {
                int modePenalty = partWork.getMode().expReduction;

                if (partWork.getSkillMin() > (skill.getExperienceLevel() - modePenalty)) {
                    debugLog(
                            "...... ending calculateNewMRMSWorktime with previousWorkTime due time reduction skill mod now being less that required skill - %s ns", "calculateNewMRMSWorktime",
                            System.nanoTime() - time);

                    return new WorkTimeCalculation(previousNewWorkTime);
                }
            }

            // If we have a null newWorkTime, we're done. Use the previous one.
            if (null == newWorkTime) {
                debugLog("...... ending calculateNewMRMSWorktime because newWorkTime is null - %s ns", "calculateNewMRMSWorktime",
                        System.nanoTime() - time);

                if (!increaseTime) {
                    return new WorkTimeCalculation(previousNewWorkTime);
                }

                WorkTimeCalculation wtc = new WorkTimeCalculation(null);

                if (skill.getExperienceLevel() >= highestAvailableTechSkill) {
                    wtc.setReachedMaxSkill(true);
                }

                return wtc;
            }

            // Set our new workTime and calculate the new targetRoll
            if (partWork instanceof Part) {
                ((Part) partWork).setMode(newWorkTime);
            }

            targetRoll = campaign.getTargetFor(partWork, tech);

            // If our roll is impossible, revert to the previous one
            if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) || (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL)
                    || (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
                debugLog("...... ending calculateNewMRMSWorktime due to impossible role - %s ns", "calculateNewMRMSWorktime",
                        System.nanoTime() - time);

                return new WorkTimeCalculation(previousNewWorkTime);
            }

            if (increaseTime) {
                // If we've reached our BTH, kick out. Otherwise we'll loop
                // around again
                if (targetRoll.getValue() <= mrmsOption.getBthMin()) {
                    debugLog(
                            "...... ending calculateNewMRMSWorktime because we have reached our BTH goal - %s ns", "calculateNewMRMSWorktime",
                            System.nanoTime() - time);

                    return new WorkTimeCalculation(newWorkTime);
                }
            } else {
                if (targetRoll.getValue() > mrmsOption.getBthMax()) {
                    debugLog(
                            "...... ending calculateNewMRMSWorktime because we have reached our BTH goal - %s ns", "calculateNewMRMSWorktime",
                            System.nanoTime() - time);

                    return new WorkTimeCalculation(previousNewWorkTime);
                } else if (targetRoll.getValue() > mrmsOption.getBthMax()) {
                    debugLog(
                            "...... ending calculateNewMRMSWorktime because we have reached our BTH goal - %s ns", "calculateNewMRMSWorktime",
                            System.nanoTime() - time);

                    return new WorkTimeCalculation(newWorkTime);
                }
            }
        }

        return new WorkTimeCalculation();
    }

    private static void debugLog(String msg, String methodName, Object... replacements) {
        if ((null != replacements) && (replacements.length > 0)) {
            msg = String.format(msg, replacements);
        }

        LogManager.getLogger().debug(msg);
    }

    private static class WorkTimeCalculation {
        private WorkTime workTime = WorkTime.NORMAL;
        private boolean reachedMaxSkill = false;

        public WorkTimeCalculation() {

        }

        public WorkTimeCalculation(WorkTime workTime) {
            this.workTime = workTime;
        }

        public WorkTime getWorkTime() {
            return workTime;
        }

        public boolean isReachedMaxSkill() {
            return reachedMaxSkill;
        }

        public void setReachedMaxSkill(boolean reachedMaxSkill) {
            this.reachedMaxSkill = reachedMaxSkill;
        }
    }

    private static class TechSorter implements Comparator<Person> {
        private IPartWork partWork;

        public TechSorter(IPartWork _part) {
            this.partWork = _part;
        }

        @Override
        public int compare(Person tech1, Person tech2) {
            // Sort the valid techs by applicable skill. Let's start with the least experienced and
            // work our way up until we find someone who can perform the work. If we have two techs
            // with the same skill, put the one with the lesser XP in the front. If we have tech
            // with the same XP, put the one with the more time ahead.
            Skill skill1 = tech1.getSkillForWorkingOn(partWork);
            Skill skill2 = tech2.getSkillForWorkingOn(partWork);

            if (skill1.getExperienceLevel() == skill2.getExperienceLevel()) {
                if ((tech1.getXP() == tech2.getXP()) || (skill1.getLevel() == SkillType.EXP_ELITE)) {
                    return tech1.getMinutesLeft() - tech2.getMinutesLeft();
                } else {
                    return (tech1.getXP() < tech2.getXP()) ? -1 : 1;
                }
            }

            return skill1.getExperienceLevel() < skill2.getExperienceLevel() ? -1 : 1;
        }
    }

    public static class MRMSPartAction {
        public enum STATUS {
            REPAIRED, MAX_SKILL_REACHED, MRO_DISABLED, NO_TECHS
        }

        private IPartWork partWork;
        private STATUS status;
        private int maxTechSkill;
        private int configuredBTHMin;

        public MRMSPartAction(IPartWork partWork) {
            this.partWork = partWork;
        }

        public MRMSPartAction(IPartWork partWork, STATUS status) {
            this(partWork);

            this.status = status;
        }

        public IPartWork getPartWork() {
            return partWork;
        }

        public void setPartWork(IPartWork partWork) {
            this.partWork = partWork;
        }

        public STATUS getStatus() {
            return status;
        }

        public void setStatus(STATUS status) {
            this.status = status;
        }

        public boolean isStatusRepaired() {
            return status == STATUS.REPAIRED;
        }

        public boolean isStatusMaxSkillReached() {
            return status == STATUS.MAX_SKILL_REACHED;
        }

        public boolean isStatusOptionDisabled() {
            return status == STATUS.MRO_DISABLED;
        }

        public boolean isStatusNoTechs() {
            return status == STATUS.NO_TECHS;
        }

        public int getMaxTechSkill() {
            return maxTechSkill;
        }

        public void setMaxTechSkill(int maxTechSkill) {
            this.maxTechSkill = maxTechSkill;
        }

        public int getConfiguredBTHMin() {
            return configuredBTHMin;
        }

        public void setConfiguredBTHMin(int configuredBTHMin) {
            this.configuredBTHMin = configuredBTHMin;
        }

        public static MRMSPartAction createRepaired(IPartWork partWork) {
            return new MRMSPartAction(partWork, STATUS.REPAIRED);
        }

        public static MRMSPartAction createMaxSkillReached(final IPartWork partWork,
                                                           final int maxSkill, final int bthMin) {
            final MRMSPartAction mrmsPartAction = new MRMSPartAction(partWork, STATUS.MAX_SKILL_REACHED);
            mrmsPartAction.setMaxTechSkill(maxSkill);
            mrmsPartAction.setConfiguredBTHMin(bthMin);
            return mrmsPartAction;
        }

        public static MRMSPartAction createOptionDisabled(final IPartWork partWork) {
            return new MRMSPartAction(partWork, STATUS.MRO_DISABLED);
        }

        public static MRMSPartAction createNoTechs(final IPartWork partWork) {
            return new MRMSPartAction(partWork, STATUS.NO_TECHS);
        }
    }

    public static class MRMSPartSet {
        private Map<MRMSPartAction.STATUS, List<MRMSPartAction>> partActionsByStatus = new HashMap<>();

        public void addPartAction(MRMSPartAction partAction) {
            if (partAction == null) {
                return;
            }

            List<MRMSPartAction> list = partActionsByStatus.computeIfAbsent(partAction.getStatus(), k -> new ArrayList<>());

            list.add(partAction);
        }

        public Map<MRMSPartAction.STATUS, List<MRMSPartAction>> getPartActions() {
            return partActionsByStatus;
        }

        public boolean isHasRepairs() {
            return partActionsByStatus.containsKey(MRMSPartAction.STATUS.REPAIRED);
        }

        public int countRepairs() {
            if (!isHasRepairs()) {
                return 0;
            }

            return partActionsByStatus.get(MRMSPartAction.STATUS.REPAIRED).size();
        }

        public boolean isOnlyNoTechs() {
            if (!partActionsByStatus.containsKey(MRMSPartAction.STATUS.NO_TECHS)) {
                return false;
            }

            return partActionsByStatus.size() <= 1;
        }
    }

    public static class MRMSUnitAction {
        public enum STATUS {
            NO_ACTIONS, ACTIONS_PERFORMED, NO_TECHS, UNFIXABLE_LIMB, NO_PARTS, ALL_PARTS_IN_PROCESS
        }

        private Unit unit;
        private MRMSPartSet partSet = new MRMSPartSet();
        private STATUS status;
        private boolean salvaging;

        public MRMSUnitAction(Unit unit, boolean salvaging, STATUS status) {
            this.unit = unit;
            this.salvaging = salvaging;
            this.status = status;
        }

        public Unit getUnit() {
            return unit;
        }

        public void setUnit(Unit unit) {
            this.unit = unit;
        }

        public MRMSPartSet getPartSet() {
            return partSet;
        }

        public void setPartSet(MRMSPartSet partSet) {
            this.partSet = partSet;
        }

        public STATUS getStatus() {
            return status;
        }

        public void setStatus(STATUS status) {
            this.status = status;
        }

        public boolean isSalvaging() {
            return salvaging;
        }

        public void setSalvaging(boolean salvaging) {
            this.salvaging = salvaging;
        }

        public boolean isStatusNoActions() {
            return status == STATUS.NO_ACTIONS;
        }

        public boolean isStatusActionsPerformed() {
            return status == STATUS.ACTIONS_PERFORMED;
        }

        public boolean isStatusNoTechs() {
            return status == STATUS.NO_TECHS;
        }

        public boolean isStatusUnfixableLimb() {
            return status == STATUS.UNFIXABLE_LIMB;
        }

        public boolean isStatusNoParts() {
            return status == STATUS.NO_PARTS;
        }

        public void addPartAction(MRMSPartAction partAction) {
            partSet.addPartAction(partAction);
        }

        public void resetPartSet() {
            partSet = new MRMSPartSet();
        }

        public void merge(MRMSUnitAction currentUnitAction) {
            for (List<MRMSPartAction> partActionList : currentUnitAction.getPartSet().getPartActions().values()) {
                for (MRMSPartAction partAction : partActionList) {
                    getPartSet().addPartAction(partAction);
                }
            }
        }
    }
}
