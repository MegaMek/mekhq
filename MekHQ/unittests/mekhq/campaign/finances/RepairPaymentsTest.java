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
package mekhq.campaign.finances;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.EquipmentPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Which tasks Pay for Repairs charges for, and so which ones a force that cannot afford them is stopped from doing.
 */
class RepairPaymentsTest {
    private Campaign campaign;
    private CampaignOptions campaignOptions;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_REPAIRS, true);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
    }

    @Test
    void fixingADamagedPartIsPaidFor() {
        assertTrue(RepairPayments.isPaidFor(campaign, mock(EquipmentPart.class), true));
    }

    @Test
    void aRefitIsNeverPaidForHere() {
        // a refit reaches the same checks as a fix, but has its own costs
        assertFalse(RepairPayments.isPaidFor(campaign, mock(Refit.class), true));
    }

    @Test
    void armorIsNotPaidFor() {
        assertFalse(RepairPayments.isPaidFor(campaign, mock(Armor.class), true));
    }

    @Test
    void aReplacementOrSalvageIsNotPaidFor() {
        assertFalse(RepairPayments.isPaidFor(campaign, mock(EquipmentPart.class), false));
    }

    @Test
    void nothingIsPaidForWithTheOptionOff() {
        campaignOptions.set(CampaignOption.PAY_FOR_REPAIRS, false);

        assertFalse(RepairPayments.isPaidFor(campaign, mock(EquipmentPart.class), true));
    }
}
