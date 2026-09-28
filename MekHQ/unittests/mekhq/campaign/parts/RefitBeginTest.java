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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A unit has at most one refit: starting another while one is in progress is refused and leaves the first untouched
 * (issue #10198, where Begin Refit in the Mek Lab could start a second refit).
 */
class RefitBeginTest {
    @Test
    void aSecondRefitOnTheSameUnitIsRefused() throws Exception {
        PartsScenario scenario = PartsScenario.create();
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Refit firstRefit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), false, false, false);
        assertTrue(firstRefit.begin());
        int ordersAfterFirstRefit = scenario.getCampaign().getPlayerForce().getShoppingList().getShoppingList().size();

        Refit secondRefit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), false, false, false);
        boolean isSecondStarted = secondRefit.begin();

        assertFalse(isSecondStarted);
        assertSame(firstRefit, locust.getRefit(), "The refit in progress is untouched");
        assertEquals(ordersAfterFirstRefit,
              scenario.getCampaign().getPlayerForce().getShoppingList().getShoppingList().size(),
              "The refused refit orders nothing");
    }
}
