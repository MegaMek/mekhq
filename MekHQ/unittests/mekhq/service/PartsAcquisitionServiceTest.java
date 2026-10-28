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
package mekhq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.equipment.EquipmentPart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The parts dialog only reports pod stock for parts that can really sit in an OmniPod. Armor and ammunition spares
 * match whether or not they are flagged as podded, so ordinary spare stock was shown as pod stock (issue #3885).
 */
class PartsAcquisitionServiceTest {
    private static final int SPARES_IN_WAREHOUSE = 6;

    private Campaign campaign;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        // Like armor and ammunition in a real warehouse, every lookup finds the spares whatever the podded flag says
        PartInventory inventory = new PartInventory();
        inventory.setSupply(SPARES_IN_WAREHOUSE);
        when(campaign.getPartInventory(any(Part.class))).thenReturn(inventory);
    }

    @Test
    void spareArmorIsNotCountedAsPodStock() {
        Armor armor = new Armor(1, EquipmentType.T_ARMOR_STANDARD, 16, Entity.LOC_NONE, false, false, campaign);

        assertEquals(0, PartsAcquisitionService.countPoddedSpares(campaign, armor));
    }

    @Test
    void spareAmmunitionIsNotCountedAsPodStock() {
        AmmoStorage ammunition = new AmmoStorage(1, (AmmoType) EquipmentType.get("ISAC5 Ammo"), 20, campaign);

        assertEquals(0, PartsAcquisitionService.countPoddedSpares(campaign, ammunition));
    }

    @Test
    void poddableWeaponCountsPodStockAndKeepsItsFlag() {
        EquipmentPart mediumLaser = new EquipmentPart(50, EquipmentType.get("ISMediumLaser"), Entity.LOC_NONE, 1.0,
              false, campaign);

        assertEquals(SPARES_IN_WAREHOUSE, PartsAcquisitionService.countPoddedSpares(campaign, mediumLaser));
        assertFalse(mediumLaser.isOmniPodded());
    }

    @Test
    void partAlreadyPoddedIsNotCountedAgain() {
        EquipmentPart mediumLaser = new EquipmentPart(50, EquipmentType.get("ISMediumLaser"), Entity.LOC_NONE, 1.0,
              true, campaign);

        assertEquals(0, PartsAcquisitionService.countPoddedSpares(campaign, mediumLaser));
    }
}
