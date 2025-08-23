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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.getEntityForUnitTesting;

import java.io.IOException;

import megamek.common.units.Entity;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.Player;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.universe.Faction;
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
        EquipmentType.initializeTypes();
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
        Faction faction = new Faction();
        Entity entity = getShadowHawk();

        SkillLevel skill = SkillLevel.VETERAN;
        createEntityWithCrew(faction, skill, campaign, entity);

        assertTrue(entity.getCrew().getNickname(0).isEmpty());
    }

    private static Entity getShadowHawk() {
        String unitName = "Shadow Hawk SHD-2H";
        Entity entity = getEntityForUnitTesting(unitName, false);
        assertNotNull(entity, unitName + " couldn't be found");
        return entity;
    }

    @Test
    public void testCreateEntityWithCrew_allPossible() {
        // Auto-generated callsigns enabled for all
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.isAutoGenerateOpForCallsigns()).thenReturn(true);
        when(options.getMinimumCallsignSkillLevel()).thenReturn(SkillLevel.ULTRA_GREEN);

        // Auto-generated callsigns disabled
        Faction faction = new Faction();
        Entity entity = getShadowHawk();

        SkillLevel skill = SkillLevel.ULTRA_GREEN;
        createEntityWithCrew(faction, skill, campaign, entity);

        assertFalse(entity.getCrew().getNickname(0).isEmpty());
    }

    @Test
    public void testCreateEntityWithCrew_RegularPlus() {
        // Auto-generated callsigns enabled for pilots above a certain skill
        // VETERAN will always be >= REGULAR even with randomization
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.isAutoGenerateOpForCallsigns()).thenReturn(true);
        when(options.getMinimumCallsignSkillLevel()).thenReturn(SkillLevel.REGULAR);

        // Two mekwarriors, both alike in dignity (but not in exp or pay grade)
        Faction faction = new Faction();
        Entity entity1 = getShadowHawk();
        Entity entity2 = getShadowHawk();

        // First crew, scrub, gets no callsign
        SkillLevel skill = SkillLevel.ULTRA_GREEN;
        createEntityWithCrew(faction, skill, campaign, entity1);
        assertTrue(entity1.getCrew().getNickname(0).isEmpty());

        // 2nd crew, vet, gets a callsign
        skill = SkillLevel.VETERAN;
        createEntityWithCrew(faction, skill, campaign, entity2);
        assertFalse(entity2.getCrew().getNickname(0).isEmpty());
    }

    @Test
    public void testCreateEntityWithCrew_HeroicPlus() {
        // Auto-generated callsigns enabled for pilots above a certain skill
        // VETERAN will always be < HEROIC even with randomization
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.isAutoGenerateOpForCallsigns()).thenReturn(true);
        when(options.getMinimumCallsignSkillLevel()).thenReturn(SkillLevel.HEROIC);

        Faction faction = new Faction();
        Entity entity1 = getShadowHawk();
        Entity entity2 = getShadowHawk();
        Entity entity3 = getShadowHawk();

        // First crew, scrub, gets no callsign
        SkillLevel skill = SkillLevel.ULTRA_GREEN;
        createEntityWithCrew(faction, skill, campaign, entity1);
        assertTrue(entity1.getCrew().getNickname(0).isEmpty());

        // 2nd crew, vet, gets no callsign
        skill = SkillLevel.VETERAN;
        createEntityWithCrew(faction, skill, campaign, entity2);
        assertTrue(entity2.getCrew().getNickname(0).isEmpty());

        // 2nd crew, vet, gets a callsign
        skill = SkillLevel.LEGENDARY;
        createEntityWithCrew(faction, skill, campaign, entity3);
        assertFalse(entity3.getCrew().getNickname(0).isEmpty());
    }
}
