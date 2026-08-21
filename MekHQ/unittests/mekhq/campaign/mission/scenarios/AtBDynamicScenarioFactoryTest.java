/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.scenarios;

import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.mission.scenarios.AtBDynamicScenarioFactory.createEntityWithCrew;
import static mekhq.campaign.mission.scenarios.Scenario.T_GROUND;
import static mekhq.campaign.mission.scenarios.Scenario.T_SPACE;
import static mekhq.campaign.mission.utilities.CombatRole.CADRE;
import static mekhq.campaign.mission.utilities.CombatRole.FRONTLINE;
import static mekhq.campaign.mission.utilities.CombatRole.MANEUVER;
import static mekhq.campaign.mission.utilities.CombatRole.PATROL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.getEntityForUnitTesting;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;

import megamek.client.generator.RandomNameGenerator;
import megamek.common.Player;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.unit.Unit;
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
    public static void setUpBeforeClass() throws DOMException {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    public void setUp() {
        // Initialize the mock objects
        campaign = mockCampaign();
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.NON_BINARY_DICE_SIZE)).thenReturn(60);
        when(options.get(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS)).thenReturn(false);
        when(options.get(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL)).thenReturn(SkillLevel.VETERAN);
        when(options.get(CampaignOption.USE_TACTICS)).thenReturn(false);
        when(options.get(CampaignOption.USE_INITIATIVE_BONUS)).thenReturn(false);

        RandomSkillPreferences randomSkillPreferences = mock(RandomSkillPreferences.class);
        when(randomSkillPreferences.randomizeSkill()).thenReturn(false);
        when(randomSkillPreferences.getCommandSkillsModifier(org.mockito.ArgumentMatchers.anyInt())).thenReturn(0);

        when(campaign.getPlayer()).thenReturn(player);
        when(campaign.getGame()).thenReturn(game);

        when(campaign.getCampaignOptions()).thenReturn(options);
        lenient().when(options.get(CampaignOption.USE_IMPLANTS)).thenReturn(false);
        lenient().when(options.get(CampaignOption.USE_SENSIBLE_TACTICS)).thenReturn(false);
        when(campaign.getRandomSkillPreferences()).thenReturn(randomSkillPreferences);

        when(campaign.getGameYear()).thenReturn(3025);
    }

    @Test
    public void testCreateEntityWithCrewNoCallSigns() {
        // Auto-generated call signs disabled
        Faction faction = new Faction();
        Entity entity = getShadowHawk();

        SkillLevel skill = SkillLevel.VETERAN;
        createEntityWithCrew(faction, skill, campaign, entity, true);

        assertTrue(entity.getCrew().getNickname(0).isEmpty());
    }

    @Test
    void createEntityWithCrewPreservesConfiguredNameFaction() {
        RandomNameGenerator nameGenerator = RandomNameGenerator.getInstance();
        String previousFaction = nameGenerator.getChosenFaction();
        Faction capellanFaction = mock(Faction.class);
        when(capellanFaction.getNameGenerator()).thenReturn("CC");
        when(capellanFaction.getShortName()).thenReturn("CC");

        try {
            nameGenerator.setChosenFaction("FS");
            createEntityWithCrew(capellanFaction, SkillLevel.VETERAN, campaign, getShadowHawk(), true);

            assertEquals("FS", nameGenerator.getChosenFaction());
        } finally {
            nameGenerator.setChosenFaction(previousFaction);
        }
    }

    private static Entity getShadowHawk() {
        String unitName = "Shadow Hawk SHD-2H";
        Entity entity = getEntityForUnitTesting(unitName, false);
        assertNotNull(entity, unitName + " couldn't be found");
        return entity;
    }

    @Test
    public void testCreateEntityWithCrew_allPossible() {
        // Auto-generated call signs enabled for all
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.get(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS)).thenReturn(true);
        when(options.get(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL)).thenReturn(SkillLevel.ULTRA_GREEN);

        // Auto-generated call signs disabled
        Faction faction = new Faction();
        Entity entity = getShadowHawk();

        SkillLevel skill = SkillLevel.ULTRA_GREEN;
        createEntityWithCrew(faction, skill, campaign, entity, true);

        assertFalse(entity.getCrew().getNickname(0).isEmpty());
    }

    @Test
    public void testCreateEntityWithCrew_RegularPlus() {
        // Auto-generated call signs enabled for pilots above a certain skill
        // VETERAN will always be >= REGULAR even with randomization
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.get(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS)).thenReturn(true);
        when(options.get(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL)).thenReturn(SkillLevel.REGULAR);

        // Two mekwarriors, both alike in dignity (but not in exp or pay grade)
        Faction faction = new Faction();
        Entity entity1 = getShadowHawk();
        Entity entity2 = getShadowHawk();

        // First crew, scrub, gets no callsign
        SkillLevel skill = SkillLevel.ULTRA_GREEN;
        createEntityWithCrew(faction, skill, campaign, entity1, true);
        assertTrue(entity1.getCrew().getNickname(0).isEmpty());

        // 2nd crew, vet, gets a callsign
        skill = SkillLevel.VETERAN;
        createEntityWithCrew(faction, skill, campaign, entity2, true);
        assertFalse(entity2.getCrew().getNickname(0).isEmpty());
    }

    @Test
    public void testCreateEntityWithCrew_HeroicPlus() {
        // Auto-generated call signs enabled for pilots above a certain skill
        // VETERAN will always be < HEROIC even with randomization
        CampaignOptions options = campaign.getCampaignOptions();
        when(options.get(CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS)).thenReturn(true);
        when(options.get(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL)).thenReturn(SkillLevel.HEROIC);

        Faction faction = new Faction();
        Entity entity1 = getShadowHawk();
        Entity entity2 = getShadowHawk();
        Entity entity3 = getShadowHawk();

        // First crew, scrub, gets no callsign
        SkillLevel skill = SkillLevel.ULTRA_GREEN;
        createEntityWithCrew(faction, skill, campaign, entity1, true);
        assertTrue(entity1.getCrew().getNickname(0).isEmpty());

        // 2nd crew, vet, gets no callsign
        skill = SkillLevel.VETERAN;
        createEntityWithCrew(faction, skill, campaign, entity2, true);
        assertTrue(entity2.getCrew().getNickname(0).isEmpty());

        // 2nd crew, vet, gets a callsign
        skill = SkillLevel.LEGENDARY;
        createEntityWithCrew(faction, skill, campaign, entity3, true);
        assertFalse(entity3.getCrew().getNickname(0).isEmpty());
    }

    @Test
    void calculateEffectiveBVNoSeedForceReturnsDefaultWhenNoValidCombatTeamsExist() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        AtBDynamicScenario scenario = mockScenario(T_GROUND);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of());

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, false);

        assertEquals(10000, actual);
    }

    @Test
    void calculateEffectiveBVNoSeedForceIgnoresInvalidCombatRoles() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        LocalHangar hangar = mock(LocalHangar.class);
        AtBDynamicScenario scenario = mockScenario(T_GROUND);

        CombatTeam cadreTeam = mockCombatTeam(CADRE, mockFormationWithBV(campaign, hangar, false, 4000));
        CombatTeam patrolTeam = mockCombatTeam(PATROL, mockFormationWithBV(campaign, hangar, false, 6000));

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHangar()).thenReturn(hangar);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(cadreTeam, patrolTeam));

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, false);

        assertEquals(10000, actual);
    }

    @Test
    void calculateEffectiveBVNoSeedForceUsesFrontlineFormationBV() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        LocalHangar hangar = mock(LocalHangar.class);
        AtBDynamicScenario scenario = mockScenario(T_GROUND);

        CombatTeam frontlineTeam = mockCombatTeam(FRONTLINE, mockFormationWithBV(campaign, hangar, false, 4500));

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHangar()).thenReturn(hangar);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(frontlineTeam));

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, false);

        assertEquals(4500, actual);
    }

    @Test
    void calculateEffectiveBVNoSeedForceUsesManeuverFormationBV() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        LocalHangar hangar = mock(LocalHangar.class);
        AtBDynamicScenario scenario = mockScenario(T_GROUND);

        CombatTeam maneuverTeam = mockCombatTeam(MANEUVER, mockFormationWithBV(campaign, hangar, false, 7250));

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHangar()).thenReturn(hangar);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(maneuverTeam));

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, false);

        assertEquals(7250, actual);
    }

    @Test
    void calculateEffectiveBVNoSeedForceIgnoresZeroBVFormations() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        LocalHangar hangar = mock(LocalHangar.class);
        AtBDynamicScenario scenario = mockScenario(T_GROUND);

        CombatTeam zeroBvTeam = mockCombatTeam(FRONTLINE, mockFormationWithBV(campaign, hangar, false, 0));
        CombatTeam validTeam = mockCombatTeam(MANEUVER, mockFormationWithBV(campaign, hangar, false, 5000));

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHangar()).thenReturn(hangar);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(zeroBvTeam, validTeam));

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, false);

        assertEquals(5000, actual);
    }

    @Test
    void calculateEffectiveBVNoSeedForceUsesStandardBVWhenForced() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        LocalHangar hangar = mock(LocalHangar.class);
        AtBDynamicScenario scenario = mockScenario(T_GROUND);

        Formation formation = mock(Formation.class);
        when(formation.getUnitsAsUnits(hangar)).thenReturn(List.of());
        when(formation.getTotalBV(campaign, false)).thenReturn(3000);
        when(formation.getTotalBV(campaign, true)).thenReturn(3500);

        CombatTeam frontlineTeam = mockCombatTeam(FRONTLINE, formation);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHangar()).thenReturn(hangar);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(frontlineTeam));

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, true);

        assertEquals(3500, actual);
    }

    @Test
    void calculateEffectiveBVNoSeedForceSkipsFormationDoomedOnGround() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        LocalHangar hangar = mock(LocalHangar.class);
        AtBDynamicScenario scenario = mockScenario(T_GROUND);

        Formation doomedFormation = mockFormationWithBVAndEntity(campaign, hangar, false, 9000, true, false, false);
        CombatTeam doomedTeam = mockCombatTeam(FRONTLINE, doomedFormation);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHangar()).thenReturn(hangar);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(doomedTeam));

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, false);

        assertEquals(10000, actual);
    }

    @Test
    void calculateEffectiveBVNoSeedForceSkipsFormationDoomedInSpace() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        LocalHangar hangar = mock(LocalHangar.class);
        AtBDynamicScenario scenario = mockScenario(T_SPACE);

        Formation doomedFormation = mockFormationWithBVAndEntity(campaign, hangar, false, 9000, false, true, false);
        CombatTeam doomedTeam = mockCombatTeam(FRONTLINE, doomedFormation);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHangar()).thenReturn(hangar);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(doomedTeam));

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, false);

        assertEquals(10000, actual);
    }

    @Test
    void calculateEffectiveBVNoSeedForceSkipsFormationDoomedInLowAtmosphere() {
        Campaign campaign = mockCampaignWithNoSeedForces();
        PlayerForce playerForce = mock(PlayerForce.class);
        LocalHangar hangar = mock(LocalHangar.class);
        AtBDynamicScenario scenario = mockScenario(AtBScenario.T_ATMOSPHERE);

        Formation doomedFormation = mockFormationWithBVAndEntity(campaign, hangar, false, 9000, false, false, true);
        CombatTeam doomedTeam = mockCombatTeam(FRONTLINE, doomedFormation);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHangar()).thenReturn(hangar);
        when(playerForce.getCombatTeamsAsList(campaign)).thenReturn(List.of(doomedTeam));

        int actual = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign, false);

        assertEquals(10000, actual);
    }

    private static Campaign mockCampaignWithNoSeedForces() {
        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        lenient().when(campaignOptions.get(CampaignOption.USE_IMPLANTS)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.USE_SENSIBLE_TACTICS)).thenReturn(false);
        when(campaignOptions.get(CampaignOption.NO_SEED_FORCES)).thenReturn(true);
        when(campaignOptions.isUseStratConSinglesMode()).thenReturn(false);
        when(campaignOptions.get(CampaignOption.USE_GENERIC_BATTLE_VALUE)).thenReturn(false);

        Faction faction = mock(Faction.class);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);

        return campaign;
    }

    private static AtBDynamicScenario mockScenario(int boardType) {
        AtBDynamicScenario scenario = mock(AtBDynamicScenario.class);

        when(scenario.getBoardType()).thenReturn(boardType);
        when(scenario.getEffectivePlayerBVMultiplier()).thenReturn(0.0);
        when(scenario.getNumBots()).thenReturn(0);

        return scenario;
    }

    private static CombatTeam mockCombatTeam(CombatRole role, Formation formation) {
        CombatTeam combatTeam = mock(CombatTeam.class);

        when(combatTeam.getRole()).thenReturn(role);
        when(combatTeam.getFormation(org.mockito.ArgumentMatchers.any(Campaign.class))).thenReturn(formation);

        return combatTeam;
    }

    private static Formation mockFormationWithBV(Campaign campaign, LocalHangar hangar,
          boolean forceStandardBattleValue, int battleValue) {
        Formation formation = mock(Formation.class);

        when(formation.getUnitsAsUnits(hangar)).thenReturn(List.of());
        when(formation.getTotalBV(campaign, forceStandardBattleValue)).thenReturn(battleValue);

        return formation;
    }

    private static Formation mockFormationWithBVAndEntity(Campaign campaign, LocalHangar hangar,
          boolean forceStandardBattleValue, int battleValue, boolean doomedOnGround, boolean doomedInSpace,
          boolean doomedInAtmosphere) {
        Entity entity = mock(Entity.class);
        when(entity.getUnitType()).thenReturn(MEK);
        when(entity.doomedOnGround()).thenReturn(doomedOnGround);
        when(entity.doomedInSpace()).thenReturn(doomedInSpace);
        when(entity.doomedInAtmosphere()).thenReturn(doomedInAtmosphere);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);

        Formation formation = mock(Formation.class);
        when(formation.getUnitsAsUnits(hangar)).thenReturn(List.of(unit));
        when(formation.getTotalBV(campaign, forceStandardBattleValue)).thenReturn(battleValue);

        return formation;
    }
}
