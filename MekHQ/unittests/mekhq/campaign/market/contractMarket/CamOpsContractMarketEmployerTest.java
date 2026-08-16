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
package mekhq.campaign.market.contractMarket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.universe.FactionTag;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CamOpsContractMarket#matchesAllEmployerTags}: a faction sits in a rolled employer column only when it
 * carries <em>every</em> tag of that column (e.g. PERIPHERY + MAJOR means "major Periphery power", not "anything
 * Periphery").
 */
class CamOpsContractMarketEmployerTest {

    private static Faction mockFaction(boolean independent, FactionTag... tags) {
        Faction faction = mock(Faction.class);
        when(faction.isIndependent()).thenReturn(independent);
        for (FactionTag tag : tags) {
            when(faction.is(tag)).thenReturn(true);
        }
        return faction;
    }

    @Test
    void factionCarryingEveryTagMatches() {
        Faction majorPeripheryPower = mockFaction(false, FactionTag.PERIPHERY, FactionTag.MAJOR);

        assertTrue(CamOpsContractMarket.matchesAllEmployerTags(majorPeripheryPower,
                    List.of(FactionTag.PERIPHERY, FactionTag.MAJOR)),
              "A faction carrying every tag of the employer column should qualify");
    }

    /**
     * Regression test: the old loop added a faction to the pool after each individually matching tag, so a faction
     * matching only the first tag of a multi-tag column still slipped in (and full matches were added twice).
     */
    @Test
    void factionMissingAnyTagDoesNotMatch() {
        Faction minorPeripheryPower = mockFaction(false, FactionTag.PERIPHERY, FactionTag.MINOR);

        assertFalse(CamOpsContractMarket.matchesAllEmployerTags(minorPeripheryPower,
                    List.of(FactionTag.PERIPHERY, FactionTag.MAJOR)),
              "A faction matching only part of a multi-tag employer column must not qualify");
    }

    @Test
    void independentFactionSatisfiesSmallTagWithoutCarryingIt() {
        Faction independentWorld = mockFaction(true, FactionTag.PLANETARY_GOVERNMENT);

        assertTrue(CamOpsContractMarket.matchesAllEmployerTags(independentWorld,
                    List.of(FactionTag.SMALL, FactionTag.PLANETARY_GOVERNMENT)),
              "Independence stands in for the SMALL tag, which independent factions don't carry directly");
    }

    @Test
    void nonIndependentFactionWithoutSmallTagDoesNotMatch() {
        Faction greatHouse = mockFaction(false, FactionTag.MAJOR);

        assertFalse(CamOpsContractMarket.matchesAllEmployerTags(greatHouse, List.of(FactionTag.SMALL)),
              "A faction neither tagged SMALL nor independent must not sit in the small-employers column");
    }
}
