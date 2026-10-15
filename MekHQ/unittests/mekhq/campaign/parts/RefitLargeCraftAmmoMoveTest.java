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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Choosing a refit kit for some DropShips stopped with an error before the list of kits appeared. The Union (3055)
 * carries its LRM ammunition in different arcs from the Union (2708b), so a refit between them moves a bin of bay
 * ammunition, and that move asked for an install time large craft ammunition does not have (issue #3698).
 */
class RefitLargeCraftAmmoMoveTest {

    @Test
    void aRefitThatMovesBayAmmunitionToAnotherArcCanBePlanned() {
        Unit union = PartsScenario.create().withUnit(UnitFixture.UNION_3055_DROPSHIP);

        assertDoesNotThrow(() -> new Refit(union, UnitFixture.UNION_2708B_DROPSHIP.loadEntity(), false, false,
              false));
    }

    @Test
    void aRefitThatKeepsBayAmmunitionWhereItIsCanBePlanned() {
        Unit union = PartsScenario.create().withUnit(UnitFixture.UNION_2708B_DROPSHIP);

        assertDoesNotThrow(() -> new Refit(union, UnitFixture.UNION_2708B_DROPSHIP.loadEntity(), false, false,
              false));
    }
}
