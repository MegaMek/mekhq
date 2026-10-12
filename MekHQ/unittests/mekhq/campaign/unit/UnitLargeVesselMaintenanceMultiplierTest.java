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

package mekhq.campaign.unit;

import static mekhq.campaign.personnel.Person.PRIMARY_ROLE_SUPPORT_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import megamek.common.units.Dropship;
import megamek.common.units.Jumpship;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;

/**
 * Large vessels are maintained by their own crew and must start at a x1 maintenance multiplier. At the general x4
 * default a DropShip needs 720 minutes, more than its engineer has in a day, so it is never maintained (#10394).
 */
class UnitLargeVesselMaintenanceMultiplierTest {

    @Test
    void newDropShipStartsAtSingleMultiplier() {
        Unit unit = new Unit(mock(Dropship.class), mock(Campaign.class));

        assertEquals(1, unit.getMaintenanceMultiplier());
        assertEquals(180, unit.getMaintenanceTime());
    }

    @Test
    void newDropShipFitsInItsEngineersDay() {
        Unit unit = new Unit(mock(Dropship.class), mock(Campaign.class));

        assertTrue(unit.getMaintenanceTime() <= PRIMARY_ROLE_SUPPORT_TIME);
    }

    @Test
    void newJumpShipStartsAtSingleMultiplier() {
        Unit unit = new Unit(mock(Jumpship.class), mock(Campaign.class));

        assertEquals(1, unit.getMaintenanceMultiplier());
        assertEquals(360, unit.getMaintenanceTime());
    }

    @Test
    void otherUnitsKeepStandardMultiplier() {
        Unit unit = new Unit(mock(Tank.class), mock(Campaign.class));

        assertEquals(4, unit.getMaintenanceMultiplier());
    }
}
