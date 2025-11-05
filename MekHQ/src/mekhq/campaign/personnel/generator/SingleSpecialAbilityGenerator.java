/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.generator;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * Generates a single special ability for a {@link Person}.
 */
public class SingleSpecialAbilityGenerator extends AbstractSpecialAbilityGenerator {
    @Override
    public boolean generateSpecialAbilities(final Campaign campaign, final Person person,
          final int expLvl) {
        return campaign.getCampaignOptions().isUseAbilities() && (rollSPA(campaign, person) != null);
    }

    /**
     * Rolls and assigns a random Special Personnel Ability (SPA) to the specified person within the given campaign,
     * using default behavior (eligibility and weighting are both considered).
     *
     * @param campaign the campaign context to use for ability selection
     * @param person   the person to whom the special ability should be assigned
     *
     * @return the display name of the assigned special ability, or {@code null} if no ability was assigned
     */
    public @Nullable String rollSPA(final Campaign campaign, final Person person) {
        return rollSPA(campaign, person, false, false, false);
    }

    /**
     * Rolls and assigns a random Special Personnel Ability (SPA) to the specified person within the given campaign,
     * using the provided weighting and eligibility criteria.
     *
     * <p>This method selects an available special ability for the person according to the specified options,
     * acquires that ability (and any required specialization) for the person, and returns its display name.</p>
     *
     * <p>If the selected ability requires a specialization, one is chosen and appended to the display name.</p>
     *
     * @param campaign                the campaign context to use for ability selection and related criteria
     * @param person                  the person to whom the special ability should be assigned
     * @param useAlternativeWeighting if {@code true}, positive-cost abilities are weighted more heavily in the
     *                                selection
     * @param ignoreEligibility       if {@code true}, skips eligibility checks and considers all abilities available
     * @param isVeterancyAward        if {@code true}, some SPAs may be excluded based on veterancy award eligibility
     *                                status
     *
     * @return the display name (including specialization if applicable) of the assigned special ability, or
     *       {@code null} if no ability could be rolled or assigned
     */
    public @Nullable String rollSPA(final Campaign campaign, final Person person, boolean useAlternativeWeighting,
          boolean ignoreEligibility, boolean isVeterancyAward) {
        List<SpecialAbility> abilityList = getSpecialAbilities(person,
              useAlternativeWeighting,
              ignoreEligibility,
              isVeterancyAward);
        if (abilityList.isEmpty()) {
            return null;
        }

        final String name = ObjectUtility.getRandomItem(abilityList).getName();
        String displayName = SpecialAbility.getDisplayName(name);
        switch (name) {
            case OptionsConstants.GUNNERY_SPECIALIST: {
                final String special = switch (Compute.randomInt(2)) {
                    case 0 -> Crew.SPECIAL_ENERGY;
                    case 1 -> Crew.SPECIAL_BALLISTIC;
                    default -> Crew.SPECIAL_MISSILE;
                };
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.GUNNERY_RANGE_MASTER: {
                final String special = switch (Compute.randomInt(2)) {
                    case 0 -> Crew.RANGEMASTER_MEDIUM;
                    case 1 -> Crew.RANGEMASTER_LONG;
                    default -> Crew.RANGEMASTER_EXTREME;
                };
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.MISC_ENV_SPECIALIST: {
                final String special = switch (Compute.randomInt(4)) {
                    case 0 -> Crew.ENVIRONMENT_SPECIALIST_FOG;
                    case 1 -> Crew.ENVIRONMENT_SPECIALIST_LIGHT;
                    case 2 -> Crew.ENVIRONMENT_SPECIALIST_RAIN;
                    case 3 -> Crew.ENVIRONMENT_SPECIALIST_SNOW;
                    default -> Crew.ENVIRONMENT_SPECIALIST_WIND;
                };
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.MISC_HUMAN_TRO: {
                final String special = switch (Compute.randomInt(3)) {
                    case 0 -> Crew.HUMAN_TRO_MEK;
                    case 1 -> Crew.HUMAN_TRO_AERO;
                    case 2 -> Crew.HUMAN_TRO_VEE;
                    default -> Crew.HUMAN_TRO_BA;
                };
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.GUNNERY_WEAPON_SPECIALIST: {
                final String special = SpecialAbility.chooseWeaponSpecialization(person,
                      campaign.getCampaignOptions().getTechLevel(), campaign.getGameYear(), false);
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.GUNNERY_SANDBLASTER: {
                final String special = SpecialAbility.chooseWeaponSpecialization(person,
                      campaign.getCampaignOptions().getTechLevel(), campaign.getGameYear(), true);
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            default: {
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, true);
                break;
            }
        }

        return displayName;
    }

    /**
     * Compiles and returns a list of {@link SpecialAbility} objects available to the given person, according to
     * eligibility and weighting rules.
     *
     * <p>If {@code ignoreEligibility} is true, all valid special abilities (excluding those limited to character
     * creation or restricted by currently present invalid abilities) are considered, and positive-cost abilities may be
     * emphasized using alternative weighting if specified. Otherwise, the list of eligible special abilities is
     * determined by {@link #getEligibleSPAs(Person, boolean)}.</p>
     *
     * <p>If the resulting list is empty, {@code null} is returned.</p>
     *
     * @param person                  the person for whom to collect available special abilities
     * @param useAlternativeWeighting if {@code true}, positive-cost abilities are weighted more heavily (reducing the
     *                                chance of characters receiving flaws)
     * @param ignoreEligibility       if {@code true}, all valid abilities are considered regardless of other
     *                                eligibility requirements
     * @param isVeterancyAward        if {@code true}, some SPAs may be excluded based on veterancy award eligibility
     *                                status
     *
     * @return a list of available special abilities based on the criteria, or an empty list is none are found
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<SpecialAbility> getSpecialAbilities(Person person, boolean useAlternativeWeighting,
          boolean ignoreEligibility, boolean isVeterancyAward) {
        final PersonnelOptions options = person.getOptions();
        List<SpecialAbility> abilityList = new ArrayList<>();
        final List<SpecialAbility> positiveAbilities = new ArrayList<>();
        final List<SpecialAbility> negativeAbilities = new ArrayList<>();

        if (ignoreEligibility) {
            for (SpecialAbility ability : SpecialAbility.getSpecialAbilities().values()) {
                if (isVeterancyAward && ability.getOriginOnly()) {
                    continue;
                }

                Vector<String> invalidAbilities = ability.getInvalidAbilities();
                boolean isValid = true;
                if (!invalidAbilities.isEmpty()) {
                    for (String abilityCode : invalidAbilities) {
                        if (options.booleanOption(abilityCode)) {
                            isValid = false;
                            break; // Invalid if any code is present
                        }
                    }
                }
                if (isValid) {
                    abilityList.add(ability);
                    if (ability.getCost() >= 0) {
                        positiveAbilities.add(ability);
                    } else {
                        negativeAbilities.add(ability);
                    }
                }
            }
        } else {
            abilityList = getEligibleSPAs(person, isVeterancyAward);
        }

        // Alternative weighting will pre-determine whether the SPA is positive or negative.
        if (useAlternativeWeighting) {
            if (negativeAbilities.isEmpty()) {
                return positiveAbilities;
            } else if (positiveAbilities.isEmpty()) {
                return negativeAbilities;
            } else {
                int roll = Compute.randomInt(40);
                if (roll == 0) {
                    abilityList.addAll(negativeAbilities);
                } else {
                    abilityList.addAll(positiveAbilities);
                }
            }
        } else {
            abilityList = SpecialAbility.getWeightedSpecialAbilities(abilityList);
        }
        return abilityList;
    }

    private List<SpecialAbility> getEligibleSPAs(Person person, boolean isVeterancyAward) {
        List<SpecialAbility> eligible = new ArrayList<>();
        for (Enumeration<IOption> i = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (!ability.booleanValue()) {
                SpecialAbility spa = SpecialAbility.getAbility(ability.getName());
                if (isVeterancyAward && spa.getOriginOnly()) {
                    continue;
                }

                if ((spa == null) || (spa.getWeight() <= 0)
                          || (!spa.isEligible(person.isClanPersonnel(), person.getSkills(), person.getOptions()))) {
                    continue;
                }
                eligible.add(spa);
            }
        }
        return eligible;
    }
}
