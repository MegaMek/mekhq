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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.parts.missing.MissingEnginePart;
import mekhq.campaign.parts.missing.MissingMekLocation;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A primitive unit is a different unit type from its standard version. Refitting one into the other is allowed, but it
 * rebuilds the whole Mek, so it is a Class F refit that replaces every location and the engine.
 */
class RefitPrimitiveTest {
    private final PartsScenario scenario = PartsScenario.create();

    private static long countOf(Refit refit, Class<? extends Part> partType) {
        long count = 0;
        for (Part part : refit.getShoppingList()) {
            if (partType.isInstance(part)) {
                count++;
            }
        }
        return count;
    }

    private void assertWholeMekRebuilt(Refit refit, Unit unit) {
        assertNull(refit.checkFixable(), "The refit is allowed");
        assertEquals(Refit.CLASS_F, refit.getRefitClass(), "Changing between primitive and standard is Class F");
        assertEquals(unit.getEntity().locations(), countOf(refit, MissingMekLocation.class),
              "Every location is replaced");
        assertEquals(1, countOf(refit, MissingEnginePart.class), "The engine is replaced");
        assertTrue(refit.getTime() > 0);
    }

    @Test
    void aPrimitiveGriffinRefitsToAStandardOneByRebuildingIt() {
        Unit primitiveGriffin = scenario.withUnit(UnitFixture.GRIFFIN_GRF_1A);
        assertTrue(primitiveGriffin.getEntity().isPrimitive());

        Refit refit = new Refit(primitiveGriffin, UnitFixture.GRIFFIN_GRF_1N.loadEntity(), true, false, false);

        assertWholeMekRebuilt(refit, primitiveGriffin);
    }

    @Test
    void aStandardGriffinRefitsToAPrimitiveOneByRebuildingIt() {
        Unit standardGriffin = scenario.withUnit(UnitFixture.GRIFFIN_GRF_1N);

        Refit refit = new Refit(standardGriffin, UnitFixture.GRIFFIN_GRF_1A.loadEntity(), true, false, false);

        assertWholeMekRebuilt(refit, standardGriffin);
    }
}
