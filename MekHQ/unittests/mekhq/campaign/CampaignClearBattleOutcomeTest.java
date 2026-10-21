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
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A unit wrecked in a battle and then recovered must not carry that battle's result into the next one. It used to,
 * so when it only fled the next battle it was written off and its pilot listed as missing (MegaMek issue #9282).
 */
class CampaignClearBattleOutcomeTest {
    /** The id the earlier battle's killer had. */
    private static final int EARLIER_KILLER_ID = 34;

    @Test
    void clearingGameDataForgetsHowTheUnitLeftTheBattle() {
        Campaign campaign = PartsScenario.create().getCampaign();
        Entity wreckedUnit = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        wreckedUnit.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        Entity killer = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        killer.setId(EARLIER_KILLER_ID);
        killer.addKill(wreckedUnit);

        campaign.clearGameData(wreckedUnit);

        assertEquals(IEntityRemovalConditions.REMOVE_UNKNOWN, wreckedUnit.getRemovalCondition(),
              "The wreck result from the last battle must not follow the unit into the next one");
        assertEquals(Entity.NONE, wreckedUnit.getKillerId(), "The last battle's kill credit must be cleared");
    }

    @Test
    void clearingGameDataForgetsTheEdgeTheUnitFledBy() {
        Campaign campaign = PartsScenario.create().getCampaign();
        Entity fledUnit = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        fledUnit.setRetreatedDirection(OffBoardDirection.NORTH);

        campaign.clearGameData(fledUnit);

        assertEquals(OffBoardDirection.NONE, fledUnit.getRetreatedDirection(),
              "The edge the unit fled by in the last battle must not follow it into the next one");
    }
}
