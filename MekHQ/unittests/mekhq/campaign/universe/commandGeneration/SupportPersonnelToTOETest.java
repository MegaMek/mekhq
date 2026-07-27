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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.CarrierSpec;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.EchelonProfile;
import org.junit.jupiter.api.Test;

class SupportPersonnelToTOETest {

    private static final EchelonProfile IS = SupportPersonnelToTOE.innerSphereProfile();
    private static final EchelonProfile CLAN = SupportPersonnelToTOE.clanProfile();

    // --- Inner Sphere packing: platoon 28, squad 7. Remainder becomes squads unless > 3.5 squads (>= 25). ---

    @Test
    void packPool_is_lone_ridesInSmallestSquadNotItsOwnUnit() {
        assertEquals(List.of("Support Squad (2 person):1"), pack(1, IS));
    }

    @Test
    void packPool_is_belowSquad_singleUnderstaffedSquad() {
        assertEquals(List.of("Support Squad (2 person):2"), pack(2, IS));
    }

    @Test
    void packPool_is_fullSquad() {
        assertEquals(List.of("Support Squad (7 person):7"), pack(7, IS));
    }

    @Test
    void packPool_is_aboveSquad_overflowsToSquadsNotPlatoon() {
        // 8 is only ~1.1 squads, well under the 3.5-squad threshold -> two squads, not a platoon of 8.
        assertEquals(List.of("Support Squad (7 person):7", "Support Squad (2 person):1"), pack(8, IS));
    }

    @Test
    void packPool_is_justBelowThreshold_staysSquads() {
        // 24 = 3.43 squads (<= 3.5) -> four squads (7,7,7,3), not an understaffed platoon.
        assertEquals(List.of("Support Squad (7 person):7", "Support Squad (7 person):7", "Support Squad (7 person):7", "Support Squad (3 person):3"),
              pack(24, IS));
    }

    @Test
    void packPool_is_aboveThreshold_becomesPlatoon() {
        // 25 = 3.57 squads (> 3.5) -> one understaffed platoon rather than a near-full squad stack.
        assertEquals(List.of("Support Platoon (28 person):25"), pack(25, IS));
    }

    @Test
    void packPool_is_fullPlatoon() {
        assertEquals(List.of("Support Platoon (28 person):28"), pack(28, IS));
    }

    @Test
    void packPool_is_platoonPlusLoneRemainder() {
        assertEquals(List.of("Support Platoon (28 person):28", "Support Squad (2 person):1"), pack(29, IS));
    }

    @Test
    void packPool_is_battalionMekTechs() {
        // 36 = one full platoon + remainder 8 -> two squads (7,1).
        assertEquals(List.of("Support Platoon (28 person):28", "Support Squad (7 person):7", "Support Squad (2 person):1"), pack(36, IS));
    }

    @Test
    void packPool_is_battalionAstechs() {
        // 216 = 7 full platoons (196) + remainder 20 (2.86 squads) -> three squads (7,7,6).
        List<String> carriers = pack(216, IS);
        assertEquals(10, carriers.size());
        assertEquals("Support Platoon (28 person):28", carriers.get(0));
        assertEquals(List.of("Support Squad (7 person):7", "Support Squad (7 person):7", "Support Squad (6 person):6"),
              carriers.subList(7, 10));
    }

    // --- Clan packing: Point 25, squad 5. Remainder becomes squads unless > 4.5 squads (>= 23). ---

    @Test
    void packPool_clan_fullPoint() {
        assertEquals(List.of("Clan Support Point (25 person):25"), pack(25, CLAN));
    }

    @Test
    void packPool_clan_pointPlusSquad() {
        assertEquals(List.of("Clan Support Point (25 person):25", "Clan Support Squad (5 person):5"), pack(30, CLAN));
    }

    @Test
    void packPool_clan_belowThreshold_staysSquads() {
        assertEquals(List.of("Clan Support Squad (5 person):5", "Clan Support Squad (5 person):5", "Clan Support Squad (2 person):2"),
              pack(12, CLAN));
    }

    @Test
    void packPool_clan_aboveThreshold_becomesPoint() {
        // 23 = 4.6 squads (> 4.5) -> understaffed Point.
        assertEquals(List.of("Clan Support Point (25 person):23"), pack(23, CLAN));
    }

    // --- Invariants ---

    @Test
    void packPool_neverLosesPeople() {
        for (int count = 0; count <= 130; count++) {
            assertEquals(count, totalCrew(SupportPersonnelToTOE.packPool(people(count), IS, "Tech")),
                  "IS pack lost people at count=" + count);
            assertEquals(count, totalCrew(SupportPersonnelToTOE.packPool(people(count), CLAN, "Tech")),
                  "Clan pack lost people at count=" + count);
        }
    }

    @Test
    void packPool_neverExceedsPlatoonCapacityAndNeverPersonUnit() {
        for (int count = 0; count <= 130; count++) {
            for (CarrierSpec spec : SupportPersonnelToTOE.packPool(people(count), IS, "Tech")) {
                assertTrue(spec.crew().size() <= IS.topUnitSize(), "over capacity at count=" + count);
                assertTrue(!spec.unitName().contains("Person"), "person unit at count=" + count);
            }
        }
    }

    @Test
    void packPool_empty_noCarriers() {
        assertTrue(SupportPersonnelToTOE.packPool(people(0), IS, "Tech").isEmpty());
    }

    @Test
    void packPool_stampsLabelOnEveryCarrier() {
        for (CarrierSpec spec : SupportPersonnelToTOE.packPool(people(36), IS, "Command")) {
            assertEquals("Command", spec.professionLabel());
        }
    }

    // --- Grouping keeps professions separate ---

    @Test
    void groupByPrimaryRole_splitsRolesAndPreservesCounts() {
        List<Person> people = new ArrayList<>();
        people.addAll(peopleWithRole(PersonnelRole.MEK_TECH, 3));
        people.addAll(peopleWithRole(PersonnelRole.ASTECH, 5));
        people.addAll(peopleWithRole(PersonnelRole.MEK_TECH, 2));

        Map<PersonnelRole, List<Person>> grouped = SupportPersonnelToTOE.groupByPrimaryRole(people);

        assertEquals(2, grouped.size());
        assertEquals(5, grouped.get(PersonnelRole.MEK_TECH).size());
        assertEquals(5, grouped.get(PersonnelRole.ASTECH).size());
    }

    // --- Guards ---

    @Test
    void organize_nullOrEmpty_noOp() {
        Campaign campaign = mock(Campaign.class);
        assertDoesNotThrow(() -> SupportPersonnelToTOE.organize(null, people(3), false));
        assertDoesNotThrow(() -> SupportPersonnelToTOE.organize(campaign, null, false));
        assertDoesNotThrow(() -> SupportPersonnelToTOE.organize(campaign, List.of(), false));
    }

    // --- Helpers ---

    /** Packs {@code count} people and renders each carrier as "unitName:crewSize" for comparison. */
    private static List<String> pack(int count, EchelonProfile profile) {
        List<String> rendered = new ArrayList<>();
        for (CarrierSpec spec : SupportPersonnelToTOE.packPool(people(count), profile, "Tech")) {
            rendered.add(spec.unitName() + ":" + spec.crew().size());
        }
        return rendered;
    }

    private static int totalCrew(List<CarrierSpec> specs) {
        int total = 0;
        for (CarrierSpec spec : specs) {
            total += spec.crew().size();
        }
        return total;
    }

    /** A list of {@code count} distinct Person mocks; packPool only slices, so no stubbing needed. */
    private static List<Person> people(int count) {
        List<Person> people = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            people.add(mock(Person.class));
        }
        return people;
    }

    private static List<Person> peopleWithRole(PersonnelRole role, int count) {
        List<Person> people = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(role);
            people.add(person);
        }
        return people;
    }
}
