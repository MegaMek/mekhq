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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.OffBoardDirection;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Orders a player gives a unit's bot during one battle stay with that battle. Before, a flee order carried back into
 * the campaign on the unit, and the unit fled by the same edge in the next fight (issue #10600).
 */
class ClearGameDataUnitOrdersTest {
    private static final int ROUND_OF_THE_ORDER = 1;

    @Test
    void aFleeOrderFromTheLastBattleIsCleared() {
        Campaign campaign = PartsScenario.create().getCampaign();
        Entity entityFromTheBattle = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        entityFromTheBattle.setUnitOrders(UnitOrderAction.EXIT_BY_EDGE.apply(UnitOrders.NONE, List.of(),
              OffBoardDirection.NORTH, UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, ROUND_OF_THE_ORDER));
        assertEquals(EdgeOrder.EXIT_BY, entityFromTheBattle.getUnitOrders().getEdgeOrder(),
              "The unit comes back from the battle still ordered off the north edge");

        campaign.clearGameData(entityFromTheBattle);

        assertTrue(entityFromTheBattle.getUnitOrders().isEmpty(), "The next battle starts with no orders");
    }
}
