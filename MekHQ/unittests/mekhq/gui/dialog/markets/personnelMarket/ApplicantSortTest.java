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
package mekhq.gui.dialog.markets.personnelMarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.ApplicantFacts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApplicantSortTest {
    private static final LocalDate TODAY = LocalDate.of(3051, 3, 12);

    private final Map<Person, ApplicantFacts> facts = new HashMap<>();
    private final List<Person> applicants = new ArrayList<>();

    @BeforeEach
    void setUp() {
        facts.clear();
        applicants.clear();
    }

    private Person applicant(String name, PersonnelRole role, int experienceLevel, double cost, int age) {
        return applicant(name, role, experienceLevel, cost, age, 0, 0);
    }

    private Person applicant(String name, PersonnelRole role, int experienceLevel, double cost, int age,
          int abilities, int flaws) {
        Person person = mock(Person.class);
        when(person.getFullName()).thenReturn(name);
        when(person.getPrimaryRole()).thenReturn(role);
        when(person.getAge(any(LocalDate.class))).thenReturn(age);
        facts.put(person, new ApplicantFacts(experienceLevel, Money.of(cost), abilities, flaws));
        applicants.add(person);
        return person;
    }

    private List<Person> sorted(ApplicantSort sort, boolean isReversed) {
        List<Person> ordered = new ArrayList<>(applicants);
        ordered.sort(sort.comparator(facts::get, TODAY, isReversed));
        return ordered;
    }

    @Test
    void experienceSortPutsTheMostExperiencedFirstAndBreaksTiesByName() {
        Person green = applicant("Green", PersonnelRole.SOLDIER, 1, 100, 20);
        Person veteranB = applicant("Bravo", PersonnelRole.MEKWARRIOR, 3, 100, 30);
        Person veteranA = applicant("alpha", PersonnelRole.MEKWARRIOR, 3, 100, 30);
        Person elite = applicant("Elite", PersonnelRole.DOCTOR, 4, 100, 40);

        assertEquals(List.of(elite, veteranA, veteranB, green), sorted(ApplicantSort.EXPERIENCE, false));
    }

    @Test
    void costSortIsNumericAndCheapestFirst() {
        // Compared as text these would sort 10000 before 2000 and 3 billion before 500
        Person tenThousand = applicant("A", PersonnelRole.SOLDIER, 1, 10_000, 20);
        Person free = applicant("B", PersonnelRole.SOLDIER, 1, 0, 20);
        Person billions = applicant("C", PersonnelRole.SOLDIER, 1, 3_000_000_000.0, 20);
        Person fiveHundred = applicant("D", PersonnelRole.SOLDIER, 1, 500, 20);
        Person twoThousand = applicant("E", PersonnelRole.SOLDIER, 1, 2_000.75, 20);

        assertEquals(List.of(free, fiveHundred, twoThousand, tenThousand, billions),
              sorted(ApplicantSort.COST, false));
    }

    @Test
    void abilitySortPutsTheMostAbilitiesFirst() {
        Person none = applicant("A", PersonnelRole.SOLDIER, 1, 100, 20, 0, 0);
        Person three = applicant("B", PersonnelRole.SOLDIER, 1, 100, 20, 3, 0);
        Person one = applicant("C", PersonnelRole.SOLDIER, 1, 100, 20, 1, 2);

        assertEquals(List.of(three, one, none), sorted(ApplicantSort.ABILITIES, false));
    }

    @Test
    void flawSortPutsTheMostFlawsFirst() {
        Person none = applicant("A", PersonnelRole.SOLDIER, 1, 100, 20, 2, 0);
        Person two = applicant("B", PersonnelRole.SOLDIER, 1, 100, 20, 0, 2);
        Person one = applicant("C", PersonnelRole.SOLDIER, 1, 100, 20, 0, 1);

        assertEquals(List.of(two, one, none), sorted(ApplicantSort.FLAWS, false));
    }

    @Test
    void examSortPutsTheHighestScoreFirstAndBreaksTiesByName() {
        // Compared as text 100 would sort below 9
        Person nine = applicant("A", PersonnelRole.SOLDIER, 4, 100, 20);
        Person hundred = applicant("B", PersonnelRole.SOLDIER, 1, 100, 20);
        Person fiftyB = applicant("Bravo", PersonnelRole.SOLDIER, 3, 100, 20);
        Person fiftyA = applicant("alpha", PersonnelRole.SOLDIER, 2, 100, 20);
        when(nine.getPerformanceExamScore()).thenReturn(9);
        when(hundred.getPerformanceExamScore()).thenReturn(100);
        when(fiftyB.getPerformanceExamScore()).thenReturn(50);
        when(fiftyA.getPerformanceExamScore()).thenReturn(50);

        assertEquals(List.of(hundred, fiftyA, fiftyB, nine), sorted(ApplicantSort.EXAM, false));
        assertEquals(List.of(nine, fiftyA, fiftyB, hundred), sorted(ApplicantSort.EXAM, true));
    }

    @Test
    void roleSortGroupsByProfessionWithTheMostExperiencedFirst() {
        Person soldierGreen = applicant("A", PersonnelRole.SOLDIER, 1, 100, 20);
        Person soldierVeteran = applicant("B", PersonnelRole.SOLDIER, 3, 100, 20);
        Person doctor = applicant("C", PersonnelRole.DOCTOR, 1, 100, 20);

        boolean isDoctorFirst = PersonnelRole.DOCTOR.toString()
                                      .compareToIgnoreCase(PersonnelRole.SOLDIER.toString()) < 0;
        List<Person> expected = isDoctorFirst ? List.of(doctor, soldierVeteran, soldierGreen)
                                      : List.of(soldierVeteran, soldierGreen, doctor);
        assertEquals(expected, sorted(ApplicantSort.ROLE, false));
    }

    @Test
    void nameSortIgnoresCase() {
        Person bravo = applicant("bravo", PersonnelRole.SOLDIER, 1, 100, 20);
        Person alpha = applicant("Alpha", PersonnelRole.SOLDIER, 1, 100, 20);
        Person charlie = applicant("Charlie", PersonnelRole.SOLDIER, 1, 100, 20);

        assertEquals(List.of(alpha, bravo, charlie), sorted(ApplicantSort.NAME, false));
    }

    @Test
    void ageSortPutsTheYoungestFirstAndBreaksTiesByName() {
        Person older = applicant("A", PersonnelRole.SOLDIER, 1, 100, 45);
        Person youngB = applicant("Bravo", PersonnelRole.SOLDIER, 1, 100, 19);
        Person youngA = applicant("Alpha", PersonnelRole.SOLDIER, 1, 100, 19);

        assertEquals(List.of(youngA, youngB, older), sorted(ApplicantSort.AGE, false));
    }

    @Test
    void reversingRunsTheOrderTheOtherWayButKeepsTiesAlphabetical() {
        Person cheapB = applicant("Bravo", PersonnelRole.SOLDIER, 1, 100, 20);
        Person cheapA = applicant("Alpha", PersonnelRole.SOLDIER, 1, 100, 20);
        Person dear = applicant("Charlie", PersonnelRole.SOLDIER, 1, 9_000, 20);

        assertEquals(List.of(dear, cheapA, cheapB), sorted(ApplicantSort.COST, true));
    }

    @ParameterizedTest
    @EnumSource(ApplicantSort.class)
    void everyOrderSeparatesApplicantsWithDifferentNames(ApplicantSort sort) {
        // Two applicants alike in everything but their names must never compare as equal, or the board would
        // shuffle them between refreshes.
        Person first = applicant("First", PersonnelRole.SOLDIER, 2, 1_000, 25, 1, 1);
        Person second = applicant("Second", PersonnelRole.SOLDIER, 2, 1_000, 25, 1, 1);

        assertNotEquals(0, sort.comparator(facts::get, TODAY, false).compare(first, second));
        assertNotEquals(0, sort.comparator(facts::get, TODAY, true).compare(first, second));
    }

    @ParameterizedTest
    @EnumSource(value = ApplicantSort.class, mode = EnumSource.Mode.EXCLUDE, names = "NAME")
    void reversingAnOrderReversesApplicantsThatDiffer(ApplicantSort sort) {
        Person low = applicant("A", PersonnelRole.DOCTOR, 1, 100, 20, 0, 0);
        Person high = applicant("B", PersonnelRole.SOLDIER, 5, 9_000, 60, 4, 4);
        when(low.getPerformanceExamScore()).thenReturn(10);
        when(high.getPerformanceExamScore()).thenReturn(90);

        int forward = Integer.signum(sort.comparator(facts::get, TODAY, false).compare(low, high));
        int backward = Integer.signum(sort.comparator(facts::get, TODAY, true).compare(low, high));
        assertTrue(forward != 0 && forward == -backward);
    }
}
