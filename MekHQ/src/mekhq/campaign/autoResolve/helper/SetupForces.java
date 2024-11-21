package mekhq.campaign.autoResolve.helper;

import io.sentry.Sentry;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.common.*;
import megamek.common.force.Forces;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFFormationConverter;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SetupForces {
    private static final MMLogger logger = MMLogger.create(SetupForces.class);

    private final Campaign campaign;
    private final List<Unit> units;
    private final AtBScenario scenario;
    private final AutoResolveGame game;
    private final List<Force> forces = new ArrayList<>();

    public SetupForces(Campaign campaign, List<Unit> units, AtBScenario scenario, AutoResolveGame game) {
        this.campaign = campaign;
        this.units = units;
        this.scenario = scenario;
        this.game = game;
    }

    // from ATB Game thread
    public void createForcesOnGame() {
        setupMapSettings();
        getPlanetaryConditions();
        setupPlayerForces(campaign, units, scenario);
        var localBots = new HashMap<String, BotClient>();
        /* Add bots */
        for (int i = 0; i < scenario.getNumBots(); i++) {
            BotForce bf = scenario.getBotForce(i);
            String name = bf.getName();
            if (localBots.containsKey(name)) {
                int append = 2;
                while (localBots.containsKey(name + append)) {
                    append++;
                }
                name += append;
            }
            Princess botClient = new Princess(name, "", 0);
            botClient.setLocalPlayerNumber(game.getNoOfPlayers() + 1);
            botClient.setBehaviorSettings(bf.getBehaviorSettings());
            localBots.put(name, botClient);
            game.addPlayer(botClient.getLocalPlayerNumber(), botClient.getLocalPlayer());
            configureBot(botClient.getLocalPlayer(), bf);
        }

        // convert forces:
        for(var force : game.getForces().getAllForces()) {
            var entitiesToRemoveFromForce = game.getForces().getForce(force.getId()).getEntities();
            var formation = new SBFFormationConverter(force, game).convert();
            if (formation == null) {
                System.out.println("Error, formation is null for force " + force.getName());
            } else {
                game.addUnit(formation);
                game.getForces().addEntity(formation, force.getId());
            }
            entitiesToRemoveFromForce.forEach(id -> game.getForces().removeEntityFromForces(id));
        }
    }

    private void setupPlayerForces(Campaign campaign, List<Unit> units, AtBScenario scenario) {
        var player = campaign.getPlayer();
        game.addPlayer(player.getId(), player);
        player.setCamouflage(campaign.getCamouflage().clone());
        player.setColour(campaign.getColour());
        player.setStartingPos(scenario.getStartingPos());
        player.setStartOffset(scenario.getStartOffset());
        player.setStartWidth(scenario.getStartWidth());
        player.setStartingAnyNWx(scenario.getStartingAnyNWx());
        player.setStartingAnyNWy(scenario.getStartingAnyNWy());
        player.setStartingAnySEx(scenario.getStartingAnySEx());
        player.setStartingAnySEy(scenario.getStartingAnySEy());
        player.setTeam(1);
        player.setNbrMFActive(scenario.getNumPlayerMinefields(Minefield.TYPE_ACTIVE));
        player.setNbrMFConventional(scenario.getNumPlayerMinefields(Minefield.TYPE_CONVENTIONAL));
        player.setNbrMFInferno(scenario.getNumPlayerMinefields(Minefield.TYPE_INFERNO));
        player.setNbrMFVibra(scenario.getNumPlayerMinefields(Minefield.TYPE_VIBRABOMB));

        var entities = new ArrayList<Entity>();
        /*
         * If the player is making a combat drop (either required by scenario
         * or player chose to deploy a DropShip), do not use deployment
         * delay for slower scout units.
         */
        boolean useDropship = false;
        if (scenario.getLanceRole().isScouting()) {
            for (Entity en : scenario.getAlliesPlayer()) {
                if (en.getUnitType() == UnitType.DROPSHIP) {
                    useDropship = true;
                    break;
                }
            }
            if (!useDropship) {
                for (Unit unit : units) {
                    if (unit.getEntity().getUnitType() == UnitType.DROPSHIP) {
                        useDropship = true;
                        break;
                    }
                }
            }
        }

        for (Unit unit : units) {
            // Get the Entity
            Entity entity = unit.getEntity();
            // Set the TempID for auto reporting
            entity.setExternalIdAsString(unit.getId().toString());
            // Set the owner
            entity.setOwner(player);

            // If this unit is a spacecraft, set the crew size and marine size values
            if (entity.isLargeCraft() || (entity.getUnitType() == UnitType.SMALL_CRAFT)) {
                entity.setNCrew(unit.getActiveCrew().size());
                entity.setNMarines(unit.getMarineCount());
            }
            // Calculate deployment round
            int deploymentRound = entity.getDeployRound();
            if (!(scenario instanceof AtBDynamicScenario)) {
                int speed = entity.getWalkMP();
                if (entity.getJumpMP() > 0) {
                    if (entity instanceof Infantry) {
                        speed = entity.getJumpMP();
                    } else {
                        speed++;
                    }
                }
                // Set scenario type-specific delay
                deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
                // Lances deployed in scout roles always deploy units in 6-walking speed turns
                if (scenario.getLanceRole().isScouting() && (scenario.getLance(campaign) != null)
                    && (scenario.getLance(campaign).getForceId() == scenario.getLanceForceId())
                    && !useDropship) {
                    deploymentRound = Math.max(deploymentRound, 6 - speed);
                }
            }
            entity.setDeployRound(deploymentRound);
            Force force = campaign.getForceFor(unit);
            if (force != null) {
                entity.setForceString(force.getFullMMName());
            }
        }

        for (Entity entity : scenario.getAlliesPlayer()) {
            if (null == entity) {
                continue;
            }
            entity.setOwner(player);

            int deploymentRound = entity.getDeployRound();
            if (!(scenario instanceof AtBDynamicScenario)) {
                int speed = entity.getWalkMP();
                if (entity.getJumpMP() > 0) {
                    if (entity instanceof Infantry) {
                        speed = entity.getJumpMP();
                    } else {
                        speed++;
                    }
                }
                deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
                if (!useDropship && scenario.getLanceRole().isScouting()
                    && (scenario.getLance(campaign).getForceId() == scenario.getLanceForceId())) {
                    deploymentRound = Math.max(deploymentRound, 6 - speed);
                }
            }

            entity.setDeployRound(deploymentRound);
            entities.add(entity);
        }

        sendEntities(entities);
    }

    private void setupMapSettings() {

        MapSettings mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(scenario.getMapX(), scenario.getMapY());
        mapSettings.setMapSize(1, 1);
        mapSettings.getBoardsSelectedVector().clear();

        // if the scenario is taking place in space, do space settings instead
        if (scenario.getBoardType() == Scenario.T_SPACE
            || scenario.getTerrainType().equals("Space")) {
            mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
            mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
        } else if (scenario.isUsingFixedMap()) {
            String board = scenario.getMap().replace(".board", "");
            board = board.replace("\\", "/");
            mapSettings.getBoardsSelectedVector().add(board);

            if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
            }
        } else {
            File mapgenFile = new File("data/mapgen/" + scenario.getMap() + ".xml");
            try (InputStream is = new FileInputStream(mapgenFile)) {
                mapSettings = MapSettings.getInstance(is);
            } catch (IOException ex) {
                Sentry.captureException(ex);
                logger.error(
                    String.format("Could not load map file data/mapgen/%s.xml", scenario.getMap()),
                    ex);
            }

            if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
                mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
            }

            // duplicate code, but getting a new instance of map settings resets the size
            // parameters
            mapSettings.setBoardSize(scenario.getMapX(), scenario.getMapY());
            mapSettings.setMapSize(1, 1);
            mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
        }
        game.setMapSettings(mapSettings);
    }

    private void configureBot(Player bot, BotForce botForce) {
        game.addPlayer(bot.getId(), bot);
        bot.setTeam(botForce.getTeam());

        // set deployment
        bot.setStartingPos(botForce.getStartingPos());
        bot.setStartOffset(botForce.getStartOffset());
        bot.setStartWidth(botForce.getStartWidth());
        bot.setStartingAnyNWx(botForce.getStartingAnyNWx());
        bot.setStartingAnyNWy(botForce.getStartingAnyNWy());
        bot.setStartingAnySEx(botForce.getStartingAnySEx());
        bot.setStartingAnySEy(botForce.getStartingAnySEy());

        // set camo
        bot.setCamouflage(botForce.getCamouflage().clone());
        bot.setColour(botForce.getColour());
        setupBotEntities(bot, botForce);
    }

    private void setupBotEntities(Player bot, BotForce botForce) {
        String forceName = bot.getName() + "|1";
        var entities = new ArrayList<Entity>();
        botForce.generateRandomForces(units, campaign);
        for (Entity entity : botForce.getFullEntityList(campaign)) {
            entity.setOwner(bot);
            entity.setForceString(forceName);

            if (entity.getDeployRound() == 0) {
                entity.setDeployRound(botForce.getDeployRound());
            }
            entities.add(entity);
        }
        sendEntities(entities);
    }

    private void getPlanetaryConditions() {
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        if (campaign.getCampaignOptions().isUseLightConditions()) {
            planetaryConditions.setLight(scenario.getLight());
        }
        if (campaign.getCampaignOptions().isUseWeatherConditions()) {
            planetaryConditions.setWeather(scenario.getWeather());
            planetaryConditions.setWind(scenario.getWind());
            planetaryConditions.setFog(scenario.getFog());
            planetaryConditions.setEMI(scenario.getEMI());
            planetaryConditions.setBlowingSand(scenario.getBlowingSand());
            planetaryConditions.setTemperature(scenario.getModifiedTemperature());
        }
        if (campaign.getCampaignOptions().isUsePlanetaryConditions()) {
            planetaryConditions.setAtmosphere(scenario.getAtmosphere());
            planetaryConditions.setGravity(scenario.getGravity());
        }
        game.setPlanetaryConditions(planetaryConditions);
    }

    private void sendEntities(List<Entity> entities) {
        Map<Integer, Integer> forceMapping = new HashMap<>();

        for (final Entity entity : new ArrayList<>(entities)) {
            entity.restore();
            if (entity instanceof ProtoMek) {
                int numPlayerProtos = game.getSelectedEntityCount(new EntitySelector() {
                    private final int ownerId = entity.getOwnerId();

                    @Override
                    public boolean accept(Entity entity) {
                        return (entity instanceof ProtoMek) && (ownerId == entity.getOwnerId());
                    }
                });

                entity.setUnitNumber((short) (numPlayerProtos / 5));
            }

            if (Entity.NONE == entity.getId()) {
                entity.setId(game.getNextEntityId());
            }
            // game.addEntity(entity);

            // Give the unit a spotlight, if it has the spotlight quirk
            entity.setExternalSearchlight(entity.hasExternalSearchlight()
                || entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));

            game.getPlayer(entity.getOwnerId()).changeInitialEntityCount(1);
            game.getPlayer(entity.getOwnerId()).changeInitialBV(entity.calculateBattleValue());

            // Restore forces from MULs or other external sources from the forceString, if
            // any
            if (!entity.getForceString().isBlank()) {
                List<megamek.common.force.Force> forceList = Forces.parseForceString(entity);
                int realId = megamek.common.force.Force.NO_FORCE;
                boolean topLevel = true;

                for (megamek.common.force.Force force : forceList) {
                    if (!forceMapping.containsKey(force.getId())) {
                        if (topLevel) {
                            realId = game.getForces().addTopLevelForce(force, entity.getOwner());
                        } else {
                            megamek.common.force.Force parent = game.getForces().getForce(realId);
                            realId = game.getForces().addSubForce(force, parent);
                        }
                        forceMapping.put(force.getId(), realId);
                    } else {
                        realId = forceMapping.get(force.getId());
                    }
                    topLevel = false;
                }
                entity.setForceString("");
                game.getForces().addEntity(entity, realId);
                // add force
            }
        }
    }
}
