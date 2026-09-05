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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Pins the deployment gate for support carriers: closed today, one place to open it later.
 */
class SupportCarrierDeploymentTest {

    @Test
    void gateIsClosedByDefault() {
        assertFalse(SupportCarrierDeployment.isAllowed(mock(Scenario.class)));
        assertFalse(SupportCarrierDeployment.isAllowed(null));
    }

    @Test
    void staysHome_carrierStaysAndFighterGoes() {
        Unit carrier = mock(Unit.class);
        when(carrier.isCarrier()).thenReturn(true);
        Unit mek = mock(Unit.class);
        when(mek.isCarrier()).thenReturn(false);

        assertTrue(SupportCarrierDeployment.staysHome(carrier, mock(Scenario.class)));
        assertTrue(SupportCarrierDeployment.staysHome(carrier, null));
        assertFalse(SupportCarrierDeployment.staysHome(mek, mock(Scenario.class)));
        assertFalse(SupportCarrierDeployment.staysHome(null, mock(Scenario.class)));
    }

    @Test
    void canDeploy_refusesACarrierOnItsOwn() {
        Scenario scenario = new Scenario("gate test");
        Unit carrier = mock(Unit.class);
        when(carrier.isCarrier()).thenReturn(true);

        assertFalse(scenario.canDeploy(carrier, mock(Campaign.class)));
    }

    @Test
    void canDeploy_nullUnitIsNotACarrier() {
        // Callers pass campaign.getUnit(id), which is null for a stale formation entry. Before the carrier gate a
        // bare scenario returned true for that; the gate must not turn it into an NPE.
        Scenario scenario = new Scenario("gate test");

        assertTrue(scenario.canDeploy(null, mock(Campaign.class)));
    }

    @Test
    void canDeploy_stillAcceptsAFightingUnit() {
        Scenario scenario = new Scenario("gate test");
        Unit mek = mock(Unit.class);
        when(mek.isCarrier()).thenReturn(false);

        // No traitors and no deployment limit on a bare scenario, so a fighting unit is eligible.
        assertTrue(scenario.canDeploy(mek, mock(Campaign.class)));
    }
}
