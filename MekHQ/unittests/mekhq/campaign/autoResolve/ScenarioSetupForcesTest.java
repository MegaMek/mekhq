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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.autoResolve;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.getEntityForUnitTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.client.ui.util.PlayerColour;
import megamek.common.autoResolve.acar.SimulationContext;
import megamek.common.autoResolve.acar.SimulationOptions;
import megamek.common.autoResolve.converter.FlattenForces;
import megamek.common.board.Board;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MapSettings;
import megamek.common.util.BoardUtilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.camOpsReputation.ReputationController;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import testUtilities.MHQTestUtilities;

/**
 * Tests for {@link ScenarioSetupForces}, specifically verifying that player entities
 * are correctly added to the simulation regardless of their forceString state.
 * Regression tests for <a href="https://github.com/MegaMek/mekhq/issues/8385">#8385</a>.
 */
class ScenarioSetupForcesTest {

    private static final Board BOARD = BoardUtilities.generateRandom(MapSettings.getInstance());

    @Mock
    private BotForce botForce;

    @BeforeAll
    static void setupClass() throws Exception {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
        SkillType.initializeTypes();
        Systems.setInstance(Systems.loadDefault());
    }

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Entities whose forceString starts with an empty root force name (e.g. "|1||Company|29||...")
     * must still produce player formations in the simulation. This is the primary regression
     * case for #8385 — the campaign root force has no name, causing Forces.verifyForceName to
     * reject the top-level force, which silently dropped all player entities.
     */
    @Test
    void testEntitiesWithEmptyRootForceNameProduceFormations() {
        var result = runSimulation("|1||Company|29||Lance|544||");
        assertPlayerFormationsExist(result);
    }

    /**
     * Entities with a completely blank forceString (no formation assignment) must still
     * be added to the simulation with a default force.
     */
    @Test
    void testEntitiesWithBlankForceStringProduceFormations() {
        var result = runSimulation("");
        assertPlayerFormationsExist(result);
    }

    /**
     * Entities with a normal, valid forceString must continue to work as before.
     */
    @Test
    void testEntitiesWithValidForceStringProduceFormations() {
        var result = runSimulation("Company|1||Lance|18||");
        assertPlayerFormationsExist(result);
    }

    private void assertPlayerFormationsExist(SimulationContext context) {
        var playerFormations = context.getActiveFormations(context.getPlayer(0));
        assertFalse(playerFormations.isEmpty(),
              "Player should have at least one formation in the simulation");

        var botFormations = context.getActiveFormations().stream()
              .filter(f -> f.getOwnerId() != 0)
              .toList();
        assertFalse(botFormations.isEmpty(),
              "Bot should have at least one formation in the simulation");

        assertTrue(context.getPlayersList().size() >= 2,
              "There should be at least 2 players (player + bot)");
    }

    private SimulationContext runSimulation(String playerForceString) {
        var campaign = createCampaign();
        var units = createUnits(campaign, playerForceString);
        var scenario = createScenario(campaign);
        var botEntities = createBotEntities();

        when(botForce.getCamouflage()).thenReturn(Camouflage.of(PlayerColour.MAROON));
        when(botForce.getColour()).thenReturn(PlayerColour.MAROON);
        when(botForce.getName()).thenReturn("OpFor");
        when(botForce.getTeam()).thenReturn(2);
        when(botForce.getFullEntityList(any())).thenReturn(botEntities);

        var setupForces = new StratConSetupForces(campaign, units, scenario, new FlattenForces());
        return new SimulationContext(SimulationOptions.empty(), setupForces, BOARD, new PlanetaryConditions());
    }

    private Campaign createCampaign() {
        var campaign = MHQTestUtilities.getTestCampaign();
        campaign.setName("Test Player");
        var reputationController = mock(ReputationController.class);
        when(reputationController.getAverageSkillLevel()).thenReturn(SkillLevel.REGULAR);
        campaign.setReputation(reputationController);
        campaign.addFormation(new Formation("Heroes"), campaign.getFormation(0));
        return campaign;
    }

    private AtBDynamicScenario createScenario(Campaign campaign) {
        var contract = mock(AtBContract.class);
        when(contract.getEnemySkill()).thenReturn(SkillLevel.REGULAR);
        when(contract.getAllySkill()).thenReturn(SkillLevel.REGULAR);

        var scenario = mock(AtBDynamicScenario.class);
        when(scenario.getContract(any())).thenReturn(contract);
        when(scenario.getCombatRole()).thenReturn(CombatRole.MANEUVER);
        when(scenario.getId()).thenReturn(11);
        when(scenario.getBotForce(anyInt())).thenReturn(botForce);
        when(scenario.getNumBots()).thenReturn(1);

        for (var force : campaign.getAllFormations()) {
            force.setScenarioId(11, campaign);
        }

        return scenario;
    }

    // CHECKSTYLE IGNORE ForbiddenWords FOR 2 LINES
    private static final String[] UNIT_NAMES = { "Enforcer III ENF-6M", "Shadow Hawk SHD-5D" };
    private static final String[] BOT_UNIT_NAMES = { "Hatchetman HCT-6D", "Osiris OSR-5D" };

    private List<Unit> createUnits(Campaign campaign, String forceString) {
        var units = new ArrayList<Unit>();
        var crew = new Crew(CrewType.SINGLE, "Test Pilot", 1, 4, 5, Gender.FEMALE, false, null);

        for (String name : UNIT_NAMES) {
            Entity entity = getEntityForUnitTesting(name, false);
            if (entity == null) {
                throw new RuntimeException("Could not load entity: " + name);
            }

            var unit = new Unit();
            unit.setCampaign(campaign);
            entity.setOwner(campaign.getPlayer());
            entity.setForceString(forceString);
            entity.setCrew(crew);
            entity.calculateBattleValue();
            unit.setEntity(entity);
            unit.setId(UUID.randomUUID());

            var person = new Person(campaign);
            person.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.MEKWARRIOR);
            person.addSkill(SkillType.S_GUN_MEK, 4, 0);
            person.addSkill(SkillType.S_PILOT_MEK, 5, 0);
            campaign.importPerson(person);
            unit.addPilotOrSoldier(person);

            units.add(unit);
        }
        return units;
    }

    private List<Entity> createBotEntities() {
        var entities = new ArrayList<Entity>();
        var crew = new Crew(CrewType.SINGLE, "Bot Pilot", 1, 4, 5, Gender.FEMALE, false, null);

        for (String name : BOT_UNIT_NAMES) {
            Entity entity = getEntityForUnitTesting(name, false);
            if (entity == null) {
                throw new RuntimeException("Could not load entity: " + name);
            }
            entity.setCrew(crew);
            entity.calculateBattleValue();
            entities.add(entity);
        }
        return entities;
    }
}
