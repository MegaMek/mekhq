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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ConnectionsLevelTest {
    @Test
    void testParseConnectionsLevelFromInt_ValidLevel() {
        int inputValue = 5;
        ConnectionsLevel expected = ConnectionsLevel.CONNECTIONS_FIVE;

        ConnectionsLevel result = ConnectionsLevel.parseConnectionsLevelFromInt(inputValue);

        assertEquals(expected, result, "Expected the parsed level to be CONNECTIONS_FIVE");
    }

    @Test
    void testParseConnectionsLevelFromInt_ZeroLevel() {
        int inputValue = 0;
        ConnectionsLevel expected = ConnectionsLevel.CONNECTIONS_ZERO;

        ConnectionsLevel result = ConnectionsLevel.parseConnectionsLevelFromInt(inputValue);

        assertEquals(expected, result, "Expected the parsed level to be CONNECTIONS_ZERO");
    }

    @Test
    void testParseConnectionsLevelFromInt_InvalidHighLevel() {
        int inputValue = 11;
        ConnectionsLevel expected = ConnectionsLevel.CONNECTIONS_ZERO;

        ConnectionsLevel result = ConnectionsLevel.parseConnectionsLevelFromInt(inputValue);

        assertEquals(expected, result, "Expected the parsed level to be CONNECTIONS_ZERO for invalid high level");
    }

    @Test
    void testParseConnectionsLevelFromInt_NegativeLevel() {
        int inputValue = -1;
        ConnectionsLevel expected = ConnectionsLevel.CONNECTIONS_ZERO;

        ConnectionsLevel result = ConnectionsLevel.parseConnectionsLevelFromInt(inputValue);

        assertEquals(expected, result, "Expected the parsed level to be CONNECTIONS_ZERO for negative level");
    }

    @ParameterizedTest
    @EnumSource(value = ConnectionsLevel.class)
    void testAllLevelsAreUnique(ConnectionsLevel connectionsLevel) {
        for (ConnectionsLevel otherLevel : ConnectionsLevel.values()) {
            if (otherLevel.equals(connectionsLevel)) {
                continue;
            }

            int levelValue = connectionsLevel.getLevel();
            int otherLevelValue = otherLevel.getLevel();

            assertNotEquals(levelValue,
                  otherLevelValue,
                  "Duplicate Connections Level: " + connectionsLevel.name() + ", " + otherLevel.name());
        }
    }

    @Test
    void testAllBurnChancesAreDecremental() {
        int iterations = ConnectionsLevel.values().length;

        int lastBurnChance = Integer.MAX_VALUE;
        for (int i = 0; i < iterations; i++) {
            ConnectionsLevel connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(i);

            int currentBurnChance = connectionsLevel.getBurnChance();

            assertTrue(currentBurnChance <= lastBurnChance,
                  "Burn Chance is not decremental for " + connectionsLevel.name());

            lastBurnChance = currentBurnChance;
        }
    }

    @Test
    void testAllWealthValuesAreIncremental() {
        int iterations = ConnectionsLevel.values().length;

        Money lastWealth = Money.of(-1);
        for (int i = 0; i < iterations; i++) {
            ConnectionsLevel connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(i);

            Money currentWealth = connectionsLevel.getWealth();

            assertTrue(currentWealth.isGreaterThan(lastWealth),
                  "Wealth is not incremental for " + connectionsLevel.name());

            lastWealth = currentWealth;
        }
    }

    @Test
    void testAllRecruitValuesAreIncremental() {
        int iterations = ConnectionsLevel.values().length;

        int lastRecruits = 0;
        for (int i = 0; i < iterations; i++) {
            ConnectionsLevel connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(i);

            int currentRecruits = connectionsLevel.getRecruits();

            assertTrue(currentRecruits >= lastRecruits,
                  "Recruits is not incremental for " + connectionsLevel.name());

            lastRecruits = currentRecruits;
        }
    }
}
