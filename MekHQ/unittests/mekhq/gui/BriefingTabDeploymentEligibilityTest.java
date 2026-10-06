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
package mekhq.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.mission.scenarios.Scenario;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9960 (#9952): a space-only unit (WarShip, JumpShip, space station) must be refused on a
 * ground or atmospheric map, since Commander mode has no lobby to catch it. Aerospace fighters and DropShips are not
 * blocked here - they follow the scenario's own rules.
 */
class BriefingTabDeploymentEligibilityTest {

    private static Entity entityOfType(int unitType) {
        Entity entity = mock(Entity.class);
        when(entity.getUnitType()).thenReturn(unitType);
        return entity;
    }

    @Test
    void warShipIsBlockedOnGround() {
        assertTrue(BriefingTab.isSpaceOnlyUnitOnNonSpaceBoard(entityOfType(UnitType.WARSHIP), Scenario.T_GROUND));
    }

    @Test
    void warShipIsBlockedInAtmosphere() {
        assertTrue(BriefingTab.isSpaceOnlyUnitOnNonSpaceBoard(entityOfType(UnitType.WARSHIP), Scenario.T_ATMOSPHERE));
    }

    @Test
    void warShipIsAllowedInSpace() {
        assertFalse(BriefingTab.isSpaceOnlyUnitOnNonSpaceBoard(entityOfType(UnitType.WARSHIP), Scenario.T_SPACE));
    }

    @Test
    void jumpShipAndSpaceStationAreBlockedOnGround() {
        assertTrue(BriefingTab.isSpaceOnlyUnitOnNonSpaceBoard(entityOfType(UnitType.JUMPSHIP), Scenario.T_GROUND));
        assertTrue(BriefingTab.isSpaceOnlyUnitOnNonSpaceBoard(entityOfType(UnitType.SPACE_STATION), Scenario.T_GROUND));
    }

    @Test
    void mekIsAllowedOnGround() {
        assertFalse(BriefingTab.isSpaceOnlyUnitOnNonSpaceBoard(entityOfType(UnitType.MEK), Scenario.T_GROUND));
    }

    @Test
    void dropShipIsNotBlockedHere() {
        // DropShips can make ground/atmospheric landfall, so they follow the scenario's own rules rather than being
        // refused by this space-only guard.
        assertFalse(BriefingTab.isSpaceOnlyUnitOnNonSpaceBoard(entityOfType(UnitType.DROPSHIP), Scenario.T_GROUND));
    }

    @Test
    void aerospaceFighterIsNotBlockedHere() {
        assertFalse(BriefingTab.isSpaceOnlyUnitOnNonSpaceBoard(entityOfType(UnitType.AEROSPACE_FIGHTER),
              Scenario.T_GROUND));
    }
}
