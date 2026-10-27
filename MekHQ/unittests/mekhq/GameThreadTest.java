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
package mekhq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * MegaMek builds its lobby force tree in the order units arrive, so the units of a deployment are sent in TO&amp;E
 * order. Before, they were sent in the order of a hash map, which scrambled lances and the units inside them.
 *
 * @see GameThread#inTOEOrder
 */
class GameThreadTest {

    private static Unit unitWithId(UUID id) {
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(id);
        return unit;
    }

    @Test
    void sendsUnitsInTOEOrder() {
        // Company: its own command unit, then Alpha Lance (two units), then Bravo Lance (one unit)
        UUID commandId = UUID.randomUUID();
        UUID alphaOneId = UUID.randomUUID();
        UUID alphaTwoId = UUID.randomUUID();
        UUID bravoOneId = UUID.randomUUID();
        Formation company = new Formation("Company");
        Formation alphaLance = new Formation("Alpha Lance");
        Formation bravoLance = new Formation("Bravo Lance");
        company.addUnit(commandId);
        alphaLance.addUnit(alphaOneId);
        alphaLance.addUnit(alphaTwoId);
        bravoLance.addUnit(bravoOneId);
        company.addSubFormation(alphaLance, true);
        company.addSubFormation(bravoLance, true);

        Campaign campaign = mockCampaign();
        when(campaign.getPlayerForce().getAllUnitsInTheTOE(false)).thenReturn(company.getAllUnits(false));

        Unit command = unitWithId(commandId);
        Unit alphaOne = unitWithId(alphaOneId);
        Unit alphaTwo = unitWithId(alphaTwoId);
        Unit bravoOne = unitWithId(bravoOneId);

        List<Unit> ordered = GameThread.inTOEOrder(List.of(bravoOne, alphaTwo, command, alphaOne), campaign);

        assertEquals(List.of(command, alphaOne, alphaTwo, bravoOne), ordered);
    }

    @Test
    void unitsOutsideTheTOEGoLastInTheirGivenOrder() {
        UUID inTOEId = UUID.randomUUID();
        Campaign campaign = mockCampaign();
        when(campaign.getPlayerForce().getAllUnitsInTheTOE(false)).thenReturn(List.of(inTOEId));

        Unit firstOutside = unitWithId(UUID.randomUUID());
        Unit inTOE = unitWithId(inTOEId);
        Unit secondOutside = unitWithId(UUID.randomUUID());

        List<Unit> ordered = GameThread.inTOEOrder(List.of(firstOutside, inTOE, secondOutside), campaign);

        assertEquals(List.of(inTOE, firstOutside, secondOutside), ordered);
    }
}
