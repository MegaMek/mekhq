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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;
import mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.ApplicantFacts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicantMatchesTest {
    private static final ApplicantFilter MEKWARRIORS = ApplicantFilter.of(PersonnelRole.MEKWARRIOR);
    private static final ApplicantFilter DOCTORS = ApplicantFilter.of(PersonnelRole.DOCTOR);
    private static final ApplicantFilter COMBAT = ApplicantFilter.of(PersonnelRoleSubType.COMBAT);
    private static final ApplicantFilter SUPPORT = ApplicantFilter.of(PersonnelRoleSubType.SUPPORT);
    private static final ApplicantFilter CIVILIAN = ApplicantFilter.of(PersonnelRoleSubType.CIVILIAN);
    private static final int GREEN = 1;
    private static final int VETERAN = 3;

    private final Map<Person, ApplicantFacts> facts = new HashMap<>();
    private final List<Person> applicants = new ArrayList<>();

    private Person greenMekWarrior;
    private Person veteranMekWarrior;
    private Person veteranDoctor;
    private Person greenBarber;

    @BeforeEach
    void setUp() {
        facts.clear();
        applicants.clear();
        greenMekWarrior = applicant(PersonnelRole.MEKWARRIOR, GREEN, 0, 1);
        veteranMekWarrior = applicant(PersonnelRole.MEKWARRIOR, VETERAN, 2, 0);
        veteranDoctor = applicant(PersonnelRole.DOCTOR, VETERAN, 1, 1);
        greenBarber = applicant(PersonnelRole.BARBER, GREEN, 0, 0);
    }

    private Person applicant(PersonnelRole role, int level, int abilities, int flaws) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(role);
        facts.put(person, new ApplicantFacts(level, Money.zero(), abilities, flaws));
        applicants.add(person);
        return person;
    }

    private ApplicantMatches matches(ApplicantFilter role, boolean isAbilitiesOnly, boolean isFlawsOnly,
          Integer... levels) {
        return new ApplicantMatches(applicants, facts::get, role, isAbilitiesOnly, isFlawsOnly, Set.of(levels));
    }

    @Test
    void withNothingChosenEveryCountIsOfEveryApplicant() {
        ApplicantMatches matches = matches(ApplicantFilter.ALL, false, false);

        assertEquals(applicants, matches.shown());
        assertEquals(4, matches.countOf(ApplicantFilter.ALL));
        assertEquals(2, matches.countOf(COMBAT));
        assertEquals(1, matches.countOf(SUPPORT));
        assertEquals(1, matches.countOf(CIVILIAN));
        assertEquals(2, matches.countOf(MEKWARRIORS));
        assertEquals(2, matches.withAbilities());
        assertEquals(2, matches.withFlaws());
        assertEquals(2, matches.withLevel(GREEN));
        assertEquals(2, matches.withLevel(VETERAN));
        assertEquals(0, matches.withLevel(5));
    }

    @Test
    void theChosenRoleNarrowsTheOtherCountsButNotTheRoles() {
        ApplicantMatches matches = matches(MEKWARRIORS, false, false);

        assertEquals(List.of(greenMekWarrior, veteranMekWarrior), matches.shown());
        assertEquals(4, matches.countOf(ApplicantFilter.ALL));
        assertEquals(1, matches.countOf(DOCTORS));
        assertEquals(1, matches.withAbilities());
        assertEquals(1, matches.withFlaws());
        assertEquals(1, matches.withLevel(GREEN));
        assertEquals(1, matches.withLevel(VETERAN));
    }

    @Test
    void theTickedLevelsNarrowTheOtherCountsButNotTheLevels() {
        ApplicantMatches matches = matches(ApplicantFilter.ALL, false, false, VETERAN);

        assertEquals(List.of(veteranMekWarrior, veteranDoctor), matches.shown());
        assertEquals(2, matches.countOf(ApplicantFilter.ALL));
        assertEquals(1, matches.countOf(MEKWARRIORS));
        assertEquals(0, matches.countOf(CIVILIAN));
        assertEquals(2, matches.withAbilities());
        assertEquals(1, matches.withFlaws());
        assertEquals(2, matches.withLevel(GREEN));
        assertEquals(2, matches.withLevel(VETERAN));
    }

    @Test
    void severalTickedLevelsLetAnyOfThemThrough() {
        assertEquals(applicants, matches(ApplicantFilter.ALL, false, false, GREEN, VETERAN).shown());
    }

    @Test
    void tickingAbilitiesNarrowsTheOtherCountsButNotItsOwn() {
        ApplicantMatches matches = matches(ApplicantFilter.ALL, true, false);

        assertEquals(List.of(veteranMekWarrior, veteranDoctor), matches.shown());
        assertEquals(2, matches.withAbilities());
        assertEquals(1, matches.withFlaws());
        assertEquals(1, matches.countOf(MEKWARRIORS));
        assertEquals(0, matches.withLevel(GREEN));
        assertEquals(2, matches.withLevel(VETERAN));
    }

    @Test
    void tickingAbilitiesAndFlawsNeedsBoth() {
        ApplicantMatches matches = matches(ApplicantFilter.ALL, true, true);

        assertEquals(List.of(veteranDoctor), matches.shown());
        assertEquals(1, matches.withAbilities());
        assertEquals(1, matches.withFlaws());
        assertEquals(0, matches.countOf(COMBAT));
        assertEquals(1, matches.countOf(SUPPORT));
    }

    @Test
    void theChosenRolesCountIsHowManyAreShown() {
        for (ApplicantFilter role : ApplicantFilter.groups()) {
            ApplicantMatches matches = matches(role, false, true, GREEN, VETERAN);

            assertEquals(matches.shown().size(), matches.countOf(role));
        }
    }

    @Test
    void civiliansAreShownByTheirGroup() {
        assertEquals(List.of(greenBarber), matches(CIVILIAN, false, false).shown());
    }
}
