/*
 * AtBGameThread.java
 *
 * Derived from GameThread.java, Copyright (c) 2011.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.JOptionPane;

import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MapSettings;
import megamek.common.Minefield;
import megamek.common.PlanetaryConditions;
import megamek.common.UnitType;
import megamek.common.logging.LogLevel;
import mekhq.campaign.againstTheBot.enums.AtBLanceRole;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * @author Neoancient
 *
 * Enhanced version of GameThread which imports settings and non-player
 * units into the MM game
 */
public class AtBGameThread extends GameThread {

    AtBScenario scenario;

    public AtBGameThread(String name, String password, Client c, MekHQ app, List<Unit> units,
                         AtBScenario scenario) {
        this(name, password, c, app, units, scenario, true);
    }

    public AtBGameThread(String name, String password, Client c, MekHQ app, List<Unit> units,
                         AtBScenario scenario, boolean started) {
        super(name, password, c, app, units, started);
        this.scenario = scenario;
    }

    // String tokens for dialog boxes used for transport loading
    private static final String LOAD_FTR_DIALOG_TEXT = "Would you like the fighters assigned to %s to deploy loaded into its bays?";
    private static final String LOAD_FTR_DIALOG_TITLE = "Load Fighters on Transport?";
    private static final String LOAD_GND_DIALOG_TEXT = "Would you like the ground units assigned to %s to deploy loaded into its bays?";
    private static final String LOAD_GND_DIALOG_TITLE = "Load Ground Units on Transport?";


    @Override
    public void run() {
        client.addCloseClientListener(this);

        if (swingGui != null) {
            for (Client client2 : swingGui.getBots().values()) {
                client2.die();
            }
            swingGui.getBots().clear();
        }
        createController();
        swingGui = new ClientGUI(client, controller);
        controller.clientgui = swingGui;
        swingGui.initialize();

        try {
            client.connect();
        } catch (Exception ex) {
            MekHQ.getLogger().error("MegaMek client failed to connect to server", ex);
            return;
        }

        try {
            while (client.getLocalPlayer() == null) {
                Thread.sleep(50);
            }

            // if game is running, shouldn't do the following, so detect the
            // phase
            for (int i = 0; (i < 1000) && (client.getGame().getPhase() == IGame.Phase.PHASE_UNKNOWN); i++) {
                Thread.sleep(50);
                MekHQ.getLogger().error("Thread in unknown stage");
            }

            if (((client.getGame() != null) && (client.getGame().getPhase() == IGame.Phase.PHASE_LOUNGE))) {
                MekHQ.getLogger().info("Thread in lounge");

                client.getLocalPlayer().setCamouflage(app.getCampaign().getCamouflage().clone());
                client.getLocalPlayer().setColour(app.getCampaign().getColour());

                if (started) {
                    client.getGame().getOptions().loadOptions();
                    client.sendGameOptions(password, app.getCampaign().getGameOptionsVector());
                    Thread.sleep(MekHQ.getMekHQOptions().getStartGameDelay());
                }

                MapSettings mapSettings = MapSettings.getInstance();

                // if the scenario is taking place in space, do space settings instead
                if ((scenario != null) &&
                        scenario.getTerrainType() == AtBScenario.TER_SPACE) {
                    mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                } else {
                    File mapgenFile = new File("data/mapgen/" + scenario.getMap() + ".xml");
                    try (InputStream is = new FileInputStream(mapgenFile)) {
                        mapSettings = MapSettings.getInstance(is);
                    } catch (FileNotFoundException ex) {
                        MekHQ.getLogger().error("Could not load map file data/mapgen/" + scenario.getMap() + ".xml", ex);
                    }

                    if (scenario.getTerrainType() == AtBScenario.TER_LOW_ATMO) {
                        mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
                    }
                }

                mapSettings.setBoardSize(scenario.getMapX(), scenario.getMapY());
                mapSettings.setMapSize(1,  1);
                mapSettings.getBoardsSelectedVector().clear();
                mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
                client.sendMapSettings(mapSettings);
                Thread.sleep(MekHQ.getMekHQOptions().getStartGameDelay());

                PlanetaryConditions planetaryConditions = new PlanetaryConditions();
                planetaryConditions.setLight(scenario.getLight());
                planetaryConditions.setWeather(scenario.getWeather());
                planetaryConditions.setWindStrength(scenario.getWind());
                planetaryConditions.setFog(scenario.getFog());
                planetaryConditions.setAtmosphere(scenario.getAtmosphere());
                planetaryConditions.setGravity(scenario.getGravity());
                client.sendPlanetaryConditions(planetaryConditions);
                Thread.sleep(MekHQ.getMekHQOptions().getStartGameDelay());

                client.getLocalPlayer().setStartingPos(scenario.getStart());
                client.getLocalPlayer().setTeam(1);
                
                //minefields
                client.getLocalPlayer().setNbrMFActive(scenario.getNumPlayerMinefields(Minefield.TYPE_ACTIVE));
                client.getLocalPlayer().setNbrMFCommand(scenario.getNumPlayerMinefields(Minefield.TYPE_COMMAND_DETONATED));
                client.getLocalPlayer().setNbrMFConventional(scenario.getNumPlayerMinefields(Minefield.TYPE_CONVENTIONAL));
                client.getLocalPlayer().setNbrMFInferno(scenario.getNumPlayerMinefields(Minefield.TYPE_INFERNO));
                client.getLocalPlayer().setNbrMFVibra(scenario.getNumPlayerMinefields(Minefield.TYPE_VIBRABOMB));

                /* If the player is making a combat drop (either required by scenario
                 * or player chose to deploy a DropShip), do not use deployment
                 * delay for slower scout units.
                 */
                boolean useDropship = false;
                if (scenario.getLanceRole() == AtBLanceRole.SCOUTING) {
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

                var entities = new ArrayList<Entity>();
                for (Unit unit : units) {
                    // Get the Entity
                    Entity entity = unit.getEntity();
                    // Set the TempID for auto reporting
                    entity.setExternalIdAsString(unit.getId().toString());
                    // Set the owner
                    entity.setOwner(client.getLocalPlayer());
                    if (unit.hasTransportedUnits()) {
                        //Store this unit as a potential transport to load
                        scenario.getPlayerTransportLinkages().put(unit.getId(), new ArrayList<>());
                    }
                    // If this unit is a spacecraft, set the crew size and marine size values
                    if (entity.isLargeCraft() || (entity.getUnitType() == UnitType.SMALL_CRAFT)) {
                        entity.setNCrew(unit.getActiveCrew().size());
                        //TODO: Change this when marines are fully implemented
                        entity.setNMarines(unit.getMarineCount());
                    }
                    // Calculate deployment round
                    int deploymentRound = entity.getDeployRound();
                    if (!(scenario instanceof AtBDynamicScenario)) {
                        int speed = entity.getWalkMP();
                        if (entity.getJumpMP() > 0) {
                            if (entity instanceof megamek.common.Infantry) {
                                speed = entity.getJumpMP();
                            } else {
                                speed++;
                            }
                        }
                        // Set scenario type-specific delay
                        deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
                        // Lances deployed in scout roles always deploy units in 6-walking speed turns
                        if ((scenario.getLanceRole() == AtBLanceRole.SCOUTING)
                                && (scenario.getLance(campaign) != null)
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
                    entities.add(entity);
                }
                client.sendAddEntity(entities);
                
                // Run through the units again. This time add transported units to the correct linkage,
                // but only if the transport itself is in the game too.
                for (Unit unit : units) {
                    if (unit.hasTransportShipAssignment()) {
                        Unit transportShip = unit.getTransportShipAssignment().getTransportShip();
                        if (scenario.getPlayerTransportLinkages().containsKey(transportShip.getId())) {
                            scenario.addPlayerTransportRelationship(transportShip.getId(), unit.getId());
                        }
                    }
                }
                // Now, clean the list of any transports that don't have deployed units in the game
                Set<UUID> emptyTransports = new HashSet<>();
                for (UUID id : scenario.getPlayerTransportLinkages().keySet()) {
                    if (scenario.getPlayerTransportLinkages().get(id).isEmpty()) {
                        emptyTransports.add(id);
                    }
                }
                for (UUID id : emptyTransports) {
                    scenario.getPlayerTransportLinkages().remove(id);
                }

                /* Add player-controlled ally units */
                entities.clear();
                for (Entity entity : scenario.getAlliesPlayer()) {
                    if (null == entity) {
                        continue;
                    }
                    entity.setOwner(client.getLocalPlayer());

                    int deploymentRound = entity.getDeployRound();
                    if (!(scenario instanceof AtBDynamicScenario)) {
                        int speed = entity.getWalkMP();
                        if (entity.getJumpMP() > 0) {
                            if (entity instanceof megamek.common.Infantry) {
                                speed = entity.getJumpMP();
                            } else {
                                speed++;
                            }
                        }
                        deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
                        if ((scenario.getLanceRole() == AtBLanceRole.SCOUTING)
                                && (scenario.getLance(campaign).getForceId() == scenario.getLanceForceId())
                                && !useDropship) {
                            deploymentRound = Math.max(deploymentRound, 6 - speed);
                        }
                    }

                    entity.setDeployRound(deploymentRound);
                    entities.add(entity);
                }
                client.sendAddEntity(entities);
                client.sendPlayerInfo();

                /* Add bots */
                for (int i = 0; i < scenario.getNumBots(); i++) {
                    BotForce bf = scenario.getBotForce(i);
                    String name = bf.getName();
                    if (swingGui.getBots().containsKey(name)) {
                        int append = 2;
                        while (swingGui.getBots().containsKey(name + append)) {
                            append++;
                        }
                        name += append;
                    }
                    Princess botClient = new Princess(name, client.getHost(), client.getPort(), LogLevel.ERROR);
                    botClient.setBehaviorSettings(bf.getBehaviorSettings());
                    try {
                        botClient.connect();
                    } catch (Exception e) {
                        MekHQ.getLogger().error("Could not connect with Bot name " + bf.getName(), e);
                    }
                    swingGui.getBots().put(name, botClient);

                    configureBot(botClient, bf);
                    
                    // we need to wait until the game has actually started to do transport loading
                    // This will load the bot's infantry into APCs
                    Thread.sleep(MekHQ.getMekHQOptions().getStartGameDelay());
                    if (scenario != null) {
                        AtBDynamicScenarioFactory.loadTransports(scenario, botClient);
                    }
                }

                // All player and bot units have been added to the lobby
                // Prompt the player to auto-load units into transports
                if (!scenario.getPlayerTransportLinkages().isEmpty()) {
                    for (UUID id : scenario.getPlayerTransportLinkages().keySet()) {
                        boolean loadFighters = false;
                        boolean loadGround = false;
                        Unit transport = campaign.getUnit(id);
                        Set<Integer> toLoad = new HashSet<>();
                        // Let the player choose to load fighters and/or ground units on each transport
                        if (transport.isCarryingAero()) {
                            loadFighters = (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,
                                                String.format(AtBGameThread.LOAD_FTR_DIALOG_TEXT, transport.getName()),
                                                AtBGameThread.LOAD_FTR_DIALOG_TITLE, JOptionPane.YES_NO_OPTION));
                        }
                        if (transport.isCarryingGround()) {
                            loadGround = (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,
                                    String.format(AtBGameThread.LOAD_GND_DIALOG_TEXT, transport.getName()),
                                    AtBGameThread.LOAD_GND_DIALOG_TITLE, JOptionPane.YES_NO_OPTION));
                        }
                        // Now, send the load commands
                        if (loadFighters || loadGround) {
                            // List of technicians assigned to transported units. Several units can share a tech.
                            Set<Person> cargoTechs = new HashSet<>();
                            for (UUID cargoId : scenario.getPlayerTransportLinkages().get(id)) {
                                Unit unit = campaign.getUnit(cargoId);
                                if (unit != null) {
                                    //Convert the list of Unit UUIDs to MM EntityIds
                                    toLoad.add(unit.getEntity().getId());
                                    if (unit.getTech() != null) {
                                        cargoTechs.add(unit.getTech());
                                    }
                                }
                            }
                            //Update the transport's passenger count with assigned techs
                            transport.getEntity().setNPassenger(transport.getEntity().getNPassenger() + (cargoTechs.size()));
                            client.sendUpdateEntity(transport.getEntity());
                            //And now load the units. Unit crews load as passengers here.
                            Utilities.loadPlayerTransports(transport.getEntity().getId(), toLoad, client, loadFighters, loadGround);
                        }
                    }
                }
            }

            while (!stop) {
                Thread.sleep(50);
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        } finally {
            client.die();
            client = null;
            swingGui = null;
            controller = null;
        }

    }

    /**
     * wait for the server to add the bot client, then send starting position,
     * camo, and entities
     *
     * @param botClient
     * @param botForce
     */
    private void configureBot(BotClient botClient, BotForce botForce) {
        try {
            /* Wait for the server to add the bot client, but allow a timeout
             * rather than blocking
             */
            int retries = 50;
            while ((retries-- > 0) && (null == botClient.getLocalPlayer())) {
                sleep(50);
            }
            if (null == botClient.getLocalPlayer()) {
                MekHQ.getLogger().error("Could not configure bot " + botClient.getName());
            } else {
                botClient.getLocalPlayer().setTeam(botForce.getTeam());
                botClient.getLocalPlayer().setStartingPos(botForce.getStart());

                botClient.getLocalPlayer().setCamouflage(botForce.getCamouflage().clone());
                botClient.getLocalPlayer().setColour(botForce.getColour());

                botClient.sendPlayerInfo();
                
                String forceName = botClient.getLocalPlayer().getName() + "|1";
                var entities = new ArrayList<Entity>();
                for (Entity entity : botForce.getEntityList()) {
                    if (null == entity) {
                        continue;
                    }
                    entity.setOwner(botClient.getLocalPlayer());
                    entity.setForceString(forceName);
                    entities.add(entity);
                }
                botClient.sendAddEntity(entities);
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
    }
}
