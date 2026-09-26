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
package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.mission.scenarios.salvage.SalvageTechFilter.HiddenReason;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SalvageTechFilterTest {
    private static SalvageTechCandidate candidate(SkillLevel skillLevel, boolean isInjured, boolean isPregnant,
          boolean hasTechUnits, String searchText) {
        return new SalvageTechCandidate(mock(Person.class), skillLevel, isInjured, isPregnant, hasTechUnits, 480,
              searchText);
    }

    private static SalvageTechCandidate healthy(SkillLevel skillLevel) {
        return candidate(skillLevel, false, false, false, "sgt ana rios mek tech atlas");
    }

    @Test
    void showAllShowsEveryone() {
        assertTrue(SalvageTechFilter.SHOW_ALL.shows(candidate(SkillLevel.ULTRA_GREEN, true, true, true, "x")));
    }

    @Test
    void injuredTechsCanBeHidden() {
        SalvageTechFilter filter = new SalvageTechFilter(true, false, false, null, "");

        assertEquals(HiddenReason.INJURED, filter.getHiddenReason(candidate(SkillLevel.REGULAR, true, false, false,
              "")));
        assertTrue(filter.shows(healthy(SkillLevel.REGULAR)));
    }

    @Test
    void pregnantTechsCanBeHidden() {
        SalvageTechFilter filter = new SalvageTechFilter(false, true, false, null, "");

        assertEquals(HiddenReason.PREGNANT, filter.getHiddenReason(candidate(SkillLevel.REGULAR, false, true, false,
              "")));
        assertTrue(filter.shows(healthy(SkillLevel.REGULAR)));
    }

    @Test
    void unitTechsCanBeHidden() {
        SalvageTechFilter filter = new SalvageTechFilter(false, false, true, null, "");

        assertEquals(HiddenReason.UNIT_TECH, filter.getHiddenReason(candidate(SkillLevel.REGULAR, false, false, true,
              "")));
        assertTrue(filter.shows(healthy(SkillLevel.REGULAR)));
    }

    @ParameterizedTest
    @EnumSource(value = SkillLevel.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void experienceFilterIsAnExactMatch(SkillLevel skillLevel) {
        SalvageTechFilter filter = new SalvageTechFilter(false, false, false, SkillLevel.REGULAR, "");

        boolean isRegular = skillLevel == SkillLevel.REGULAR;
        assertEquals(isRegular, filter.shows(healthy(skillLevel)), skillLevel.name());
        if (!isRegular) {
            assertEquals(HiddenReason.OTHER_EXPERIENCE, filter.getHiddenReason(healthy(skillLevel)));
        }
    }

    @Test
    void techsWithoutASkillLevelMatchNoLevel() {
        SalvageTechFilter filter = new SalvageTechFilter(false, false, false, SkillLevel.ULTRA_GREEN, "");

        assertFalse(filter.shows(healthy(SkillLevel.NONE)));
    }

    @Test
    void searchMatchesAnyPartOfTheSearchText() {
        assertTrue(new SalvageTechFilter(false, false, false, null, "rios").shows(healthy(SkillLevel.REGULAR)));
        assertTrue(new SalvageTechFilter(false, false, false, null, "  ATLAS ").shows(healthy(SkillLevel.REGULAR)));
        assertEquals(HiddenReason.NO_SEARCH_MATCH,
              new SalvageTechFilter(false, false, false, null, "locust").getHiddenReason(healthy(SkillLevel.REGULAR)));
    }

    @Test
    void blankSearchMatchesEveryone() {
        assertNull(new SalvageTechFilter(false, false, false, null, "   ").getHiddenReason(healthy(SkillLevel.GREEN)));
    }

    @Test
    void theFirstReasonWins() {
        SalvageTechFilter filter = new SalvageTechFilter(true, true, true, SkillLevel.ELITE, "nobody");

        assertEquals(HiddenReason.INJURED, filter.getHiddenReason(candidate(SkillLevel.GREEN, true, true, true, "")));
        assertEquals(HiddenReason.PREGNANT, filter.getHiddenReason(candidate(SkillLevel.GREEN, false, true, true,
              "")));
        assertEquals(HiddenReason.UNIT_TECH, filter.getHiddenReason(candidate(SkillLevel.GREEN, false, false, true,
              "")));
        assertEquals(HiddenReason.OTHER_EXPERIENCE, filter.getHiddenReason(candidate(SkillLevel.GREEN, false, false,
              false, "")));
    }

    @Test
    void hiddenTechsAreCountedOnceUnderTheirFirstReason() {
        SalvageTechFilter filter = new SalvageTechFilter(true, true, true, SkillLevel.REGULAR, "");
        List<SalvageTechCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            candidates.add(candidate(SkillLevel.GREEN, true, true, true, "")); // injured wins
        }
        for (int i = 0; i < 3; i++) {
            candidates.add(candidate(SkillLevel.VETERAN, false, true, false, ""));
        }
        for (int i = 0; i < 211; i++) {
            candidates.add(candidate(SkillLevel.VETERAN, false, false, true, ""));
        }
        for (int i = 0; i < 43; i++) {
            candidates.add(candidate(SkillLevel.GREEN, false, false, false, ""));
        }
        for (int i = 0; i < 42; i++) {
            candidates.add(healthy(SkillLevel.REGULAR));
        }

        Map<HiddenReason, Integer> counts = filter.countHidden(candidates);

        assertEquals(Map.of(HiddenReason.INJURED, 19, HiddenReason.PREGNANT, 3, HiddenReason.UNIT_TECH, 211,
              HiddenReason.OTHER_EXPERIENCE, 43), counts);
    }

    @Test
    void candidateSearchIgnoresCase() {
        assertTrue(healthy(SkillLevel.REGULAR).matches("Mek Tech"));
        assertFalse(healthy(SkillLevel.REGULAR).matches("aero"));
    }
}
