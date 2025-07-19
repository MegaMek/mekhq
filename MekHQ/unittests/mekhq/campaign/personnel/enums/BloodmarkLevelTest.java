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
package mekhq.campaign.personnel.enums;

import static mekhq.campaign.personnel.Person.MAXIMUM_BLOODMARK;
import static mekhq.campaign.personnel.Person.MINIMUM_BLOODMARK;
import static mekhq.campaign.personnel.enums.BloodmarkLevel.BLOODMARK_ZERO;
import static mekhq.campaign.personnel.enums.BloodmarkLevel.parseBloodmarkLevelFromInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BloodmarkLevelTest {
    @Test
    void test_allLevelsAreUnique() {
        List<Integer> allLevels = new ArrayList<>();
        for (BloodmarkLevel bloodmarkLevel : BloodmarkLevel.values()) {
            int level = bloodmarkLevel.getLevel();
            assertFalse(allLevels.contains(level), "Duplicate Bloodmark Level: " + level);
            allLevels.add(level);
        }
    }

    @Test
    void test_allLevelsAreValid() {
        for (int level = MINIMUM_BLOODMARK; level < MAXIMUM_BLOODMARK; level++) {
            BloodmarkLevel bloodmark = parseBloodmarkLevelFromInt(level);
            int bloodmarkLevel = bloodmark.getLevel();
            assertEquals(level, bloodmarkLevel, "Invalid Bloodmark Level: " + level);
        }
    }

    @Test
    void test_allBountiesAreIncremental() {
        Money lastBounty = Money.of(-1);
        for (BloodmarkLevel bloodmarkLevel : BloodmarkLevel.values()) {
            Money bounty = bloodmarkLevel.getBounty();
            assertTrue(bounty.isGreaterThan(lastBounty), "Bounty is not incremental for " + bloodmarkLevel.name());
            lastBounty = bounty;
        }
    }

    @Test
    void test_allBountyHunterSkillsAreDecremental() {
        int lastHunterSkill = Integer.MAX_VALUE;
        for (BloodmarkLevel bloodmarkLevel : BloodmarkLevel.values()) {
            int hunterSkill = bloodmarkLevel.getBountyHunterSkill();
            assertTrue(hunterSkill <= lastHunterSkill,
                  "Bounty Hunter Skill is not decremental for " + bloodmarkLevel.name());
            lastHunterSkill = hunterSkill;
        }
    }

    @Test
    void test_allRollFrequenciesAreDecremental() {
        int lastRollFrequency = Integer.MAX_VALUE;
        for (BloodmarkLevel bloodmarkLevel : BloodmarkLevel.values()) {
            if (bloodmarkLevel == BLOODMARK_ZERO) {
                continue;
            }

            int rollFrequency = bloodmarkLevel.getRollFrequency();
            assertTrue(rollFrequency <= lastRollFrequency,
                  "Roll frequency is not decremental for " + bloodmarkLevel.name());
            lastRollFrequency = rollFrequency;
        }
    }

    @ParameterizedTest
    @EnumSource(value = BloodmarkLevel.class)
    void test_allBountiesAreValid(BloodmarkLevel bloodmarkLevel) {
        if (bloodmarkLevel == BLOODMARK_ZERO) {
            assertEquals(Money.zero(), bloodmarkLevel.getBounty(),
                  "Invalid Bounty for " + bloodmarkLevel.name() + ". Should be 0");
        } else {
            assertTrue(bloodmarkLevel.getBounty().isPositive(),
                  "Invalid Bounty for " + bloodmarkLevel.name() + ". Should be greater than 0");
        }
    }

    @ParameterizedTest
    @EnumSource(value = BloodmarkLevel.class)
    void test_allRollFrequenciesAreValid(BloodmarkLevel bloodmarkLevel) {
        if (bloodmarkLevel == BLOODMARK_ZERO) {
            assertEquals(0, bloodmarkLevel.getRollFrequency(),
                  "Invalid Roll Frequency for " + bloodmarkLevel.name() + ". Should be 0");
        } else {
            assertTrue(bloodmarkLevel.getRollFrequency() > 0,
                  "Invalid Roll Frequency for " + bloodmarkLevel.name() + ". Should be greater than 0");
        }
    }

    @ParameterizedTest
    @EnumSource(value = BloodmarkLevel.class)
    void test_allRollDivisorsAreValid(BloodmarkLevel bloodmarkLevel) {
        if (bloodmarkLevel == BLOODMARK_ZERO) {
            assertEquals(0, bloodmarkLevel.getRollDivisor(),
                  "Invalid Roll Divisor for " + bloodmarkLevel.name() + ". Should be 0");
        } else {
            assertTrue(bloodmarkLevel.getRollDivisor() > 0,
                  "Invalid Roll Divisor for " + bloodmarkLevel.name() + ". Should be greater than 0");
        }
    }
}
