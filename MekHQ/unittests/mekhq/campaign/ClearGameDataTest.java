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

import megamek.common.OffBoardDirection;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A unit that comes back from a battle is cleared of how it left that battle before it is sent into another (issue
 * #9332). Before, a Mek wrecked in one battle kept its salvageable removal condition until MekHQ was restarted, and
 * fleeing a later battle then left a wreck rather than counting as a retreat.
 */
class ClearGameDataTest {
    private Campaign campaign;
    private Entity entityFromTheBattle;

    @BeforeEach
    void setUp() {
        campaign = PartsScenario.create().getCampaign();
        entityFromTheBattle = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
    }

    @Test
    void theRemovalConditionFromTheBattleIsCleared() {
        entityFromTheBattle.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);

        campaign.clearGameData(entityFromTheBattle);

        assertEquals(IEntityRemovalConditions.REMOVE_UNKNOWN, entityFromTheBattle.getRemovalCondition());
    }

    @Test
    void aMekWreckedEarlierCountsAsRetreatingFromTheNextBattle() {
        entityFromTheBattle.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        campaign.clearGameData(entityFromTheBattle);

        entityFromTheBattle.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);

        assertEquals(IEntityRemovalConditions.REMOVE_IN_RETREAT, entityFromTheBattle.getRemovalCondition(),
              "Fleeing the next battle is a retreat, not a wreck");
    }

    @Test
    void theRetreatDirectionFromTheBattleIsCleared() {
        entityFromTheBattle.setRetreatedDirection(OffBoardDirection.NORTH);

        campaign.clearGameData(entityFromTheBattle);

        assertEquals(OffBoardDirection.NONE, entityFromTheBattle.getRetreatedDirection());
    }
}
