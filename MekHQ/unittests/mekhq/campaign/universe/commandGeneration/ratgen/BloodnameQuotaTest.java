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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ratgenerator.ForceDescriptor;
import org.junit.jupiter.api.Test;

/**
 * Covers the share of a Clan force that holds a Bloodname, and how it turns into a count.
 */
class BloodnameQuotaTest {

    @Test
    void aBetterForceCarriesMoreBloodnamesAndNoneAboveTheClanWideCeiling() {
        double green = BloodnameQuota.share(ForceDescriptor.EXP_GREEN);
        double regular = BloodnameQuota.share(ForceDescriptor.EXP_REGULAR);
        double veteran = BloodnameQuota.share(ForceDescriptor.EXP_VETERAN);
        double elite = BloodnameQuota.share(ForceDescriptor.EXP_ELITE);

        assertTrue(green < regular, "a green force holds fewer than a regular one");
        assertTrue(regular < veteran, "a regular force holds fewer than a veteran one");
        assertTrue(veteran < elite, "a veteran force holds fewer than an elite one");
        assertTrue(elite <= 0.15, "forty Bloodnames of twenty-five holders in a Clan of seven thousand is the ceiling");
    }

    @Test
    void anExperienceLeftToChanceCountsAsRegular() {
        assertEquals(BloodnameQuota.share(ForceDescriptor.EXP_REGULAR), BloodnameQuota.share(null));
    }

    @Test
    void theQuotaIsTheShareOfTheEligibleWarriorsToTheNearestWhole() {
        assertEquals(15, BloodnameQuota.quota(103, BloodnameQuota.SHARE_ELITE), "a Cluster of 103 at the ceiling");
        assertEquals(5, BloodnameQuota.quota(103, BloodnameQuota.SHARE_REGULAR));
        assertEquals(2, BloodnameQuota.quota(103, BloodnameQuota.SHARE_GREEN));
        assertEquals(1, BloodnameQuota.quota(5, BloodnameQuota.SHARE_VETERAN), "a Star still gets one at ten percent");
        assertEquals(0, BloodnameQuota.quota(5, BloodnameQuota.SHARE_GREEN));
    }
}
