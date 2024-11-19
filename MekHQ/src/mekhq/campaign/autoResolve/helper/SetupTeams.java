package mekhq.campaign.autoResolve.helper;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.*;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.autoResolve.scenarioResolver.components.AutoResolveForce;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SetupTeams {

    static public Map<Integer, List<AutoResolveForce>> setupTeams(Campaign campaign, List<Unit> units, AtBScenario scenario) {
        Map<Integer, List<AutoResolveForce>> tmpTeams = new HashMap<>();
        for (AutoResolveForce force : getAutoResolveForces(campaign, units, scenario)) {
            tmpTeams.computeIfAbsent(force.team(), k -> new ArrayList<>()).add(force);
        }
        return tmpTeams;
    }

    static private List<AutoResolveForce> getAutoResolveForces(Campaign campaign, List<Unit> units, AtBScenario scenario) {
        AtomicInteger i = new AtomicInteger(0);

        List<AutoResolveForce> forces = new ArrayList<>();
        forces.add(playerForce(i.incrementAndGet(), campaign.getPlayer().getName(), units));
        forces.addAll(getBotForces(campaign, units, scenario, i));
        return forces;
    }

    static private List<AutoResolveForce> getBotForces(Campaign campaign, List<Unit> units, AtBScenario scenario, AtomicInteger i) {
        return new ArrayList<>(scenario.getBotForces().stream().map(botForce ->
            new AutoResolveForce(botForce.getTeam(), botForce.getName(), i.get(),
                setupBotEntities(campaign, units, botForce, scenario, i.getAndIncrement()))
        ).toList());
    }

    static private AutoResolveForce playerForce(int playerID, String forceName, List<Unit> units) {
        return new AutoResolveForce(1, forceName, playerID, units.stream().map(Unit::getEntity).toList());
    }

    static private List<Entity> setupBotEntities(Campaign campaign, List<Unit> units, BotForce botForce, Scenario scenario, int forceID) {
        String forceName = botForce.getName() + "|0||%s Lance|%s||";
        var player = new Player(forceID, botForce.getName());
        player.setTeam(botForce.getTeam());
        player.isEnemyOf(campaign.getPlayer());
        var entities = new ArrayList<Entity>();
        int i = 0;
        int forceIdLance = 1;
        String lastType = "";
        final RandomCallsignGenerator RCG = RandomCallsignGenerator.getInstance();
        String lanceName = RCG.generate();
        botForce.generateRandomForces(units, campaign);
        List<Entity> entitiesSorted = botForce.getFullEntityList(campaign);
        AtBContract contract = (AtBContract) campaign.getMission(scenario.getMissionId());
        int lanceSize;

        if (botForce.getTeam() == 2) {
            lanceSize = Lance.getStdLanceSize(contract.getEnemy());
        } else {
            lanceSize = Lance.getStdLanceSize(contract.getEmployerFaction());
        }

        Comparator<Entity> comp = Comparator.comparing(((Entity e) -> Entity.getEntityMajorTypeName(e.getEntityType())));
        comp = comp.thenComparing((Entity::getRunMP), Comparator.reverseOrder());
        comp = comp.thenComparing(((Entity e) -> e.getRole().toString()));
        entitiesSorted.sort(comp);

        for (Entity entity : entitiesSorted) {
            if (null == entity) {
                continue;
            }
            entity.setForceId(forceID);
            entity.setOwner(player);
            if ((i != 0)
                && !lastType.equals(Entity.getEntityMajorTypeName(entity.getEntityType()))) {
                forceIdLance++;
                lanceName = RCG.generate();
                i = forceIdLance * lanceSize;
            }

            lastType = Entity.getEntityMajorTypeName(entity.getEntityType());
            String fName = String.format(forceName, lanceName, forceIdLance);
            entity.setForceString(fName);
            entity.setDestroyed(false);
            entities.add(entity);
            i++;
            if (entity instanceof GunEmplacement gun) {
                gun.initializeArmor(50, 0);
            }
            if (i % lanceSize == 0) {
                forceIdLance++;
                lanceName = RCG.generate();
            }
        }

        return entities;
    }

}
