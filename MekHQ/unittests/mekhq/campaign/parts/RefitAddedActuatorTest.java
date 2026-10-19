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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.CriticalSlot;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.missing.MissingMekActuator;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Adding a hand or lower arm actuator is a change to the unit, a Class D refit, so the refit can be made (issue #6748).
 * Before, the refit class ignored added actuators, so a Mek given a hand counted as "no change" and the refit could not
 * be started.
 */
class RefitAddedActuatorTest {
    private final PartsScenario scenario = PartsScenario.create();

    /** A Wolverine WVR-6R with its left hand taken out. */
    private Unit wolverineWithoutLeftHand() {
        Entity wolverine = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        for (int slot = 0; slot < wolverine.getNumberOfCriticalSlots(Mek.LOC_LEFT_ARM); slot++) {
            CriticalSlot criticalSlot = wolverine.getCritical(Mek.LOC_LEFT_ARM, slot);
            if ((criticalSlot != null) && (criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM)
                      && (criticalSlot.getIndex() == Mek.ACTUATOR_HAND)) {
                wolverine.setCritical(Mek.LOC_LEFT_ARM, slot, null);
            }
        }
        return scenario.getCampaign().addNewUnit(wolverine, false, 0, PartQuality.QUALITY_D);
    }

    @Test
    void addingAHandIsAClassDRefit() {
        Unit wolverine = wolverineWithoutLeftHand();

        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6R.loadEntity(), true, false, false);

        assertEquals(Refit.CLASS_D, refit.getRefitClass());
    }

    @Test
    void theAddedHandIsBought() {
        Unit wolverine = wolverineWithoutLeftHand();

        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6R.loadEntity(), true, false, false);

        boolean isBuyingTheHand = refit.getShoppingList()
                                        .stream()
                                        .anyMatch(part -> part instanceof MissingMekActuator);
        assertTrue(isBuyingTheHand, "The new hand is on the shopping list");
    }
}
