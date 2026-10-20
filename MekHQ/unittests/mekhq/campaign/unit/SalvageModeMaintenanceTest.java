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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A unit in salvage mode is being stripped for parts, so it is not maintained: no maintenance checks, no upkeep and
 * none of its tech's time (MekHQ #5316).
 */
class SalvageModeMaintenanceTest {
    private static final int MAINTENANCE_MINUTES = 60;

    private Campaign campaign;
    private Unit unit;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        lenient().when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        lenient().when(campaignOptions.get(CampaignOption.IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION)).thenReturn(false);
        unit = spy(new Unit(mock(Entity.class), campaign));
        doReturn(MAINTENANCE_MINUTES).when(unit).getMaintenanceTime();
        doReturn(Money.of(100)).when(unit).getWeeklyMaintenanceCost();
    }

    @Test
    void unitInRepairModeIsMaintained() {
        unit.setSalvage(false);

        assertTrue(unit.requiresMaintenance());
        assertTrue(unit.isUnmaintained(), "with no tech assigned, a unit in repair mode is flagged unmaintained");
    }

    @Test
    void unitInSalvageModeIsNotMaintained() {
        unit.setSalvage(true);

        assertFalse(unit.requiresMaintenance());
        assertFalse(unit.isUnmaintained(), "a unit in salvage mode is not flagged as missing maintenance");
    }

    @Test
    void unitInSalvageModeCostsNoUpkeep() {
        unit.setSalvage(false);
        assertEquals(Money.of(100), Accountant.getWeeklyMaintenanceTotal(List.of(unit)));

        unit.setSalvage(true);
        assertEquals(Money.zero(), Accountant.getWeeklyMaintenanceTotal(List.of(unit)));
    }

    @Test
    void unitInSalvageModeTakesNoneOfItsTechsTime() {
        Person tech = new Person(campaign);
        tech.addTechUnit(unit);

        unit.setSalvage(false);
        assertEquals(MAINTENANCE_MINUTES, tech.getMaintenanceTimeUsing());

        unit.setSalvage(true);
        assertEquals(0, tech.getMaintenanceTimeUsing());
    }

    @Test
    void immediateMaintenanceExplainsSalvageMode() {
        String text = getFormattedTextAt("mekhq.resources.GUI", "maintenanceAdHoc.salvage", "Atlas AS7-D");

        assertTrue(text.startsWith("Atlas AS7-D is in salvage mode"), text);
    }
}
