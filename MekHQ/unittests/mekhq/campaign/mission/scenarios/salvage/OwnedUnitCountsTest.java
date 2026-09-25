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
package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Tests the "you already own N of these" counts shown on each wreck in the salvage recovery console (#719).
 */
class OwnedUnitCountsTest {
    private static Entity entity(String chassis, String model) {
        Entity entity = mock(Mek.class);
        when(entity.getChassis()).thenReturn(chassis);
        when(entity.getModel()).thenReturn(model);
        return entity;
    }

    private static Unit ownedUnit(String chassis, String model) {
        Entity entity = entity(chassis, model);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }

    @Test
    void countsExactVariantsAndWholeChassisSeparately() {
        OwnedUnitCounts counts = OwnedUnitCounts.of(List.of(ownedUnit("Wolverine", "WVR-6R"),
              ownedUnit("Wolverine", "WVR-6R"),
              ownedUnit("Wolverine", "WVR-6M"),
              ownedUnit("Shadow Hawk", "SHD-2H")));

        Entity wreck = entity("Wolverine", "WVR-6R");

        assertEquals(2, counts.getVariantCount(wreck));
        assertEquals(3, counts.getChassisCount(wreck));
    }

    @Test
    void unownedWreckCountsZero() {
        OwnedUnitCounts counts = OwnedUnitCounts.of(List.of(ownedUnit("Wolverine", "WVR-6R")));

        Entity wreck = entity("Atlas", "AS7-D");

        assertEquals(0, counts.getVariantCount(wreck));
        assertEquals(0, counts.getChassisCount(wreck));
    }

    @Test
    void sameModelOnADifferentChassisIsADifferentVariant() {
        OwnedUnitCounts counts = OwnedUnitCounts.of(List.of(ownedUnit("Locust", "Prime")));

        Entity wreck = entity("Mad Cat", "Prime");

        assertEquals(0, counts.getVariantCount(wreck));
    }

    @Test
    void ownedUnitsWithoutAnEntityAreSkipped() {
        Unit unitWithoutEntity = mock(Unit.class);

        OwnedUnitCounts counts = OwnedUnitCounts.of(List.of(unitWithoutEntity, ownedUnit("Wolverine", "WVR-6R")));

        Entity wreck = entity("Wolverine", "WVR-6R");
        assertEquals(1, counts.getVariantCount(wreck));
        assertEquals(1, counts.getChassisCount(wreck));
    }

    @Test
    void wreckWithoutAnEntityCountsZero() {
        OwnedUnitCounts counts = OwnedUnitCounts.of(List.of(ownedUnit("Wolverine", "WVR-6R")));

        assertEquals(0, counts.getVariantCount(null));
        assertEquals(0, counts.getChassisCount(null));
    }
}
