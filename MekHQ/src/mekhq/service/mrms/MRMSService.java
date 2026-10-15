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
package mekhq.service.mrms;

import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Formation;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingMekLocation;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.RepairLocationCheck;
import mekhq.campaign.work.RepairTechEligibility;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.service.mrms.MRMSService.MRMSUnitAction.STATUS;
import mekhq.utilities.ReportingUtilities;

public class MRMSService {
    private static final MMLogger LOGGER = MMLogger.create(MRMSService.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.MRMS";
    @Deprecated(since = "0.50.10")
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_BUNDLE,
          MekHQ.getMHQOptions().getLocale());

    private MRMSService() {

    }

    public static boolean isValidMRMSUnit(Unit unit, MRMSConfiguredOptions configuredOptions) {
        boolean isRepairOff = !unit.isSalvage() && !configuredOptions.useRepair();
        boolean isSalvageOff = unit.isSalvage() && !configuredOptions.useSalvage();
        if (isRepairOff || isSalvageOff) {
            return false;
        }
        return isMassRepairableUnit(unit);
    }

    /**
     * Whether Mass Repair can work on a unit at all. The Mass Repair dialog, Instant and New Day Mass Repair and the
     * right-click menu all ask this, so they offer the same units. A unit qualifies when it is a type a tech can be
     * assigned to, the same types {@link Person#canTech} accepts. A DropShip, JumpShip, WarShip or space station
     * qualifies too: its engineer does the work, as in the Repair tab. So does conventional infantry: when it maintains
     * itself its ranking soldier does the work, such as reloading its field guns, and otherwise a Mechanic does.
     *
     * @param unit the unit
     *
     * @return {@code true} if Mass Repair can work on the unit
     */
    public static boolean isMassRepairableUnit(Unit unit) {
        Entity entity = unit.getEntity();
        boolean isMekTechWork = (entity instanceof Mek) || (entity instanceof ProtoMek)
                                      || (entity instanceof HandheldWeapon);
        boolean isOtherTechWork = (entity instanceof Aero) || (entity instanceof BattleArmor)
                                        || (entity instanceof Tank);
        boolean isConventionalInfantry = (entity != null) && entity.isConventionalInfantry();
        return isMekTechWork || isOtherTechWork || isConventionalInfantry;
    }

    public static MRMSPartSet performWarehouseMRMS(List<IPartWork> selectedParts,
          MRMSConfiguredOptions configuredOptions, Campaign campaign) {
        if (!configuredOptions.useRepair()) { // Warehouse only uses repair
            campaign.addReport(TECHNICAL, resources.getString("MRMS.CompleteDisabled.report"));
            return new MRMSPartSet();
        }
        campaign.addReport(TECHNICAL, resources.getString("MRMS.StartWarehouse.report"));

        List<Person> techs = getTechsWithTimeLeftToday(campaign);

        MRMSPartSet partSet = new MRMSPartSet();

        if (techs.isEmpty()) {
            campaign.addReport(TECHNICAL, resources.getString("MRMS.NoAvailableTechs.report"));
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
                    WorkTime playerChosenMode = part.getMode();
                    part.resetModeToNormal();

                    List<Person> validTechs = filterTechs(partWork, techs, mrmsOptionsByType, campaign);

                    if (validTechs.isEmpty()) {
                        restorePlayerChosenMode(part, playerChosenMode);
                        continue;
                    }

                    int originalQuantity = part.getQuantity();

                    for (int i = 0; i < originalQuantity; i++) {
                        partSet.addPartAction(repairPart(campaign,
                              part,
                              null,
                              validTechs,
                              mrmsOptionsByType,
                              configuredOptions,
                              true));
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
            campaign.addReport(TECHNICAL, msg);
            return msg;
        } else if ((!unit.isSalvage() && !configuredOptions.useRepair()) ||
                         (unit.isSalvage() && !configuredOptions.useSalvage())) {
            String message = getFormattedTextAt(RESOURCE_BUNDLE, "MRMS.CompleteTypeDisabled.report",
                  unit.isSalvage() ? getTextAt(RESOURCE_BUNDLE, "Salvage") : getTextAt(RESOURCE_BUNDLE, "Repair"));
            campaign.addReport(TECHNICAL, message);
            return message;
        } else if (campaign.getPlayerForce()
                         .getHumanResources()
                         .requiresAdditionalAsTechs(campaign.getCampaignOptions())) {
            String message = resources.getString("MRMS.InsufficientAstechs.report");
            campaign.addReport(TECHNICAL, message);
            return message;
        }

        List<MRMSOption> activeMRMSOptions = configuredOptions.getActiveMRMSOptions();
        MRMSUnitAction unitAction = performUnitMRMS(campaign,
              unit,
              unit.isSalvage(),
              activeMRMSOptions,
              configuredOptions);

        String actionDescriptor = unit.isSalvage() ? resources.getString("Salvage") : resources.getString("Repair");
        String msg = getFormattedTextAt(RESOURCE_BUNDLE, "MRMS.UnitComplete.report",
              ReportingUtilities.getPositiveColor(), actionDescriptor, unit.getName());

        String statusKey = switch (unitAction.getStatus()) {
            case ACTIONS_PERFORMED -> "MRMS.UnitActionsPerformed.report";
            case NO_PARTS -> "MRMS.UnitNoMatchingParts.report";
            case ALL_PARTS_IN_PROCESS -> "MRMS.UnitAllPartsInProcess.report";
            case NO_TECHS -> "MRMS.UnitNoTechs.report";
            case UNFIXABLE_LIMB -> "MRMS.UnitUnfixableLimb.report";
            case NO_ACTIONS -> null;
        };
        if (statusKey != null) {
            msg += ' ' + getFormattedTextAt(RESOURCE_BUNDLE, statusKey, unitAction.getPartSet().countRepairs());
        }
        if (unitAction.isStatusNoTechs()) {
            String toolKitNote = describeTechsWithoutToolKit(campaign, unit);
            if (!toolKitNote.isEmpty()) {
                msg += ' ' + toolKitNote;
            }
        }

        campaign.addReport(TECHNICAL, msg);

        List<Person> techs = campaign.getPlayerForce()
                                   .getHumanResources()
                                   .getTechs(campaign.getPlayerForce().getHangar().getUnits(),
                                         campaign.getCampaignOptions(),
                                         campaign.getPlayerForce().isClanForce(),
                                         campaign.getLocalDate());

        if (!techs.isEmpty()) {
            List<IPartWork> parts = unit.getPartsNeedingService(true);
            parts = filterParts(parts, null, techs, campaign);

            if (!parts.isEmpty()) {
                String color = spanOpeningWithCustomColor(getWarningColor());
                if (parts.size() == 1) {
                    campaign.addReport(TECHNICAL,
                          getFormattedTextAt(RESOURCE_BUNDLE, "inProgress.one", color, CLOSING_SPAN_TAG));
                } else {
                    campaign.addReport(TECHNICAL,
                          getFormattedTextAt(RESOURCE_BUNDLE, "inProgress.many", color, parts.size(),
                                CLOSING_SPAN_TAG));
                }
            }
        }

        return String.format("Mass %s complete on %s.", actionDescriptor, unit.getName());
    }

    public static void performSingleLocationMRMS(Campaign campaign, Unit unit, IPartWork part) {
        MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(campaign);
        List<Person> techs = getTechsWithTimeLeftToday(campaign);
        // Quick Strip is a direct order on one location, so the force-wide Use Repair and Use Salvage switches do not
        // apply to it
        LOGGER.debug("[MRMS] Quick Strip of {} on {}", part.getPartName(), unit.getName());
        if (campaign.getPlayerForce()
                         .getHumanResources()
                         .requiresAdditionalAsTechs(campaign.getCampaignOptions())) {
            String message = getTextAt(RESOURCE_BUNDLE, "MRMS.InsufficientAstechs.report");
            campaign.addReport(TECHNICAL, message);
            return;
        }

        for (int i = techs.size() - 1; i >= 0; i--) {
            Person tech = techs.get(i);

            if (!tech.canTech(unit.getEntity())) {
                techs.remove(i);
            }
        }

        List<MRMSOption> mrmsOptions = configuredOptions.getActiveMRMSOptions();
        Map<PartRepairType, MRMSOption> mrmsOptionsByType = new HashMap<>();
        for (MRMSOption mrmsOption : mrmsOptions) {
            mrmsOptionsByType.put(mrmsOption.getType(), mrmsOption);
        }

        boolean performMoreRepairs = true;
        while (performMoreRepairs) {
            List<IPartWork> partsList = new ArrayList<>();
            partsList.add(part);

            MRMSUnitAction currentUnitAction = scrapLocationAndRemoveEquipment(campaign,
                  unit,
                  techs,
                  mrmsOptionsByType,
                  configuredOptions,
                  partsList);

            performMoreRepairs = currentUnitAction.getPartSet().isHasRepairs();
        }
    }

    public static void mrmsAllUnits(Campaign campaign) {
        MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(campaign);
        if (!configuredOptions.isEnabled()) {
            campaign.addReport(TECHNICAL, resources.getString("MRMS.CompleteDisabled.report"));
            return;
        } else if (campaign.getPlayerForce()
                         .getHumanResources()
                         .requiresAdditionalAsTechs(campaign.getCampaignOptions())) {
            campaign.addReport(TECHNICAL, resources.getString("MRMS.InsufficientAstechs.report"));
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

    public static void mrmsUnits(Campaign campaign, List<Unit> units, MRMSConfiguredOptions configuredOptions) {
        // This shouldn't happen but is being added preventatively
        if (!configuredOptions.isEnabled()) {
            campaign.addReport(TECHNICAL, resources.getString("MRMS.CompleteDisabled.report"));
            return;
        }
        Map<MRMSUnitAction.STATUS, List<MRMSUnitAction>> unitActionsByStatus = new HashMap<>();
        List<MRMSOption> activeMRMSOptions = configuredOptions.getActiveMRMSOptions();

        for (Unit unit : units) {
            MRMSUnitAction unitAction = performUnitMRMS(campaign,
                  unit,
                  unit.isSalvage(),
                  activeMRMSOptions,
                  configuredOptions);

            List<MRMSUnitAction> list = unitActionsByStatus.computeIfAbsent(unitAction.getStatus(),
                  k -> new ArrayList<>());

            list.add(unitAction);
        }

        if (unitActionsByStatus.isEmpty()) {
            campaign.addReport(TECHNICAL, resources.getString("MRMS.CompleteNoUnits.report"));
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
                List<MRMSUnitAction> unitsByStatus = unitActionsByStatus.get(MRMSUnitAction.STATUS.ACTIONS_PERFORMED);

                for (MRMSUnitAction mrmsUnitAction : unitsByStatus) {
                    actionsPerformed += mrmsUnitAction.getPartSet().countRepairs();
                }
            }

            StringBuilder sb = new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE, "MRMS.Summary.report",
                  ReportingUtilities.getPositiveColor(), totalCount));

            if (actionsPerformed > 0) {
                sb.append(' ').append(getFormattedTextAt(RESOURCE_BUNDLE, "MRMS.Summary.actionsPerformed",
                      actionsPerformed));
            }

            sb.append(generateUnitRepairSummary("MRMS.Summary.unitsWorkedOn",
                  unitActionsByStatus,
                  MRMSUnitAction.STATUS.ACTIONS_PERFORMED));
            sb.append(generateUnitRepairSummary("MRMS.Summary.unitsWithNoMatchingParts",
                  unitActionsByStatus,
                  MRMSUnitAction.STATUS.NO_PARTS));
            sb.append(generateUnitRepairSummary("MRMS.Summary.unitsWithNoTechs",
                  unitActionsByStatus,
                  MRMSUnitAction.STATUS.NO_TECHS));
            if (unitActionsByStatus.containsKey(MRMSUnitAction.STATUS.NO_TECHS)) {
                String toolKitNote = describeTechsWithoutToolKit(campaign, null);
                if (!toolKitNote.isEmpty()) {
                    sb.append("<br/>").append(toolKitNote);
                }
            }
            sb.append(generateUnitRepairSummary("MRMS.Summary.unitsWithUnfixableLimbs",
                  unitActionsByStatus,
                  MRMSUnitAction.STATUS.UNFIXABLE_LIMB));

            campaign.addReport(TECHNICAL, sb.toString());
        }

        generateCampaignLogForUnitStatus(unitActionsByStatus,
              MRMSUnitAction.STATUS.NO_PARTS,
              getTextAt(RESOURCE_BUNDLE, "MRMS.UnitList.noMatchingParts"),
              campaign);
        generateCampaignLogForUnitStatus(unitActionsByStatus,
              MRMSUnitAction.STATUS.NO_TECHS,
              getTextAt(RESOURCE_BUNDLE, "MRMS.UnitList.noTechs"),
              campaign);
        generateCampaignLogForUnitStatus(unitActionsByStatus,
              MRMSUnitAction.STATUS.UNFIXABLE_LIMB,
              getTextAt(RESOURCE_BUNDLE, "MRMS.UnitList.unfixableLimbs"),
              campaign);

        if (!unitActionsByStatus.isEmpty()) {
            List<Person> techs = campaign.getPlayerForce()
                                       .getHumanResources()
                                       .getTechs(campaign.getPlayerForce().getHangar().getUnits(),
                                             campaign.getCampaignOptions(),
                                             campaign.getPlayerForce().isClanForce(),
                                             campaign.getLocalDate());

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
                    campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE, "MRMS.StillNotWorkedOn.report",
                          ReportingUtilities.getNegativeColor(), count, unitCount));
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

    /**
     * When Mass Repair found no valid techs and the campaign requires a tool kit, says how many techs carry none. Without
     * this the report only says there were no valid techs, and a tech holding a Descartes scanner looks equipped.
     *
     * @param campaign the campaign
     * @param unit     the unit worked on, or {@code null} for a summary across units
     *
     * @return the note, or an empty string when tool kits are not required or every tech carries one
     */
    private static String describeTechsWithoutToolKit(Campaign campaign, @Nullable Unit unit) {
        if (!EquipmentKitCatalog.isToolKitRequired(campaign.getCampaignOptions(), unit)) {
            return "";
        }
        List<Person> techs = campaign.getPlayerForce()
                                   .getHumanResources()
                                   .getTechs(campaign.getPlayerForce().getHangar().getUnits(),
                                         campaign.getCampaignOptions(),
                                         campaign.getPlayerForce().isClanForce(),
                                         campaign.getLocalDate());
        int techsWithoutToolKit = EquipmentKitCatalog.countWithoutToolKit(techs);
        LOGGER.debug("[MRMS] No valid techs; {} of {} techs carry no tool kit", techsWithoutToolKit, techs.size());
        if (techsWithoutToolKit == 0) {
            return "";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "MRMS.TechsWithoutToolKit.report", techsWithoutToolKit);
    }

    /**
     * @param resourceKey the summary line's key in the MRMS resources; it takes the number of units as {@code {0}}
     *
     * @return the summary line, on a new line, for the units that ended with the given status, or an empty string
     *       when none did
     */
    private static String generateUnitRepairSummary(String resourceKey,
          Map<MRMSUnitAction.STATUS, List<MRMSUnitAction>> unitActionsByStatus, MRMSUnitAction.STATUS status) {

        if (!unitActionsByStatus.containsKey(status)) {
            return "";
        }

        int count = unitActionsByStatus.get(status).size();

        return "<br/>" + getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, count);
    }

    private static void generateCampaignLogForUnitStatus(
          Map<MRMSUnitAction.STATUS, List<MRMSUnitAction>> unitActionsByStatus, MRMSUnitAction.STATUS status,
          String statusDesc, Campaign campaign) {
        if (!unitActionsByStatus.containsKey(status) || unitActionsByStatus.get(status).isEmpty()) {
            return;
        }

        StringBuilder sbMsg = new StringBuilder();
        sbMsg.append(statusDesc);

        List<MRMSUnitAction> unitsByStatus = unitActionsByStatus.get(status);

        for (MRMSUnitAction mrmsUnitAction : unitsByStatus) {
            sbMsg.append("<br/>- ").append(mrmsUnitAction.getUnit().getName());
        }

        campaign.addReport(TECHNICAL, sbMsg.toString());
    }

    private static MRMSUnitAction performUnitMRMS(Campaign campaign, Unit unit, boolean isSalvage,
          List<MRMSOption> mrmsOptions, MRMSConfiguredOptions configuredOptions) {
        List<Person> techs = unit.isSelfCrewed()
                                   ? getEngineerWithTimeLeftToday(campaign, unit)
                                   : getTechsWithTimeLeftToday(campaign);

        if (techs.isEmpty()) {
            return new MRMSUnitAction(unit, isSalvage, MRMSUnitAction.STATUS.NO_TECHS);
        }

        MRMSUnitAction unitAction = new MRMSUnitAction(unit, isSalvage, MRMSUnitAction.STATUS.NO_ACTIONS);

        // Filter our tech list to only our techs that can work on this unit. A self-crewed unit's engineer is its only
        // tech whatever their training, as in the Repair tab: infantry that maintains itself is led by a soldier
        if (!unit.isSelfCrewed()) {
            for (int i = techs.size() - 1; i >= 0; i--) {
                Person tech = techs.get(i);

                if (!tech.canTech(unit.getEntity())) {
                    techs.remove(i);
                }
            }
        }

        Map<PartRepairType, MRMSOption> mrmsOptionsByType = new HashMap<>();

        for (MRMSOption mrmsOption : mrmsOptions) {
            mrmsOptionsByType.put(mrmsOption.getType(), mrmsOption);
        }

        // Possibly call this multiple times. Sometimes some actions are first dependent
        // upon
        // others being finished, also failed actions can be performed again by a tech
        // with a higher
        // skill.
        boolean performMoreRepairs = true;

        long time = System.nanoTime();

        while (performMoreRepairs) {
            MRMSUnitAction currentUnitAction = performUnitMassTechAction(campaign,
                  unit,
                  techs,
                  mrmsOptionsByType,
                  isSalvage,
                  configuredOptions);

            performMoreRepairs = currentUnitAction.getPartSet().isHasRepairs();
            unitAction.merge(currentUnitAction);

            if (unitAction.isStatusNoActions()) {
                unitAction.setStatus(currentUnitAction.getStatus());
            }
        }

        debugLog("Finished fixing %s in %s ns", "performUnitMRMS", unit.getName(), System.nanoTime() - time);

        return unitAction;
    }

    private static MRMSUnitAction performUnitMassTechAction(Campaign campaign, Unit unit, List<Person> techs,
          Map<PartRepairType, MRMSOption> mrmsOptionsByType, boolean salvaging,
          MRMSConfiguredOptions configuredOptions) {
        boolean wasSalvaging = unit.isSalvage();
        boolean allowedCarryover = configuredOptions.isAllowCarryover();
        Map<PodSpace, Boolean> repairInPlaceBefore = new HashMap<>();
        for (PodSpace podSpace : unit.getPodSpace()) {
            repairInPlaceBefore.put(podSpace, podSpace.isRepairInPlace());
        }
        try {
            return performUnitMassTechActionOnce(campaign, unit, techs, mrmsOptionsByType, salvaging,
                  configuredOptions);
        } finally {
            restoreAfterLimbStripping(unit, wasSalvaging, configuredOptions, allowedCarryover);
            restoreRepairInPlace(unit, repairInPlaceBefore);
        }
    }

    /**
     * Mass Repair picks repair-in-place or swap for every pod space of an Omni unit. A pod space it left without a job
     * goes back to its earlier setting, so a pod job started by hand afterwards is not steered by Mass Repair's
     * choice. A pod job Mass Repair started and carried over keeps the choice until the job ends.
     */
    private static void restoreRepairInPlace(Unit unit, Map<PodSpace, Boolean> repairInPlaceBefore) {
        for (PodSpace podSpace : unit.getPodSpace()) {
            Boolean wasRepairInPlace = repairInPlaceBefore.get(podSpace);
            if ((wasRepairInPlace != null) && !podSpace.isBeingWorkedOn()) {
                podSpace.setRepairInPlace(wasRepairInPlace);
            }
        }
    }

    /**
     * Stripping a limb with a broken hip or shoulder switches the unit to salvage mode and turns carryover off for a
     * while. Both go back to how they were whatever happens, so an early return or an error cannot leave the unit in
     * salvage mode or carryover off for every later unit in the batch.
     */
    private static void restoreAfterLimbStripping(Unit unit, boolean wasSalvaging,
          MRMSConfiguredOptions configuredOptions, boolean allowedCarryover) {
        if (unit.isSalvage() != wasSalvaging) {
            LOGGER.debug("[MRMS] {}: salvage mode put back to {}", unit.getName(), wasSalvaging);
            unit.setSalvage(wasSalvaging);
        }
        configuredOptions.setAllowCarryover(allowedCarryover);
    }

    private static MRMSUnitAction performUnitMassTechActionOnce(Campaign campaign, Unit unit, List<Person> techs,
          Map<PartRepairType, MRMSOption> mrmsOptionsByType, boolean salvaging,
          MRMSConfiguredOptions configuredOptions) {
        List<IPartWork> parts = unit.getPartsNeedingService(true);

        if (parts.isEmpty()) {
            parts = unit.getPartsNeedingService(false);

            if (!parts.isEmpty()) {
                return new MRMSUnitAction(unit, salvaging, MRMSUnitAction.STATUS.ALL_PARTS_IN_PROCESS);
            }

            return new MRMSUnitAction(unit, salvaging, MRMSUnitAction.STATUS.NO_PARTS);
        }

        // If we're performing an action on a unit, and we allow auto-scrapping of parts
        // that can't be fixed by an elite tech, let's first get rid of those parts and start with
        // a cleaner slate
        if (configuredOptions.isScrapImpossible()) {
            boolean refreshParts = false;

            for (IPartWork partWork : parts) {
                if ((partWork instanceof Part) && (partWork.getSkillMin() > SkillType.EXP_LEGENDARY)) {
                    campaign.addReport(TECHNICAL, ((Part) partWork).scrap());
                    refreshParts = true;
                }
            }

            if (refreshParts) {
                parts = unit.getPartsNeedingService(true);
            }
        }

        if (unit.getEntity().isOmni() && !unit.isSalvage()) {
            for (PodSpace podSpace : unit.getPodSpace()) {
                // a pod job already under way keeps the choice it was started with
                if (!podSpace.isBeingWorkedOn()) {
                    podSpace.setRepairInPlace(!configuredOptions.isReplacePodParts());
                }
            }

            // Damaged pod equipment with a spare in stock is left to the pod swap, but only when the swap will
            // run; otherwise nothing would ever repair it
            boolean willPodSpacesSwapParts = configuredOptions.isReplacePodParts()
                                                   && mrmsOptionsByType.containsKey(PartRepairType.POD_SPACE);
            if (willPodSpacesSwapParts) {
                parts = withoutPodPartsTheSwapWillReplace(parts, salvaging);
            } else {
                LOGGER.debug("[MRMS] {}: pod equipment is repaired in place; the OmniPod swap will not run",
                      unit.getName());
            }
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
         * Pre-checking for hips/shoulders on repairable meks. If we have a bad
         * hip or shoulder, we're not going to do anything until we get those
         * parts out of the location and scrap it. Once we're at a happy place,
         * we'll proceed. A salvage run takes every part off anyway, so the limb
         * needs no special handling and the check would only stop the strip.
         */

        boolean isRepairingMek = !salvaging && (unit.getEntity() instanceof Mek);
        if (isRepairingMek) {
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
                    if (!(partWork instanceof MekLocation) &&
                              !(partWork instanceof MissingMekLocation) &&
                              locationMap.containsKey(partWork.getLocation()) &&
                              partWork.isSalvaging()) {
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
                    scrappingLimbMode = scrapEmptyLimb(locationMap, campaign, scrappingLimbMode, salvaging, unit);
                    parts = unit.getPartsNeedingService(true);
                } else {
                    processPartsInLocation(campaign, unit, countOfPartsPerLocation, locationMap);
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

        MRMSUnitAction unitAction = new MRMSUnitAction(unit, salvaging, MRMSUnitAction.STATUS.ACTIONS_PERFORMED);

        for (IPartWork partWork : parts) {
            performPartWork(campaign, unit, techs, mrmsOptionsByType, configuredOptions, partWork, unitAction);
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

    private static MRMSUnitAction scrapLocationAndRemoveEquipment(Campaign campaign, Unit unit,
          List<Person> techs, Map<PartRepairType, MRMSOption> mrmsOptionsByType,
          MRMSConfiguredOptions configuredOptions, List<IPartWork> parts) {
        boolean wasSalvaging = unit.isSalvage();
        boolean allowedCarryover = configuredOptions.isAllowCarryover();
        try {
            return scrapLocationAndRemoveEquipmentOnce(campaign, unit, techs, mrmsOptionsByType, configuredOptions,
                  parts);
        } finally {
            restoreAfterLimbStripping(unit, wasSalvaging, configuredOptions, allowedCarryover);
        }
    }

    private static MRMSUnitAction scrapLocationAndRemoveEquipmentOnce(Campaign campaign, Unit unit,
          List<Person> techs, Map<PartRepairType, MRMSOption> mrmsOptionsByType,
          MRMSConfiguredOptions configuredOptions, List<IPartWork> parts) {

        if (parts.isEmpty()) {
            return new MRMSUnitAction(unit, false, MRMSUnitAction.STATUS.ALL_PARTS_IN_PROCESS);
        }

        if (techs.isEmpty()) {
            return new MRMSUnitAction(unit, false, MRMSUnitAction.STATUS.NO_TECHS);
        }

        // If we're a mek and we have a limb with a bad shoulder/hip, we're going to try to flip it to salvageable
        // and remove all the parts so that we can nuke the limb. If we do this, when we're finally done we need to
        // flip the mek back to repairable so that we don't accidentally strip everything off it.
        boolean scrappingLimbMode = false;

        // Pre-checking for hips/shoulders on repairable meks. If we have a bad hip or shoulder, we're not going to
        // do anything until we get those parts out of the location and scrap it. Once we're at a happy place, we'll proceed.
        boolean isSalvaging = unit.isSalvage();
        if ((unit.getEntity() instanceof Mek)) {
            Map<Integer, Part> locationMap = new HashMap<>();

            for (IPartWork partWork : parts) {
                if ((partWork instanceof MekLocation) && ((MekLocation) partWork).onBadHipOrShoulder()) {
                    locationMap.put(((MekLocation) partWork).getLoc(), (MekLocation) partWork);
                }
            }

            if (!locationMap.isEmpty()) {
                // Find our parts in our bad locations. If we don't actually have, just scrap the limbs and move on
                // with our normal work
                scrappingLimbMode = true;

                if (!isSalvaging) {
                    unit.setSalvage(true);
                }

                List<IPartWork> partsTemp = unit.getPartsNeedingService(true);
                List<IPartWork> partsToBeRemoved = new ArrayList<>();
                Map<Integer, Integer> countOfPartsPerLocation = new HashMap<>();

                for (IPartWork partWork : partsTemp) {
                    if (!(partWork instanceof MekLocation) &&
                              locationMap.containsKey(partWork.getLocation()) &&
                              partWork.isSalvaging()) {
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
                    scrapEmptyLimb(locationMap, campaign, scrappingLimbMode, isSalvaging, unit);
                    // The limb is gone, so there is nothing left to strip; carrying on would work on the scrapped limb
                    LOGGER.debug("[MRMS] {}: Quick Strip finished, the empty limb was scrapped", unit.getName());
                    return new MRMSUnitAction(unit, isSalvaging, MRMSUnitAction.STATUS.ACTIONS_PERFORMED);
                } else {
                    processPartsInLocation(campaign, unit, countOfPartsPerLocation, locationMap);

                    parts = partsToBeRemoved;
                }
            }
        }

        boolean originalAllowCarryover = configuredOptions.isAllowCarryover();

        // If we're scrapping limbs, we don't want salvage repairs to go into a new day otherwise it can be confusing
        // when trying to figure why a unit can't be repaired because 'salvage' repairs don't show up on the task
        // list as scheduled if we're in 'repair' mode.
        if (scrappingLimbMode) {
            configuredOptions.setAllowCarryover(false);
        }

        // Filter our parts list to only those that aren't being worked on or those that meet our criteria as defined
        // in the campaign configurations
        parts = filterParts(parts, mrmsOptionsByType, techs, campaign);

        if (parts.isEmpty()) {
            if (scrappingLimbMode) {
                unit.setSalvage(false);
                configuredOptions.setAllowCarryover(originalAllowCarryover);
            }

            return new MRMSUnitAction(unit, isSalvaging, MRMSUnitAction.STATUS.NO_PARTS);
        }

        MRMSUnitAction unitAction = new MRMSUnitAction(unit, isSalvaging, MRMSUnitAction.STATUS.ACTIONS_PERFORMED);

        for (IPartWork partWork : parts) {
            performPartWork(campaign, unit, techs, mrmsOptionsByType, configuredOptions, partWork, unitAction);
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

    private static void performPartWork(Campaign campaign, Unit unit, List<Person> techs,
          Map<PartRepairType, MRMSOption> mrmsOptionsByType, MRMSConfiguredOptions configuredOptions,
          IPartWork partWork, MRMSUnitAction unitAction) {
        WorkTime playerChosenMode = partWork.getMode();
        if (partWork instanceof Part) {
            ((Part) partWork).resetModeToNormal();
        }

        List<Person> validTechs = filterTechs(partWork, techs, mrmsOptionsByType, campaign);

        if (validTechs.isEmpty()) {
            restorePlayerChosenMode(partWork, playerChosenMode);
            unitAction.addPartAction(MRMSPartAction.createNoTechs(partWork));
            return;
        }

        MRMSPartAction partAction = repairPart(campaign,
              partWork,
              unit,
              validTechs,
              mrmsOptionsByType,
              configuredOptions,
              false);
        boolean isAssigned = partAction.getStatus() == MRMSPartAction.STATUS.REPAIRED;
        if (!isAssigned) {
            restorePlayerChosenMode(partWork, playerChosenMode);
        }
        unitAction.addPartAction(partAction);
    }

    private static void processPartsInLocation(Campaign campaign, Unit unit,
          Map<Integer, Integer> countOfPartsPerLocation,
          Map<Integer, Part> locationMap) {
        for (int locId : countOfPartsPerLocation.keySet()) {
            boolean unfixable = false;
            Part loc = null;

            if (locationMap.containsKey(locId)) {
                loc = locationMap.get(locId);
                unfixable = (loc instanceof MekLocation);
            }

            if (unfixable) {
                campaign.addReport(TECHNICAL, String.format(
                      "<font color='" +
                            getWarningColor()
                            +
                            "'>Found an unfixable limb (%s) on %s which contains %s parts. Going to remove all parts and scrap the limb before proceeding with other repairs.</font>",
                      loc.getName(), unit.getName(), countOfPartsPerLocation.get(locId)));
            } else {
                campaign.addReport(TECHNICAL, String.format(
                      "<font color='" +
                            getWarningColor()
                            +
                            "'>Found missing location (%s) on %s which contains %s parts. Going to remove all parts before proceeding with other repairs.</font>",
                      loc != null ? loc.getName() : Integer.toString(locId), unit.getName(),
                      countOfPartsPerLocation.get(locId)));
            }
        }
    }

    /*
     * We have no parts left on our unfixable locations, so we'll just scrap those locations and
     * rebuild the parts list and reset back our normal repair mode
     */
    private static boolean scrapEmptyLimb(Map<Integer, Part> locationMap, Campaign campaign,
          boolean scrappingLimbMode, boolean isSalvaging, Unit unit) {
        for (Part part : locationMap.values()) {
            if (part instanceof MekLocation) {
                campaign.addReport(TECHNICAL, part.scrap());
            }
        }

        scrappingLimbMode = false;

        if (!isSalvaging) {
            unit.setSalvage(false);
        }

        return scrappingLimbMode;
    }

    /**
     * @return the tasks without the damaged pod-mounted equipment that has a spare in stock, which the pod swap
     *       replaces instead
     */
    private static List<IPartWork> withoutPodPartsTheSwapWillReplace(List<IPartWork> parts, boolean salvaging) {
        List<IPartWork> remainingParts = new ArrayList<>();
        for (IPartWork partWork : parts) {
            boolean isPodMounted = (partWork instanceof Part part) && part.isOmniPodded();
            // Ammunition is reloaded in place when repairing; only salvage swaps it out
            boolean isSwappedOut = !(partWork instanceof AmmoBin) || salvaging;
            boolean isReplacedBySwap = isPodMounted && isSwappedOut && hasSpareInStock(partWork);
            if (!isReplacedBySwap) {
                remainingParts.add(partWork);
            }
        }
        return remainingParts;
    }

    private static boolean hasSpareInStock(IPartWork partWork) {
        MissingPart missingPart = partWork.getMissingPart();
        return (missingPart != null) && missingPart.isReplacementAvailable();
    }

    /**
     * Mass Repair tries out work times on a task while it looks for a tech. A task it does not end up assigning gets
     * back the work time the player chose for it.
     */
    private static void restorePlayerChosenMode(IPartWork partWork, WorkTime playerChosenMode) {
        if (partWork instanceof Part part) {
            part.setMode(playerChosenMode);
        }
    }

    private static MRMSPartAction repairPart(Campaign campaign, IPartWork partWork, Unit unit, List<Person> techs,
          Map<PartRepairType, MRMSOption> mrmsOptionsByType, MRMSConfiguredOptions configuredOptions,
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
            SkillModifierData skillModifierData = tech.getSkillModifierData();
            Skill skill = tech.getSkillForWorkingOn(partWork);
            int experienceLevel = skill.getExperienceLevel(skillModifierData);

            if (experienceLevel > highestAvailableTechSkill) {
                highestAvailableTechSkill = experienceLevel;
            }

            if (highestAvailableTechSkill == SkillType.EXP_LEGENDARY) {
                break;
            }
        }

        debugLog("Starting with %s techs on %s", "repairPart", techs.size(), partWork.getPartName());

        boolean canChangeWorkTime = (partWork instanceof Part) && partWork.canChangeWorkMode();

        for (int i = techs.size() - 1; i >= 0; i--) {
            long time = System.nanoTime();

            Person tech = techs.get(i);

            debugLog("Checking tech %s", "repairPart", tech.getFullName());

            // If this tech has less time than required, ignore them for this part
            if (RepairTechEligibility.getTimeLeftToday(campaign, tech) < mrmsOptions.getDailyTimeMin()) {
                debugLog("... doesn't have enough time left due to configuration", "repairPart");
                continue;
            }

            Skill skill = tech.getSkillForWorkingOn(partWork);
            SkillModifierData skillModifierData = tech.getSkillModifierData();

            if (canChangeWorkTime) {
                ((Part) partWork).resetModeToNormal();
            }

            // We really only have to check one tech of each skill level
            SkillLevel skillLevel = skill.getSkillLevel(skillModifierData);
            int experienceLevel = skillLevel.getExperienceLevel();
            if (!techSkillToWorktimeMap.containsKey(skill.getType().getName() + "-" + experienceLevel)) {
                TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);
                WorkTime selectedWorktime = WorkTime.NORMAL;

                if (!canChangeWorkTime) {
                    debugLog("... can't increase the time because this part can't have its workMode changed",
                          "repairPart");
                } else if (targetRoll.getValue() > mrmsOptions.getTargetNumberPreferred() &&
                                 !configuredOptions.isUseExtraTime()) {
                    debugLog("... is above preferred TN but can't increase time due to configuration", "repairPart");
                } else if (targetRoll.getValue() > mrmsOptions.getTargetNumberPreferred() &&
                                 configuredOptions.isUseExtraTime()) {
                    debugLog("... is above preferred TN and trying to increase time", "repairPart");
                    WorkTimeCalculation workTimeCalc = calculateNewMRMSWorktime(partWork, tech,
                          mrmsOptions, campaign, true, highestAvailableTechSkill);

                    if (workTimeCalc.getWorkTime() != null) {
                        selectedWorktime = workTimeCalc.getWorkTime();
                    } else if (workTimeCalc.isReachedMaxSkill()) {
                        debugLog("... is above preferred TN but has no suitable time settings with max available tech",
                              "repairPart");

                        return MRMSPartAction.createMaxSkillReached(partWork, highestAvailableTechSkill,
                              mrmsOptions.getTargetNumberPreferred());
                    } else {
                        debugLog("... is above preferred TN but has no suitable time settings", "repairPart");

                        continue;
                    }
                } else if (targetRoll.getValue() < mrmsOptions.getTargetNumberPreferred() &&
                                 !configuredOptions.isUseRushJob()) {
                    debugLog("... is below preferred TN but can't decrease time due to configuration", "repairPart");
                } else if (targetRoll.getValue() < mrmsOptions.getTargetNumberPreferred() &&
                                 configuredOptions.isUseRushJob()) {
                    debugLog("... is below preferred TN and trying to decrease time", "repairPart");
                    WorkTimeCalculation workTimeCalc = calculateNewMRMSWorktime(partWork, tech,
                          mrmsOptions, campaign, false, highestAvailableTechSkill);

                    if (workTimeCalc.getWorkTime() != null) {
                        selectedWorktime = workTimeCalc.getWorkTime();
                    } else if (workTimeCalc.isReachedMaxSkill()) {
                        debugLog("... is below preferred TN but has no suitable time settings with max available tech",
                              "repairPart");

                        return MRMSPartAction.createMaxSkillReached(partWork, highestAvailableTechSkill,
                              mrmsOptions.getTargetNumberPreferred());
                    } else {
                        debugLog("... is above preferred TN but has no suitable time settings", "repairPart");

                        continue;
                    }
                }

                techSkillToWorktimeMap.put(skill.getType().getName() + "-" + experienceLevel, selectedWorktime);

                if (canChangeWorkTime) {
                    ((Part) partWork).resetModeToNormal();
                }
            }

            // Fallback TN check to account for discrepancies between Techs
            TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);
            if (canChangeWorkTime) {
                WorkTime workTime = techSkillToWorktimeMap.get(skill.getType().getName() + "-" + experienceLevel);
                if (null == workTime) {
                    debugLog("[ERROR] Null work-time from techToWorktimeMap for %s", "repairPart", tech.getFullName());
                    workTime = WorkTime.NORMAL;
                }
                ((Part) partWork).setMode(workTime);
                // Get updated TN with worktime in mind
                targetRoll = campaign.getTargetFor(partWork, tech);
                ((Part) partWork).resetModeToNormal();
            }

            if (targetRoll.getValue() > mrmsOptions.getTargetNumberMax()) {
                debugLog("... is above max TN allowed", "repairPart");

                continue;
            }

            boolean assigned = false;

            if ((unit != null) && configuredOptions.isUseAssignedTechsFirst()) {
                int id = unit.getFormationId();
                Formation formation = campaign.getPlayerForce().getFormation(id);

                if ((formation != null) && (formation.getTechID()) != null) {
                    assigned = formation.getTechID().toString().equals(tech.getId().toString());
                }

                if (!assigned && !tech.getTechUnits().isEmpty()) {
                    assigned = tech.getTechUnits().contains(unit);
                }
            }

            boolean isSameDayTech;

            WorkTime workTime = getWorkTime(canChangeWorkTime, techSkillToWorktimeMap, skill, tech, experienceLevel);
            int expectedTime = getExpectedWorkTime((partWork), workTime);

            if (RepairTechEligibility.getTimeLeftToday(campaign, tech) < expectedTime) {
                boolean isCarryoverAllowed = configuredOptions.isAllowCarryover()
                                                   || isCrewJobLongerThanAFullDay(campaign, unit, tech, expectedTime);
                if (!isCarryoverAllowed) {
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

        Person tech = validTechs.getFirst();

        Skill skill = tech.getSkillForWorkingOn(partWork);
        SkillModifierData skillModifierData = tech.getSkillModifierData();
        SkillLevel skillLevel = skill.getSkillLevel(skillModifierData);
        int experienceLevel = skillLevel.getExperienceLevel();
        WorkTime workTime = getWorkTime(canChangeWorkTime, techSkillToWorktimeMap, skill, tech, experienceLevel);

        setPartWorkTime(partWork, workTime);

        if (warehouseMode && (partWork instanceof Part)) {
            campaign.fixWarehousePart((Part) partWork, tech);
        } else {
            campaign.fixPart(partWork, tech);
        }

        // If this tech has no time left, filter them out so we don't
        // spend cycles on them in the future
        if (RepairTechEligibility.getTimeLeftToday(campaign, tech) <= 0) {
            techs.remove(tech);
        }

        Thread.yield();

        debugLog("Ending after %s ns", "repairPart", System.nanoTime() - repairPartTime);

        return MRMSPartAction.createRepaired(partWork);
    }

    private static void setPartWorkTime(IPartWork partWork, WorkTime workTime) {
        if (partWork instanceof Part part) {
            part.setMode(workTime);
        } else {
            LOGGER.debug("Tried to set part work time for non-part " + partWork.getPartName());
        }
    }

    private static WorkTime getWorkTime(boolean canChangeWorkTime, Map<String, WorkTime> techSkillToWorktimeMap,
          Skill skill, Person tech, int experienceLevel) {
        WorkTime workTime = WorkTime.NORMAL;
        if (canChangeWorkTime) {

            workTime = techSkillToWorktimeMap.get(skill.getType().getName() + "-" + experienceLevel);

            if (null == workTime) {
                debugLog("[ERROR] Null work-time from techToWorktimeMap for %s", "repairPart", tech.getFullName());
                workTime = WorkTime.NORMAL;
            }
        }
        return workTime;
    }

    private static int getExpectedWorkTime(IPartWork part, WorkTime workTime) {
        WorkTime priorWorkTime = part.getMode();
        setPartWorkTime(part, workTime);
        int expectedTime = part.getActualTime();
        setPartWorkTime(part, priorWorkTime);
        return expectedTime;
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

            if (partWork instanceof MissingPart missingPart) {
                // A part the player has queued for fabrication is being built from scratch on purpose; MRMS must
                // leave it alone rather than replace it from warehouse stock and undo that choice.
                if (missingPart.isFabricating()) {
                    continue;
                }

                if (!missingPart.isReplacementAvailable()) {
                    continue;
                }
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

            if (!checkAmmoSupply(partWork)) {
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
                // This placeholder represents an idealized, fully equipped tech for the feasibility check. When the
                // "Techs Need a Tool Kit" campaign option is enabled, getTargetFor treats any kit-less tech as unable to
                // work, so the placeholder must carry a tool kit, or every part would be wrongly reported as impossible.
                tech.setRepairKitName(EquipmentKitCatalog.KIT_BASIC_TOOLKIT);

                techCache.put(skillName, tech);
            }

            // The stand-in tech belongs to no unit and no place, so only the skill side of the check applies
            TargetRoll roll = campaign.getSkillTargetFor(partWork, tech);

            if ((roll.getValue() == TargetRoll.IMPOSSIBLE) ||
                      (roll.getValue() == TargetRoll.AUTOMATIC_FAIL) ||
                      (roll.getValue() == TargetRoll.CHECK_FALSE)) {
                continue;
            }

            newParts.add(partWork);
        }

        return newParts;
    }

    /**
     * Whether a self-crewed unit's job runs past its engineer's whole working day, overtime included when the campaign
     * allows it. The crew is the unit's only tech team, so such a job, common on ships, can only ever be done over
     * several days, and Mass Repair starts it even with carryover off. Any other unit, and a job that fits in one day,
     * follows the setting.
     *
     * @return {@code true} if the unit is self-crewed and the job is longer than its engineer's full day
     */
    private static boolean isCrewJobLongerThanAFullDay(Campaign campaign, Unit unit, Person tech, int expectedTime) {
        if (!unit.isSelfCrewed()) {
            return false;
        }
        boolean isTechsUseAdministration = campaign.getCampaignOptions().get(CampaignOption.TECHS_USE_ADMINISTRATION);
        int fullDay = tech.getDailyAvailableTechTime(isTechsUseAdministration);
        if (campaign.isOvertimeAllowed()) {
            fullDay += (int) Math.round(Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME
                                              * tech.calculateTechTimeMultiplier(isTechsUseAdministration));
        }
        boolean isLongerThanAFullDay = expectedTime > fullDay;
        if (isLongerThanAFullDay) {
            LOGGER.debug("[MRMS] {}: a {} minute job is longer than the engineer's {} minute day, so it carries over",
                  unit.getName(), expectedTime, fullDay);
        }
        return isLongerThanAFullDay;
    }

    /**
     * A ship's crew is its only tech team (Campaign Operations, pp. 190-191), and infantry that maintains itself is
     * led by its ranking soldier. So Mass Repair on a self-crewed unit works through its engineer alone, the same
     * person the Repair tab offers for it.
     *
     * @return the unit's engineer if they can still work today, otherwise an empty list
     */
    private static List<Person> getEngineerWithTimeLeftToday(Campaign campaign, Unit unit) {
        List<Person> techs = new ArrayList<>();
        Person engineer = unit.getEngineer();
        if (engineer == null) {
            LOGGER.debug("[MRMS] {}: no engineer, so its crew cannot repair it", unit.getName());
            return techs;
        }
        if (RepairTechEligibility.getTimeLeftToday(campaign, engineer) <= 0) {
            LOGGER.debug("[MRMS] {}: engineer {} has no time left today", unit.getName(), engineer.getFullName());
            return techs;
        }
        techs.add(engineer);
        return techs;
    }

    /**
     * @return the techs who can still work today, counting overtime when the campaign allows it, as the Repair tab
     *       does
     */
    private static List<Person> getTechsWithTimeLeftToday(Campaign campaign) {
        List<Person> techs = campaign.getPlayerForce()
                                   .getHumanResources()
                                   .getTechs(campaign.getPlayerForce().getHangar().getUnits(),
                                         campaign.getCampaignOptions(),
                                         campaign.getPlayerForce().isClanForce(),
                                         campaign.getLocalDate(),
                                         false);
        // Techs with no regular minutes are kept here and dropped below only if they have no overtime either
        List<Person> techsWithTimeLeft = new ArrayList<>();
        for (Person tech : techs) {
            if (RepairTechEligibility.getTimeLeftToday(campaign, tech) > 0) {
                techsWithTimeLeft.add(tech);
            }
        }
        return techsWithTimeLeft;
    }

    private static List<Person> filterTechs(IPartWork partWork, List<Person> techs,
          Map<PartRepairType, MRMSOption> mrmsOptionsByType, Campaign campaign) {
        List<Person> validTechs = new ArrayList<>();

        if (techs.isEmpty()) {
            return validTechs;
        }

        MRMSOption mrmsOption = mrmsOptionsByType.get(IPartWork.findCorrectMRMSType(partWork));

        if (null == mrmsOption) {
            return validTechs;
        }

        int techsElsewhere = 0;
        for (int i = techs.size() - 1; i >= 0; i--) {
            Person tech = techs.get(i);

            if (RepairTechEligibility.getTimeLeftToday(campaign, tech) <= 0) {
                continue;
            }

            // The repair refuses a tech who is not where the task is; picking one anyway would repeat forever
            if (!RepairLocationCheck.isTechAtTask(tech, partWork)) {
                techsElsewhere++;
                continue;
            }

            // Only techs with the skill the task requires may be assigned; with granular tech skills a tech lacking
            // the specialist skill would otherwise fall back to an unrelated skill and trigger the wrong-tech-type
            // warning
            if (!tech.isRightTechTypeFor(partWork)) {
                continue;
            }

            Skill skill = tech.getSkillForWorkingOn(partWork);
            if (skill == null) {
                continue;
            }

            SkillModifierData skillModifierData = tech.getSkillModifierData();
            if (mrmsOption.getSkillMin() > skill.getExperienceLevel(skillModifierData)) {
                continue;
            }

            if (mrmsOption.getSkillMax() < skill.getExperienceLevel(skillModifierData)) {
                continue;
            }

            // Check if we can actually even repair this part. This also applies the task's minimum skill, with the
            // work mode's penalty, except under Destroy by Margin, where any tech may try
            TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);

            if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) ||
                      (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL) ||
                      (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
                continue;
            }

            validTechs.add(tech);
        }

        if (techsElsewhere > 0) {
            LOGGER.debug("[MRMS] {}: {} techs skipped because they are at a different location", partWork.getPartName(),
                  techsElsewhere);
        }
        return validTechs;
    }

    private static boolean checkArmorSupply(IPartWork part) {
        if (part.isSalvaging()) {
            return true;
        }

        return (!(part instanceof Armor)) || ((Armor) part).isInSupply();
    }

    /**
     * Checks whether an AmmoBin has ammo available in the warehouse. Non-AmmoBin parts always pass. Salvaging parts
     * always pass.
     *
     * @param part The part to check.
     *
     * @return {@code true} if the part is not an AmmoBin, or if ammo is available.
     */
    private static boolean checkAmmoSupply(IPartWork part) {
        if (part.isSalvaging()) {
            return true;
        }

        return (!(part instanceof AmmoBin)) || ((AmmoBin) part).isAmmoAvailable();
    }

    private static WorkTimeCalculation calculateNewMRMSWorktime(IPartWork partWork, Person tech, MRMSOption mrmsOption,
          Campaign campaign, boolean increaseTime, int highestAvailableTechSkill) {
        long time = System.nanoTime();

        debugLog("...... starting calculateNewMRMSWorktime", "calculateNewMRMSWorktime");

        boolean canChangeWorkTime = (partWork instanceof Part) && partWork.canChangeWorkMode();

        if (canChangeWorkTime) {
            ((Part) partWork).resetModeToNormal();
        }

        TargetRoll targetRoll = campaign.getTargetFor(partWork, tech);

        if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) ||
                  (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL) ||
                  (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
            debugLog("...... ending calculateNewMRMSWorktime due to impossible role - %s ns",
                  "calculateNewMRMSWorktime",
                  System.nanoTime() - time);

            return new WorkTimeCalculation();
        }

        WorkTime newWorkTime = partWork.getMode();
        WorkTime previousNewWorkTime;

        Skill skill = tech.getSkillForWorkingOn(partWork);

        while (null != newWorkTime) {
            previousNewWorkTime = newWorkTime;
            newWorkTime = newWorkTime.moveTimeToNextLevel(increaseTime);

            debugLog("...... looping workTime check. NewWorkTime: %s, PreviousWorkTime: %s",
                  "calculateNewMRMSWorktime",
                  (null == newWorkTime ? "NULL" : newWorkTime.name()),
                  previousNewWorkTime.name());

            // If we're trying to a rush a job, our effective skill goes down
            // Let's make sure we don't put it so high that we can't fix it
            // anymore
            SkillModifierData skillModifierData = tech.getSkillModifierData();
            if (!increaseTime) {
                int modePenalty = partWork.getMode().expReduction;
                if (partWork.getSkillMin() >
                          (skill.getExperienceLevel(skillModifierData) - modePenalty)) {
                    debugLog(
                          "...... ending calculateNewMRMSWorktime with previousWorkTime due time reduction skill mod now being less that required skill - %s ns",
                          "calculateNewMRMSWorktime",
                          System.nanoTime() - time);

                    return new WorkTimeCalculation(previousNewWorkTime);
                }
            }

            // If we have a null newWorkTime, we're done. Use the previous one unless it exceeds the set max TN.
            if (null == newWorkTime) {
                debugLog("...... ending calculateNewMRMSWorktime because newWorkTime is null - %s ns",
                      "calculateNewMRMSWorktime",
                      System.nanoTime() - time);

                targetRoll = campaign.getTargetFor(partWork, tech);

                WorkTimeCalculation workTimeCalculation = new WorkTimeCalculation(null);
                int targetNumberLimit = increaseTime ? mrmsOption.getTargetNumberMax()
                      : getRushTargetNumberLimit(mrmsOption);
                if (targetRoll.getValue() <= targetNumberLimit) {
                    workTimeCalculation.setWorkTime(previousNewWorkTime);
                }

                if (skill.getExperienceLevel(skillModifierData) >=
                          highestAvailableTechSkill) {
                    workTimeCalculation.setReachedMaxSkill(true);
                }

                return workTimeCalculation;
            }

            // Set our new workTime and calculate the new targetRoll
            if (canChangeWorkTime) {
                ((Part) partWork).setMode(newWorkTime);
            }

            targetRoll = campaign.getTargetFor(partWork, tech);

            // If our roll is impossible, revert to the previous one
            if ((targetRoll.getValue() == TargetRoll.IMPOSSIBLE) ||
                      (targetRoll.getValue() == TargetRoll.AUTOMATIC_FAIL) ||
                      (targetRoll.getValue() == TargetRoll.CHECK_FALSE)) {
                debugLog("...... ending calculateNewMRMSWorktime due to impossible role - %s ns",
                      "calculateNewMRMSWorktime",
                      System.nanoTime() - time);

                return new WorkTimeCalculation(previousNewWorkTime);
            }

            if (increaseTime) {
                // If we've reached our TN, kick out. Otherwise, we'll loop
                // around again
                if (targetRoll.getValue() <= mrmsOption.getTargetNumberPreferred()) {
                    debugLog(
                          "...... ending calculateNewMRMSWorktime because we have reached our TN goal - %s ns",
                          "calculateNewMRMSWorktime",
                          System.nanoTime() - time);

                    return new WorkTimeCalculation(newWorkTime);
                }
            } else {
                // Rushing trades a harder roll for time, but never past the preferred target number
                if (targetRoll.getValue() > getRushTargetNumberLimit(mrmsOption)) {
                    debugLog(
                          "...... ending calculateNewMRMSWorktime because we have reached our TN goal - %s ns",
                          "calculateNewMRMSWorktime",
                          System.nanoTime() - time);

                    return new WorkTimeCalculation(previousNewWorkTime);
                }
            }
        }

        return new WorkTimeCalculation();
    }

    /**
     * @return the highest target number a Rush Job may push a task to: the preferred target number, or the maximum if
     *       that is lower
     */
    private static int getRushTargetNumberLimit(MRMSOption mrmsOption) {
        return Math.min(mrmsOption.getTargetNumberPreferred(), mrmsOption.getTargetNumberMax());
    }

    private static void debugLog(String msg, String methodName, Object... replacements) {
        if ((null != replacements) && (replacements.length > 0)) {
            msg = String.format(msg, replacements);
        }

        LOGGER.debug(msg);
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

        public void setWorkTime(WorkTime workTime) {
            this.workTime = workTime;
        }

        public boolean isReachedMaxSkill() {
            return reachedMaxSkill;
        }

        public void setReachedMaxSkill(boolean reachedMaxSkill) {
            this.reachedMaxSkill = reachedMaxSkill;
        }
    }

    private record TechSorter(IPartWork partWork) implements Comparator<Person> {

        @Override
        public int compare(Person tech1, Person tech2) {
            // Sort the valid techs by applicable skill. Let's start with the least experienced and work our way up
            // until we find someone who can perform the work. If we have two techs with the same XP, put the one with
            // the more time ahead.
            Skill skill1 = tech1.getSkillForWorkingOn(partWork);

            Skill skill2 = tech2.getSkillForWorkingOn(partWork);

            // Nulls at the end
            if (skill1 == null && skill2 == null) {
                return compareMostTimeFirst(tech1, tech2);
            }
            if (skill1 == null) {
                return 1;
            }
            if (skill2 == null) {
                return -1;
            }

            SkillModifierData tech1SkillModifierData = tech1.getSkillModifierData();
            SkillModifierData tech2SkillModifierData = tech2.getSkillModifierData();

            int experienceCompare = Integer.compare(skill1.getTotalSkillLevel(tech1SkillModifierData),
                  skill2.getTotalSkillLevel(tech2SkillModifierData));
            if (experienceCompare != 0) {
                return experienceCompare;
            }

            return compareMostTimeFirst(tech1, tech2);
        }

        /** Puts the tech with more minutes left first, so the job is most likely to be finished today. */
        private static int compareMostTimeFirst(Person tech1, Person tech2) {
            return Integer.compare(tech2.getMinutesLeft(), tech1.getMinutesLeft());
        }
    }

    public static class MRMSPartAction {
        public enum STATUS {
            REPAIRED, MAX_SKILL_REACHED, MRO_DISABLED, NO_TECHS
        }

        private IPartWork partWork;
        private STATUS status;
        private int maxTechSkill;
        private int configuredTargetNumberPreferred;

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

        public int getConfiguredTargetNumberPreferred() {
            return configuredTargetNumberPreferred;
        }

        public void setConfiguredTargetNumberPreferred(int configuredTargetNumberPreferred) {
            this.configuredTargetNumberPreferred = configuredTargetNumberPreferred;
        }

        public static MRMSPartAction createRepaired(IPartWork partWork) {
            return new MRMSPartAction(partWork, STATUS.REPAIRED);
        }

        public static MRMSPartAction createMaxSkillReached(final IPartWork partWork,
              final int maxSkill, final int targetNumberPreferred) {
            final MRMSPartAction mrmsPartAction = new MRMSPartAction(partWork, STATUS.MAX_SKILL_REACHED);
            mrmsPartAction.setMaxTechSkill(maxSkill);
            mrmsPartAction.setConfiguredTargetNumberPreferred(targetNumberPreferred);
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
        private final Map<MRMSPartAction.STATUS, List<MRMSPartAction>> partActionsByStatus = new HashMap<>();

        public void addPartAction(MRMSPartAction partAction) {
            if (partAction == null) {
                return;
            }

            List<MRMSPartAction> list = partActionsByStatus.computeIfAbsent(partAction.getStatus(),
                  k -> new ArrayList<>());

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

            return partActionsByStatus.size() == 1;
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
