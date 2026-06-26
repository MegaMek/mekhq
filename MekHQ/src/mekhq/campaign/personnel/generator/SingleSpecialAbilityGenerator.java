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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.eras.Era;
import mekhq.campaign.universe.eras.Eras;

/**
 * Generates a single special ability for a {@link Person}.
 */
public class SingleSpecialAbilityGenerator extends AbstractSpecialAbilityGenerator {
    private static final MMLogger LOGGER = MMLogger.create(SingleSpecialAbilityGenerator.class);

    @Override
    public boolean generateSpecialAbilities(final Campaign campaign, final Person person,
          final int expLvl) {
        return campaign.getCampaignOptions().isUseAbilities() && (rollSPA(campaign, person) != null);
    }

    /**
     * Rolls and assigns a random Special Personnel Ability (SPA) to the specified person within the given campaign,
     * using default behavior (eligibility, weighting, and flaws are all considered).
     *
     * @param campaign the campaign context to use for ability selection
     * @param person   the person to whom the special ability should be assigned
     *
     * @return the display name of the assigned special ability, or {@code null} if no ability was assigned
     */
    public @Nullable String rollSPA(final Campaign campaign, final Person person) {
        return rollSPA(campaign, person, false, false, false, false);
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
     * @param excludeOriginAwards     if {@code true}, some SPAs may be excluded based on veterancy award eligibility
     *                                status
     * @param noNegativeAbilities     if {@code true}, negative abilities are not considered in the selection
     *
     * @return the display name (including specialization if applicable) of the assigned special ability, or
     *       {@code null} if no ability could be rolled or assigned
     */
    public @Nullable String rollSPA(final Campaign campaign, final Person person, boolean useAlternativeWeighting,
          boolean ignoreEligibility, boolean excludeOriginAwards, boolean noNegativeAbilities) {
        List<SpecialAbility> abilityList = getSpecialAbilities(person,
              useAlternativeWeighting,
              ignoreEligibility,
              excludeOriginAwards,
              noNegativeAbilities);
        if (person.isClanPersonnel()) {
            filterOutMeleeSPA(campaign.getLocalDate(), person.getOriginFaction(), abilityList);
        }

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
     * Filters out melee-related Special Personnel Abilities (SPAs) from the given list.
     *
     * <p>Melee SPAs ({@code "melee_specialist"} and {@code "melee_master"}) are removed if the origin faction is a
     * Clan, and either:</p>
     * <ul>
     *     <li>the faction is a Homeworld Clan, or</li>
     *     <li>the current date falls before the end of the Late Republic era ({@code "LREP"})</li>
     * </ul>
     *
     * <p>The 'end of the Republic era' date was picked, as by the Dark Ages it appears the Clan dislike of melee
     * has faded. At least among clans that had settled the Inner Sphere.</p>
     *
     * @param today         the current date, used to determine era eligibility
     * @param originFaction the faction whose rules govern SPA eligibility
     * @param abilityList   the mutable list of {@link SpecialAbility} objects to filter in-place
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static void filterOutMeleeSPA(LocalDate today, Faction originFaction, List<SpecialAbility> abilityList) {
        Era lateRepublicEra = Eras.getInstance().getEra("LREP");
        if (lateRepublicEra == null) {
            LOGGER.error("Late Republic era (LREP) not found in Eras.getInstance()");
            return;
        }

        LocalDate endDate = lateRepublicEra.getEnd();

        if (originFaction.isClan()) {
            if (originFaction.isHomeworldClan() || today.isBefore(endDate)) {
                abilityList.removeIf(spa -> spa.getName().equalsIgnoreCase("melee_specialist"));
                abilityList.removeIf(spa -> spa.getName().equalsIgnoreCase("melee_master"));
            }
        }
    }

    /**
     * Compiles and returns a list of {@link SpecialAbility} objects available to the given person, according to
     * eligibility and weighting rules.
     *
     * <p>Abilities are first partitioned into positive-cost and negative-cost buckets. When
     * {@code useAlternativeWeighting} is {@code true}, the bucket to draw from is chosen randomly (1-in-40 chance of
     * selecting from negatives); standard weighting via {@link SpecialAbility#getWeightedSpecialAbilities(Collection)}
     * is applied otherwise. When {@code noNegativeAbilities} is {@code true}, negative-cost abilities are excluded
     * regardless of the weighting mode chosen.</p>
     *
     * <p>When {@code ignoreEligibility} is {@code true}, every registered ability is considered (subject only to
     * {@code excludeOriginAwards} and invalid-ability checks). Otherwise, the candidate set is produced by
     * {@link #getEligibleSPAs(Person, boolean)}.</p>
     *
     * @param person                  the person for whom to collect available special abilities
     * @param useAlternativeWeighting if {@code true}, positive-cost abilities are weighted more heavily (reducing the
     *                                chance of characters receiving flaws)
     * @param ignoreEligibility       if {@code true}, all valid abilities are considered regardless of other
     *                                eligibility requirements
     * @param excludeOriginAwards     if {@code true}, some SPAs may be excluded based on veterancy award eligibility
     *                                status
     * @param noNegativeAbilities     if {@code true}, negative abilities are excluded from selection
     *
     * @return a list of available special abilities based on the criteria, or an empty list if none are found
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<SpecialAbility> getSpecialAbilities(Person person, boolean useAlternativeWeighting,
          boolean ignoreEligibility, boolean excludeOriginAwards, boolean noNegativeAbilities) {

        final List<SpecialAbility> candidates = ignoreEligibility
                                                      ?
                                                      getValidAbilitiesIgnoringEligibility(person, excludeOriginAwards)
                                                      :
                                                      getEligibleSPAs(person, excludeOriginAwards);

        return applyAbilityWeighting(candidates, useAlternativeWeighting, noNegativeAbilities);
    }

    List<SpecialAbility> applyAbilityWeighting(List<SpecialAbility> candidates, boolean useAlternativeWeighting,
          boolean noNegativeAbilities) {
        final List<SpecialAbility> positiveAbilities = new ArrayList<>();
        final List<SpecialAbility> negativeAbilities = new ArrayList<>();

        for (SpecialAbility ability : candidates) {
            if (ability.getCost() >= 0) {
                positiveAbilities.add(ability);
            } else if (!noNegativeAbilities) {
                // Respect noNegativeAbilities in both weighting modes
                negativeAbilities.add(ability);
            }
        }

        final List<SpecialAbility> result;
        if (useAlternativeWeighting) {
            result = pickBucketWithAlternativeWeighting(positiveAbilities, negativeAbilities);
        } else {
            final List<SpecialAbility> combined = new ArrayList<>(positiveAbilities);
            combined.addAll(negativeAbilities);
            result = SpecialAbility.getWeightedSpecialAbilities(combined);
        }

        if (noNegativeAbilities) {
            result.removeIf(ability -> ability.getCost() < 0);
        }

        return result;
    }

    /**
     * Returns every registered {@link SpecialAbility} that passes the veterancy-award filter and has no currently
     * active invalid-ability conflict for the given person.
     *
     * <p>Extracted from {@link #getSpecialAbilities} to eliminate duplicated iteration logic and make the
     * {@code ignoreEligibility} path independently testable.</p>
     *
     * @param person           the person whose active options are checked against invalid-ability lists
     * @param isVeterancyAward if {@code true}, abilities flagged as origin-only are skipped
     *
     * @return a mutable list of abilities that cleared all filters; never {@code null}
     */
    private List<SpecialAbility> getValidAbilitiesIgnoringEligibility(Person person,
          boolean isVeterancyAward) {

        final PersonnelOptions options = person.getOptions();
        final List<SpecialAbility> result = new ArrayList<>();

        for (SpecialAbility ability : SpecialAbility.getSpecialAbilities().values()) {
            if (isVeterancyAward && ability.getOriginOnly()) {
                continue;
            }
            if (hasActiveInvalidAbility(options, ability.getInvalidAbilities())) {
                continue;
            }
            result.add(ability);
        }

        return result;
    }

    /**
     * Returns {@code true} if any of the supplied ability codes is currently active (i.e., its boolean option is
     * {@code true}) for the given options set.
     *
     * @param options          the personnel options to query
     * @param invalidAbilities the list of ability codes that would make an ability ineligible
     *
     * @return {@code true} if at least one invalid code is active; {@code false} otherwise
     */
    boolean hasActiveInvalidAbility(PersonnelOptions options,
          Vector<String> invalidAbilities) {

        for (String abilityCode : invalidAbilities) {
            if (options.booleanOption(abilityCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Selects and returns one of the two ability buckets using alternative weighting: a 1-in-40 chance of returning the
     * negative bucket; otherwise the positive bucket is returned.
     *
     * <p>If either bucket is empty, the other is returned unconditionally, preserving the original behavior while
     * avoiding an unnecessary random roll.</p>
     *
     * @param positiveAbilities abilities with cost &ge; 0
     * @param negativeAbilities abilities with cost &lt; 0
     *
     * @return the chosen bucket (can be empty if both inputs are empty)
     */
    List<SpecialAbility> pickBucketWithAlternativeWeighting(
          List<SpecialAbility> positiveAbilities, List<SpecialAbility> negativeAbilities) {

        if (negativeAbilities.isEmpty()) {
            return positiveAbilities;
        }
        if (positiveAbilities.isEmpty()) {
            return negativeAbilities;
        }

        final boolean pickNegative = (Compute.randomInt(40) == 0);
        return pickNegative ? negativeAbilities : positiveAbilities;
    }

    private List<SpecialAbility> getEligibleSPAs(Person person, boolean isVeterancyAward) {
        List<SpecialAbility> eligible = new ArrayList<>();
        for (Enumeration<IOption> i = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (!ability.booleanValue()) {
                SpecialAbility spa = SpecialAbility.getAbility(ability.getName());
                if (spa == null) {
                    // Generally we hit this null protection if the ability exists, but is not enabled in the
                    // player's campaign. No need to spam up the log reporting on this, it's working as intended.
                    continue;
                }

                if (isVeterancyAward && spa.getOriginOnly()) {
                    continue;
                }

                boolean isIneligible = !spa.isEligible(person.isClanPersonnel(), person.getSkills(),
                      person.getOptions());
                if (spa.getWeight() <= 0 || isIneligible) {
                    continue;
                }

                eligible.add(spa);
            }
        }
        return eligible;
    }
}
