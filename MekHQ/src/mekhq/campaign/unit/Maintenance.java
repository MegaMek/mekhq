/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.utilities.MHQInternationalization.getFormattedText;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.PlanetaryCostReductions;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.PlanetaryWorkConditions;
import mekhq.utilities.ReportingUtilities;

/**
 * The code in this class previously lived in {@link Campaign} and were migrated as part of an effort to reduce the size
 * of that class. The move took place in the 0.50.10 cycle, and no changes were made beyond those necessary to
 * facilitate the move.
 */
public class Maintenance {
    private static final MMLogger LOGGER = MMLogger.create(Maintenance.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Maintenance";
    /** @deprecated Use {@link #RESOURCE_BUNDLE} instead */
    @Deprecated(since = "0.50.10", forRemoval = true)
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
          MekHQ.getMHQOptions().getLocale());

    public static void doMaintenance(Campaign campaign, Unit unit) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.get(CampaignOption.CHECK_MAINTENANCE)) {
            return;
        }
        if (!unit.requiresMaintenance()) {
            LOGGER.debug("[Maintenance] {}: no maintenance today (present={}, deployed={}, refitting={}, "
                               + "mothballing={}, mothballed={})",
                  unit.getName(), unit.isPresent(), unit.isDeployed(), unit.isRefitting(), unit.isMothballing(),
                  unit.isMothballed());
            return;
        }
        // Maintenance is paused, rather than skipped: the cycle does not advance, so nothing is charged or checked
        if (!isMaintenanceEnabledForSituation(campaign, unit)) {
            return;
        }
        // let's start by checking times
        int minutesUsed = unit.getMaintenanceTime();
        int asTechsUsed = 0;
        boolean maintained;
        boolean paidMaintenance = true;

        double daysBeforeIncrement = unit.getDaysSinceMaintenance();
        unit.incrementDaysSinceMaintenance(campaign, false, asTechsUsed);

        int ruggedMultiplier = 1;
        if (unit.getEntity().hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_1)) {
            ruggedMultiplier = 2;
        }

        if (unit.getEntity().hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_2)) {
            ruggedMultiplier = 3;
        }

        int cycleDays = campaignOptions.get(CampaignOption.MAINTENANCE_CYCLE_DAYS) * ruggedMultiplier;
        boolean isCheckDue = unit.getDaysSinceMaintenance() >= cycleDays;
        LOGGER.debug("[Maintenance] {}: {} of {} days since last check (clock +{} today, rugged x{}), check {}",
              unit.getName(), unit.getDaysSinceMaintenance(), cycleDays,
              unit.getDaysSinceMaintenance() - daysBeforeIncrement, ruggedMultiplier,
              isCheckDue ? "due" : "not due");

        if (isCheckDue) {
            Person tech = unit.getTech();
            if (tech == null) {
                LOGGER.debug("[Maintenance] {}: no tech or engineer, checked as unmaintained", unit.getName());
            } else {
                LOGGER.debug("[Maintenance] {}: {} has {} minutes, needs {} (multiplier x{})",
                      unit.getName(), tech.getFullName(), tech.getMinutesLeft(), minutesUsed,
                      unit.getMaintenanceMultiplier());
            }
            if (tech != null && !LocationUtils.areSameEffectiveLocation(unit, tech)) {
                // Tech is at a different location; maintenance cannot be performed this cycle.
                campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                      "Maintenance.techAtDifferentLocation", unit.getName()));
                tech = null;
            }
            if (tech != null) {
                int availableMinutes = tech.getMinutesLeft();
                maintained = (availableMinutes >= minutesUsed);

                // A tech mothballing another unit still maintains this one: maintenance comes first each day, and
                // the mothballing only uses the minutes left over, as a refit does
                if (!maintained) {
                    // At this point, insufficient minutes is the only reason why maintenance would fail.
                    campaign.addReport(TECHNICAL, String.format(resources.getString("maintenanceNotAvailable.text"),
                          unit.getName()));
                }

                if (maintained) {
                    tech.setMinutesLeft(availableMinutes - minutesUsed);
                    asTechsUsed = campaign.getPlayerForce().getHumanResources().getAvailableAsTechs(minutesUsed,
                          false,
                          campaign.isOvertimeAllowed(),
                          campaign.getCampaignOptions());
                    int minutes = campaign.getPlayerForce().getHumanResources().getAsTechPoolMinutes() -
                                        (asTechsUsed * minutesUsed);
                    campaign.getPlayerForce().getHumanResources().setAsTechPoolMinutes(minutes);
                }
                if (!maintained) {
                    // A tech who could not do the work does not make the check: the unit is checked as unmaintained
                    LOGGER.debug("[Maintenance] {}: {} could not maintain it today, checked as unmaintained",
                          unit.getName(), tech.getFullName());
                    tech = null;
                }
            }

            // maybe use the money. Straight support covers repairs, not maintenance (Hot Spots: Draconis Reach pg 27),
            // so none of this is reimbursed.
            if (campaignOptions.isChargingMaintenance()) {
                Money maintenanceCost = unit.getMaintenanceCost()
                                              .multipliedBy(PlanetaryCostReductions.getMaintenanceMultiplier(campaign))
                                              .multipliedBy(getSituationalMaintenanceCostMultiplier(campaign, unit));
                if (!(campaign.getPlayerForce().getFinances().debit(TransactionType.MAINTENANCE,
                      campaign.getLocalDate(),
                      maintenanceCost,
                      "Maintenance for " + unit.getName()))) {
                    campaign.addReport(TECHNICAL, "<font color='" +
                                                        getNegativeColor() +
                                                        "'><b>You cannot afford to pay maintenance costs for " +
                                                        unit.getHyperlinkedName() +
                                                        "!</b></font>");
                    paidMaintenance = false;
                }
            }
            // it is time for a maintenance check
            PartQuality qualityOrig = unit.getQuality();
            String techName = "Nobody";
            String techNameLinked = techName;
            if (null != tech) {
                techName = tech.getFullTitle();
                techNameLinked = tech.getHyperlinkedFullTitle();
            }
            // don't do actual damage until we clear the for loop to avoid
            // concurrent mod problems
            // put it into a hash - 4 points of damage will mean destruction
            Map<Part, Integer> partsToDamage = new HashMap<>();
            StringBuilder maintenanceReport = new StringBuilder("<strong>" +
                                                                      techName +
                                                                      " performing maintenance</strong><br><br>");
            for (Part part : unit.getParts()) {
                try {
                    String partReport = doMaintenanceOnUnitPart(campaign,
                          tech,
                          part,
                          partsToDamage,
                          paidMaintenance,
                          asTechsUsed);
                    if (partReport != null) {
                        maintenanceReport.append(partReport).append("<br>");
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex,
                          "Could not perform maintenance on part {} ({}) for {} ({}) due to an error",
                          part.getName(),
                          part.getId(),
                          unit.getName(),
                          unit.getId().toString());
                    campaign.addReport(TECHNICAL, String.format(
                          "ERROR: An error occurred performing maintenance on %s for unit %s, check the log",
                          part.getName(),
                          unit.getName()));
                }
            }

            int nDamage = 0;
            int nDestroy = 0;
            for (Map.Entry<Part, Integer> p : partsToDamage.entrySet()) {
                int damage = p.getValue();
                if (damage > 3) {
                    nDestroy++;
                    p.getKey().remove(false);
                } else {
                    p.getKey().doMaintenanceDamage(damage);
                    nDamage++;
                }
            }

            unit.setLastMaintenanceReport(maintenanceReport.toString());

            if (campaignOptions.get(CampaignOption.LOG_MAINTENANCE)) {
                LOGGER.info(maintenanceReport.toString());
            }

            PartQuality quality = unit.getQuality();
            String qualityString;
            boolean reverse = campaignOptions.get(CampaignOption.REVERSE_QUALITY_NAMES);
            if (quality.toNumeric() > qualityOrig.toNumeric()) {
                qualityString = ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorPositiveHexColor(),
                      "Overall quality improves from " +
                            qualityOrig.toName(reverse) +
                            " to " +
                            quality.toName(reverse));
            } else if (quality.toNumeric() < qualityOrig.toNumeric()) {
                qualityString = ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorNegativeHexColor(),
                      "Overall quality declines from " +
                            qualityOrig.toName(reverse) +
                            " to " +
                            quality.toName(reverse));
            } else {
                qualityString = "Overall quality remains " + quality.toName(reverse);
            }
            String damageString = getDamageString(unit, nDamage, nDestroy);
            String paidString = "";
            if (!paidMaintenance) {
                paidString = "<font color='" +
                                   getNegativeColor() +
                                   "'>Could not afford maintenance costs, so check is at a penalty.</font>";
            }
            campaign.addReport(TECHNICAL, techNameLinked +
                                                " performs maintenance on " +
                                                unit.getHyperlinkedName() +
                                                ". " +
                                                paidString +
                                                qualityString +
                                                ". " +
                                                damageString +
                                                " [<a href='MAINTENANCE|" +
                                                unit.getId() +
                                                "'>Get details</a>]");

            unit.resetDaysSinceMaintenance();
        }
    }

    /**
     * Checks whether the campaign is on a contract that runs maintenance at the full, weekly pace. Only active
     * contracts other than Garrison Duty and Retainer count; otherwise maintenance progresses at x0.25 speed.
     *
     * @param campaign the campaign to check
     *
     * @return {@code true} if any active contract is neither Garrison Duty nor a Retainer
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isOnCombatContract(Campaign campaign) {
        for (AbstractContract contract : campaign.getActiveContracts()) {
            ContractObjectiveType objectiveType = contract.getObjectiveType();
            if (!objectiveType.isGarrisonDuty() && !objectiveType.isRetainer()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a unit is travelling: away from a planet (including waiting at a jump point), or moving overland
     * in a ground convoy.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if the unit is in transit; a unit with no location is never in transit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isUnitInTransit(Unit unit) {
        if (!unit.hasLocation()) {
            return false;
        }
        return !unit.isOnPlanet() || unit.isInTransit();
    }

    /**
     * Checks whether the campaign options allow maintenance on a unit in its current situation. Being on or off
     * contract always applies, and being in transit applies on top of that; maintenance only runs if every situation
     * that applies is enabled.
     *
     * @param campaign the campaign providing options and active contracts
     * @param unit     the unit to check
     *
     * @return {@code true} if maintenance may run on the unit today
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isMaintenanceEnabledForSituation(Campaign campaign, Unit unit) {
        return isMaintenanceEnabledForSituation(campaign.getCampaignOptions(),
              !campaign.getActiveContracts().isEmpty(),
              isUnitInTransit(unit));
    }

    /**
     * As {@link #isMaintenanceEnabledForSituation(Campaign, Unit)}, for a given situation rather than the current one.
     *
     * @param campaignOptions the campaign options
     * @param isOnContract    whether any contract is active
     * @param isUnitInTransit whether the unit is travelling
     *
     * @return {@code true} if maintenance may run on a unit in that situation
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isMaintenanceEnabledForSituation(CampaignOptions campaignOptions, boolean isOnContract,
          boolean isUnitInTransit) {
        CampaignOption<Boolean> contractOption = isOnContract
                                                       ? CampaignOption.MAINTENANCE_ON_CONTRACT
                                                       : CampaignOption.MAINTENANCE_OFF_CONTRACT;
        if (!campaignOptions.get(contractOption)) {
            return false;
        }
        return campaignOptions.get(CampaignOption.MAINTENANCE_IN_TRANSIT) || !isUnitInTransit;
    }

    /**
     * Returns the multiplier applied to a unit's maintenance cost for its current situation. On a combat contract
     * maintenance runs weekly rather than monthly, so the combat contract multiplier offsets the more frequent
     * charges. Units other than large craft that are in transit are stowed, so the in-transit multiplier applies to
     * them as well. Both multipliers stack.
     *
     * @param campaign the campaign providing options and active contracts
     * @param unit     the unit being maintained
     *
     * @return the combined situational cost multiplier
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getSituationalMaintenanceCostMultiplier(Campaign campaign, Unit unit) {
        return getSituationalMaintenanceCostMultiplier(campaign.getCampaignOptions(),
              unit,
              isOnCombatContract(campaign),
              isUnitInTransit(unit));
    }

    /**
     * As {@link #getSituationalMaintenanceCostMultiplier(Campaign, Unit)}, for a given situation rather than the
     * current one.
     *
     * @param campaignOptions    the campaign options
     * @param unit               the unit being maintained
     * @param isOnCombatContract whether a contract other than Garrison Duty or a Retainer is active
     * @param isUnitInTransit    whether the unit is travelling
     *
     * @return the combined situational cost multiplier
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static double getSituationalMaintenanceCostMultiplier(CampaignOptions campaignOptions, Unit unit,
          boolean isOnCombatContract, boolean isUnitInTransit) {
        double multiplier = 1.0;
        if (isOnCombatContract) {
            multiplier *= campaignOptions.get(CampaignOption.COMBAT_CONTRACT_MAINTENANCE_COST_MULTIPLIER);
        }

        Entity entity = unit.getEntity();
        boolean isVessel = entity != null && entity.isLargeCraft();
        if (!isVessel && isUnitInTransit) {
            multiplier *= campaignOptions.get(CampaignOption.IN_TRANSIT_MAINTENANCE_COST_MULTIPLIER);
        }
        return multiplier;
    }

    /**
     * Projects the maintenance a unit will be charged per day, on average, in a given situation. This mirrors
     * {@link #doMaintenance(Campaign, Unit)}: a cycle is charged every {@link CampaignOption#MAINTENANCE_CYCLE_DAYS}
     * (stretched by Rugged quirks), and away from a combat contract the cycle advances at x0.25 speed (see
     * {@link Unit#incrementDaysSinceMaintenance}). The situational toggles and cost multipliers apply as they would on
     * the day. Planetary cost reductions are not applied: they never apply on contract, and a unit in transit is not on
     * a planet.
     *
     * @param campaign           the campaign providing options
     * @param unit               the unit to project
     * @param isOnContract       whether a contract will be active
     * @param isOnCombatContract whether that contract is neither Garrison Duty nor a Retainer
     * @param isTravelling       whether the force will be travelling; a unit with no location never travels
     *
     * @return the average daily maintenance cost, or zero if the unit will not be charged maintenance
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getProjectedDailyMaintenanceCost(Campaign campaign, Unit unit, boolean isOnContract,
          boolean isOnCombatContract, boolean isTravelling) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.isChargingMaintenance()
                  || !campaignOptions.get(CampaignOption.CHECK_MAINTENANCE)
                  || !unit.requiresMaintenance()) {
            return Money.zero();
        }

        boolean isUnitInTransit = isTravelling && unit.hasLocation();
        if (!isMaintenanceEnabledForSituation(campaignOptions, isOnContract, isUnitInTransit)) {
            return Money.zero();
        }

        int cycleDays = unit.getMaintenanceCycleDuration(campaignOptions.get(CampaignOption.MAINTENANCE_CYCLE_DAYS));
        if (cycleDays <= 0) {
            return Money.zero();
        }

        double cyclesPerDay = (isOnCombatContract ? 1.0 : 0.25) / cycleDays;
        double multiplier = getSituationalMaintenanceCostMultiplier(campaignOptions,
              unit,
              isOnCombatContract,
              isUnitInTransit);
        return unit.getMaintenanceCost().multipliedBy(cyclesPerDay * multiplier);
    }

    private static String getDamageString(Unit unit, int nDamage, int nDestroy) {
        String damageString = "";
        if (nDamage > 0) {
            damageString += nDamage + " parts were damaged. ";
        }
        if (nDestroy > 0) {
            damageString += nDestroy + " parts were destroyed.";
        }
        if (!damageString.isEmpty()) {
            damageString = "<b><font color='" +
                                 getNegativeColor() +
                                 "'>" +
                                 damageString +
                                 "</b></font> [<a href='REPAIR|" +
                                 unit.getId() +
                                 "'>Repair bay</a>]";
        }
        return damageString;
    }

    /**
     * @param maintenanceTech the tech who did the maintenance, or {@code null} when nobody could, in which case the
     *                        part is checked as unmaintained
     */
    private static String doMaintenanceOnUnitPart(Campaign campaign, @Nullable Person maintenanceTech, Part part,
          Map<Part, Integer> partsToDamage,
          boolean paidMaintenance, int asTechsUsed) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        String partReport = "<b>" + part.getName() + "</b> (Quality " + part.getQualityName() + ')';
        if (!part.needsMaintenance()) {
            return null;
        }
        PartQuality oldQuality = part.getQuality();
        TargetRoll target = getTargetForMaintenance(campaign, part, maintenanceTech, asTechsUsed);
        if (!paidMaintenance) {
            // TODO : Make campaign modifier user configurable
            target.addModifier(1, "did not pay for maintenance");
        }

        Skill maintenanceSkill = (maintenanceTech == null) ? null : maintenanceTech.getSkillForWorkingOn(part);
        int roll;
        if (maintenanceSkill == null) {
            roll = d6(2);
            // getValueAsString() renders an IMPOSSIBLE target as a word rather than its Integer.MAX_VALUE sentinel.
            partReport += getFormattedTextAt(RESOURCE_BUNDLE, "Maintenance.check.reportNoSkill",
                  target.getValueAsString(), target.getDesc(), String.valueOf(roll));
        } else {
            // withoutSubject: this report already names the tech; getReport(false): the numeric margin is appended
            // once, below, so the utility's own margin label is suppressed to avoid printing the margin twice.
            ActionCheckResult result = new SkillCheck(maintenanceTech, maintenanceSkill.getType(), target)
                                             .withoutSubject()
                                             .resolve(false, null);
            roll = result.getRollResult();
            partReport += getFormattedTextAt(RESOURCE_BUNDLE, "Maintenance.check.report",
                  result.getReport(false), target.getDesc());
        }
        int margin = roll - target.getValue();
        partReport += getFormattedTextAt(RESOURCE_BUNDLE, "Maintenance.check.margin", String.valueOf(margin));

        switch (part.getQuality()) {
            case QUALITY_A: {
                if (margin >= 4) {
                    part.improveQuality();
                }
                if (!campaignOptions.get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE)) {
                    if (margin < -6) {
                        partsToDamage.put(part, 4);
                    } else if (margin < -4) {
                        partsToDamage.put(part, 3);
                    } else if (margin == -4) {
                        partsToDamage.put(part, 2);
                    } else if (margin < -1) {
                        partsToDamage.put(part, 1);
                    }
                } else if (margin < -6) {
                    partsToDamage.put(part, 1);
                }
                break;
            }
            case QUALITY_B: {
                if (margin >= 4) {
                    part.improveQuality();
                } else if (margin < -5) {
                    part.reduceQuality();
                }
                if (!campaignOptions.get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE)) {
                    if (margin < -6) {
                        partsToDamage.put(part, 2);
                    } else if (margin < -2) {
                        partsToDamage.put(part, 1);
                    }
                }
                break;
            }
            case QUALITY_C: {
                if (margin < -4) {
                    part.reduceQuality();
                } else if (margin >= 5) {
                    part.improveQuality();
                }
                if (!campaignOptions.get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE)) {
                    if (margin < -6) {
                        partsToDamage.put(part, 2);
                    } else if (margin < -3) {
                        partsToDamage.put(part, 1);
                    }
                }
                break;
            }
            case QUALITY_D: {
                if (margin < -3) {
                    part.reduceQuality();
                    if ((margin < -4) && !campaignOptions.get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE)) {
                        partsToDamage.put(part, 1);
                    }
                } else if (margin >= 5) {
                    part.improveQuality();
                }
                break;
            }
            case QUALITY_E:
                if (margin < -2) {
                    part.reduceQuality();
                    if ((margin < -5) && !campaignOptions.get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE)) {
                        partsToDamage.put(part, 1);
                    }
                } else if (margin >= 6) {
                    part.improveQuality();
                }
                break;
            case QUALITY_F:
            default:
                if (margin < -2) {
                    part.reduceQuality();
                    if (margin < -6 && !campaignOptions.get(CampaignOption.USE_UNOFFICIAL_MAINTENANCE)) {
                        partsToDamage.put(part, 1);
                    }
                }

                break;
        }
        if (part.getQuality().toNumeric() > oldQuality.toNumeric()) {
            partReport += ": " +
                                ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorPositiveHexColor(),
                                      "new quality is " + part.getQualityName());
        } else if (part.getQuality().toNumeric() < oldQuality.toNumeric()) {
            partReport += ": " +
                                ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorNegativeHexColor(),
                                      "new quality is " + part.getQualityName());
        } else {
            partReport += ": quality remains " + part.getQualityName();
        }
        if (null != partsToDamage.get(part)) {
            if (partsToDamage.get(part) > 3) {
                partReport += ", " +
                                    ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                              .getFontColorNegativeHexColor(),
                                          "<b>part destroyed</b>");
            } else {
                partReport += ", " +
                                    ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                              .getFontColorNegativeHexColor(),
                                          "<b>part damaged</b>");
            }
        }

        return partReport;
    }

    public static TargetRoll getTargetForMaintenance(Campaign campaign, IPartWork partWork, Person tech,
          int asTechsUsed) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        int value = 10;
        String skillLevel = "Unmaintained";
        SkillModifierData skillModifierData = null;
        if (null != tech) {
            Skill skill = tech.getMaintenanceOrRefitSkill(partWork.getUnit());
            skillModifierData = tech.getSkillModifierData();
            if (null != skill) {
                value = skill.getFinalSkillValue(skillModifierData);
                skillLevel = skill.getSkillLevel(skillModifierData).toString();
            }
        }

        TargetRoll target = new TargetRoll(value, skillLevel);
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllModsForMaintenance());

        // A technician with a Descartes diagnostic scanner (or a Deluxe Toolkit) gets a bonus to the maintenance check.
        int maintenanceKitBonus = EquipmentKitCatalog.maintenanceBonus(tech);
        if (maintenanceKitBonus != 0) {
            target.addModifier(-maintenanceKitBonus, "technician kit");
        }

        Familiarity familiarity = campaignOptions.get(CampaignOption.CHASSIS_FAMILIARITY_MODE);
        Unit partUnit = partWork.getUnit();
        if (familiarity.isEnabled() && tech != null && partUnit != null) {
            Entity partEntity = partUnit.getEntity();
            if (partEntity != null) {
                int bonus = tech.getChassisFamiliarityTechBonus(familiarity, partEntity, false);
                if (bonus != 0) {
                    target.addModifier(-bonus, "Chassis Familiarity");
                }
            }
        }

        if (campaignOptions.get(CampaignOption.USE_ERA_MODS)) {
            target.addModifier(campaign.getPlayerForce().getFaction().getEraMod(campaign.getGameYear()), "era");
        }

        PlanetaryWorkConditions.addRollModifiers(campaign, partUnit, tech, target);

        if (tech != null && partUnit != null) {
            // the AsTech issue is crazy, because you can actually be better off
            // not maintaining
            // than going it short-handed, but that is just the way it is.
            // Still, there is also some fuzziness about what happens if you are
            // short AsTechs
            // for part of the cycle.
            final int helpMod;
            if (partUnit.isSelfCrewed()) {
                helpMod = campaign.getShorthandedModForCrews(partUnit.getEntity().getCrew());
            } else {
                helpMod = campaign.getShorthandedMod(asTechsUsed, false);
            }

            if (helpMod > 0) {
                target.addModifier(helpMod, "shorthanded");
            }

            // like repairs, per CamOps page 208 extra time gives a
            // reduction to the TN based on x2, x3, x4
            if (partUnit.getMaintenanceMultiplier() > 1) {
                target.addModifier(-(partUnit.getMaintenanceMultiplier() - 1), "extra time");
            }
        }

        return target;
    }

    /**
     * Verifies and corrects per-technician maintenance schedules so that each day does not exceed the technician's
     * available minutes and urgent work is not missed.
     *
     * <p>The method:</p>
     * <ul>
     *   <li>Sorts each day's jobs by shortest maintenance time first to maximize the number of units maintained per
     *   day.</li>
     *   <li>If a scheduled day overflows, backfills to the latest earlier day with capacity.</li>
     *   <li>If the only day with capacity is today, defers the actual call to immediate maintenance until after
     *   scheduling completes.</li>
     *   <li>If no day (including today) has capacity, records the unit as unable to maintain and reports it.</li>
     *   <li>For engineers (who only maintain their own unit), validates that today's available minutes are
     *   sufficient and reports if not.</li>
     * </ul>
     *
     * <p>A unit is marked as 'unable to maintain' when...</p>
     * <ul>
     *   <li>No earlier day (today -> scheduled−1) has enough remaining minutes.</li>
     *   <li>Today is the only candidate but lacks sufficient minutes.</li>
     * </ul>
     *
     * @param campaign the active campaign providing options, dates, personnel, unit data, and the reporting sink
     *
     * @author Illiani
     * @see #performImmediateMaintenance(Campaign, Unit)
     * @since 0.50.10
     */
    public static void checkAndCorrectMaintenanceSchedule(Campaign campaign) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.get(CampaignOption.CHECK_MAINTENANCE)) {
            LOGGER.debug("[Maintenance] schedule not checked: maintenance checks are off");
            return;
        }
        final int maintenanceCycleDuration = campaignOptions.get(CampaignOption.MAINTENANCE_CYCLE_DAYS);
        final boolean techsUseAdmin = campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION);

        final boolean hasActiveMission = !campaign.getActiveContracts().isEmpty();
        final LocalDate today = campaign.getLocalDate();

        List<Person> allTechs = campaign.getPlayerForce()
                                      .getHumanResources()
                                      .getTechsExpanded(campaign.getPlayerForce().getHangar().getUnits(),
                                            campaign.getCampaignOptions(),
                                            campaign.getPlayerForce().isClanForce(),
                                            campaign.getLocalDate());
        for (Person tech : allTechs) {
            int dailyWorkMinutes = tech.getDailyAvailableTechTime(techsUseAdmin);

            // Engineers only maintain their own unit, so we're just going to verify that they will have enough time
            // to perform that maintenance.
            if (tech.isEngineer()) {
                String report = performEngineerCheck(tech, dailyWorkMinutes);
                if (!report.isBlank()) {
                    campaign.addReport(TECHNICAL, report);
                }
                continue;
            }

            // Units whose maintenance is paused in their current situation are not due, so they are not scheduled
            List<Unit> techUnits = new ArrayList<>();
            for (Unit techUnit : tech.getTechUnits()) {
                if (isMaintenanceEnabledForSituation(campaign, techUnit)) {
                    techUnits.add(techUnit);
                }
            }
            if (techUnits.isEmpty()) {
                continue;
            }

            // Build a map of dates -> units initially scheduled
            LinkedHashMap<LocalDate, List<Unit>> correctedSchedule = new LinkedHashMap<>();
            LinkedHashMap<LocalDate, List<Unit>> maintenanceSchedule = buildSchedule(techUnits,
                  maintenanceCycleDuration,
                  hasActiveMission,
                  today,
                  correctedSchedule);

            // Remaining minutes per day
            Map<LocalDate, Integer> remainingMinutesPerDay = new LinkedHashMap<>();
            for (LocalDate day : correctedSchedule.keySet()) {
                remainingMinutesPerDay.put(day, dailyWorkMinutes);
            }

            List<Unit> immediateToday = new ArrayList<>();
            List<Unit> unableToMaintain = new ArrayList<>();
            for (Map.Entry<LocalDate, List<Unit>> schedule : maintenanceSchedule.entrySet()) {
                evaluateMaintenanceSchedule(schedule,
                      remainingMinutesPerDay,
                      correctedSchedule,
                      today,
                      immediateToday,
                      unableToMaintain);
            }

            // Post-evaluation reports - this is where we tell the player what's going on.
            for (Unit maintainedUnit : immediateToday) {
                String report = getFormattedTextAt(RESOURCE_BUNDLE, "Maintenance.immediateToday",
                      tech.getHyperlinkedFullTitle(), maintainedUnit.getHyperlinkedName());
                campaign.addReport(TECHNICAL, report);
                performImmediateMaintenance(campaign, maintainedUnit);
            }

            for (Unit maintainedUnit : unableToMaintain) {
                String report = getFormattedTextAt(RESOURCE_BUNDLE, "Maintenance.unableToMaintain",
                      spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG,
                      tech.getHyperlinkedFullTitle(), maintainedUnit.getHyperlinkedName());
                campaign.addReport(TECHNICAL, report);
            }
        }
    }

    /**
     * Builds the initial and corrected maintenance schedules for a technician's assigned units, based on each unit's
     * remaining maintenance cycle time and the current campaign conditions.
     *
     * <p><b>Behavior</b></p>
     * <ul>
     *   <li>Calculates the number of days until each unit's next maintenance is due using
     *   {@link Unit#getDaysUntilNextMaintenance(int)}.</li>
     *   <li>Any negative or zero values are treated as due today to ensure overdue units are handled promptly.</li>
     *   <li>If the campaign has no active mission, multiplies the computed days by four to account for slower
     *   maintenance progress when idle. This only affects scheduling, not technician work time.</li>
     *   <li>Populates {@code maintenanceSchedule} with each unit keyed by its scheduled maintenance date.</li>
     *   <li>Determines the final day represented in the schedule and populates {@code correctedSchedule} with all
     *   dates from today through that day, initializing each with an empty list to ensure coverage for all days in
     *   the range.</li>
     *   <li>Finally, sorts the maintenance schedule chronologically (soonest to latest) and returns it as a
     *   {@link LinkedHashMap}.</li>
     * </ul>
     *
     * @param techUnits                the list of units assigned to the technician
     * @param maintenanceCycleDuration the duration of a full maintenance cycle in days
     * @param hasActiveMission         {@code true} if the campaign currently has an active mission, affecting
     *                                 scheduling
     * @param today                    the current in-game date
     * @param correctedSchedule        the map to populate with empty lists for all days between today and the last
     *                                 scheduled maintenance date
     *
     * @return a {@link LinkedHashMap} of maintenance jobs sorted by maintenance date (soonest -> latest)
     */
    private static LinkedHashMap<LocalDate, List<Unit>> buildSchedule(List<Unit> techUnits,
          int maintenanceCycleDuration, boolean hasActiveMission, LocalDate today,
          LinkedHashMap<LocalDate, List<Unit>> correctedSchedule) {
        LinkedHashMap<LocalDate, List<Unit>> maintenanceSchedule = new LinkedHashMap<>();

        for (Unit maintainedUnit : techUnits) {
            if (!maintainedUnit.requiresMaintenance()) {
                continue;
            }

            // Treat negative/zero as due today (this is just for added security)
            double daysUntilNextMaintenance =
                  max(0.0, maintainedUnit.getDaysUntilNextMaintenance(maintenanceCycleDuration));

            // Adjust when not under contract: maintenance progress is x0.25 normal
            if (!hasActiveMission) {
                daysUntilNextMaintenance *= 4;
            }

            LocalDate scheduleMaintenance = today.plusDays((int) Math.ceil(daysUntilNextMaintenance));
            maintenanceSchedule.computeIfAbsent(scheduleMaintenance, k -> new ArrayList<>())
                  .add(maintainedUnit);
        }

        LocalDate lastDay = maintenanceSchedule.keySet().stream()
                                  .max(Comparator.naturalOrder())
                                  .orElse(today);

        for (LocalDate day = today; !day.isAfter(lastDay); day = day.plusDays(1)) {
            correctedSchedule.put(day, new ArrayList<>());
        }

        // Sort the initial schedule by day (soonest -> latest)
        maintenanceSchedule = maintenanceSchedule.entrySet().stream()
                                    .sorted(Map.Entry.comparingByKey())
                                    .collect(Collectors.toMap(
                                          Map.Entry::getKey,
                                          Map.Entry::getValue,
                                          (a, b) -> a,
                                          LinkedHashMap::new
                                    ));
        return maintenanceSchedule;
    }

    /**
     * Evaluates a single day's maintenance schedule for a technician and adjusts assignments to ensure daily work
     * limits are not exceeded. Jobs that cannot fit on their scheduled day are backfilled to the latest earlier day
     * with remaining capacity. If only today has capacity, the job is marked for immediate maintenance. If no earlier
     * day has available time, the unit is recorded as unable to maintain.
     *
     * <p><b>Algorithm</b></p>
     * <ul>
     *   <li>Sorts all jobs for the scheduled day by shortest maintenance time first (shortest-job-first) to maximize
     *   the number of units maintained.</li>
     *   <li>Checks whether each job fits in the scheduled day. If not, iterates backward from the scheduled day − 1
     *   to today, selecting the latest day with enough remaining minutes.</li>
     *   <li>If that day is today, the job is queued for immediate maintenance.</li>
     *   <li>If no day (including today) has sufficient capacity, the unit is added to {@code unableToMaintain}.</li>
     * </ul>
     *
     * @param schedule               the entry representing one scheduled day and its units
     * @param remainingMinutesPerDay map tracking each day's remaining available minutes for maintenance
     * @param correctedSchedule      the corrected per-day schedule being built
     * @param today                  the current in-game date
     * @param immediateToday         list collecting units requiring immediate maintenance today
     * @param unableToMaintain       list collecting units that cannot be scheduled or maintained
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void evaluateMaintenanceSchedule(Map.Entry<LocalDate, List<Unit>> schedule,
          Map<LocalDate, Integer> remainingMinutesPerDay, LinkedHashMap<LocalDate, List<Unit>> correctedSchedule,
          LocalDate today, List<Unit> immediateToday, List<Unit> unableToMaintain) {
        LocalDate day = schedule.getKey();
        List<Unit> maintenanceJobs = new ArrayList<>(schedule.getValue());

        // Shortest-job-first maximizes the count of units maintained
        maintenanceJobs.sort(Comparator.comparingInt(Unit::getMaintenanceTime));

        for (Unit maintainedUnit : maintenanceJobs) {
            int maintenanceTime = maintainedUnit.getMaintenanceTime();

            // Try the scheduled day first
            if (maintenanceTime <= remainingMinutesPerDay.get(day)) {
                correctedSchedule.get(day).add(maintainedUnit);
                remainingMinutesPerDay.put(day, remainingMinutesPerDay.get(day) - maintenanceTime);
                continue;
            }

            // Backfill: latest earlier day with capacity (today..day-1)
            LocalDate assignedDate = null;
            for (LocalDate potentialDay = day.minusDays(1);
                  !potentialDay.isBefore(today);
                  potentialDay = potentialDay.minusDays(1)) {
                if (maintenanceTime <= remainingMinutesPerDay.get(potentialDay)) {
                    assignedDate = potentialDay;
                    break; // latest first because we iterate backward
                }
            }

            if (assignedDate != null) {
                if (assignedDate.equals(today)) {
                    // Only today can fit -> immediate maintenance
                    if (maintenanceTime <= remainingMinutesPerDay.get(today)) {
                        immediateToday.add(maintainedUnit);
                        remainingMinutesPerDay.put(today, remainingMinutesPerDay.get(today) - maintenanceTime);
                    } else {
                        unableToMaintain.add(maintainedUnit);
                    }
                } else {
                    correctedSchedule.get(assignedDate).add(maintainedUnit);
                    remainingMinutesPerDay.put(assignedDate,
                          remainingMinutesPerDay.get(assignedDate) - maintenanceTime);
                }
            } else {
                // No earlier day (including today) has capacity
                unableToMaintain.add(maintainedUnit);
            }
        }
    }

    /**
     * Checks whether an engineer has sufficient daily time to perform maintenance on their assigned unit and, if not,
     * returns a formatted warning message for reporting.
     *
     * @param tech             the engineer to evaluate
     * @param dailyWorkMinutes the engineer's available minutes for the day
     *
     * @return a formatted warning string if maintenance time exceeds available minutes; otherwise an empty string
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String performEngineerCheck(Person tech, int dailyWorkMinutes) {
        Unit techUnit = tech.getUnit();
        if (techUnit == null) {
            LOGGER.warn("[Maintenance] {} is flagged as an engineer but serves on no unit; skipping the engineer check",
                  tech.getFullName());
            return "";
        }
        int maintenanceTime = techUnit.getMaintenanceTime();
        if (techUnit.requiresMaintenance() && maintenanceTime > dailyWorkMinutes) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "Maintenance.largeVessel",
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG,
                  tech.getHyperlinkedFullTitle(), techUnit.getHyperlinkedName());
        }

        return "";
    }

    /**
     * Attempts to perform immediate maintenance on a unit using its assigned technician's remaining minutes for today.
     *
     * @param campaign the campaign context used for maintenance processing and reporting
     * @param unit     the unit to service immediately
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void performImmediateMaintenance(Campaign campaign, Unit unit) {
        // doMaintenance does nothing in either case, so the loop below would never end
        if (!campaign.getCampaignOptions().get(CampaignOption.CHECK_MAINTENANCE)) {
            LOGGER.debug("[Maintenance] {}: no immediate maintenance, maintenance checks are off", unit.getName());
            return;
        }
        if (!unit.requiresMaintenance()) {
            LOGGER.debug("[Maintenance] {}: no immediate maintenance, the unit does not need it", unit.getName());
            return;
        }
        // doMaintenance does nothing while maintenance is paused, so the loop below would never end
        if (!isMaintenanceEnabledForSituation(campaign, unit)) {
            campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE, "Maintenance.pausedForSituation",
                  unit.getHyperlinkedName()));
            return;
        }
        Person tech = unit.getTech(); // This gets the engineer, instead, if appropriate
        if (tech == null) {
            return;
        }

        int time = tech.getMinutesLeft();
        int maintenanceTime = unit.getMaintenanceTime();

        if ((time - maintenanceTime) >= 0) {
            // This will increase the number of days until maintenance and then perform the maintenance. We
            // do it this way to ensure that everything is processed cleanly.
            while (unit.getDaysSinceMaintenance() != 0) {
                Maintenance.doMaintenance(campaign, unit);
            }
        } else {
            campaign.addReport(TECHNICAL, getFormattedText("maintenanceAdHoc.unable",
                  tech.getHyperlinkedFullTitle(),
                  unit.getHyperlinkedName()));
        }
    }
}
