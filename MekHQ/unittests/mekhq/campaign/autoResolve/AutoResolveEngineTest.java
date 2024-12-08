package mekhq.campaign.autoResolve;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.options.StaticGameOptions;
import megamek.common.planetaryconditions.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveConcludedEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.generator.DefaultSkillGenerator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

public class AutoResolveEngineTest {

    @BeforeAll
    public static void setup() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
        SkillType.initializeTypes();
        SpecialAbility.initializeSPA();
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ignored) {}
    }

    private BotForce createBotForce() {
        var entities = getEntities();
        var botForce = mock(BotForce.class);
        when(botForce.getFullEntityList(any())).thenReturn(entities);
        when(botForce.getCamouflage()).thenReturn(Camouflage.of(PlayerColour.BLUE));
        when(botForce.getColour()).thenReturn(PlayerColour.BLUE);
        return botForce;
    }

    private AtBScenario createScenario() {

        var contract = mock(AtBContract.class);
        when(contract.getEnemySkill()).thenReturn(SkillLevel.REGULAR);
        when(contract.getAllySkill()).thenReturn(SkillLevel.REGULAR);

        var scenario = mock(AtBDynamicScenario.class);
        when(scenario.getContract(any())).thenReturn(contract);

        // Board setup
        when(scenario.getBoardType()).thenReturn(0);
        when(scenario.getTerrainType()).thenReturn("Tundra");
        when(scenario.getMapX()).thenReturn(100);
        when(scenario.getMapY()).thenReturn(100);

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
        when(scenario.getLanceRole()).thenReturn(AtBLanceRole.FIGHTING);

        // Bots setup
        var botForce = createBotForce();
        when(scenario.getBotForce(anyInt())).thenReturn(botForce);
        when(scenario.getNumBots()).thenReturn(1);

        return scenario;
    }

    private Campaign createCampaign() {
        var campaign = new Campaign();
        var reputationController = mock(ReputationController.class);
        when(reputationController.getAverageSkillLevel()).thenReturn(SkillLevel.REGULAR);
        campaign.setReputation(reputationController);

        return campaign;
    }

    private static final AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(new RandomSkillPreferences());

    private Person randomPerson(Campaign campaign) {
        var person = new Person(campaign);
        person.setPrimaryRole(campaign, PersonnelRole.MEKWARRIOR);
        skillGenerator.generateSkills(campaign, person, SkillType.EXP_REGULAR);
        return person;
    }

    private final String[] unitNames = {
        "Enforcer III ENF-6M",
        "Shadow Hawk SHD-5D",
        "Hatchetman HCT-6D",
        "Osiris OSR-5D"
    };

    private Crew createCrew() {
        return new Crew(CrewType.SINGLE, "John Doe", 1, 4, 5, Gender.FEMALE, false, null);
    }

    private List<Entity> getEntities() {
        var entities = new ArrayList<Entity>();
        for (var fullName : unitNames) {
            var entity = MekSummary.loadEntity(fullName);
            assert entity != null;
            entity.setCrew(createCrew());
            entity.setForceString("OpFor|Alpha Lance");
        }
        return entities;
    }

    private List<Unit> getUnits(Campaign campaign) {
        var units = new ArrayList<Unit>();
        for (var fullName : unitNames) {
            var unit = new Unit();
            unit.setCampaign(campaign);
            var entity = MekSummary.loadEntity(fullName);
            assert entity != null;
            entity.setOwner(campaign.getPlayer());
            entity.setForceString("Fighters|Attack Lance");
            unit.setEntity(entity);
            unit.setId(UUID.randomUUID());
            unit.addPilotOrSoldier(randomPerson(campaign));
            units.add(unit);
        }
        return units;
    }

    @Test
    void testAutoResolve() {
        var campaign = createCampaign();
        var units = getUnits(campaign);
        var scenario = createScenario();
        AutoResolveEngine autoResolveEngine = new AutoResolveEngine(AutoResolveMethod.ABSTRACT_COMBAT);
        autoResolveEngine.resolveBattle(
            campaign,
            units,
            scenario,
            StaticGameOptions.empty(),
            this::assertAutoResolveConcludedEvent
        );
    }

    private void assertAutoResolveConcludedEvent(AutoResolveConcludedEvent event) {
        assertTrue(event.controlledScenario());
    }
}
