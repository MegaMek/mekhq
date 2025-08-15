/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import megamek.common.CriticalSlot;
import megamek.common.LandAirMek;
import megamek.common.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

public class MissingMekLocationTest {
    @Test
    public void missingLAMTorsoRepairableOnlyWithMissingAvionicsAndLandingGear() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        LandAirMek entity = mock(LandAirMek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        int location = Mek.LOC_RT;
        MissingMekLocation missing = new MissingMekLocation(location, 30, 0, false, false, false, mockCampaign);
        missing.setUnit(unit);

        // 2 criticals
        doReturn(2).when(entity).getNumberOfCriticals(eq(location));
        CriticalSlot mockLandingGear = mock(CriticalSlot.class);
        when(mockLandingGear.isEverHittable()).thenReturn(true);
        when(mockLandingGear.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockLandingGear.getIndex()).thenReturn(LandAirMek.LAM_LANDING_GEAR);
        doReturn(mockLandingGear).when(entity).getCritical(eq(location), eq(0));
        CriticalSlot mockAvionics = mock(CriticalSlot.class);
        when(mockAvionics.isEverHittable()).thenReturn(true);
        when(mockAvionics.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockAvionics.getIndex()).thenReturn(LandAirMek.LAM_AVIONICS);
        doReturn(mockAvionics).when(entity).getCritical(eq(location), eq(1));

        // No missing parts
        doAnswer(inv -> null).when(unit).findPart(any());

        // We cannot repair this torso
        String message = missing.checkFixable();
        assertNotNull(message);
        assertTrue(message.contains("Avionics"));
        assertTrue(message.contains("Landing Gear"));

        // Only missing landing gear, avionics are still good
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            return predicate.test(missingLandingGear) ? missingLandingGear : null;
        }).when(unit).findPart(any());

        // We cannot repair this torso
        message = missing.checkFixable();
        assertNotNull(message);
        assertTrue(message.contains("Avionics"));

        // Only missing avionics, landing gear is still good
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We cannot repair this torso
        message = missing.checkFixable();
        assertNotNull(message);
        assertTrue(message.contains("Landing Gear"));

        // Missing both Landing Gear and Avionics
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            if (predicate.test(missingLandingGear)) {
                return missingLandingGear;
            }

            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We CAN repair this torso
        assertNull(missing.checkFixable());
    }

    @Test
    public void missingLAMHeadRepairableOnlyWithMissingAvionics() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        LandAirMek entity = mock(LandAirMek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        int location = Mek.LOC_HEAD;
        MissingMekLocation missing = new MissingMekLocation(location, 30, 0, false, false, false, mockCampaign);
        missing.setUnit(unit);

        // 1 critical
        doReturn(1).when(entity).getNumberOfCriticals(eq(location));
        CriticalSlot mockAvionics = mock(CriticalSlot.class);
        when(mockAvionics.isEverHittable()).thenReturn(true);
        when(mockAvionics.getType()).thenReturn(CriticalSlot.TYPE_SYSTEM);
        when(mockAvionics.getIndex()).thenReturn(LandAirMek.LAM_AVIONICS);
        doReturn(mockAvionics).when(entity).getCritical(eq(location), eq(0));

        // No missing parts
        doAnswer(inv -> null).when(unit).findPart(any());

        // We cannot repair this head
        String message = missing.checkFixable();
        assertNotNull(message);
        assertTrue(message.contains("Avionics"));

        // Missing avionics
        doAnswer(inv -> {
            Predicate<Part> predicate = inv.getArgument(0);
            MissingLandingGear missingLandingGear = mock(MissingLandingGear.class);
            if (predicate.test(missingLandingGear)) {
                return missingLandingGear;
            }

            MissingAvionics missingAvionics = mock(MissingAvionics.class);
            return predicate.test(missingAvionics) ? missingAvionics : null;
        }).when(unit).findPart(any());

        // We CAN repair this head
        assertNull(missing.checkFixable());
    }
}
