/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.prisoners;

import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.personnel.enums.PersonnelRole.*;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.enums.SkillLevel;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonUtility;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;

/**
 * Utility class for generating non-combatant prisoners captured following a base assault.
 *
 * <p>The generator creates support staff, guards, and civilian camp followers using the current {@link Campaign}
 * and the mission's target {@link SkillLevel}. The resulting captives are returned as
 * {@link ResolveScenarioTracker.OppositionPersonnelStatus} entries flagged as captured and keyed by their
 * {@link Person} IDs.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class NonCombatPrisoners {
    public static WeightedIntMap<PersonnelRole> SUPPORT_ROLES = buildSupportRoles();
    public static List<PersonnelRole> CIVILIAN_ROLES = PersonnelRole.getCivilianRoles();
    public static int INJURY_CHANCE = 10;

    /**
     * Builds the weighted map of support roles used when generating support personnel captives.
     *
     * <p>The weights reflect the relative frequency of each role in a typical base or support installation (e.g.,
     * many more astechs than doctors).</p>
     *
     * @return a {@link WeightedIntMap} where each {@link PersonnelRole} is associated with its selection weight
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static WeightedIntMap<PersonnelRole> buildSupportRoles() {
        Map<PersonnelRole, Integer> supportRoleWeights =
              Map.ofEntries(
                    Map.entry(MEK_TECH, 12),
                    Map.entry(MECHANIC, 4),
                    Map.entry(AERO_TEK, 1),
                    Map.entry(BA_TECH, 1),
                    Map.entry(ASTECH, 108), // 6 per tech 'pick'
                    Map.entry(DOCTOR, 1),
                    Map.entry(MEDIC, 4), // 4 per doctor
                    Map.entry(ADMINISTRATOR_COMMAND, 1),
                    Map.entry(ADMINISTRATOR_LOGISTICS, 5),
                    Map.entry(ADMINISTRATOR_TRANSPORT, 1),
                    Map.entry(ADMINISTRATOR_HR, 3)
              );

        WeightedIntMap<PersonnelRole> weightSortedMap = new WeightedIntMap<>();
        for (Map.Entry<PersonnelRole, Integer> roleEntry : supportRoleWeights.entrySet()) {
            weightSortedMap.add(roleEntry.getValue(), roleEntry.getKey());
        }

        return weightSortedMap;
    }

    /**
     * Generates non-combatant prisoners (support staff, guards, and civilian camp followers) captured as part of the
     * given mission.
     *
     * <p>The number of captives in each category is determined by die rolls. Each generated {@link Person} has their
     * skills overridden according to the current {@link CampaignOptions} and a target {@link SkillLevel}: for
     * {@link AtBContract} missions the enemy skill is used, otherwise {@link SkillLevel#REGULAR} is assumed.</p>
     *
     * <p>All returned {@link ResolveScenarioTracker.OppositionPersonnelStatus} entries are flagged as captured and
     * keyed by the captive's {@link Person#getId() ID}.</p>
     *
     * @param campaign the campaign used to generate new personnel and read campaign options
     * @param mission  the mission that produced these prisoners; may be an {@link AtBContract} to derive the target
     *                 skill level
     *
     * @return a {@link Hashtable} mapping each captive's {@link UUID} to their corresponding opposition personnel
     *       status
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static Hashtable<UUID, ResolveScenarioTracker.OppositionPersonnelStatus> getCivilianCaptives(
          Campaign campaign, Mission mission) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean adminsHaveNegotiation = campaignOptions.isAdminsHaveNegotiation();
        boolean doctorsHaveAdministration = campaignOptions.isDoctorsUseAdministration();
        boolean techsHaveAdministration = campaignOptions.isTechsUseAdministration();
        boolean isUseArtillery = campaignOptions.isUseArtillery();
        boolean isUseAdvancedMedical = campaignOptions.isUseAdvancedMedical();
        boolean isUseExtraRandom = campaign.getRandomSkillPreferences().randomizeSkill();

        SkillLevel targetSkillLevel = SkillLevel.REGULAR;
        if (mission instanceof AtBContract contract) {
            targetSkillLevel = contract.getEnemySkill();
        }

        int supportCount = d6(3); // Support Personnel
        int soldierCount = d6(5); // Guards
        int civilianCount = d6(1); // Camp Followers

        Hashtable<UUID, ResolveScenarioTracker.OppositionPersonnelStatus> civilianCaptives =
              new Hashtable<>();
        for (int i = 0; i < supportCount; i++) {
            PersonnelRole role = SUPPORT_ROLES.randomItem();
            Person captive = generateCaptive(campaign, role, isUseAdvancedMedical);

            addCaptive(captive,
                  civilianCaptives,
                  targetSkillLevel,
                  adminsHaveNegotiation,
                  doctorsHaveAdministration,
                  techsHaveAdministration,
                  isUseArtillery,
                  isUseExtraRandom);
        }

        for (int i = 0; i < soldierCount; i++) {
            Person captive = generateCaptive(campaign, SOLDIER, isUseAdvancedMedical);

            addCaptive(captive,
                  civilianCaptives,
                  targetSkillLevel,
                  adminsHaveNegotiation,
                  doctorsHaveAdministration,
                  techsHaveAdministration,
                  isUseArtillery,
                  isUseExtraRandom);
        }

        for (int i = 0; i < civilianCount; i++) {
            PersonnelRole role = ObjectUtility.getRandomItem(CIVILIAN_ROLES);
            Person captive = generateCaptive(campaign, role, isUseAdvancedMedical);

            addCaptive(captive,
                  civilianCaptives,
                  targetSkillLevel,
                  adminsHaveNegotiation,
                  doctorsHaveAdministration,
                  techsHaveAdministration,
                  isUseArtillery,
                  isUseExtraRandom);
        }

        return civilianCaptives;
    }

    /**
     * Generates a single {@link Person} captive with the given role and applies a chance of injury based on whether the
     * role is combat-oriented.
     *
     * <p>The injury chance is determined by a die roll up to {@code INJURY_CHANCE} (or half that value for combat
     * roles). A result of zero indicates that the captive is injured during their capture.</p>
     *
     * <p>If advanced medical rules are enabled, injuries are applied using
     * {@link InjuryUtil#resolveAfterCombat(Campaign, Person, int)}. Otherwise, the captive simply receives one
     * hit.</p>
     *
     * @param campaign             the campaign used to create the new person and apply medical results
     * @param role                 the {@link PersonnelRole} assigned to the captive
     * @param isUseAdvancedMedical whether advanced medical rules should be used when applying an injury
     *
     * @return a newly generated {@link Person} representing the captive
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static Person generateCaptive(Campaign campaign, PersonnelRole role, boolean isUseAdvancedMedical) {
        Person captive = campaign.newPerson(role);
        int injuryDieSize = role.isCombat() ? INJURY_CHANCE / 2 : INJURY_CHANCE;
        if (randomInt(injuryDieSize) == 0) {
            if (isUseAdvancedMedical) {
                InjuryUtil.resolveAfterCombat(campaign, captive, 1);
            } else {
                captive.setHits(1);
            }
        }
        return captive;
    }


    /**
     * Adds a single generated {@link Person} to the collection of civilian captives, applying campaign-specific skill
     * overrides and marking the person as captured.
     *
     * <p>This helper encapsulates the logic for applying skill overrides via
     * {@link PersonUtility#overrideSkills(boolean, boolean, boolean, boolean, boolean, Person, PersonnelRole,
     * SkillLevel)} and constructing the {@link ResolveScenarioTracker.OppositionPersonnelStatus} wrapper.</p>
     *
     * @param person                    the {@link Person} being added as a captive
     * @param civilianCaptives          the table to which the captive will be added, keyed by {@link Person#getId()}
     * @param targetSkillLevel          the target {@link SkillLevel} used when overriding the captive's skills
     * @param adminsHaveNegotiation     whether administrators gain Negotiation as part of their skill set
     * @param doctorsHaveAdministration whether doctors gain Administration as part of their skill set
     * @param techsHaveAdministration   whether technicians gain Administration as part of their skill set
     * @param isUseArtillery            whether artillery skills should be factored into skill generation
     * @param isUseExtraRandom          whether additional randomization should be applied to the captive's skills
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void addCaptive(Person person,
          Hashtable<UUID, ResolveScenarioTracker.OppositionPersonnelStatus> civilianCaptives,
          SkillLevel targetSkillLevel, boolean adminsHaveNegotiation,
          boolean doctorsHaveAdministration, boolean techsHaveAdministration,
          boolean isUseArtillery, boolean isUseExtraRandom) {
        PersonUtility.overrideSkills(adminsHaveNegotiation,
              doctorsHaveAdministration,
              techsHaveAdministration,
              isUseArtillery,
              isUseExtraRandom,
              person,
              person.getPrimaryRole(),
              targetSkillLevel);

        ResolveScenarioTracker.OppositionPersonnelStatus status =
              new ResolveScenarioTracker.OppositionPersonnelStatus(
                    person.getFullName(),
                    null,
                    person);
        status.setCaptured(true);

        civilianCaptives.put(person.getId(), status);
    }
}
