/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.turnoverAndRetention;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.List;
import java.util.UUID;
import java.util.Vector;

import megamek.common.enums.SkillLevel;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.stratCon.StratConRulesManager;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;

/**
 * The {@code Fatigue} class provides utility methods for managing and processing fatigue-related mechanics in the
 * campaign. This includes calculating effective fatigue, processing daily and weekly fatigue recovery, handling field
 * kitchen requirements, and generating related reports.
 *
 * <p>Fatigue is a system that affects personnel as part of campaign management. This class
 * ensures consistent handling of fatigue calculations, recovery, and statuses based on campaign settings and personnel
 * conditions.</p>
 */
public class Fatigue {
    final private static String RESOURCE_BUNDLE = "mekhq.resources.Fatigue";

    static final private int FATIGUE_RECOVERY_RATE = 1;

    /**
     * Calculates the total field kitchen capacity for a given list of units.
     *
     * <p>Deployed, damaged, uncrewed, or partially crewed units are excluded from the calculation.
     * Each remaining unit contributes to the overall capacity based on the presence of the
     * {@link MiscType#F_FIELD_KITCHEN} flag in its equipment.</p>
     *
     * @param units                the list of units to evaluate for field kitchen capacity.
     * @param fieldKitchenCapacity the capacity provided by each field kitchen.
     *
     * @return the total field kitchen capacity available from all eligible units.
     */
    public static int checkFieldKitchenCapacity(List<Unit> units, int fieldKitchenCapacity) {
        int fieldKitchenCount = 0;

        for (Unit unit : units) {
            if ((unit.isDeployed())
                      || (unit.isDamaged())
                      || (unit.getCrewState().isUncrewed())
                      || (unit.getCrewState().isPartiallyCrewed())) {
                continue;
            }

            for (MiscMounted item : unit.getEntity().getMisc()) {
                if (item.getType().hasFlag(MiscType.F_FIELD_KITCHEN)) {
                    fieldKitchenCount++;
                }
            }
        }

        return fieldKitchenCount * fieldKitchenCapacity;
    }

    /**
     * Calculates the number of personnel who require field kitchen support.
     *
     * <p>Personnel assigned to units with sufficient onboard field kitchen facilities
     * (e.g., small or large craft units) are excluded. Non-combatant personnel may also be excluded based on the
     * {@code isUseFieldKitchenIgnoreNonCombatants} parameter.</p>
     *
     * @param activePersonnel                      the list of active personnel to evaluate.
     * @param isUseFieldKitchenIgnoreNonCombatants flag to exclude non-combatants from the total.
     *
     * @return the total number of personnel requiring field kitchen support.
     */
    public static int checkFieldKitchenUsage(List<Person> activePersonnel,
          boolean isUseFieldKitchenIgnoreNonCombatants) {
        int fieldKitchenUsage = 0;

        for (Person person : activePersonnel) {
            if (!person.isCombat() && isUseFieldKitchenIgnoreNonCombatants) {
                continue;
            }

            Unit unit = person.getUnit();

            if (unit == null) {
                fieldKitchenUsage++;
                continue;
            }

            Entity entity = unit.getEntity();

            if (entity == null) {
                fieldKitchenUsage++;
                continue;
            }

            // These units include sufficient field kitchen capacity for their crews, so their
            // personnel are skipped.
            if (entity.isLargeCraft() || entity.isSmallCraft()) {
                continue;
            }

            fieldKitchenUsage++;
        }

        return fieldKitchenUsage;
    }

    /**
     * Checks if the available field kitchen capacity is sufficient to meet the requirements.
     *
     * @param fieldKitchenCapacity the total available field kitchen capacity.
     * @param fieldKitchenUsage    the total field kitchen usage based on personnel requirements.
     *
     * @return {@code true} if the available capacity is sufficient; {@code false} otherwise.
     */
    public static boolean areFieldKitchensWithinCapacity(int fieldKitchenCapacity, int fieldKitchenUsage) {
        return fieldKitchenCapacity >= fieldKitchenUsage;
    }

    /**
     * Processes fatigue-related actions for a given person in the campaign.
     *
     * <p>This method calculates the effective fatigue of the person, determines their fatigue
     * state (e.g., tired, fatigued, exhausted, critical), generates reports based on their fatigue level, and updates
     * their recovery status. If the fatigue exceeds the campaign's leave threshold, the person's status is updated to
     * {@link PersonnelStatus#ON_LEAVE}.</p>
     *
     * @param campaign the campaign context in which the person operates.
     * @param person   the person whose fatigue actions are being processed.
     */
    public static void processFatigueActions(Campaign campaign, Person person) {
        int effectiveFatigue = getEffectiveFatigue(person.getFatigue(), person.isClanPersonnel(),
              person.getSkillLevel(campaign, false));

        if (!campaign.getCampaignOptions().isUseFatigue()) {
            return;
        }

        if ((effectiveFatigue >= 5) && (effectiveFatigue < 9)) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "fatigueTired.text",
                  person.getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()),
                  CLOSING_SPAN_TAG));

            person.setIsRecoveringFromFatigue(true);
        } else if ((effectiveFatigue >= 9) && (effectiveFatigue < 12)) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "fatigueFatigued.text",
                  person.getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()),
                  CLOSING_SPAN_TAG));

            person.setIsRecoveringFromFatigue(true);
        } else if ((effectiveFatigue >= 12) && (effectiveFatigue < 16)) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "fatigueExhausted.text",
                  person.getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                  CLOSING_SPAN_TAG));

            person.setIsRecoveringFromFatigue(true);
        } else if (effectiveFatigue >= 17) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "fatigueCritical.text",
                  person.getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                  CLOSING_SPAN_TAG));

            person.setIsRecoveringFromFatigue(true);
        }

        if ((campaign.getCampaignOptions().getFatigueLeaveThreshold() != 0)
                  && (effectiveFatigue >= campaign.getCampaignOptions().getFatigueLeaveThreshold())) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ON_LEAVE);
        }
    }

    /**
     * Processes all combat teams in the campaign to evaluate deployment fatigue responses.
     *
     * <p>For each combat team, this method checks whether the fatigue and deployment rules are enabled in the
     * campaign options. If enabled, it iterates through each unit and crew member of the team's force, calculating
     * effective fatigue. If a unit's crew contains at least one individual with effective fatigue above the
     * undeployment threshold, that unit is counted as fatigued.</p>
     *
     * <p>If at least half of the units in a force are fatigued above the threshold, a formatted warning message is
     * created to indicate that the force has reached critical fatigue levels.</p>
     *
     * @param campaign the campaign containing the combat teams, options, and units to be checked
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void processDeploymentFatigueResponses(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.isUseStratCon() || !campaignOptions.isUseFatigue()) {
            return;
        }

        int leaveThreshold = campaignOptions.getFatigueUndeploymentThreshold();

        for (CombatTeam combatTeam : campaign.getAllCombatTeams()) {
            Force force = combatTeam.getForce(campaign);
            if (force == null || force.isDeployed()) {
                // 'isDeployed' will only return true if the force is deployed to a scenario. In which cases we don't
                // want to yank the unit back until the next Monday checkpoint.
                continue;
            }

            Vector<UUID> unitsInForce = force.getAllUnits(false);
            int fatiguedUnits = 0;
            for (UUID unitId : unitsInForce) {
                Unit unit = campaign.getUnit(unitId);
                if (unit == null || !StratConRulesManager.isUnitDeployedToStratCon(unit)) {
                    continue;
                }

                for (Person person : unit.getCrew()) {
                    int fatigue = person.getFatigue();
                    boolean isClan = person.isClanPersonnel();
                    SkillLevel experienceLevel = person.getSkillLevel(campaign, false);
                    int effectiveFatigue = getEffectiveFatigue(fatigue, isClan, experienceLevel);

                    if (effectiveFatigue >= leaveThreshold) {
                        fatiguedUnits++;
                        break;
                    }
                }
            }

            if (fatiguedUnits >= (unitsInForce.size() / 2)) {
                for (AtBContract contract : campaign.getActiveAtBContracts()) {
                    if (contract.getStratconCampaignState() != null) {
                        for (StratConTrackState track : contract.getStratconCampaignState().getTracks()) {
                            track.unassignForce(force.getId());
                        }
                    }
                }

                String fatigueMessage = getFormattedTextAt(RESOURCE_BUNDLE, "fatigueUndeployed.text",
                      spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG, force.getName());
                campaign.addReport(fatigueMessage);
            }
        }
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    public static int getEffectiveFatigue(int fatigue, boolean isClan, SkillLevel skillLevel,
          boolean areFieldKitchensWithinCapacity) {
        return getEffectiveFatigue(fatigue, isClan, skillLevel);
    }


    /**
     * Calculates the effective fatigue level for a given person based on various modifiers.
     *
     * <p>The base fatigue level is adjusted by factors such as:</p>
     * <ul>
     *     <li>Whether the person is classified as Clan personnel.</li>
     *     <li>The person's skill level, with higher-skilled personnel suffering less fatigue.</li>
     *     <li>Whether field kitchens are operating within their required capacity.</li>
     * </ul>
     *
     * @param fatigue    the base fatigue level of the person.
     * @param isClan     flag indicating whether the person is Clan personnel.
     * @param skillLevel the person's skill level.
     *
     * @return the calculated effective fatigue value.
     */
    public static int getEffectiveFatigue(int fatigue, boolean isClan, SkillLevel skillLevel) {
        int effectiveFatigue = fatigue;

        if (isClan) {
            effectiveFatigue -= 2;
        }

        switch (skillLevel) {
            case VETERAN -> effectiveFatigue--;
            case ELITE, HEROIC, LEGENDARY -> effectiveFatigue -= 2;
            default -> {}
        }

        return effectiveFatigue;
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    public static void processFatigueRecovery(Campaign campaign, Person person) {
        processFatigueRecovery(campaign, person, false);
    }

    /**
     * Handles daily fatigue recovery for a specific person in the campaign.
     *
     * <p>If the person has fatigue, their fatigue is reduced based on a standard recovery rate,
     * with additional adjustments if they are on leave or if the campaign has no active contracts. If fatigue becomes
     * zero or less, the person's recovery state is cleared, and their status may be updated to {@code ACTIVE} if they
     * were previously on leave.</p>
     *
     * @param campaign                       the campaign context in which the fatigue recovery occurs.
     * @param person                         the person whose fatigue recovery is being handled.
     * @param fieldKitchensAreWithinCapacity flag indicating if field kitchens are within capacity.
     */
    public static void processFatigueRecovery(Campaign campaign, Person person,
          boolean fieldKitchensAreWithinCapacity) {
        if (person.getFatigue() > 0) {
            int fatigueAdjustment = FATIGUE_RECOVERY_RATE;

            if (person.getStatus().isOnLeave() || campaign.getActiveContracts().isEmpty()) {
                fatigueAdjustment++;
            }

            if (fieldKitchensAreWithinCapacity) {
                fatigueAdjustment++;
            }

            person.changeFatigue(-fatigueAdjustment);

            if (person.getFatigue() < 0) {
                person.setFatigue(0);
            }
        }

        if (campaign.getCampaignOptions().isUseFatigue()) {
            if ((!person.getStatus().isOnLeave()) && (!person.getIsRecoveringFromFatigue())) {
                processFatigueActions(campaign, person);
            }

            if (person.getIsRecoveringFromFatigue()) {
                if (person.getFatigue() <= 0) {
                    campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "fatigueRecovered.text",
                          person.getHyperlinkedFullTitle(),
                          spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                          CLOSING_SPAN_TAG));

                    person.setIsRecoveringFromFatigue(false);

                    if ((campaign.getCampaignOptions().getFatigueLeaveThreshold() != 0)
                              && (person.getStatus().isOnLeave())) {
                        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
                    }
                }
            }
        }
    }
}
