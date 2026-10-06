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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.options.IOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/**
 * Tests the unofficial SPA training costs (Hot Spots: Draconis Reach first printing pg 34 table) in
 * {@link SpaUtilities}: which abilities count as flaws, how a person's non-flaw SPAs are counted, and the price of the
 * next one.
 */
class SpaTrainingCostTest {
    private static final int SUPPORT_POINTS_TO_C_BILLS = 10_000;

    // region isFlaw

    @Test
    void negativeCostAbilityIsAFlaw() {
        assertTrue(SpaUtilities.isFlaw(ability(-1, false)));
    }

    @Test
    void positiveCostAbilityIsNotAFlaw() {
        assertFalse(SpaUtilities.isFlaw(ability(2, false)));
    }

    @Test
    void freeAbilityIsNotAFlaw() {
        assertFalse(SpaUtilities.isFlaw(ability(0, false)));
    }

    /** Origin-only abilities can carry a negative cost but are character-creation picks, not flaws. */
    @Test
    void negativeCostOriginOnlyAbilityIsNotAFlaw() {
        assertFalse(SpaUtilities.isFlaw(ability(-1, true)));
    }

    @Test
    void positiveCostOriginOnlyAbilityIsNotAFlaw() {
        assertFalse(SpaUtilities.isFlaw(ability(3, true)));
    }

    // endregion isFlaw

    // region getSpaTrainingCost(int)

    @ParameterizedTest
    @CsvSource({ "0, 60", "1, 180", "2, 360", "3, 600", "4, 900" })
    void eachOfTheFirstFiveSpasHasItsOwnPrice(int heldSpas, int expectedSupportPoints) {
        assertEquals(Money.of(expectedSupportPoints * SUPPORT_POINTS_TO_C_BILLS),
              SpaUtilities.getSpaTrainingCost(heldSpas));
    }

    @ParameterizedTest
    @CsvSource({ "5", "6", "10", "100", "2147483647" })
    void everySpaAfterTheFifthCostsTheFifthPrice(int heldSpas) {
        assertEquals(Money.of(900 * SUPPORT_POINTS_TO_C_BILLS), SpaUtilities.getSpaTrainingCost(heldSpas));
    }

    @Test
    void spaPricesNeverFall() {
        Money previous = SpaUtilities.getSpaTrainingCost(0);
        for (int heldSpas = 1; heldSpas <= 10; heldSpas++) {
            Money current = SpaUtilities.getSpaTrainingCost(heldSpas);
            assertFalse(current.isLessThan(previous), "SPA " + (heldSpas + 1));
            previous = current;
        }
    }

    // endregion getSpaTrainingCost(int)

    // region countNonFlawSpas and getSpaTrainingCost(Person)

    @Test
    void personWithNoAbilitiesHoldsNoSpas() {
        Person person = personWithOptions(List.of());

        assertEquals(0, SpaUtilities.countNonFlawSpas(person));
    }

    @Test
    void flawsAndUnselectedOptionsAreNotCounted() {
        Person person = personWithOptions(List.of(
              option("good_one", true),
              option("good_two", true),
              option("flaw", true),
              option("not_taken", false)));

        try (MockedStatic<SpecialAbility> specialAbility = mockStatic(SpecialAbility.class)) {
            SpecialAbility goodOneAbility = ability(2, false);
            specialAbility.when(() -> SpecialAbility.getAbility("good_one")).thenReturn(goodOneAbility);
            SpecialAbility goodTwoAbility = ability(1, false);
            specialAbility.when(() -> SpecialAbility.getAbility("good_two")).thenReturn(goodTwoAbility);
            SpecialAbility flawAbility = ability(-2, false);
            specialAbility.when(() -> SpecialAbility.getAbility("flaw")).thenReturn(flawAbility);
            SpecialAbility notTakenAbility = ability(2, false);
            specialAbility.when(() -> SpecialAbility.getAbility("not_taken")).thenReturn(notTakenAbility);

            assertEquals(2, SpaUtilities.countNonFlawSpas(person));
        }
    }

    @Test
    void originOnlyAbilitiesCountTowardsTheTotal() {
        Person person = personWithOptions(List.of(option("origin", true), option("bought", true)));

        try (MockedStatic<SpecialAbility> specialAbility = mockStatic(SpecialAbility.class)) {
            SpecialAbility originAbility = ability(-1, true);
            specialAbility.when(() -> SpecialAbility.getAbility("origin")).thenReturn(originAbility);
            SpecialAbility boughtAbility = ability(2, false);
            specialAbility.when(() -> SpecialAbility.getAbility("bought")).thenReturn(boughtAbility);

            assertEquals(2, SpaUtilities.countNonFlawSpas(person));
        }
    }

    /** An ability removed from the campaign's SPA list stays on the person but isn't counted. */
    @Test
    void abilitiesMissingFromTheSpaListAreSkipped() {
        Person person = personWithOptions(List.of(option("removed", true), option("known", true)));

        try (MockedStatic<SpecialAbility> specialAbility = mockStatic(SpecialAbility.class)) {
            specialAbility.when(() -> SpecialAbility.getAbility(anyString())).thenReturn(null);
            SpecialAbility knownAbility = ability(1, false);
            specialAbility.when(() -> SpecialAbility.getAbility("known")).thenReturn(knownAbility);

            assertEquals(1, SpaUtilities.countNonFlawSpas(person));
        }
    }

    @Test
    void personalTrainingCostIsPricedByTheSpasAlreadyHeld() {
        Person person = personWithOptions(List.of(
              option("one", true), option("two", true), option("flaw", true)));

        try (MockedStatic<SpecialAbility> specialAbility = mockStatic(SpecialAbility.class)) {
            SpecialAbility oneAbility = ability(1, false);
            specialAbility.when(() -> SpecialAbility.getAbility("one")).thenReturn(oneAbility);
            SpecialAbility twoAbility = ability(1, false);
            specialAbility.when(() -> SpecialAbility.getAbility("two")).thenReturn(twoAbility);
            SpecialAbility flawAbility = ability(-1, false);
            specialAbility.when(() -> SpecialAbility.getAbility("flaw")).thenReturn(flawAbility);

            // Two non-flaw SPAs held, so the next is the third: 360 SP
            assertEquals(Money.of(360 * SUPPORT_POINTS_TO_C_BILLS), SpaUtilities.getSpaTrainingCost(person));
        }
    }

    @ParameterizedTest
    @CsvSource({ "0, 0", "1, 60", "2, 240", "3, 600", "5, 2100", "7, 3900" })
    void accumulatedCostAddsUpEverySpaHeld(int heldSpas, int expectedSupportPoints) {
        try (MockedStatic<SpaUtilities> spaUtilities = mockStatic(SpaUtilities.class, CALLS_REAL_METHODS)) {
            Person person = personWithOptions(List.of());
            spaUtilities.when(() -> SpaUtilities.countNonFlawSpas(person)).thenReturn(heldSpas);

            assertEquals(Money.of(expectedSupportPoints * SUPPORT_POINTS_TO_C_BILLS),
                  SpaUtilities.getAccumulatedSpaTrainingCost(person));
        }
    }

    @Test
    void capIsFive() {
        assertEquals(5, SpaUtilities.SPA_CAP);
    }

    // endregion countNonFlawSpas and getSpaTrainingCost(Person)

    private static SpecialAbility ability(int cost, boolean isOriginOnly) {
        SpecialAbility ability = mock(SpecialAbility.class);
        when(ability.getCost()).thenReturn(cost);
        when(ability.getOriginOnly()).thenReturn(isOriginOnly);
        return ability;
    }

    private static IOption option(String name, boolean isSelected) {
        IOption option = mock(IOption.class);
        when(option.getName()).thenReturn(name);
        when(option.booleanValue()).thenReturn(isSelected);
        return option;
    }

    private static Person personWithOptions(List<IOption> options) {
        Person person = mock(Person.class);
        // A fresh enumeration per call, as the real method returns
        when(person.getOptions(PersonnelOptions.LVL3_ADVANTAGES))
              .thenAnswer(invocation -> Collections.enumeration(new ArrayList<>(options)));
        return person;
    }
}
