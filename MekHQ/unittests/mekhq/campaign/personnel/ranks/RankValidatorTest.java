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
package mekhq.campaign.personnel.ranks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.personnel.enums.RankSystemType;
import org.junit.jupiter.api.Test;

class RankValidatorTest {
    private static final String[] EMPTY_TIER = { "-", "-", "-", "-", "-", "-", "-", "-", "-" };

    @Test
    void acceptsSystemWhoseFirstRankNamesTheMekWarriorColumn() {
        RankSystem rankSystem = userDataSystem("None", "--MW", "--MW", "--MW", "--MW", "--MW", "--MW", "--MW", "--MW");

        assertTrue(new RankValidator().validate(rankSystem, false));
    }

    @Test
    void rejectsSystemWhoseFirstRankOnlyRedirects() {
        RankSystem rankSystem = userDataSystem("--ASF", "--MW", "--MW", "--MW", "--MW", "--MW", "--MW", "--MW",
              "--MW");

        assertFalse(new RankValidator().validate(rankSystem, false));
    }

    /**
     * A user data rank system with the given E0 names and every other tier empty, so the E0 row alone decides whether
     * it is valid.
     */
    private static RankSystem userDataSystem(String... firstRankNames) {
        List<Rank> ranks = new ArrayList<>();
        ranks.add(new Rank(firstRankNames, false, 1.0));
        for (int index = 1; index < Rank.RC_NUM; index++) {
            ranks.add(new Rank(EMPTY_TIER, false, 1.0));
        }

        RankSystem rankSystem = mock(RankSystem.class);
        when(rankSystem.getType()).thenReturn(RankSystemType.USER_DATA);
        when(rankSystem.getRanks()).thenReturn(ranks);
        when(rankSystem.getRank(0)).thenReturn(ranks.getFirst());
        return rankSystem;
    }
}
