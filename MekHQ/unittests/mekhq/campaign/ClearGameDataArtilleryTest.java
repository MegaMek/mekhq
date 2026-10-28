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

import megamek.common.board.Coords;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Artillery fire adjustment from one battle stays with that battle. Before, a unit that had adjusted its fire on a hex
 * in one battle still had the adjustment in the next one (issue #8941).
 *
 * <p>MegaMek's EntityPersistenceRegressionTest checks everything Entity.clearGameState resets; this only checks that
 * clearGameData still calls it.</p>
 */
class ClearGameDataArtilleryTest {
    private static final int ADJUSTED_FIRE_MODIFIER = -2;
    private static final Coords TARGET_HEX = new Coords(4, 4);

    @Test
    void artilleryFireAdjustmentFromTheLastBattleIsCleared() {
        Campaign campaign = PartsScenario.create().getCampaign();
        Entity entityFromTheBattle = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        Mounted<?> weapon = entityFromTheBattle.getWeaponList().get(0);
        entityFromTheBattle.aTracker.addWeapon(weapon);
        entityFromTheBattle.aTracker.setModifier(ADJUSTED_FIRE_MODIFIER, TARGET_HEX);
        assertEquals(ADJUSTED_FIRE_MODIFIER, entityFromTheBattle.aTracker.getModifier(weapon, TARGET_HEX),
              "The unit comes back from the battle with its fire adjusted on the target hex");

        campaign.clearGameData(entityFromTheBattle);

        assertEquals(0, entityFromTheBattle.aTracker.getModifier(weapon, TARGET_HEX),
              "The next battle starts with no fire adjustment");
    }
}
