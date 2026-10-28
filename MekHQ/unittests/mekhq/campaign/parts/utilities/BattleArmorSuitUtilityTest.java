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
package mekhq.campaign.parts.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.battleValue.BattleArmorBVCalculator;
import megamek.common.equipment.WeaponMounted;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import testUtilities.parts.UnitFixture;

/**
 * Looking up a suit's details reads its unit file, so it happens once per chassis and model (issue #10552).
 */
class BattleArmorSuitUtilityTest {
    private static final UnitFixture SUIT = UnitFixture.IS_STANDARD_BATTLE_ARMOR_LASER;
    private static final String MODEL = "(Sqd4)";

    /** The details are cached for the whole session, so each test uses a chassis no other test has looked up. */
    private static String uniqueChassis() {
        return "Cache Test Suit " + UUID.randomUUID();
    }

    private static MekSummary summaryOf(UnitFixture fixture) {
        MekSummary summary = mock(MekSummary.class);
        when(summary.getSourceFile()).thenReturn(fixture.getFile());
        return summary;
    }

    @Test
    void aSuitHasTheDetailsOfItsUnit() {
        BattleArmor suit = (BattleArmor) SUIT.loadEntity();
        int expectedBV = ((BattleArmorBVCalculator) suit.getBvCalculator()).singleTrooperBattleValue();
        int expectedWeaponHash = suit.getIndividualWeaponList().stream().map(WeaponMounted::getType).toList()
                                       .hashCode();

        MekSummary summary = summaryOf(SUIT);
        MekSummaryCache summaries = mock(MekSummaryCache.class);
        when(summaries.getMek(anyString())).thenReturn(summary);
        try (MockedStatic<MekSummaryCache> cache = mockStatic(MekSummaryCache.class)) {
            cache.when(MekSummaryCache::getInstance).thenReturn(summaries);

            BattleArmorSuitUtility utility = new BattleArmorSuitUtility(uniqueChassis(), MODEL);

            assertTrue(utility.hasEntity());
            assertEquals(expectedBV, utility.getBattleArmorSuitBV());
            assertEquals(expectedWeaponHash, utility.getWeaponTypeListHash());
        }
    }

    @Test
    void theSameSuitReadsItsUnitFileOnce() {
        String chassis = uniqueChassis();
        MekSummary summary = summaryOf(SUIT);
        MekSummaryCache summaries = mock(MekSummaryCache.class);
        when(summaries.getMek(anyString())).thenReturn(summary);
        try (MockedStatic<MekSummaryCache> cache = mockStatic(MekSummaryCache.class)) {
            cache.when(MekSummaryCache::getInstance).thenReturn(summaries);

            BattleArmorSuitUtility first = new BattleArmorSuitUtility(chassis, MODEL);
            BattleArmorSuitUtility second = new BattleArmorSuitUtility(chassis, MODEL);

            verify(summary, times(1)).getSourceFile();
            assertTrue(second.hasEntity());
            assertEquals(first.getBattleArmorSuitBV(), second.getBattleArmorSuitBV());
            assertEquals(first.getWeaponTypeListHash(), second.getWeaponTypeListHash());
        }
    }

    @Test
    void aSuitWhoseUnitWasMissingIsFoundOnceTheUnitIsAvailable() {
        String chassis = uniqueChassis();
        MekSummary summary = summaryOf(SUIT);
        MekSummaryCache summaries = mock(MekSummaryCache.class);
        try (MockedStatic<MekSummaryCache> cache = mockStatic(MekSummaryCache.class)) {
            cache.when(MekSummaryCache::getInstance).thenReturn(summaries);

            BattleArmorSuitUtility missing = new BattleArmorSuitUtility(chassis, MODEL);
            assertFalse(missing.hasEntity());
            assertEquals(0, missing.getBattleArmorSuitBV());
            assertEquals(0, missing.getWeaponTypeListHash());

            when(summaries.getMek(anyString())).thenReturn(summary);
            assertTrue(new BattleArmorSuitUtility(chassis, MODEL).hasEntity());
        }
    }
}
