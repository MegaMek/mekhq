/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static mekhq.campaign.mission.AtBDynamicScenarioFactory.createEntityWithCrew;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.Player;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.w3c.dom.DOMException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AtBDynamicScenarioFactoryTest {
    Campaign campaign;
    Player player = new Player(1, "Test");
    Game game = new Game();

    @BeforeAll
    public static void setUpBeforeClass() throws IOException, DOMException {
    }

    @BeforeEach
    public void setUp() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.getNonBinaryDiceSize()).thenReturn(60);
        when(options.isAutoGenerateOpForCallsigns()).thenReturn(false);
        when(options.getMinimumCallsignSkillLevel()).thenReturn(SkillLevel.VETERAN);
        when(options.isUseTactics()).thenReturn(false);
        when(options.isUseInitiativeBonus()).thenReturn(false);

        when(campaign.getPlayer()).thenReturn(player);
        when(campaign.getGame()).thenReturn(game);

        when(campaign.getCampaignOptions()).thenReturn(options);

        when(campaign.getGameYear()).thenReturn(3025);
    }

    @Test
    public void testCreateEntityWithCrewNoCallsigns() {
        // Auto-generated callsigns disabled
        String factionCode = "LC";
        String unitName = "Shadow Hawk SHD-2H";
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        SkillLevel skill = SkillLevel.VETERAN;
        Entity entity = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
        assertTrue(entity.getCrew().getNickname(0).isEmpty());
    }

    @Test
    public void testCreateEntityWithCrewCallsigns() {
        // Auto-generated callsigns enabled for all
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.isAutoGenerateOpForCallsigns()).thenReturn(true);
        when(options.getMinimumCallsignSkillLevel()).thenReturn(SkillLevel.ULTRA_GREEN);

        String factionCode = "LC";
        String unitName = "Shadow Hawk SHD-2H";
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        SkillLevel skill = SkillLevel.ULTRA_GREEN;

        Entity entity = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
        assertFalse(entity.getCrew().getNickname(0).isEmpty());
    }

    @Test
    public void testCreateEntityWithCrewCutoffForCallsigns() {
        // Auto-generated callsigns enabled for pilots above a certain skill
        // VETERAN will always be >= REGULAR even with randomization
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.isAutoGenerateOpForCallsigns()).thenReturn(true);
        when(options.getMinimumCallsignSkillLevel()).thenReturn(SkillLevel.REGULAR);

        // Two mekwarriors, both alike in dignity (but not in exp or pay grade)
        String factionCode = "LC";
        String unitName = "Shadow Hawk SHD-2H";
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        SkillLevel skill = SkillLevel.ULTRA_GREEN;

        // First crew, scrub, gets no callsign
        Entity entity = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
        assertTrue(entity.getCrew().getNickname(0).isEmpty());

        // 2nd crew, vet, gets a callsign
        skill = SkillLevel.VETERAN;

        Entity entity2 = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
        assertFalse(entity2.getCrew().getNickname(0).isEmpty());
    }

    @Test
    public void testCreateEntityWithCrewHigherCutoffForCallsigns() {
        // Auto-generated callsigns enabled for pilots above a certain skill
        // VETERAN will always be < HEROIC even with randomization
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.isAutoGenerateOpForCallsigns()).thenReturn(true);
        when(options.getMinimumCallsignSkillLevel()).thenReturn(SkillLevel.HEROIC);

        String factionCode = "LC";
        String unitName = "Shadow Hawk SHD-2H";
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        SkillLevel skill = SkillLevel.ULTRA_GREEN;

        // First crew, scrub, gets no callsign
        Entity entity = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
        assertTrue(entity.getCrew().getNickname(0).isEmpty());

        // 2nd crew, regular, gets no callsign
        skill = SkillLevel.REGULAR;

        Entity entity2 = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
        assertTrue(entity2.getCrew().getNickname(0).isEmpty());

        // 3rd crew, vet, gets no callsign
        skill = SkillLevel.VETERAN;

        Entity entity3 = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
        assertTrue(entity3.getCrew().getNickname(0).isEmpty());

        // 4th crew, HEROIC, gets a callsign
        skill = SkillLevel.LEGENDARY;

        Entity entity4 = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
        assertFalse(entity4.getCrew().getNickname(0).isEmpty());
    }

}
