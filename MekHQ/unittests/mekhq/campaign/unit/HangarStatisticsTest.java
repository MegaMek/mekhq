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
 * of The Topps Company Inc. All Rights Reserved.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Hangar;
import org.junit.jupiter.api.Test;

class HangarStatisticsTest {
    private final Hangar hangar = mock(Hangar.class);
    private final HangarStatistics hangarStatistics = new HangarStatistics(hangar);

    @Test
    void getNumberOfSuperHeavyVehiclesCountsSuperHeavyVehicleBayType() {
        Unit superHeavyVehicle = presentUnitWithEntity(vehicleWithWeight(150.0));

        when(hangar.getUnits()).thenReturn(List.of(superHeavyVehicle));

        assertEquals(1, hangarStatistics.getNumberOfSuperHeavyVehicles());
        assertEquals(0, hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK));
    }

    @Test
    void getOccupiedSuperHeavyVehicleBaysCapsAtPresentCapacity() {
        Unit firstSuperHeavyVehicle = presentUnitWithEntity(vehicleWithWeight(150.0));
        Unit secondSuperHeavyVehicle = presentUnitWithEntity(vehicleWithWeight(150.0));
        Unit carrier = presentUnitWithEntity(mock(Entity.class));
        when(carrier.getSuperHeavyVehicleCapacity()).thenReturn(1.0);

        when(hangar.getUnits()).thenReturn(List.of(firstSuperHeavyVehicle, secondSuperHeavyVehicle, carrier));

        assertEquals(1, hangarStatistics.getOccupiedSuperHeavyVehicleBays());
    }

    @Test
    void getTotalSuperHeavyVehicleBaysIgnoresUnitsNotPresent() {
        Unit presentCarrier = presentUnitWithEntity(mock(Entity.class));
        when(presentCarrier.getSuperHeavyVehicleCapacity()).thenReturn(3.0);

        Unit orderedCarrier = unitWithEntity(mock(Entity.class));
        when(orderedCarrier.getSuperHeavyVehicleCapacity()).thenReturn(4.0);

        when(hangar.getUnits()).thenReturn(List.of(presentCarrier, orderedCarrier));

        assertEquals(3, hangarStatistics.getTotalSuperHeavyVehicleBays());
    }

    private static Unit presentUnitWithEntity(Entity entity) {
        Unit unit = unitWithEntity(entity);
        when(unit.isPresent()).thenReturn(true);
        return unit;
    }

    private static Unit unitWithEntity(Entity entity) {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }

    private static Entity vehicleWithWeight(double weight) {
        Entity entity = mock(Entity.class);
        when(entity.hasETypeFlag(Entity.ETYPE_TANK)).thenReturn(true);
        when(entity.getWeight()).thenReturn(weight);
        return entity;
    }
}