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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.enums.PartQuality;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Large vessels are maintained by their own crew and must start at a x1 maintenance multiplier. At the general x4
 * default a DropShip needs 720 minutes, more than its engineer has in a day, so it is never maintained (#10394).
 *
 * <p>This bug has been fixed twice. #7920 guarded the campaign's add-unit methods, but the constructor still started
 * every unit at x4, so any route that built a unit without those methods brought it back. Both layers are pinned
 * here.</p>
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

    @Test
    void campaignDefaultDoesNotReachANewDropShip() {
        // #7920: the campaign's default multiplier is applied to new units, and must skip large vessels.
        Unit unit = addThroughCampaign("Leopard (2537)", 4);

        assertInstanceOf(Dropship.class, unit.getEntity());
        assertEquals(1, unit.getMaintenanceMultiplier());
    }

    @Test
    void campaignDefaultDoesNotReachANewJumpShip() {
        Unit unit = addThroughCampaign("Invader Jumpship (2631)", 4);

        assertInstanceOf(Jumpship.class, unit.getEntity());
        assertEquals(1, unit.getMaintenanceMultiplier());
    }

    @Test
    void campaignDefaultStillReachesOtherUnits() {
        // The guard must not swallow the option for everything else: a tank takes the campaign default.
        Unit unit = addThroughCampaign("Vedette Medium Tank", 2);

        assertEquals(2, unit.getMaintenanceMultiplier());
    }

    /** Adds a real unit from the test data the way a purchase does, with the campaign's default multiplier set. */
    private static Unit addThroughCampaign(String unitFile, int defaultMultiplier) {
        Entity entity = MHQTestUtilities.getEntityForUnitTesting(unitFile, true);
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.DEFAULT_MAINTENANCE_TIME, defaultMultiplier);
        return campaign.addNewUnit(entity, false, 0, PartQuality.QUALITY_D);
    }
}
