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
import java.util.ArrayList;

import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MapSettings;
import megamek.common.PlanetaryConditions;
import megamek.common.Player;
import megamek.common.logging.LogLevel;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBScenario;
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
			MekHQ.logMessage("MegaMek client failed to connect to server");
			MekHQ.logError(ex);
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
            	System.out.println("Thread in unknown stage" );
            }

            if (((client.getGame() != null) && (client.getGame().getPhase() == IGame.Phase.PHASE_LOUNGE))) {
            	System.out.println("Thread in lounge" );

            	client.getLocalPlayer().setCamoCategory(app.getCampaign().getCamoCategory());
                client.getLocalPlayer().setCamoFileName(app.getCampaign().getCamoFileName());
            	
                if (started) {
                	client.getGame().getOptions().loadOptions();
                	client.sendGameOptions("", app.getCampaign().getGameOptionsVector());
    				Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());
                }

                MapSettings mapSettings = new MapSettings(scenario.getMapX(), scenario.getMapY(), 1, 1);
                File f = new File("data/mapgen/" + scenario.getMap() + ".xml");
                try {
                	mapSettings.load(new FileInputStream(f));
                } catch (FileNotFoundException ex) {
                	MekHQ.logError("Could not load map file data/mapgen/" + scenario.getMap() + ".xml");
                	MekHQ.logError(ex);
                }
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

                for (Unit unit : units) {
                	// Get the Entity
                	Entity entity = unit.getEntity();
                	// Set the TempID for autoreporting
                	entity.setExternalIdAsString(unit.getId().toString());
                	// Set the owner
                	entity.setOwner(client.getLocalPlayer()); 
                	// Calculate deployment round
                	int deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - entity.getWalkMP());
                	if (scenario.getLanceRole() == Lance.ROLE_SCOUT) {
                		deploymentRound = Math.max(deploymentRound, 6 - entity.getWalkMP());
                	}
                	entity.setDeployRound(deploymentRound);
                	// Add Mek to game
                	client.sendAddEntity(entity);
                	// Wait a few secs to not overuse bandwith
                	Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());
                }

                /* Add player-controlled ally units */
                for (Entity entity : scenario.getAlliesPlayer()) {
                	if (null == entity) {
                		continue;
                	}
                	entity.setOwner(client.getLocalPlayer());
                	int deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - entity.getWalkMP());
                	if (null != scenario.getLance(campaign) && scenario.getLance(campaign).getRole() == Lance.ROLE_SCOUT) {
                		deploymentRound = Math.max(deploymentRound, 6 - entity.getWalkMP());
                	}
                	entity.setDeployRound(deploymentRound);
                	client.sendAddEntity(entity);
                	Thread.sleep(campaign.getCampaignOptions().getStartGameDelay());
                }

                client.sendPlayerInfo();
                
                /* Add bots */
                for (int i = 0; i < scenario.getNumBots(); i++) {
                	AtBScenario.BotForce bf = scenario.getBotForce(i);
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
                		MekHQ.logError("Could not connect with Bot name " + bf.getName());
                	}
                	swingGui.getBots().put(name, botClient);
                	
                	configureBot(botClient, bf);
                }

            }
            
            while(!stop) {
            	Thread.sleep(50);
            }
        } catch (Exception e) {
			MekHQ.logError(e);
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
    private void configureBot(BotClient botClient, AtBScenario.BotForce botForce) {
		try {
			/* Wait for the server to add the bot client, but allow a timeout
			 * rather than blocking
			 */
			int retries = 50;
			while (retries-- > 0 && null == botClient.getLocalPlayer()) {
				sleep(50);
			}
			if (null == botClient.getLocalPlayer()) {
				MekHQ.logError("Could not configure bot " + botClient.getName());
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

				if (botForce.getCamoCategory() == Player.NO_CAMO) {
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
			MekHQ.logError(e);
		}    	
    }
}
