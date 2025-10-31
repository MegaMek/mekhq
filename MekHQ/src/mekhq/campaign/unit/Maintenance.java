/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
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
package mekhq.campaign.unit;

import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.personnel.skills.SkillType.S_ZERO_G_OPERATIONS;
import static mekhq.campaign.unit.Unit.SITE_FACILITY_BASIC;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.universe.Atmosphere;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.work.IPartWork;
import mekhq.utilities.ReportingUtilities;

/**
 * The code in this class previously lived in {@link Campaign} and were migrated as part of an effort to reduce the size
 * of that class. The move took place in the 0.50.10 cycle, and no changes were made beyond those necessary to
 * facilitate the move.
 */
public class Maintenance {
    private static final MMLogger LOGGER = MMLogger.create(Maintenance.class);

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
          MekHQ.getMHQOptions().getLocale());

    public static void doMaintenance(Campaign campaign, Unit unit) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!unit.requiresMaintenance() || !campaignOptions.isCheckMaintenance()) {
            return;
        }
        // let's start by checking times
        int minutesUsed = unit.getMaintenanceTime();
        int asTechsUsed = 0;
        boolean maintained;
        boolean paidMaintenance = true;

        unit.incrementDaysSinceMaintenance(campaign, false, asTechsUsed);

        int ruggedMultiplier = 1;
        if (unit.getEntity().hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_1)) {
            ruggedMultiplier = 2;
        }

        if (unit.getEntity().hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_2)) {
            ruggedMultiplier = 3;
        }

        if (unit.getDaysSinceMaintenance() >= (campaignOptions.getMaintenanceCycleDays() * ruggedMultiplier)) {
            Person tech = unit.getTech();
            if (tech != null) {
                int availableMinutes = tech.getMinutesLeft();
                maintained = (availableMinutes >= minutesUsed);

                if (!maintained) {
                    // At campaign point, insufficient minutes is the only reason why campaign would be failed.
                    campaign.addReport(String.format(resources.getString("maintenanceNotAvailable.text"),
                          unit.getName()));
                } else {
                    maintained = !tech.isMothballing();
                }

                if (maintained) {
                    tech.setMinutesLeft(availableMinutes - minutesUsed);
                    asTechsUsed = campaign.getAvailableAsTechs(minutesUsed, false);
                    campaign.setAsTechPoolMinutes(campaign.getAsTechPoolMinutes() - (asTechsUsed * minutesUsed));
                }
            }

            // maybe use the money
            if (campaignOptions.isPayForMaintain()) {
                if (!(campaign.getFinances().debit(TransactionType.MAINTENANCE,
                      campaign.getLocalDate(),
                      unit.getMaintenanceCost(),
                      "Maintenance for " + unit.getName()))) {
                    campaign.addReport("<font color='" +
                                             ReportingUtilities.getNegativeColor() +
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
                          unit,
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
                    campaign.addReport(String.format(
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

            if (campaignOptions.isLogMaintenance()) {
                LOGGER.info(maintenanceReport.toString());
            }

            PartQuality quality = unit.getQuality();
            String qualityString;
            boolean reverse = campaignOptions.isReverseQualityNames();
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
                                   ReportingUtilities.getNegativeColor() +
                                   "'>Could not afford maintenance costs, so check is at a penalty.</font>";
            }
            campaign.addReport(techNameLinked +
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
                                 ReportingUtilities.getNegativeColor() +
                                 "'>" +
                                 damageString +
                                 "</b></font> [<a href='REPAIR|" +
                                 unit.getId() +
                                 "'>Repair bay</a>]";
        }
        return damageString;
    }

    private static String doMaintenanceOnUnitPart(Campaign campaign, Unit unit, Part part,
          Map<Part, Integer> partsToDamage,
          boolean paidMaintenance, int asTechsUsed) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        String partReport = "<b>" + part.getName() + "</b> (Quality " + part.getQualityName() + ')';
        if (!part.needsMaintenance()) {
            return null;
        }
        PartQuality oldQuality = part.getQuality();
        TargetRoll target = getTargetForMaintenance(campaign, part, unit.getTech(), asTechsUsed);
        if (!paidMaintenance) {
            // TODO : Make campaign modifier user imputable
            target.addModifier(1, "did not pay for maintenance");
        }

        partReport += ", TN " + target.getValue() + '[' + target.getDesc() + ']';
        int roll = d6(2);
        int margin = roll - target.getValue();
        partReport += " rolled a " + roll + ", margin of " + margin;

        switch (part.getQuality()) {
            case QUALITY_A: {
                if (margin >= 4) {
                    part.improveQuality();
                }
                if (!campaignOptions.isUseUnofficialMaintenance()) {
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
                if (!campaignOptions.isUseUnofficialMaintenance()) {
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
                if (!campaignOptions.isUseUnofficialMaintenance()) {
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
                    if ((margin < -4) && !campaignOptions.isUseUnofficialMaintenance()) {
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
                    if ((margin < -5) && !campaignOptions.isUseUnofficialMaintenance()) {
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
                    if (margin < -6 && !campaignOptions.isUseUnofficialMaintenance()) {
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
        PersonnelOptions options = null;
        Attributes attributes = null;
        if (null != tech) {
            options = tech.getOptions();
            attributes = tech.getATOWAttributes();

            Skill skill = tech.getSkillForWorkingOn(partWork);
            if (null != skill) {
                value = skill.getFinalSkillValue(options, attributes);
                skillLevel = skill.getSkillLevel(options, attributes).toString();
            }
        }

        TargetRoll target = new TargetRoll(value, skillLevel);
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            return target;
        }

        target.append(partWork.getAllModsForMaintenance());

        if (campaignOptions.isUseEraMods()) {
            target.addModifier(campaign.getFaction().getEraMod(campaign.getGameYear()), "era");
        }

        if (partWork.getUnit().getSite() < SITE_FACILITY_BASIC) {
            if (campaign.getLocation().isOnPlanet() && campaignOptions.isUsePlanetaryModifiers()) {
                Planet planet = campaign.getLocation().getPlanet();
                Atmosphere atmosphere = planet.getAtmosphere(campaign.getLocalDate());
                megamek.common.planetaryConditions.Atmosphere planetaryConditions = planet.getPressure(campaign.getLocalDate());
                int temperature = planet.getTemperature(campaign.getLocalDate());

                Skill zeroGSkill = tech == null ? null : tech.getSkill(S_ZERO_G_OPERATIONS);
                int zeroGSkillLevel = 0;
                if (zeroGSkill != null) {
                    zeroGSkillLevel = zeroGSkill.getTotalSkillLevel(options, attributes);
                }

                if (planet.getGravity() < 0.8) {
                    int modifier = 2;
                    target.addModifier(modifier, "Low Gravity");
                    addZeroGOperationsModifier(zeroGSkillLevel, modifier, target);
                } else if (planet.getGravity() >= 2.0) {
                    int modifier = 4;
                    target.addModifier(modifier, "Very High Gravity");
                    addZeroGOperationsModifier(zeroGSkillLevel, modifier, target);
                } else if (planet.getGravity() > 1.2) {
                    int modifier = 1;
                    target.addModifier(modifier, "High Gravity");
                    addZeroGOperationsModifier(zeroGSkillLevel, modifier, target);
                }

                if (atmosphere.isTainted() || atmosphere.isToxic()) {
                    target.addModifier(2, "Tainted or Toxic Atmosphere");
                } else if (planetaryConditions.isVacuum()) {
                    target.addModifier(2, "Vacuum");
                }

                if (planetaryConditions.isTrace() || planetaryConditions.isVeryHigh()) {
                    target.addModifier(1, "Trace or Very High Pressure Atmosphere");
                }

                if (temperature < -30 || temperature > 50) {
                    target.addModifier(1, "Extreme Temperature");
                }
            }
        }

        if (null != partWork.getUnit() && null != tech) {
            // the AsTech issue is crazy, because you can actually be better off
            // not maintaining
            // than going it short-handed, but that is just the way it is.
            // Still, there is also some fuzziness about what happens if you are
            // short AsTechs
            // for part of the cycle.
            final int helpMod;
            if (partWork.getUnit().isSelfCrewed()) {
                helpMod = campaign.getShorthandedModForCrews(partWork.getUnit().getEntity().getCrew());
            } else {
                helpMod = campaign.getShorthandedMod(asTechsUsed, false);
            }

            if (helpMod > 0) {
                target.addModifier(helpMod, "shorthanded");
            }

            // like repairs, per CamOps page 208 extra time gives a
            // reduction to the TN based on x2, x3, x4
            if (partWork.getUnit().getMaintenanceMultiplier() > 1) {
                target.addModifier(-(partWork.getUnit().getMaintenanceMultiplier() - 1), "extra time");
            }
        }

        return target;
    }

    /**
     * Applies a Zero-G Operations skill gravityModifier to the specified {@link TargetRoll}, offsetting a penalty
     * gravityModifier.
     *
     * <ul>
     *   <li>If {@code zeroGSkillLevel} >= {@code gravityModifier}, does nothing.</li>
     *   <li>If {@code zeroGSkillLevel} < {@code gravityModifier}, applies {@code -zeroGSkillLevel}.</li>
     * </ul>
     *
     * @param zeroGSkillLevel the Zero-G Operations skill level, which negates up to that much penalty from the
     *                        gravityModifier
     * @param gravityModifier the penalty modifier value to offset
     * @param target          the {@link TargetRoll} instance to modify
     */
    private static void addZeroGOperationsModifier(int zeroGSkillLevel, int gravityModifier, TargetRoll target) {
        if (zeroGSkillLevel > 0) {
            int effectiveModifier = zeroGSkillLevel >= gravityModifier ? 0 : -zeroGSkillLevel;

            if (effectiveModifier < 0) {
                target.addModifier(effectiveModifier, "Zero-G Operations");
            }
        }
    }
}
