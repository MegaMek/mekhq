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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
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
import megamek.common.PlanetaryConditions;
import megamek.common.Player;
import megamek.common.UnitType;
import megamek.common.logging.LogLevel;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.unit.Unit;

/**
 * @author Neoancient
 *
 * Enhanced version of GameThread which imports settings and non-player
 * units into the MM game
 *
 */
public class AtBGameThread extends GameThread {

    AtBScenario scenario;

    public AtBGameThread(String name, Client c, MekHQ app, ArrayList<Unit> units, AtBScenario scenario) {
        this(name, c, app, units, scenario, true);
    }

    public AtBGameThread(String name, Client c, MekHQ app, ArrayList<Unit> units, AtBScenario scenario, boolean started) {
        super(name, c, app, units, started);
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
            MekHQ.getLogger().log(getClass(), "run", LogLevel.ERROR, //$NON-NLS-1$
                    "MegaMek client failed to connect to server"); //$NON-NLS-1$
            MekHQ.getLogger().error(getClass(), "run", ex); //$NON-NLS-1$
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
                MekHQ.getLogger().error(getClass(), "run", "Thread in unknown stage" );
            }

            if (((client.getGame() != null) && (client.getGame().getPhase() == IGame.Phase.PHASE_LOUNGE))) {
                MekHQ.getLogger().info(getClass(), "run", "Thread in lounge" );

                client.getLocalPlayer().setCamoCategory(app.getCampaign().getCamoCategory());
                client.getLocalPlayer().setCamoFileName(app.getCampaign().getCamoFileName());
                client.getLocalPlayer().setColorIndex(app.getCampaign().getColorIndex());

                if (started) {
                    client.getGame().getOptions().loadOptions();
                    client.sendGameOptions("", app.getCampaign().getGameOptionsVector());
                    Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());
                }

                MapSettings mapSettings = MapSettings.getInstance();

                // if the scenario is taking place in space, do space settings instead
                if ((scenario instanceof AtBScenario) &&
                        scenario.getTerrainType() == AtBScenario.TER_SPACE) {
                    mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                } else {
                    File mapgenFile = new File("data/mapgen/" + scenario.getMap() + ".xml");
                    try (InputStream is = new FileInputStream(mapgenFile)) {
                        mapSettings = MapSettings.getInstance(is);
                    } catch (FileNotFoundException ex) {
                        MekHQ.getLogger().log(getClass(), "run", LogLevel.ERROR, //$NON-NLS-1$
                                "Could not load map file data/mapgen/" + scenario.getMap() + ".xml"); //$NON-NLS-1$
                        MekHQ.getLogger().error(getClass(), "run", ex); //$NON-NLS-1$
                    }

                    if(scenario.getTerrainType() == AtBScenario.TER_LOW_ATMO) {
                        mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
                    }
                }

                mapSettings.setBoardSize(scenario.getMapX(), scenario.getMapY());
                mapSettings.setMapSize(1,  1);
                mapSettings.getBoardsSelectedVector().clear();
                mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
                client.sendMapSettings(mapSettings);
                Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());

                PlanetaryConditions planetaryConditions = new PlanetaryConditions();
                planetaryConditions.setLight(scenario.getLight());
                planetaryConditions.setWeather(scenario.getWeather());
                planetaryConditions.setWindStrength(scenario.getWind());
                planetaryConditions.setFog(scenario.getFog());
                planetaryConditions.setAtmosphere(scenario.getAtmosphere());
                planetaryConditions.setGravity(scenario.getGravity());
                client.sendPlanetaryConditions(planetaryConditions);
                Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());

                client.getLocalPlayer().setStartingPos(scenario.getStart());
                client.getLocalPlayer().setTeam(1);

                /* If the player is making a combat drop (either required by scenario
                 * or player chose to deploy a DropShip), do not use deployment
                 * delay for slower scout units.
                 */
                boolean useDropship = false;
                if (scenario.getLanceRole() == Lance.ROLE_SCOUT) {
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
                    entity.setOwner(client.getLocalPlayer());
                    if (!unit.getTransportedUnits().isEmpty()) {
                        //Store this unit as a potential transport to load
                        scenario.getPlayerTransportLinkages().put(unit.getId(), new ArrayList<>());
                    }
                    // Calculate deployment round
                    int deploymentRound = entity.getDeployRound();
                    if(!(scenario instanceof AtBDynamicScenario)) {
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
                        if ((scenario.getLanceRole() == Lance.ROLE_SCOUT)
                                && (scenario.getLance(campaign) != null)
                                && (scenario.getLance(campaign).getForceId() == scenario.getLanceForceId())
                                && !useDropship) {
                            deploymentRound = Math.max(deploymentRound, 6 - speed);
                        }
                    }
                    entity.setDeployRound(deploymentRound);
                    // Add Mek to game
                    client.sendAddEntity(entity);
                    // Wait a few secs to not overuse bandwidth
                    Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());
                }
                // Run through the units again. This time add transported units to the correct linkage,
                // but only if the transport itself is in the game too.
                for (Unit unit : units) {
                    if (unit.hasTransportShipId()) {
                        for (UUID trnId : unit.getTransportShipId().keySet()) {
                            if (!scenario.getPlayerTransportLinkages().containsKey(trnId)) {
                                continue;
                            }

                            scenario.addPlayerTransportRelationship(trnId, unit.getId());
                            // Set these flags so we know what prompts to display later
                            if (unit.getEntity().isAero()) {
                                campaign.getUnit(trnId).setCarryingAero(true);
                            } else {
                                campaign.getUnit(trnId).setCarryingGround(true);
                            }
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
                for (Entity entity : scenario.getAlliesPlayer()) {
                    if (null == entity) {
                        continue;
                    }
                    entity.setOwner(client.getLocalPlayer());

                    int deploymentRound = entity.getDeployRound();
                    if(!(scenario instanceof AtBDynamicScenario)) {
                        int speed = entity.getWalkMP();
                        if (entity.getJumpMP() > 0) {
                            if (entity instanceof megamek.common.Infantry) {
                                speed = entity.getJumpMP();
                            } else {
                                speed++;
                            }
                        }
                        deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
                        if (scenario.getLanceRole() == Lance.ROLE_SCOUT
                                && scenario.getLance(campaign).getForceId() == scenario.getLanceForceId()
                                && !useDropship) {
                            deploymentRound = Math.max(deploymentRound, 6 - speed);
                        }
                    }

                    entity.setDeployRound(deploymentRound);
                    client.sendAddEntity(entity);
                    Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());
                }

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
                        MekHQ.getLogger().log(getClass(), "run", LogLevel.ERROR, //$NON-NLS-1$
                                "Could not connect with Bot name " + bf.getName()); //$NON-NLS-1$
                        MekHQ.getLogger().error(getClass(), "run", e); //$NON-NLS-1$
                    }
                    swingGui.getBots().put(name, botClient);

                    configureBot(botClient, bf);

                    // we need to wait until the game has actually started to do transport loading
                    // This will load the bot's infantry into APCs
                    if(scenario instanceof AtBScenario) {
                        AtBDynamicScenarioFactory.loadTransports((AtBScenario) scenario, botClient);
                    }

                    // Prompt the player to auto-load units into transports
                    if (!scenario.getPlayerTransportLinkages().isEmpty()) {
                        boolean loadFighters = false;
                        boolean loadGround = false;
                        for (UUID id : scenario.getPlayerTransportLinkages().keySet()) {
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
                                for (UUID cargoId : scenario.getPlayerTransportLinkages().get(id)) {
                                    //Convert the list of Unit UUIDs to MM EntityIds
                                    toLoad.add(campaign.getUnit(cargoId).getEntity().getId());
                                }
                                Utilities.loadPlayerTransports(transport.getEntity().getId(), toLoad, client, loadFighters, loadGround);
                            }
                        }
                    }
                }
            }

            while(!stop) {
                Thread.sleep(50);
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(getClass(), "run", e); //$NON-NLS-1$
        }
        finally {
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
            while (retries-- > 0 && null == botClient.getLocalPlayer()) {
                sleep(50);
            }
            if (null == botClient.getLocalPlayer()) {
                MekHQ.getLogger().log(getClass(), "configureBot", LogLevel.ERROR, //$NON-NLS-1$
                        "Could not configure bot " + botClient.getName()); //$NON-NLS-1$
            } else {
                for (Entity entity : botForce.getEntityList()) {
                    if (null == entity) {
                        continue;
                    }
                    entity.setOwner(botClient.getLocalPlayer());
                    botClient.sendAddEntity(entity);
                    Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());
                }
                botClient.getLocalPlayer().setTeam(botForce.getTeam());
                botClient.getLocalPlayer().setStartingPos(botForce.getStart());

                if (botForce.getCamoCategory().equals(Player.NO_CAMO)) {
                    if (botForce.getColorIndex() >= 0) {
                        botClient.getLocalPlayer().setColorIndex(botForce.getColorIndex());
                    }
                } else {
                    botClient.getLocalPlayer().setCamoCategory(botForce.getCamoCategory());
                    botClient.getLocalPlayer().setCamoFileName(botForce.getCamoFileName());
                }

                botClient.sendPlayerInfo();
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(getClass(), "configureBot", e); //$NON-NLS-1$
        }
    }
}
