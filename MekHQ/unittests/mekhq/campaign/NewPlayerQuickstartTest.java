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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.UnitType;
import mekhq.CampaignPreset;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorSnapshot;
import org.junit.jupiter.api.Test;

class NewPlayerQuickstartTest {
    @Test
    void loadsTheNewPlayersPresetThatShipsWithMekHQ() {
        // Full presets carry a rank system, which can only be read once the rank systems are loaded
        Ranks.initializeRankSystems();

        CampaignPreset preset = NewPlayerQuickstart.loadPreset();

        assertEquals("1 - New Players Start Here", preset.getTitle());
        assertNotNull(preset.getCampaignOptions(), "the preset has no campaign options to apply");
    }

    @Test
    void asksTheCommandGeneratorForAMekCompanyOfTheCampaignsFactionAndYear() {
        Faction mercenaries = mock(Faction.class);
        when(mercenaries.getShortName()).thenReturn("MERC");
        PlayerForce playerForce = mock(PlayerForce.class);
        when(playerForce.getFaction()).thenReturn(mercenaries);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getGameYear()).thenReturn(3151);

        CommandGenerationOptions options = NewPlayerQuickstart.createCommandOptions(campaign);
        ForceDescriptorSnapshot snapshot = options.getForceDescriptorSnapshot();

        assertSame(mercenaries, options.getSpecifiedFaction());
        assertEquals("MERC", snapshot.getFaction());
        assertEquals(3151, snapshot.getYear());
        assertEquals(NewPlayerQuickstart.COMPANY_ECHELON, snapshot.getEchelon());
        assertEquals(UnitType.MEK, snapshot.getUnitType());
        assertFalse(options.shouldApplyAugmentationRules(),
              "the quickstart overwrote the preset's implant rules");
    }
}
