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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.ConvInfantry;
import megamek.common.units.EntityMovementMode;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Conventional infantry maintained by a Mechanic take that Mechanic's time, based on their movement mode, while
 * infantry that maintain themselves take none (issue #11047).
 */
class UnitConventionalInfantryMaintenanceTimeTest {

    @ParameterizedTest
    @CsvSource({
          "INF_LEG, 10",
          "INF_MOTORIZED, 20",
          "INF_UMU, 20",
          "SUBMARINE, 20",
          "INF_JUMP, 30",
          "TRACKED, 30",
          "WHEELED, 30",
          "HOVER, 30",
          "VTOL, 40"
    })
    void mechanicMaintainedInfantryTakeTimeByMovementMode(EntityMovementMode movementMode, int baseMinutes) {
        Unit unit = unitFor(movementMode, true);
        unit.setMaintenanceMultiplier(1);

        assertEquals(baseMinutes, unit.getMaintenanceTime());
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "INF_LEG", "INF_JUMP", "VTOL" })
    void maintenanceMultiplierScalesInfantryTime(EntityMovementMode movementMode) {
        Unit unit = unitFor(movementMode, true);
        unit.setMaintenanceMultiplier(1);
        int baseMinutes = unit.getMaintenanceTime();

        unit.setMaintenanceMultiplier(3);

        assertEquals(baseMinutes * 3, unit.getMaintenanceTime());
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "INF_LEG", "INF_MOTORIZED", "INF_JUMP", "VTOL" })
    void selfMaintainedInfantryTakeNoTime(EntityMovementMode movementMode) {
        assertEquals(0, unitFor(movementMode, false).getMaintenanceTime());
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "INF_LEG", "INF_JUMP" })
    void infantryWithoutCampaignTakeNoTime(EntityMovementMode movementMode) {
        assertEquals(0, new Unit(conventionalInfantry(movementMode), null).getMaintenanceTime());
    }

    private static ConvInfantry conventionalInfantry(EntityMovementMode movementMode) {
        ConvInfantry infantry = mock(ConvInfantry.class);
        when(infantry.isConventionalInfantry()).thenReturn(true);
        when(infantry.getMovementMode()).thenReturn(movementMode);
        return infantry;
    }

    private static Unit unitFor(EntityMovementMode movementMode, boolean isTechsMaintainConventionalInfantry) {
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.TECHS_MAINTAIN_CONVENTIONAL_INFANTRY))
              .thenReturn(isTechsMaintainConventionalInfantry);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        return new Unit(conventionalInfantry(movementMode), campaign);
    }
}
