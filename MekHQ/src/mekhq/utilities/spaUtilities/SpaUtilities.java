/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.utilities.spaUtilities;

import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.S_GUN_BA;
import static mekhq.campaign.personnel.skills.SkillType.getExperienceLevelName;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_PILOTING;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_CREATION_ONLY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_FLAW;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.COMBAT_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.MANEUVERING_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.UTILITY_ABILITY;

import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillPrerequisite;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

/**
 * Utility class for handling Special Pilot Ability (SPA) categorization in MekHQ.
 *
 * <p>This class provides functionality to determine and retrieve the {@link AbilityCategory} for a given
 * {@link SpecialAbility}. Categories include character creation only, flaw, combat, maneuvering, and utility abilities.
 * The logic examines the ability's cost and required skills to make this determination.</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class SpaUtilities {
    private static final MMLogger LOGGER = MMLogger.create(SpaUtilities.class);

    /**
     * The most non-flaw SPAs a character may buy when SPA totals are capped.
     */
    public static final int SPA_CAP = 5;

    /**
     * Support point cost of training a character's first, second, third, fourth, and fifth-or-later non-flaw SPA.
     * Unofficial, following the table in Hot Spots: Draconis Reach first printing pg 34.
     */
    private static final int[] SPA_TRAINING_COSTS_IN_SUPPORT_POINTS = { 60, 180, 360, 600, 900 };

    /**
     * Checks if a given {@link SpecialAbility} belongs to the specified {@link AbilityCategory}.
     *
     * @param ability  the {@code SpecialAbility} to evaluate
     * @param category the {@code AbilityCategory} to compare against
     *
     * @return {@code true} if the ability matches the category, or {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.06
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public static boolean isSpaCategory(final SpecialAbility ability, final AbilityCategory category) {
        return getSpaCategory(ability) == category;
    }

    /**
     * Determines the {@link AbilityCategory} of the provided {@link SpecialAbility} using its cost and pre-requisite
     * skills.
     * <ul>
     *   <li>If the ability cost is -1, it is categorized as {@code CHARACTER_CREATION_ONLY}.</li>
     *   <li>Otherwise, if the cost is negative, it is a {@code CHARACTER_FLAW}.</li>
     *   <li>If its required skills involve combat gunnery (not ProtoMek or BattleArmor), it is a {@code COMBAT_ABILITY}
     *   .</li>
     *   <li>If its required skills involve piloting or certain gunnery types (ProtoMek or BattleArmor), it is a
     *   {@code MANEUVERING_ABILITY}.</li>
     *   <li>Otherwise, it is classified as a {@code UTILITY_ABILITY}.</li>
     * </ul>
     *
     * @param ability the {@code SpecialAbility} for which to determine the category
     *
     * @return the determined {@code AbilityCategory}
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static AbilityCategory getSpaCategory(final SpecialAbility ability) {
        int cost = ability.getCost();
        // is the ability classified as Character Creation only?
        boolean isCharacterCreationOnly = ability.getOriginOnly();

        if (isCharacterCreationOnly) {
            return CHARACTER_CREATION_ONLY;
        }

        // Is the ability classified as a Flaw?
        boolean isFlaw = cost < 0;

        if (isFlaw) {
            return CHARACTER_FLAW;
        }

        boolean isManeuvering = false;
        // Precompile regex patterns
        final Pattern curlyBracesPattern = Pattern.compile("[{}]");
        final Pattern orPattern = Pattern.compile("OR ");
        for (SkillPrerequisite skillPrerequisite : ability.getPrereqSkills()) {
            String skillPrerequisiteString = skillPrerequisite.toString();
            // Step 1: Remove extra information
            skillPrerequisiteString = curlyBracesPattern.matcher(skillPrerequisiteString).replaceAll("");
            skillPrerequisiteString = orPattern.matcher(skillPrerequisiteString).replaceAll("");

            // Step 2: remove experience levels
            for (int i = 0; i < EXP_LEGENDARY; i++) {
                skillPrerequisiteString = skillPrerequisiteString.replaceAll(getExperienceLevelName(i) + ' ', "");
            }

            // Step 3: Split the string by <br>
            String[] parts = skillPrerequisiteString.split("<br>");

            // Step 4: Test each part
            List<String> specialAbilitySkills = List.of(S_GUN_BA);
            for (String part : parts) {
                SkillType skillType = SkillType.getType(part);
                if (part == null || skillType == null) {
                    LOGGER.warn("Invalid skill type in prerequisite: Invalid value={} - skillPrerequisiteString {}",
                          part, skillPrerequisiteString);
                    continue; // Continue if part is null or not a valid SkillType - this is a cope out, not a solution
                }
                if (skillType.isSubTypeOf(COMBAT_GUNNERY) && !specialAbilitySkills.contains(part)) {
                    return COMBAT_ABILITY;
                }

                if (skillType.isSubTypeOf(COMBAT_PILOTING) || specialAbilitySkills.contains(part)) {
                    isManeuvering = true;
                }
            }
        }

        // If it isn't a Combat or Maneuvering ability, it's a utility ability
        return isManeuvering ? MANEUVERING_ABILITY : UTILITY_ABILITY;
    }

    /**
     * @param ability the ability to check
     *
     * @return {@code true} if the ability is a flaw (negative cost, and not a character-creation-only ability)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isFlaw(final SpecialAbility ability) {
        return !ability.getOriginOnly() && ability.getCost() < 0;
    }

    /**
     * Counts the SPAs a person holds, excluding flaws. Abilities that aren't enabled in the campaign's SPA list are
     * skipped.
     *
     * @param person the person to check
     *
     * @return the number of non-flaw SPAs the person holds
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int countNonFlawSpas(final Person person) {
        int nonFlawSpaCount = 0;
        for (Enumeration<IOption> options = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES);
              options.hasMoreElements(); ) {
            IOption option = options.nextElement();
            if (!option.booleanValue()) {
                continue;
            }

            SpecialAbility ability = SpecialAbility.getAbility(option.getName());
            if (ability != null && !isFlaw(ability)) {
                nonFlawSpaCount++;
            }
        }
        return nonFlawSpaCount;
    }

    /**
     * Gets the C-bill cost of training a person's next non-flaw SPA, covering training materials and sim pod time.
     *
     * @param person the person being trained
     *
     * @return the cost of the person's next SPA
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getSpaTrainingCost(final Person person) {
        return getSpaTrainingCost(countNonFlawSpas(person));
    }

    /**
     * As {@link #getSpaTrainingCost(Person)}, for callers that have already counted the person's non-flaw SPAs.
     *
     * @param nonFlawSpaCount the number of non-flaw SPAs the person already holds
     *
     * @return the cost of the person's next SPA
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getSpaTrainingCost(final int nonFlawSpaCount) {
        int nextSpaIndex = Math.min(nonFlawSpaCount, SPA_TRAINING_COSTS_IN_SUPPORT_POINTS.length - 1);
        int trainingCostInSupportPoints = SPA_TRAINING_COSTS_IN_SUPPORT_POINTS[nextSpaIndex];
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(trainingCostInSupportPoints);
    }
}
