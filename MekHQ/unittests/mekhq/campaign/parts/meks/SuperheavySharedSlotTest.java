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
package mekhq.campaign.parts.meks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.CriticalSlot;
import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * On a superheavy Mek two one-slot items can share a critical slot; the Omega SHP-5R keeps two ammunition bins in
 * some of its slots. Putting a location back must restore both items, not only the first (issue #10277).
 */
class SuperheavySharedSlotTest {
    private Entity omega;
    private MekLocation sharedSlotLocation;
    private CriticalSlot sharedSlot;

    @BeforeEach
    void setUp() {
        Unit unit = PartsScenario.create().withUnit(UnitFixture.OMEGA_SHP_5R);
        omega = unit.getEntity();
        for (MekLocation location : PartsScenario.unitParts(unit, MekLocation.class)) {
            for (int slotIndex = 0; slotIndex < omega.getNumberOfCriticalSlots(location.getLoc()); slotIndex++) {
                CriticalSlot slot = omega.getCritical(location.getLoc(), slotIndex);
                boolean isShared = (slot != null) && (slot.getMount() != null) && (slot.getMount2() != null);
                if (isShared && (sharedSlot == null)) {
                    sharedSlot = slot;
                    sharedSlotLocation = location;
                }
            }
        }
        assertNotNull(sharedSlot, "The Omega SHP-5R has a slot holding two items");
    }

    @Test
    void reattachingALocationRestoresBothItemsInASharedSlot() {
        sharedSlot.setMissing(true);
        sharedSlot.getMount().setMissing(true);
        sharedSlot.getMount2().setMissing(true);
        sharedSlotLocation.setBlownOff(true);
        omega.setLocationBlownOff(sharedSlotLocation.getLoc(), true);

        sharedSlotLocation.fix();

        assertFalse(sharedSlot.getMount().isMissing());
        assertFalse(sharedSlot.getMount2().isMissing(), "The second item in the slot is back as well");
    }

    @Test
    void closingABreachRestoresBothItemsInASharedSlot() {
        sharedSlot.setBreached(true);
        sharedSlot.getMount().setBreached(true);
        sharedSlot.getMount2().setBreached(true);
        sharedSlotLocation.setBreached(true);

        sharedSlotLocation.fix();

        assertFalse(sharedSlot.getMount().isBreached());
        assertFalse(sharedSlot.getMount2().isBreached(), "The second item is no longer breached");
    }
}
