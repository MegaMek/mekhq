/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTestFixtures.validBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Guards the group-index contract that the Life Path wizard's sections depend on.
 *
 * <p>The wizard stores a section as numbered groups, and those numbers are the keys in the saved file. The group
 * objects themselves live in the GUI package and need a running dialog to exercise, so these tests cover the part of
 * the contract that is visible from the record: what a group index means once it reaches a Life Path, and what happens
 * to a file whose indexes are not tidy.</p>
 *
 * @since 0.50.11
 */
class LifePathGroupBehaviourTest {
    @Test
    void testGroupsAreIndependent() {
        LifePath lifePath = validBuilder().requirementsSkills(Map.of(0, Map.of("Gunnery/Mek", 3),
              1, Map.of("Piloting/Mek", 4))).build();

        assertEquals(Map.of("Gunnery/Mek", 3), lifePath.requirementsSkills().get(0),
              "Group 0 should hold only its own entries.");
        assertEquals(Map.of("Piloting/Mek", 4), lifePath.requirementsSkills().get(1),
              "Group 1 should hold only its own entries.");
    }

    @Test
    void testGroupIndexesNeedNotBeConsecutive() {
        // Empty groups are stripped on save, so a file written from a section with three groups where the middle one
        // was empty holds indexes 0 and 2.
        LifePath lifePath = validBuilder().requirementsFactions(Map.of(0, java.util.Set.of("FS"),
              2, java.util.Set.of("DC"))).build();

        assertEquals(2, lifePath.requirementsFactions().size(), "Both surviving groups should load.");
        assertNull(lifePath.requirementsFactions().get(1), "The stripped group should simply be absent.");
    }

    @Test
    void testAnUnsetEdgeIsAbsentRatherThanZero() {
        // A group the author never opened the attribute picker for must write no Edge rule at all. Storing the
        // "unset" number instead put an unintended rule into every such group.
        LifePath lifePath = validBuilder().build();

        assertTrue(lifePath.requirementsEdge().isEmpty(),
              "A Life Path nobody set an Edge rule on should carry no Edge entries.");
        assertTrue(lifePath.exclusionsEdge().isEmpty(),
              "An exclusion group nobody touched should carry no Edge entries, or it reads as banning Edge.");
    }

    @Test
    void testAnEdgeOfZeroIsStillAMeaningfulRequirement() {
        // Zero is a legitimate Edge requirement, so it must survive. Only a missing entry means "no rule".
        LifePath lifePath = validBuilder().requirementsEdge(Map.of(0, 0)).build();

        assertEquals(0, lifePath.requirementsEdge().get(0),
              "An Edge requirement of zero is a rule and should be kept.");
    }

    @Test
    void testANullEdgeValueSurvivesConstruction() {
        // The wizard holds null for "unset" while editing, so the record has to tolerate one reaching it rather than
        // unboxing it.
        Map<Integer, Integer> edge = new HashMap<>();
        edge.put(0, null);

        LifePath lifePath = validBuilder().requirementsEdge(edge).build();

        assertNull(lifePath.requirementsEdge().get(0), "A null Edge value should be carried, not unboxed.");
    }

    @Test
    void testEveryGroupMapKeyIsTheGroupIndex() {
        // The outer key of all 39 group maps is the group index, and nothing else. A test that says so out loud keeps
        // the next reader from guessing.
        LifePath lifePath = validBuilder().requirementsSkills(Map.of(7, Map.of("Gunnery/Mek", 3)))
                                  .fixedXPSkills(Map.of(7, Map.of("Gunnery/Mek", 100)))
                                  .xpCost(100)
                                  .build();

        assertTrue(lifePath.requirementsSkills().containsKey(7),
              "A requirement in group 7 should be keyed by 7.");
        assertTrue(lifePath.fixedXPSkills().containsKey(7),
              "An award in group 7 should be keyed by 7, independently of the requirement section.");
    }
}
