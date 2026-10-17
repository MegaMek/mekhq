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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import mekhq.campaign.unit.Maintenance.GravityModifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The gravity penalty for maintenance and repair outside a facility follows the Planetary Conditions Modifiers table
 * (Campaign Operations p. 193): low gravity is +1 and high gravity +2. They used to be the other way round.
 */
class MaintenanceGravityModifierTest {

    @ParameterizedTest(name = "{0} G")
    @CsvSource(delimiter = ';', value = {
          "0.5; 1; Low Gravity",
          "0.79; 1; Low Gravity",
          "1.21; 2; High Gravity",
          "1.27; 2; High Gravity",
          "1.99; 2; High Gravity",
          "2.0; 4; Very High Gravity",
          "3.5; 4; Very High Gravity" })
    void unusualGravityIsPenalised(double gravity, int expectedModifier, String expectedDescription) {
        GravityModifier gravityModifier = Maintenance.getGravityModifier(gravity);

        assertEquals(new GravityModifier(expectedModifier, expectedDescription), gravityModifier);
    }

    @ParameterizedTest(name = "{0} G")
    @ValueSource(doubles = { 0.8, 1.0, 1.2 })
    void standardGravityHasNoPenalty(double gravity) {
        assertNull(Maintenance.getGravityModifier(gravity));
    }
}
