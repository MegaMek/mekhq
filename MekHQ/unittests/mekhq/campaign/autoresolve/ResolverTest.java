/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.autoresolve;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.planetaryconditions.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.autoresolve.acar.SimulationOptions;
import mekhq.campaign.autoresolve.event.AutoResolveConcludedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * @author Luana Coppio
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResolverTest {

    // The order of the things in this file is atypical, but it is set in a way that makes it easy to find the only two tests
    // that exists in this file, however those tests do not run since this is an abstract class
    // instead, if you click "run test" in one of those functions it will ask which implementation class to run
    // and then run the tests in that class
    @EnabledIfEnvironmentVariable(named = "mm.test_auto_resolve_multiple_times", matches = "true")
    @RepeatedTest(1000)
    public void testAutoResolveMultipleTimes() {
        autoResolve(this::postAutoResolveAccumulator);
    }

    @Test
    public void testAutoResolveOnce() {
        autoResolve(this::assertGameFinishedWithAWinner);
    }

    // Counters for tracking success across multiple runs
    private static int totalRuns = 0;
    private static int team1 = 0;
    private static int team2 = 0;
    private static int draws = 0;

    public enum TeamArrangement {
        BALANCED,
        UNBALANCED,
        SAME_BV,
        SAME_BV_SAME_SKILL
    }

    static double lowerBoundTeam1() {
        return 0.35;
    }
    static double upperBoundTeam1() {
        return 0.50;
    }
    static double lowerBoundTeam2() {
        return 0.35;
    }
    static double upperBoundTeam2() {
        return 0.50;
    }
    static double lowerBoundDraw() {
        return 0.10;
    }
    static double upperBoundDraw() {
        return 0.20;
    }

    static TeamArrangement getTeamArrangement() {
        return TeamArrangement.BALANCED;
    }

    @BeforeAll
    public static void setupClass() throws IOException {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
        SkillType.initializeTypes();
        Systems.setInstance(Systems.loadDefault());
    }

    @Mock
    private BotForce botForce;

    @InjectMocks
    private Resolver resolver;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    static void resetTrackers() {
        totalRuns = 0;
        team1 = 0;
        team2 = 0;
        draws = 0;
    }

    AtBScenario createScenario(Campaign campaign) {
        var contract = mock(AtBContract.class);
        when(contract.getEnemySkill()).thenReturn(SkillLevel.REGULAR);
        when(contract.getAllySkill()).thenReturn(SkillLevel.REGULAR);

        var scenario = mock(AtBDynamicScenario.class);
        when(scenario.getContract(any())).thenReturn(contract);

        // Board setup
        when(scenario.getBoardType()).thenReturn(0);
        when(scenario.getTerrainType()).thenReturn("Woods");
        when(scenario.getMapX()).thenReturn(30);
        when(scenario.getMapY()).thenReturn(30);
        when(scenario.getMap()).thenReturn("Woods-deep");

        // Planetary conditions setup
        when(scenario.getLight()).thenReturn(Light.DAY);
        when(scenario.getWeather()).thenReturn(Weather.CLEAR);
        when(scenario.getWind()).thenReturn(Wind.CALM);
        when(scenario.getFog()).thenReturn(Fog.FOG_NONE);
        when(scenario.getEMI()).thenReturn(EMI.EMI_NONE);
        when(scenario.getBlowingSand()).thenReturn(BlowingSand.BLOWING_SAND_NONE);
        when(scenario.getAtmosphere()).thenReturn(Atmosphere.STANDARD);
        when(scenario.getGravity()).thenReturn(1.0f);
        when(scenario.getModifiedTemperature()).thenReturn(20);
        when(scenario.getBlowingSand()).thenReturn(BlowingSand.BLOWING_SAND_NONE);

        // Lance setup
        when(scenario.getCombatRole()).thenReturn(CombatRole.FIGHTING);
        when(scenario.getId()).thenReturn(11);

        // Bots setup
        when(scenario.getBotForce(anyInt())).thenReturn(botForce);
        when(scenario.getNumBots()).thenReturn(1);

        for (var force : campaign.getAllForces()) {
            force.setScenarioId(11, campaign);
        }

        return scenario;
    }

    Campaign createCampaign() {
        var campaign = new Campaign();

        var reputationController = mock(ReputationController.class);
        when(reputationController.getAverageSkillLevel()).thenReturn(SkillLevel.REGULAR);


        campaign.setReputation(reputationController);

        var force = new Force("Heroes");
        campaign.addForce(force, campaign.getForce(0));

        return campaign;
    }

    static final AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(new RandomSkillPreferences());

    Person randomPerson(Crew crew, Campaign campaign) {
        var person = new Person(campaign);
        person.setPrimaryRole(campaign, PersonnelRole.MEKWARRIOR);
        person.addSkill(SkillType.S_GUN_MEK, crew.getGunnery(), 0);
        person.addSkill(SkillType.S_PILOT_MEK, crew.getPiloting(), 0);
        return person;
    }

    private Person randomPersonForPlayer(Crew crew, Campaign campaign) {
        var person = randomPerson(crew, campaign);
        campaign.importPerson(person);
        return person;
    }

    private static final String[][] unitNames = {
        {   // Team 1/2: Team 1 and Team 2
            "Enforcer III ENF-6M",
            "Shadow Hawk SHD-5D",
            "Hatchetman HCT-6D",
            "Osiris OSR-5D"
        },
        {   // Team 1: Unbalanced Team 1
            "Enforcer III ENF-6M",
            "Shadow Hawk SHD-5D",
            "Hatchetman HCT-6D",
            "Osiris OSR-5D",
            "Wasp WSP-1"
        },
        {   // Team 1: Same BV Team 1 and Team 2 with different meks and crews
            "Shadow Hawk SHD-7CS",
            "Vulcan VT-7T",
            "Crusader CRD-7M",
            "Rifleman RFL-9T",
        },
        {   // Team 2: Same BV Team 1 and Team 2 with different meks and crews
            "Hammerhands HMH-5D",
            "Spartan SPT-N1",
            "Rifleman RFL-9T",
            "Champion CHP-3P"
        },
        {   // Team 1: Same BV and Skills
            "Griffin GRF-1E 'Sparky'",
            "Flashman FLS-7K",
            "Stalker STK-4N",
            "Victor VTR-9S",
        },
        {    // Team 2: Same BV and Skills
            "Victor VTR-9S",
            "Victor VTR-9B",
            "Zeus ZEU-6A",
            "Crockett CRK-5003-0",
        }
    };

    private static Crew createCrew(int gunnery, int piloting) {
        return new Crew(CrewType.SINGLE, "John Doe", 1, gunnery, piloting, Gender.FEMALE, false, null);
    }

    private final Crew[][] crews = {
        {   // Team 1 for same BV
            createCrew(4, 5),
            createCrew(4, 6),
            createCrew(3, 5),
            createCrew(3, 4),
        },
        {   // Team 2 for same BV
            createCrew(3, 4),
            createCrew(4, 4),
            createCrew(4, 6),
            createCrew(5, 6),
        }
    };


    List<Entity> getEntities(TeamArrangement teamArrangement) {
        var entities = new ArrayList<Entity>();

        var unitFullNames = switch (teamArrangement) {
            case BALANCED, UNBALANCED -> unitNames[0];
            case SAME_BV -> unitNames[3];
            case SAME_BV_SAME_SKILL -> unitNames[5];
        };

        for (var i = 0; i < unitFullNames.length; i++) {
            var fullName = unitFullNames[i];
            var entity = MekSummary.loadEntity(fullName);
            assert entity != null;

            var crew = switch (teamArrangement) {
                case BALANCED, UNBALANCED, SAME_BV_SAME_SKILL -> createCrew(4, 5);
                case SAME_BV -> crews[1][i % crews[1].length];
            };

            entity.setCrew(crew);
            entity.calculateBattleValue();
            entities.add(entity);
        }
        return entities;
    }

    List<Unit> getUnits(Campaign campaign, TeamArrangement teamArrangement) {
        var units = new ArrayList<Unit>();

        var unitFullNames = switch (teamArrangement) {
            case BALANCED -> unitNames[0];
            case UNBALANCED -> unitNames[1];
            case SAME_BV -> unitNames[2];
            case SAME_BV_SAME_SKILL -> unitNames[4];
        };

        for (var i = 0; i < unitFullNames.length; i++) {
            var fullName = unitFullNames[i];
            var unit = new Unit();
            unit.setCampaign(campaign);
            var entity = MekSummary.loadEntity(fullName);
            assert entity != null;
            entity.setOwner(campaign.getPlayer());
            entity.setForceString("Valkiries|1||Third Support Company|31||9th Scout Lance|544||");
            entity.calculateBattleValue();
            unit.setEntity(entity);
            unit.setId(UUID.randomUUID());

            var crew = switch (teamArrangement) {
                case BALANCED, UNBALANCED, SAME_BV_SAME_SKILL-> createCrew(4, 5);
                case SAME_BV -> crews[0][i % crews[0].length];
            };

            unit.addPilotOrSoldier(randomPersonForPlayer(crew, campaign));
            units.add(unit);
            entity.setCrew(crew);
        }
        return units;
    }

    void autoResolve(Consumer<AutoResolveConcludedEvent> autoResolveConcludedEvent) {
        var teamArrangement = getTeamArrangement();

        var campaign = createCampaign();
        var units = getUnits(campaign, teamArrangement);
        var scenario = createScenario(campaign);
        var entities = getEntities(teamArrangement);

        when(botForce.getCamouflage()).thenReturn(Camouflage.of(PlayerColour.MAROON));
        when(botForce.getColour()).thenReturn(PlayerColour.MAROON);
        when(botForce.getName()).thenReturn("OpFor");
        when(botForce.getTeam()).thenReturn(2);
        when(botForce.getFullEntityList(any())).thenReturn(entities);

        resolver = new Resolver(campaign, units, scenario, SimulationOptions.empty());
        autoResolveConcludedEvent.accept(resolver.resolveSimulation());
    }

    private void assertGameFinishedWithAWinner(AutoResolveConcludedEvent event) {
        var victoryTeam = event.getVictoryResult().getWinningTeam();
        assertTrue((0 <= victoryTeam) && (victoryTeam <= 2), "Victory team is not 1 or 2");
    }

    private void postAutoResolveAccumulator(AutoResolveConcludedEvent event) {
        totalRuns++;
        var victoryTeam = event.getVictoryResult().getWinningTeam();
        switch (victoryTeam) {
            case 1 -> team1++;
            case 2 -> team2++;
            default -> draws++;
        }
        // Each individual run asserts that event.controlledScenario() is valid.
        // If you want a per-run assertion, you could do so here. But since we
        // are aggregating results, it might be better to do final checks later.
    }

    @AfterAll
    public static void afterAllTests() {
        if (totalRuns == 0) {
            return;
        }
        double team1Rate = (double) team1 / totalRuns;
        double team2Rate = (double) team2 / totalRuns;
        double drawRate = (double) draws / totalRuns;

        System.out.println("Ran " + totalRuns + " times. \n\tTeam 1: " + team1 + " (" + team1Rate*100 + "%) " +
            "\n\tTeam 2: " + team2 + " (" + team2Rate*100 + "%) \n\tDraws: " + draws + " (" + drawRate*100 + "%)");

        assertAll("Distribution check",
            () -> assertTrue(team1Rate >= lowerBoundTeam1(),
                "Team 1 rate (" + team1Rate + ") is below lower bound " + lowerBoundTeam1() + ". Deviation: " + (lowerBoundTeam1() - team1Rate)),
            () -> assertTrue(team1Rate <= upperBoundTeam1(),
                "Team 1 rate (" + team1Rate + ") is above upper bound " + upperBoundTeam1() + ". Deviation: " + (team1Rate - upperBoundTeam1())),

            () -> assertTrue(team2Rate >= lowerBoundTeam2(),
                "Team 2 rate (" + team2Rate + ") is below lower bound " + lowerBoundTeam2() + ". Deviation: " + (lowerBoundTeam2() - team2Rate)),
            () -> assertTrue(team2Rate <= upperBoundTeam2(),
                "Team 2 rate (" + team2Rate + ") is above upper bound " + upperBoundTeam2() + ". Deviation: " + (team2Rate - upperBoundTeam2())),

            () -> assertTrue(drawRate >= lowerBoundDraw(),
                "Draw rate (" + drawRate + ") is below lower bound " + lowerBoundDraw() + ". Deviation: " + (lowerBoundDraw() - drawRate)),
            () -> assertTrue(drawRate <= upperBoundDraw(),
                "Draw rate (" + drawRate + ") is above upper bound " + upperBoundDraw() + ". Deviation: " + (drawRate - upperBoundDraw()))
        );
        resetTrackers();
    }
}
