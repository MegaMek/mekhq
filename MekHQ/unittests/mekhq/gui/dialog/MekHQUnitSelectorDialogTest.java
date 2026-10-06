/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

class MekHQUnitSelectorDialogTest {

    @Test
    void persistentUnitFilterRejectsStandaloneAssetSummary() {
        MekSummary asset = mock(MekSummary.class);
        when(asset.isBattlefieldSupportAsset()).thenReturn(true);

        assertFalse(MekHQUnitSelectorDialog.isPersistentCampaignUnitSummary(asset));
        assertFalse(MekHQUnitSelectorDialog.isPersistentCampaignUnitSummary(null));
    }

    @Test
    void persistentUnitFilterKeepsLinkedBaseUnitRow() {
        MekSummary linkedBaseUnit = mock(MekSummary.class);
        when(linkedBaseUnit.isBattlefieldSupportAsset()).thenReturn(false);

        assertTrue(MekHQUnitSelectorDialog.isPersistentCampaignUnitSummary(linkedBaseUnit));
    }

    @Test
    void campaignActionsRejectAssetEntityEvenIfFilteringIsBypassed() {
        Entity asset = mock(Entity.class);
        when(asset.isBattlefieldSupportAsset()).thenReturn(true);

        assertFalse(MekHQUnitSelectorDialog.isCampaignAcquisitionCandidate(asset));
        assertFalse(MekHQUnitSelectorDialog.isCampaignAcquisitionCandidate((Entity) null));
    }

    @Test
    void campaignActionsRejectUnsupportedEntity() {
        Entity gunEmplacement = mock(Entity.class);
        when(gunEmplacement.getUnitType()).thenReturn(UnitType.GUN_EMPLACEMENT);
        Entity drone = mock(Entity.class);
        when(drone.hasDroneOs()).thenReturn(true);

        assertFalse(MekHQUnitSelectorDialog.isCampaignAcquisitionCandidate(gunEmplacement));
        assertFalse(MekHQUnitSelectorDialog.isCampaignAcquisitionCandidate(drone));
    }

    @Test
    void campaignActionsKeepSupportedStandardEntity() {
        Entity entity = mock(Entity.class);

        assertTrue(MekHQUnitSelectorDialog.isCampaignAcquisitionCandidate(entity));
    }

}