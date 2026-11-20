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
package mekhq.campaign.personnel.generator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Vector;

import megamek.common.units.Crew;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import org.junit.jupiter.api.Test;

class SingleSpecialAbilityGeneratorTest {
    private final SingleSpecialAbilityGenerator service = new SingleSpecialAbilityGenerator();

    @Test
    void pickBucketWithAlternativeWeightingReturnsPositiveAbilitiesWhenNegativeBucketIsEmpty() {
        SpecialAbility positive = mock(SpecialAbility.class);

        List<SpecialAbility> positives = List.of(positive);
        List<SpecialAbility> negatives = List.of();

        List<SpecialAbility> result = service.pickBucketWithAlternativeWeighting(positives, negatives);

        assertSame(positives, result);
    }

    @Test
    void pickBucketWithAlternativeWeightingReturnsPositiveBucketWhenBothBucketsAreEmpty() {
        List<SpecialAbility> positives = List.of();
        List<SpecialAbility> negatives = List.of();

        List<SpecialAbility> result = service.pickBucketWithAlternativeWeighting(positives, negatives);

        assertSame(positives, result);
    }

    @Test
    void hasActiveInvalidAbilityReturnsTrueWhenInvalidAbilityIsActive() {
        PersonnelOptions options = mock(PersonnelOptions.class);

        Vector<String> invalidAbilities = new Vector<>();
        invalidAbilities.add("invalid_ability_code");

        when(options.booleanOption("invalid_ability_code")).thenReturn(true);

        boolean result = service.hasActiveInvalidAbility(options, invalidAbilities);

        assertTrue(result);
    }

    @Test
    void hasActiveInvalidAbilityReturnsTrueWhenAnyInvalidAbilityIsActive() {
        PersonnelOptions options = mock(PersonnelOptions.class);

        Vector<String> invalidAbilities = new Vector<>();
        invalidAbilities.add("inactive_invalid_ability");
        invalidAbilities.add("active_invalid_ability");

        when(options.booleanOption("inactive_invalid_ability")).thenReturn(false);
        when(options.booleanOption("active_invalid_ability")).thenReturn(true);

        boolean result = service.hasActiveInvalidAbility(options, invalidAbilities);

        assertTrue(result);
    }

    @Test
    void hasActiveInvalidAbilityReturnsFalseWhenNoInvalidAbilityIsActive() {
        PersonnelOptions options = mock(PersonnelOptions.class);

        Vector<String> invalidAbilities = new Vector<>();
        invalidAbilities.add("invalid_ability_code");

        when(options.booleanOption("invalid_ability_code")).thenReturn(false);

        boolean result = service.hasActiveInvalidAbility(options, invalidAbilities);

        assertFalse(result);
    }

    @Test
    void applyAbilityWeightingReturnsOnlyPositiveAbilitiesWhenNoNegativeAbilitiesIsTrue() {
        SpecialAbility positive = mock(SpecialAbility.class);
        SpecialAbility negative = mock(SpecialAbility.class);

        when(positive.getCost()).thenReturn(1);
        when(negative.getCost()).thenReturn(-1);

        List<SpecialAbility> result = service.applyAbilityWeighting(List.of(positive, negative),
              true,
              true);

        assertEquals(List.of(positive), result);
    }

    @Test
    void applyAbilityWeightingDoesNotReturnNegativeCostAbilitiesWithStandardWeightingWhenNoNegativeAbilitiesIsTrue() {
        SpecialAbility positive = mock(SpecialAbility.class);
        SpecialAbility zeroCost = mock(SpecialAbility.class);
        SpecialAbility firstNegative = mock(SpecialAbility.class);
        SpecialAbility secondNegative = mock(SpecialAbility.class);

        when(positive.getCost()).thenReturn(2);
        when(zeroCost.getCost()).thenReturn(0);
        when(firstNegative.getCost()).thenReturn(-1);
        when(secondNegative.getCost()).thenReturn(-2);

        List<SpecialAbility> result = service.applyAbilityWeighting(List.of(positive,
                    firstNegative,
                    zeroCost,
                    secondNegative),
              true,
              true);

        assertFalse(result.isEmpty());
        assertAll(result.stream()
                        .map(ability -> () -> assertTrue(ability.getCost() >= 0)));
        assertEquals(List.of(positive, zeroCost), result);
    }

    @Test
    void applyAbilityWeightingReturnsEmptyPoolWithStandardWeightingWhenOnlyNegativeAbilitiesExistAndNoNegativeAbilitiesIsTrue() {
        SpecialAbility firstNegative = mock(SpecialAbility.class);
        SpecialAbility secondNegative = mock(SpecialAbility.class);

        when(firstNegative.getCost()).thenReturn(-1);
        when(secondNegative.getCost()).thenReturn(-2);

        List<SpecialAbility> result = service.applyAbilityWeighting(List.of(firstNegative, secondNegative),
              false,
              true);

        assertTrue(result.isEmpty());
    }

    @Test
    void applyAbilityWeightingReturnsEmptyPoolWithAlternateWeightingWhenOnlyNegativeAbilitiesExistAndNoNegativeAbilitiesIsTrue() {
        SpecialAbility firstNegative = mock(SpecialAbility.class);
        SpecialAbility secondNegative = mock(SpecialAbility.class);

        when(firstNegative.getCost()).thenReturn(-1);
        when(secondNegative.getCost()).thenReturn(-2);

        List<SpecialAbility> result = service.applyAbilityWeighting(List.of(firstNegative, secondNegative),
              true,
              true);

        assertTrue(result.isEmpty());
    }

    @Test
    void applyAbilityWeightingAllowsNegativeCostAbilitiesWhenNoNegativeAbilitiesIsFalse() {
        SpecialAbility negative = mock(SpecialAbility.class);

        when(negative.getCost()).thenReturn(-1);
        when(negative.getWeight()).thenReturn(1);

        List<SpecialAbility> result = service.applyAbilityWeighting(List.of(negative),
              false,
              false);

        assertEquals(List.of(negative), result);
        assertTrue(result.stream().anyMatch(ability -> ability.getCost() < 0));
    }

    @Test
    void gunnerySpecialistChoicesIncludeEverySpecialization() {
        assertEquals(List.of(Crew.SPECIAL_ENERGY, Crew.SPECIAL_BALLISTIC, Crew.SPECIAL_MISSILE),
              SingleSpecialAbilityGenerator.GUNNERY_SPECIALIST_CHOICES);
    }

    @Test
    void environmentSpecialistChoicesIncludeEveryEnvironment() {
        assertEquals(List.of(Crew.ENVIRONMENT_SPECIALIST_FOG,
                    Crew.ENVIRONMENT_SPECIALIST_LIGHT,
                    Crew.ENVIRONMENT_SPECIALIST_RAIN,
                    Crew.ENVIRONMENT_SPECIALIST_SNOW,
                    Crew.ENVIRONMENT_SPECIALIST_WIND),
              SingleSpecialAbilityGenerator.ENVIRONMENT_SPECIALIST_CHOICES);
    }

    @Test
    void humanTroChoicesIncludeEveryUnitType() {
        assertEquals(List.of(Crew.HUMAN_TRO_MEK, Crew.HUMAN_TRO_AERO, Crew.HUMAN_TRO_VEE, Crew.HUMAN_TRO_BA),
              SingleSpecialAbilityGenerator.HUMAN_TRO_CHOICES);
    }

    @Test
    void filterValidAbilitiesSkipsAbilitiesThePersonAlreadyHas() {
        PersonnelOptions options = mock(PersonnelOptions.class);
        SpecialAbility held = mockAbility("held_ability", false);
        SpecialAbility notHeld = mockAbility("new_ability", false);

        when(options.booleanOption("held_ability")).thenReturn(true);
        when(options.booleanOption("new_ability")).thenReturn(false);

        List<SpecialAbility> result = service.filterValidAbilities(options, List.of(held, notHeld), true);

        assertEquals(List.of(notHeld), result);
    }

    @Test
    void filterValidAbilitiesSkipsOriginOnlyAbilitiesForVeterancyAwards() {
        PersonnelOptions options = mock(PersonnelOptions.class);
        SpecialAbility originOnly = mockAbility("origin_ability", true);
        SpecialAbility normal = mockAbility("normal_ability", false);

        List<SpecialAbility> result = service.filterValidAbilities(options, List.of(originOnly, normal), true);

        assertEquals(List.of(normal), result);
    }

    @Test
    void filterValidAbilitiesKeepsOriginOnlyAbilitiesOutsideVeterancyAwards() {
        PersonnelOptions options = mock(PersonnelOptions.class);
        SpecialAbility originOnly = mockAbility("origin_ability", true);

        List<SpecialAbility> result = service.filterValidAbilities(options, List.of(originOnly), false);

        assertEquals(List.of(originOnly), result);
    }

    @Test
    void filterValidAbilitiesSkipsAbilitiesWithAnActiveInvalidAbility() {
        PersonnelOptions options = mock(PersonnelOptions.class);
        SpecialAbility conflicted = mockAbility("conflicted_ability", false);
        Vector<String> invalidAbilities = new Vector<>();
        invalidAbilities.add("blocking_ability");
        when(conflicted.getInvalidAbilities()).thenReturn(invalidAbilities);

        when(options.booleanOption("blocking_ability")).thenReturn(true);

        List<SpecialAbility> result = service.filterValidAbilities(options, List.of(conflicted), true);

        assertTrue(result.isEmpty());
    }

    private static SpecialAbility mockAbility(String name, boolean isOriginOnly) {
        SpecialAbility ability = mock(SpecialAbility.class);
        when(ability.getName()).thenReturn(name);
        when(ability.getOriginOnly()).thenReturn(isOriginOnly);
        when(ability.getInvalidAbilities()).thenReturn(new Vector<>());
        return ability;
    }

    @Test
    void hasActiveInvalidAbilityReturnsFalseWhenInvalidAbilityListIsEmpty() {
        PersonnelOptions options = mock(PersonnelOptions.class);

        Vector<String> invalidAbilities = new Vector<>();

        boolean result = service.hasActiveInvalidAbility(options, invalidAbilities);

        assertFalse(result);
    }
}
