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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link Faction#getActiveMercenaryOrganization(int)}. Each organization's founding year is also the last year
 * of its predecessor, and that year must belong to the newly founded organization, matching the new year's
 * introduction of it.
 */
class FactionMercenaryOrganizationTest {
    private Factions originalFactions;
    private Faction mercenaryGuild;
    private Faction mercenaryReviewBoard;
    private Faction mercenaryReviewAndBondingCommission;
    private Faction mercenaryBondingAuthority;

    private static Faction organization(int startYear, int endYear) {
        Faction faction = mock(Faction.class);
        when(faction.validIn(anyInt())).thenAnswer(invocation -> {
            int year = invocation.getArgument(0);
            return (year >= startYear) && (year <= endYear);
        });
        return faction;
    }

    @BeforeEach
    void setUp() {
        originalFactions = Factions.getInstance();

        // Date ranges as in data/universe/factions
        mercenaryGuild = organization(Integer.MIN_VALUE, 2789);
        mercenaryReviewBoard = organization(2789, 3052);
        mercenaryReviewAndBondingCommission = organization(3052, 3133);
        mercenaryBondingAuthority = organization(3133, Integer.MAX_VALUE);

        Factions factions = mock(Factions.class);
        when(factions.getFaction("MG")).thenReturn(mercenaryGuild);
        when(factions.getFaction("MRB")).thenReturn(mercenaryReviewBoard);
        when(factions.getFaction("MRBC")).thenReturn(mercenaryReviewAndBondingCommission);
        when(factions.getFaction("MBA")).thenReturn(mercenaryBondingAuthority);
        Factions.setInstance(factions);
    }

    @AfterEach
    void tearDown() {
        Factions.setInstance(originalFactions);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
          "2788, MG",
          "2789, MRB",
          "3051, MRB",
          "3052, MRBC",
          "3132, MRBC",
          "3133, MBA",
          "3151, MBA"
    })
    void testActiveOrganizationPrefersTheNewestInItsFoundingYear(int gameYear, String expectedCode) {
        Faction expected = switch (expectedCode) {
            case "MG" -> mercenaryGuild;
            case "MRB" -> mercenaryReviewBoard;
            case "MRBC" -> mercenaryReviewAndBondingCommission;
            default -> mercenaryBondingAuthority;
        };

        assertSame(expected, Faction.getActiveMercenaryOrganization(gameYear));
    }
}
