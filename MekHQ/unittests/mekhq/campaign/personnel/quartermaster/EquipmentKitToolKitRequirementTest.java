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

package mekhq.campaign.personnel.quartermaster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link EquipmentKitCatalog#isToolKitRequired(CampaignOptions, Unit)}: tool kits are only required when the
 * "Techs Need a Tool Kit" option is enabled, and self-maintaining conventional infantry are always exempt.
 *
 * @author Illiani
 * @since 0.51.01
 */
class EquipmentKitToolKitRequirementTest {

    @Test
    void noToolKitRequiredWhenOptionDisabled() {
        CampaignOptions campaignOptions = optionsWithToolKitsRequired(false);

        assertFalse(EquipmentKitCatalog.isToolKitRequired(campaignOptions, unit(false)));
        assertFalse(EquipmentKitCatalog.isToolKitRequired(campaignOptions, unit(true)));
        assertFalse(EquipmentKitCatalog.isToolKitRequired(campaignOptions, null));
    }

    @Test
    void toolKitRequiredForTechMaintainedUnitsWhenOptionEnabled() {
        CampaignOptions campaignOptions = optionsWithToolKitsRequired(true);

        assertTrue(EquipmentKitCatalog.isToolKitRequired(campaignOptions, unit(false)));
    }

    @Test
    void toolKitRequiredForUnattachedPartsWhenOptionEnabled() {
        CampaignOptions campaignOptions = optionsWithToolKitsRequired(true);

        assertTrue(EquipmentKitCatalog.isToolKitRequired(campaignOptions, null));
    }

    @Test
    void selfMaintainedInfantryExemptWhenOptionEnabled() {
        CampaignOptions campaignOptions = optionsWithToolKitsRequired(true);

        assertFalse(EquipmentKitCatalog.isToolKitRequired(campaignOptions, unit(true)));
    }

    private static CampaignOptions optionsWithToolKitsRequired(boolean isTechsNeedToolKit) {
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.TECHS_NEED_TOOL_KIT)).thenReturn(isTechsNeedToolKit);
        return campaignOptions;
    }

    private static Unit unit(boolean isSelfMaintainedInfantry) {
        Unit unit = mock(Unit.class);
        when(unit.isSelfMaintainedInfantry()).thenReturn(isSelfMaintainedInfantry);
        return unit;
    }
}
