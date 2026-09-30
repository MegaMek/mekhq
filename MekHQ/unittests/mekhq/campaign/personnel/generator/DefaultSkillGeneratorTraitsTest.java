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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/**
 * Tests random trait generation in {@link DefaultSkillGenerator#generateTraits(Person)}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class DefaultSkillGeneratorTraitsTest {
    private static final int UNLUCKY_DIE_SIZE = 20;
    private static final int BLOODMARK_DIE_SIZE = 50;
    private static final int PIRATE_BLOODMARK_DIE_SIZE = 5;

    private Person person;
    private RandomSkillPreferences preferences;
    private DefaultSkillGenerator generator;

    @BeforeEach
    void beforeEach() {
        Campaign campaign = mockCampaign();
        Faction faction = mock(Faction.class);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(faction.getShortName()).thenReturn("MERC");

        person = new Person(campaign);
        person.setOriginFaction(faction);

        preferences = new RandomSkillPreferences();
        preferences.setRandomizeTraits(true);
        generator = new DefaultSkillGenerator(preferences);
    }

    private void generate(int traitRoll, int unluckyRoll, int bloodmarkRoll, int bloodmarkDieSize) {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(traitRoll);
            compute.when(() -> Compute.randomInt(UNLUCKY_DIE_SIZE)).thenReturn(unluckyRoll);
            compute.when(() -> Compute.randomInt(bloodmarkDieSize)).thenReturn(bloodmarkRoll);
            generator.generateTraits(person);
        }
    }

    @Test
    void disabledPreferenceLeavesTraitsUntouched() {
        preferences.setRandomizeTraits(false);

        generate(12, 0, 0, BLOODMARK_DIE_SIZE);

        assertEquals(0, person.getConnections());
        assertEquals(0, person.getFame());
        assertEquals(0, person.getWealth());
        assertEquals(0, person.getExtraIncomeTraitLevel());
        assertEquals(0, person.getUnlucky());
        assertEquals(0, person.getBloodmark());
    }

    @ParameterizedTest
    @CsvSource({ "2, -2", "3, -1", "5, -1", "6, 0", "7, 0", "8, 0", "9, 1", "11, 1", "12, 2" })
    void commonTraitsFollowTheTraitRollTable(int roll, int expected) {
        generate(roll, 1, 1, BLOODMARK_DIE_SIZE);

        assertEquals(Math.max(expected, 0), person.getConnections(), "Connections can't go below 0");
        assertEquals(expected, person.getFame());
        assertEquals(Math.max(expected, -1), person.getWealth(), "Wealth can't go below -1");
        assertEquals(expected, person.getExtraIncomeTraitLevel());
    }

    @Test
    void unluckyOnlyRolledOnOneInTwenty() {
        generate(12, 1, 1, BLOODMARK_DIE_SIZE);
        assertEquals(0, person.getUnlucky());

        generate(12, 0, 1, BLOODMARK_DIE_SIZE);
        assertEquals(2, person.getUnlucky());
    }

    @Test
    void unluckyNeverGoesNegative() {
        generate(2, 0, 1, BLOODMARK_DIE_SIZE);
        assertEquals(0, person.getUnlucky());
    }

    @Test
    void bloodmarkOnlyRolledOnOneInFifty() {
        generate(12, 1, 1, BLOODMARK_DIE_SIZE);
        assertEquals(0, person.getBloodmark());

        generate(12, 1, 0, BLOODMARK_DIE_SIZE);
        assertEquals(2, person.getBloodmark());
    }

    @ParameterizedTest
    @CsvSource({ "2, 2", "12, 2", "3, 1", "5, 1", "9, 1", "11, 1", "6, 0", "7, 0", "8, 0" })
    void bloodmarkFollowsTheBloodmarkRollTable(int roll, int expected) {
        generate(roll, 1, 0, BLOODMARK_DIE_SIZE);
        assertEquals(expected, person.getBloodmark());
    }

    @Test
    void piratesRollBloodmarkOnOneInFive() {
        Faction pirateFaction = mock(Faction.class);
        when(pirateFaction.isPirate()).thenReturn(true);
        person.setOriginFaction(pirateFaction);

        generate(12, 1, 0, PIRATE_BLOODMARK_DIE_SIZE);

        assertEquals(2, person.getBloodmark());
    }
}
