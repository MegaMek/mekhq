/*
 * Copyright (c) 2011-2024 - The MegaMek Team. All Rights Reserved.
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.sentry.Sentry;
import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.CloseClientListener;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.MegaMekGUI;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.Entity;
import megamek.common.MapSettings;
import megamek.common.WeaponOrderHandler;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;

class GameThread extends Thread implements CloseClientListener {
    private static final MMLogger logger = MMLogger.create(GameThread.class);

    // region Variable Declarations
    protected String myname;
    protected String password;
    protected Client client;
    protected ClientGUI swingGui;
    protected MegaMekController controller;
    protected MekHQ app;
    protected Campaign campaign;
    protected boolean started;

    protected List<Unit> units;

    protected volatile boolean stop = false;

    private final Scenario scenario;
    // endregion Variable Declarations

    // region Constructors

    /**
     * GameThread
     * <p>
     *     Initializes a new thread for a game.
     * </p>
     *
     * @param name          The player name
     * @param password      The game password
     * @param client        The client
     * @param app           The MekHQ instance
     * @param units         The list of units you intend to play with in your side
     * @param scenario      The scenario that is going to be initialized for the game
     */
    public GameThread(String name, String password, Client client, MekHQ app, List<Unit> units, Scenario scenario) {
        this(name, password, client, app, units, scenario, true);
    }


    /**
     * GameThread
     * <p>
     *     Initializes a new thread for a game.
     * </p>
     *
     * @param name          The player name
     * @param client        The client
     * @param app           The MekHQ instance
     * @param units         The list of units you intend to play with in your side
     * @param scenario      The scenario that is going to be initialized for the game
     * @param started       Whether the game has already started
     */
    public GameThread(String name, Client client, MekHQ app, List<Unit> units, Scenario scenario, boolean started) {
        this(name, "", client, app, units, scenario, started);
    }


    /**
     * GameThread
     * <p>
     *     Initializes a new thread for a game.
     * </p>
     *
     * @param name          The player name
     * @param password      The game password
     * @param client        The client
     * @param app           The MekHQ instance
     * @param units         The list of units you intend to play with in your side
     * @param scenario      The scenario that is going to be initialized for the game
     * @param started       Whether the game has already started
     */
    public GameThread(String name, String password, Client client, MekHQ app, List<Unit> units, Scenario scenario,
            boolean started) {
        super(name);
        myname = name.trim();
        this.password = password;
        this.client = client;
        this.app = app;
        this.units = Objects.requireNonNull(units);
        this.started = started;
        this.campaign = app.getCampaign();
        this.scenario = Objects.requireNonNull(scenario);
    }
    // endregion Constructors

    public Client getClient() {
        return client;
    }

    @Override
    public void run() {
        client.addCloseClientListener(this);

        if (swingGui != null) {
            for (AbstractClient client2 : swingGui.getLocalBots().values()) {
                client2.die();
            }
            swingGui.getLocalBots().clear();
        }
        createController();
        swingGui = new ClientGUI(client, controller);
        controller.clientgui = swingGui;
        swingGui.initialize();

        try {
            client.connect();
        } catch (Exception ex) {
            Sentry.captureException(ex);
            logger.error("MegaMek client failed to connect to server", ex);
            return;
        }

        try {
            while (client.getLocalPlayer() == null) {
                Thread.sleep(MekHQ.getMHQOptions().getStartGameClientDelay());
            }

            // if game is running, shouldn't do the following, so detect the phase
            for (int i = 0; (i < MekHQ.getMHQOptions().getStartGameClientRetryCount())
                    && client.getGame().getPhase().isUnknown(); i++) {
                Thread.sleep(MekHQ.getMHQOptions().getStartGameClientDelay());
                logger
                        .warn("Client has not finished initialization, and is currently in an unknown phase.");
            }

            if ((client.getGame() != null) && client.getGame().getPhase().isLounge()) {
                logger.info("Thread in lounge");
                client.getLocalPlayer().setCamouflage(app.getCampaign().getCamouflage().clone());

                if (started) {
                    client.getGame().getOptions().loadOptions();
                    client.sendGameOptions(password, app.getCampaign().getGameOptionsVector());
                    Thread.sleep(MekHQ.getMHQOptions().getStartGameDelay());
                }

                MapSettings mapSettings = MapSettings.getInstance();

                // check that we have valid conditions for setting the mapSettings
                if ((scenario.getMapSizeX() > 1) && (scenario.getMapSizeY() > 1) && (null != scenario.getMap())) {

                    mapSettings.setBoardSize(scenario.getMapSizeX(), scenario.getMapSizeY());
                    mapSettings.setMapSize(1, 1);
                    mapSettings.getBoardsSelectedVector().clear();

                    // if the scenario is taking place in space, do space settings instead
                    if (scenario.getBoardType() == Scenario.T_SPACE) {
                        mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                        mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
                    } else if (scenario.isUsingFixedMap()) {
                        String board = scenario.getMap().replace(".board", ""); // TODO : remove inline file type
                        board = board.replace("\\", "/");
                        mapSettings.getBoardsSelectedVector().add(board);

                        if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
                            mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
                        }
                    } else {
                        File mapgenFile = new File("data/mapgen/" + scenario.getMap() + ".xml"); // TODO : remove inline
                                                                                                 // file path
                        try (InputStream is = new FileInputStream(mapgenFile)) {
                            mapSettings = MapSettings.getInstance(is);
                        } catch (FileNotFoundException ex) {
                            logger
                                    .error(
                                            String.format("Could not load map file data/mapgen/%s.xml",
                                                    scenario.getMap()),
                                            ex);
                            // TODO: remove inline file path
                        }

                        if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
                            mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
                        }

                        // duplicate code, but getting a new instance of map settings resets the size
                        // parameters
                        mapSettings.setBoardSize(scenario.getMapSizeX(), scenario.getMapSizeY());
                        mapSettings.setMapSize(1, 1);
                        mapSettings.getBoardsSelectedVector().add(MapSettings.BOARD_GENERATED);
                    }
                } else {
                    logger.error(
                            String.format("invalid map settings provided for scenario %s", scenario.getName()));
                }

                client.sendMapSettings(mapSettings);
                Thread.sleep(MekHQ.getMHQOptions().getStartGameDelay());

                client.sendPlanetaryConditions(scenario.createPlanetaryConditions());
                Thread.sleep(MekHQ.getMHQOptions().getStartGameDelay());

                // set player deployment
                client.getLocalPlayer().setStartingPos(scenario.getStartingPos());
                client.getLocalPlayer().setStartOffset(scenario.getStartOffset());
                client.getLocalPlayer().setStartWidth(scenario.getStartWidth());
                client.getLocalPlayer().setStartingAnyNWx(scenario.getStartingAnyNWx());
                client.getLocalPlayer().setStartingAnyNWy(scenario.getStartingAnyNWy());
                client.getLocalPlayer().setStartingAnySEx(scenario.getStartingAnySEx());
                client.getLocalPlayer().setStartingAnySEy(scenario.getStartingAnySEy());

                client.getLocalPlayer().setTeam(1);

                var entities = new ArrayList<Entity>();
                for (Unit unit : units) {
                    Entity entity = unit.getEntity();
                    // Set the TempID for auto reporting
                    entity.setExternalIdAsString(unit.getId().toString());
                    entity.setOwner(client.getLocalPlayer());
                    Force force = campaign.getForceFor(unit);
                    entity.setForceString(force.getFullMMName());
                    entities.add(entity);
                }
                client.sendAddEntity(entities);
                client.sendPlayerInfo();

                // Add bots
                for (int i = 0; i < scenario.getNumBots(); i++) {
                    BotForce bf = scenario.getBotForce(i);
                    String name = bf.getName();
                    if (swingGui.getLocalBots().containsKey(name)) {
                        int append = 2;
                        while (swingGui.getLocalBots().containsKey(name + append)) {
                            append++;
                        }
                        name += append;
                    }
                    Princess botClient = new Princess(name, client.getHost(), client.getPort());
                    botClient.setBehaviorSettings(bf.getBehaviorSettings());
                    try {
                        botClient.connect();
                    } catch (Exception e) {
                        Sentry.captureException(e);
                        logger.error(
                                String.format("Could not connect with Bot name %s", bf.getName()), e);
                    }
                    swingGui.getLocalBots().put(name, botClient);

                    // chill out while bot is created and connects to megamek
                    Thread.sleep(MekHQ.getMHQOptions().getStartGameDelay());
                    configureBot(botClient, bf);
                }
            }

            while (!stop) {
                Thread.sleep(50);
            }
        } catch (Exception e) {
            Sentry.captureException(e);
            logger.error("", e);
        } finally {
            swingGui.setDisconnectQuietly(true);
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
     * @param botClient a BotClient to manage the bot
     * @param botForce  a BotForce that will send its info and entities to the
     *                  botClient
     */
    private void configureBot(BotClient botClient, BotForce botForce) {
        try {
            // Wait for the server to add the bot client, but allow a timeout rather than
            // blocking
            int retryCount = 0;
            while ((botClient.getLocalPlayer() == null)
                    && (retryCount++ < MekHQ.getMHQOptions().getStartGameBotClientRetryCount())) {
                try {
                    Thread.sleep(MekHQ.getMHQOptions().getStartGameBotClientDelay());
                } catch (Exception ignored) {

                }
            }

            if (botClient.getLocalPlayer() == null) {
                logger.error(
                        String.format("Could not configure bot %s", botClient.getName()));
            } else {
                botClient.getLocalPlayer().setTeam(botForce.getTeam());

                // set deployment
                botClient.getLocalPlayer().setStartingPos(botForce.getStartingPos());
                botClient.getLocalPlayer().setStartOffset(botForce.getStartOffset());
                botClient.getLocalPlayer().setStartWidth(botForce.getStartWidth());
                botClient.getLocalPlayer().setStartingAnyNWx(botForce.getStartingAnyNWx());
                botClient.getLocalPlayer().setStartingAnyNWy(botForce.getStartingAnyNWy());
                botClient.getLocalPlayer().setStartingAnySEx(botForce.getStartingAnySEx());
                botClient.getLocalPlayer().setStartingAnySEy(botForce.getStartingAnySEy());

                // set camo
                botClient.getLocalPlayer().setCamouflage(botForce.getCamouflage().clone());
                botClient.getLocalPlayer().setColour(botForce.getColour());

                botClient.sendPlayerInfo();

                List<Entity> entities = setupBotEntities(botClient, botForce, scenario);
                botClient.sendAddEntity(entities);
            }
        } catch (Exception ex) {
            Sentry.captureException(ex);
            logger.error("", ex);
        }
    }

    protected List<Entity> setupBotEntities(BotClient botClient, BotForce botForce, Scenario scenario) {
        String forceName = botClient.getLocalPlayer().getName() + "|1";
        var entities = new ArrayList<Entity>();
        botForce.generateRandomForces(units, campaign);
        for (Entity entity : botForce.getFullEntityList(campaign)) {
            entity.setOwner(botClient.getLocalPlayer());
            entity.setForceString(forceName);
            /*
             * Only overwrite deployment round for entities if they have an individual
             * deployment round of zero.
             * Otherwise, we will overwrite entity specific deployment information.
             */
            if (entity.getDeployRound() == 0) {
                entity.setDeployRound(botForce.getDeployRound());
            }
            entities.add(entity);
        }

        return entities;
    }

    /*
     * from megamek.client.CloseClientListener clientClosed() Thanks to MM for
     * adding the listener. And to MMNet for the poorly documented code change.
     */
    @Override
    public void clientClosed() {
        requestStop();
        app.stopHost();
    }

    public void requestStop() {
        PreferenceManager.getInstance().save();
        try {
            WeaponOrderHandler.saveWeaponOrderFile();
        } catch (IOException e) {
            Sentry.captureException(e);
            logger.error("Error saving custom weapon orders!", e);
        }
        stop = true;
    }

    public boolean stopRequested() {
        return stop;
    }

    public void quit() {
        client.die();
        client = null;// explicit null of the MM client. Wasn't/isn't being
        // GC'ed.
        System.gc();
    }

    public void createController() {
        MegaMekGUI megaMekGUI = new MegaMekGUI();
        megaMekGUI.createController();
        controller = MegaMekGUI.getKeyDispatcher();
    }
}
