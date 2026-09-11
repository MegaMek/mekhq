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
package mekhq.campaign.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.BattleArmorSuit;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * The parts store stocks one suit per battle armor design straight from the unit summary. Pins that the suit carries
 * the summary's figures without a unit file ever being opened. Ground MP and the quad flag are passed through too,
 * but their only accessors are deprecated for removal, so they are not asserted here.
 */
class PartsStoreBattleArmorStockTest {

    @Test
    void suitIsBuiltFromTheSummaryFigures() {
        MekSummary summary = mock(MekSummary.class);
        when(summary.getChassis()).thenReturn("Elemental");
        when(summary.getModel()).thenReturn("[Laser]");
        when(summary.getTons()).thenReturn(1.0);
        when(summary.getWeightClass()).thenReturn(2);
        when(summary.getWalkMp()).thenReturn(1);
        when(summary.getJumpMp()).thenReturn(3);
        when(summary.isClan()).thenReturn(true);
        when(summary.getMoveMode()).thenReturn(EntityMovementMode.INF_JUMP);
        Campaign campaign = MHQTestUtilities.mockCampaign();

        BattleArmorSuit suit = PartsStore.createBattleArmorSuit(summary, campaign);

        assertEquals("Elemental [Laser] Suit", suit.getName());
        assertEquals(2, suit.getWeightClass());
        assertEquals(3, suit.getJumpMP());
        assertTrue(suit.isClan());
    }
}
