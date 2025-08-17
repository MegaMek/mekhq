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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

public class MissingAvionicsTest {
    @Test
    public void missingLAMAvionicsRepairableOnlyWithBothTorsosAndHead() {
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        LandAirMek entity = mock(LandAirMek.class);
        when(unit.getEntity()).thenReturn(entity);
        when(entity.getWeight()).thenReturn(30.0);
        doCallRealMethod().when(entity).getLocationName(any());

        MissingAvionics missing = new MissingAvionics(30, mockCampaign);
        missing.setUnit(unit);

        final MissingMekLocation rightTorso = mock(MissingMekLocation.class);
        when(rightTorso.getLocation()).thenReturn(Mek.LOC_RT);

        final MissingMekLocation leftTorso = mock(MissingMekLocation.class);
        when(leftTorso.getLocation()).thenReturn(Mek.LOC_LT);

        final MissingMekLocation head = mock(MissingMekLocation.class);
        when(head.getLocation()).thenReturn(Mek.LOC_HEAD);

        // No missing parts
        when(unit.getParts()).thenReturn(new ArrayList<>());

        // We can repair the avionics if both torsos and head are available
        assertNull(missing.checkFixable());

        // Missing both side torsos and head
        when(unit.getParts()).thenReturn(Arrays.asList(rightTorso, head, leftTorso));

        // We cannot repair the avionics
        assertNotNull(missing.checkFixable());

        // Only missing the head
        when(unit.getParts()).thenReturn(List.of(head));

        // We cannot repair the avionics
        assertNotNull(missing.checkFixable());

        // Only missing the left torso
        when(unit.getParts()).thenReturn(List.of(leftTorso));

        // We cannot repair the avionics
        assertNotNull(missing.checkFixable());

        // Only missing the right torso
        when(unit.getParts()).thenReturn(List.of(rightTorso));

        // We cannot repair the avionics
        assertNotNull(missing.checkFixable());

        // Missing an arm
        final MissingMekLocation arm = mock(MissingMekLocation.class);
        when(arm.getLocation()).thenReturn(Mek.LOC_RARM);
        when(unit.getParts()).thenReturn(List.of(arm));

        // We CAN repair the avionics with just a missing arm
        assertNull(missing.checkFixable());
    }
}
