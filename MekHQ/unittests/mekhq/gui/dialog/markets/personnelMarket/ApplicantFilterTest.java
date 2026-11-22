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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ApplicantFilterTest {
    private static List<PersonnelRole> offered(List<PersonnelRole> professions, ApplicantFilter chosen) {
        return ApplicantFilter.professions(professions, chosen).stream().map(ApplicantFilter::profession).toList();
    }

    @Test
    void groupsAreEveryApplicantThenCombatSupportAndCivilian() {
        assertEquals(List.of(ApplicantFilter.ALL,
              ApplicantFilter.of(PersonnelRoleSubType.COMBAT),
              ApplicantFilter.of(PersonnelRoleSubType.SUPPORT),
              ApplicantFilter.of(PersonnelRoleSubType.CIVILIAN)), ApplicantFilter.groups());
    }

    @ParameterizedTest
    @EnumSource(PersonnelRole.class)
    void everyProfessionIsInAllAndExactlyOneGroup(PersonnelRole profession) {
        long groups = ApplicantFilter.groups().stream()
                            .filter(filter -> filter.group() != null && filter.matches(profession))
                            .count();

        assertEquals(1, groups);
        assertTrue(ApplicantFilter.ALL.matches(profession));
    }

    @Test
    void professionFilterMatchesOnlyItsProfession() {
        ApplicantFilter filter = ApplicantFilter.of(PersonnelRole.MEK_TECH);

        assertTrue(filter.matches(PersonnelRole.MEK_TECH));
        assertFalse(filter.matches(PersonnelRole.MECHANIC));
        assertFalse(filter.matches(PersonnelRole.DEPENDENT));
    }

    @Test
    void professionsOfferedAreThoseOnTheBoardOnceEachWithoutCivilians() {
        List<PersonnelRole> board = List.of(PersonnelRole.DOCTOR, PersonnelRole.DEPENDENT, PersonnelRole.MEKWARRIOR,
              PersonnelRole.DOCTOR, PersonnelRole.BARBER);

        assertEquals(List.of(PersonnelRole.MEKWARRIOR, PersonnelRole.DOCTOR), offered(board, ApplicantFilter.ALL));
    }

    @Test
    void professionsOfferedFollowTheBoard() {
        List<PersonnelRole> before = List.of(PersonnelRole.MEKWARRIOR);
        List<PersonnelRole> after = List.of(PersonnelRole.MEKWARRIOR, PersonnelRole.SOLDIER);

        assertEquals(before, offered(before, ApplicantFilter.ALL));
        assertEquals(after, offered(after, ApplicantFilter.ALL));
    }

    @Test
    void chosenProfessionStaysOnOfferWhenNobodyHasIt() {
        assertEquals(List.of(PersonnelRole.MEKWARRIOR, PersonnelRole.DOCTOR),
              offered(List.of(PersonnelRole.MEKWARRIOR), ApplicantFilter.of(PersonnelRole.DOCTOR)));
    }

    @Test
    void aBoardOfCiviliansOffersNoProfessions() {
        assertTrue(offered(List.of(PersonnelRole.DEPENDENT, PersonnelRole.BARBER),
              ApplicantFilter.of(PersonnelRoleSubType.CIVILIAN)).isEmpty());
    }

    @Test
    void everyFilterIsFoundAgainFromItsKey() {
        for (ApplicantFilter filter : ApplicantFilter.groups()) {
            assertEquals(filter, ApplicantFilter.fromKey(filter.key()));
        }
        for (ApplicantFilter filter : ApplicantFilter.professions(List.of(PersonnelRole.values()),
              ApplicantFilter.ALL)) {
            assertEquals(filter, ApplicantFilter.fromKey(filter.key()));
        }
    }

    /** Saves from before filters were saved by name hold a number; a civilian profession was never on offer. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "0", "7", "NO_SUCH_ROLE", "BARBER" })
    void unknownKeysShowEveryApplicant(String key) {
        assertEquals(ApplicantFilter.ALL, ApplicantFilter.fromKey(key));
    }
}
