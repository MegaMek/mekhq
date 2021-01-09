/*
 * Copyright (C) 2020 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import megamek.common.LandAirMech;
import megamek.common.Mech;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

public class MissingAvionicsTest {
    @Test
    public void missingLAMAvionicsRepairableOnlyWithBothTorsosAndHead() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        LandAirMech entity = mock(LandAirMech.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        MissingAvionics missing = new MissingAvionics(30, mockCampaign);
        missing.setUnit(unit);

        final MissingMekLocation rightTorso = mock(MissingMekLocation.class);
        when(rightTorso.getLocation()).thenReturn(Mech.LOC_RT);

        final MissingMekLocation leftTorso = mock(MissingMekLocation.class);
        when(leftTorso.getLocation()).thenReturn(Mech.LOC_LT);

        final MissingMekLocation head = mock(MissingMekLocation.class);
        when(head.getLocation()).thenReturn(Mech.LOC_HEAD);

        // No missing parts
        when(unit.getParts()).thenReturn(new ArrayList<>());

        // We can repair the avionics if both torsos and head are available
        assertNull(missing.checkFixable());

        // Missing both side torsos and head
        when(unit.getParts()).thenReturn(Arrays.asList(rightTorso, head, leftTorso));

        // We cannot repair the avionics
        assertNotNull(missing.checkFixable());

        // Only missing the head
        when(unit.getParts()).thenReturn(Arrays.asList(head));

        // We cannot repair the avionics
        assertNotNull(missing.checkFixable());

        // Only missing the left torso
        when(unit.getParts()).thenReturn(Arrays.asList(leftTorso));

        // We cannot repair the avionics
        assertNotNull(missing.checkFixable());

        // Only missing the right torso
        when(unit.getParts()).thenReturn(Arrays.asList(rightTorso));

        // We cannot repair the avionics
        assertNotNull(missing.checkFixable());

        // Missing an arm
        final MissingMekLocation arm = mock(MissingMekLocation.class);
        when(arm.getLocation()).thenReturn(Mech.LOC_RARM);
        when(unit.getParts()).thenReturn(Arrays.asList(arm));

        // We CAN repair the avionics with just a missing arm
        assertNull(missing.checkFixable());
    }
}
