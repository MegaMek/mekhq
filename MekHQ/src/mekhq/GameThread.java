/*
 * Copyright (C) 2011-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.sentry.Sentry;
import jakarta.annotation.Nullable;
import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.CloseClientListener;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.princess.PrincessException;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.IClientGUI;
import megamek.client.ui.clientGUI.IDisconnectSilently;
import megamek.client.ui.clientGUI.ILocalBots;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.util.MegaMekController;
import megamek.common.Player;
import megamek.common.loaders.MapSettings;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.common.weapons.handlers.WeaponOrderHandler;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.scenarios.BotForce;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ScenarioUtils;

class GameThread extends Thread implements CloseClientListener {
    private static final MMLogger LOGGER = MMLogger.create(GameThread.class);

    // region Variable Declarations
    protected String myName;
    protected String password;
    protected Client client;
    protected IClientGUI swingGui;
    protected ILocalBots localBots;
    protected MegaMekController controller;
    protected MekHQ app;
    protected Campaign campaign;
    protected boolean started;

    protected List<Unit> units;

    /** The behavior for the Princess bot that plays the player's side under Auto Resolve, or null to play normally. */
    protected final @Nullable BehaviorSettings autoResolveBehaviorSettings;

    protected volatile boolean stop = false;

    /** Ensures {@link #clientClosed()} triggers the host teardown at most once for this thread. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final Scenario scenario;
    // endregion Variable Declarations

    // region Constructors

    /**
     * GameThread
     * <p>
     * Initializes a new thread for a game.
     * </p>
     *
     * @param name     The player name
     * @param password The game password
     * @param client   The client
     * @param app      The MekHQ instance
     * @param units    The list of units you intend to play with in your side
     * @param scenario The scenario that is going to be initialized for the game
     */
    public GameThread(String name, String password, Client client, MekHQ app, List<Unit> units, Scenario scenario) {
        this(name, password, client, app, units, scenario, true);
    }


    /**
     * GameThread
     * <p>
     * Initializes a new thread for a game.
     * </p>
     *
     * @param name     The player name
     * @param client   The client
     * @param app      The MekHQ instance
     * @param units    The list of units you intend to play with in your side
     * @param scenario The scenario that is going to be initialized for the game
     * @param started  Whether the game has already started
     */
    public GameThread(String name, Client client, MekHQ app, List<Unit> units, Scenario scenario, boolean started) {
        this(name, "", client, app, units, scenario, started);
    }


    /**
     * GameThread
     * <p>
     * Initializes a new thread for a game.
     * </p>
     *
     * @param name     The player name
     * @param password The game password
     * @param client   The client
     * @param app      The MekHQ instance
     * @param units    The list of units you intend to play with in your side
     * @param scenario The scenario that is going to be initialized for the game
     * @param started  Whether the game has already started
     */
    public GameThread(String name, String password, Client client, MekHQ app, List<Unit> units, Scenario scenario,
          boolean started) {
        this(name, password, client, app, units, scenario, started, null);
    }

    /**
     * @param autoResolveBehaviorSettings the behavior for a Princess bot that plays the player's side, for Auto
     *                                    Resolve, or {@code null} for the player to play the game themselves
     */
    public GameThread(String name, String password, Client client, MekHQ app, List<Unit> units, Scenario scenario,
          boolean started, @Nullable BehaviorSettings autoResolveBehaviorSettings) {
        super(name);
        this.autoResolveBehaviorSettings = autoResolveBehaviorSettings;
        myName = name.trim();
        this.password = password;
        this.client = client;
        this.app = app;
        this.started = started;
        this.campaign = app.getCampaign();
        this.units = inTOEOrder(Objects.requireNonNull(units), campaign);
        this.scenario = Objects.requireNonNull(scenario);
    }
    // endregion Constructors

    /**
     * Returns the units in the order the TO&amp;E lists them: each formation's own units, then its sub formations,
     * depth first. MegaMek builds its force tree in the order units arrive, so sending them this way makes the lobby's
     * Force View match the TO&amp;E. Units outside the TO&amp;E come last, in their given order.
     *
     * @param units    the units to deploy, in any order
     * @param campaign the campaign whose TO&amp;E sets the order
     *
     * @return a new list holding the same units in TO&amp;E order
     */
    static List<Unit> inTOEOrder(List<Unit> units, Campaign campaign) {
        Map<UUID, Integer> toePositions = new HashMap<>();
        List<UUID> toeUnitIds = campaign.getPlayerForce().getAllUnitsInTheTOE(false);
        for (int index = 0; index < toeUnitIds.size(); index++) {
            toePositions.putIfAbsent(toeUnitIds.get(index), index);
        }
        List<Unit> result = new ArrayList<>(units);
        result.sort(Comparator.comparingInt(unit -> toePositions.getOrDefault(unit.getId(), Integer.MAX_VALUE)));
        return result;
    }

    public Client getClient() {
        return client;
    }

    protected Map<String, AbstractClient> getLocalBots() {
        if (localBots == null) {
            return Collections.emptyMap();
        }
        return localBots.getLocalBots();
    }

    @Override
    public void run() {
        client.addCloseClientListener(this);

        if (!getLocalBots().isEmpty()) {
            for (AbstractClient client2 : getLocalBots().values()) {
                client2.die();
            }
            getLocalBots().clear();
        }

        createController();
        swingGui = new ClientGUI(client, controller);
        controller.clientGUI = swingGui;
        localBots = (ClientGUI) swingGui;
        swingGui.initialize();

        try {
            client.connect();
        } catch (Exception ex) {
            Sentry.captureException(ex);
            LOGGER.error("MegaMek client failed to connect to server", ex);
            return;
        }

        try {
            while (client.getLocalPlayer() == null) {
                Thread.sleep(MekHQ.getMHQOptions().getStartGameClientDelay());
            }

            // if game is running, shouldn't do the following, so detect the phase
            for (int i = 0;
                  (i < MekHQ.getMHQOptions().getStartGameClientRetryCount()) && client.getGame().getPhase().isUnknown();
                  i++) {
                Thread.sleep(MekHQ.getMHQOptions().getStartGameClientDelay());
                LOGGER.warn("Client has not finished initialization, and is currently in an unknown phase.");
            }

            if ((client.getGame() != null) && client.getGame().getPhase().isLounge()) {
                LOGGER.info("Thread in lounge");
                client.getLocalPlayer().setCamouflage(app.getCampaign().getPlayerForce().getCamouflage().clone());

                if (started) {
                    client.getGame().getOptions().loadOptions();
                    client.sendGameOptions(password, app.getCampaign().getGameOptionsVector());
                    Thread.sleep(MekHQ.getMHQOptions().getStartGameDelay());
                }

                MapSettings mapSettings = ScenarioUtils.getMapSettings(scenario);
                client.sendMapSettings(mapSettings);
                Thread.sleep(MekHQ.getMHQOptions().getStartGameDelay());

                client.sendPlanetaryConditions(getPlanetaryConditions());
                Thread.sleep(MekHQ.getMHQOptions().getStartGameDelay());

                // set player deployment
                client.getLocalPlayer().setStartingPos(scenario.getStartingPos());
                client.getLocalPlayer().setStartOffset(scenario.getStartOffset());
                client.getLocalPlayer().setStartWidth(scenario.getStartWidth());
                client.getLocalPlayer().setStartingAnyNWx(scenario.getStartingAnyNWx());
                client.getLocalPlayer().setStartingAnyNWy(scenario.getStartingAnyNWy());
                client.getLocalPlayer().setStartingAnySEx(scenario.getStartingAnySEx());
                client.getLocalPlayer().setStartingAnySEy(scenario.getStartingAnySEy());
                client.getLocalPlayer().setConstantInitBonus(campaign.getPlayerForce().getInitiativeBonus());

                client.getLocalPlayer().setTeam(1);

                var entities = new ArrayList<Entity>();
                for (Unit unit : units) {
                    Entity entity = unit.getEntity();
                    // Set the TempID for auto reporting
                    entity.setExternalIdAsString(unit.getId().toString());
                    entity.setOwner(client.getLocalPlayer());
                    Formation formation = campaign.getPlayerForce().getFormationFor(unit);
                    entity.setForceString(formation.getFullMMName());
                    entities.add(entity);
                }
                client.sendAddEntity(entities);
                client.sendPlayerInfo();

                // Add bots
                for (int i = 0; i < scenario.getNumBots(); i++) {
                    BotForce bf = scenario.getBotForce(i);
                    String name = bf.getName();
                    if (getLocalBots().containsKey(name)) {
                        int append = 2;
                        while (getLocalBots().containsKey(name + append)) {
                            append++;
                        }
                        name += append;
                    }
                    Princess botClient = new Princess(name, client.getHost(), client.getPort());
                    botClient.setBehaviorSettings(bf.getBehaviorSettings());
                    try {
                        botClient.connect();
                        botClient.startPrecognition();
                    } catch (Exception e) {
                        LOGGER.error(e, "Could not connect with Bot name {}", bf.getName());
                    }
                    getLocalBots().put(name, botClient);

                    // chill out while bot is created and connects to megamek
                    Thread.sleep(MekHQ.getMHQOptions().getStartGameDelay());
                    configureBot(botClient, bf);
                }

                // Auto Resolve hands the player's units to a Princess bot on the player's team
                if (autoResolveBehaviorSettings != null) {
                    BotClient autoResolveBot = setupPlayerBotForAutoResolve(client.getLocalPlayer());
                    getLocalBots().put(autoResolveBot.getName(), autoResolveBot);
                    for (AbstractClient localBot : getLocalBots().values()) {
                        if (localBot instanceof BotClient botClient) {
                            botClient.sendDone(true);
                        }
                    }
                }
            }

            while (!stop) {
                Thread.sleep(50);
            }
        } catch (Exception e) {
            Sentry.captureException(e);
            LOGGER.error("", e);
        } finally {
            disconnectGuiSilently();
            client.die();
            client = null;
            swingGui = null;
            controller = null;
        }
    }

    protected void disconnectGuiSilently() {
        if (swingGui != null && swingGui instanceof IDisconnectSilently disconnectSilently) {
            disconnectSilently.setDisconnectQuietly(true);
        }
    }

    /**
     * wait for the server to add the bot client, then send starting position, camo, and entities
     *
     * @param botClient a BotClient to manage the bot
     * @param botForce  a BotForce that will send its info and entities to the botClient
     */
    private void configureBot(BotClient botClient, BotForce botForce) {
        try {
            // Wait for the server to add the bot client, but allow a timeout rather than
            // blocking
            int retryCount = 0;
            while ((botClient.getLocalPlayer() == null) &&
                         (retryCount++ < MekHQ.getMHQOptions().getStartGameBotClientRetryCount())) {
                try {
                    Thread.sleep(MekHQ.getMHQOptions().getStartGameBotClientDelay());
                } catch (Exception ignored) {

                }
            }

            if (botClient.getLocalPlayer() == null) {
                LOGGER.error("Could not configure bot {}", botClient.getName());
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
            LOGGER.error("", ex);
        }
    }

    protected List<Entity> setupBotEntities(BotClient botClient, BotForce botForce, Scenario scenario) {
        String forceName = botClient.getLocalPlayer().getName() + "|1";
        var entities = new ArrayList<Entity>();
        botForce.generateRandomForces(units, campaign);
        PlanetaryConditions conditions = getPlanetaryConditions();
        for (Entity entity : botForce.getFullEntityList(campaign)) {
            AtBDynamicScenarioFactory.ensureInfantryProtection(entity, conditions);
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

    protected PlanetaryConditions getPlanetaryConditions() {
        return campaign.getCurrentPlanetaryConditions(scenario);
    }

    /*
     * from megamek.client.CloseClientListener clientClosed() Thanks to MM for
     * adding the listener. And to MMNet for the poorly documented code change.
     */
    @Override
    public void clientClosed() {
        // Tear the host down at most once per game thread. A thread's client can be die()d twice - once when MekHQ
        // tears the game down, and again from this thread's own run() finally block - and each die() fires this
        // callback. Without this guard, a previous scenario's thread finishing after a new scenario has started could
        // call app.stopHost() and tear down the new host/client/server. See issue #9959.
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        requestStop();
        app.stopHost();
    }

    public void requestStop() {
        PreferenceManager.getInstance().save();
        try {
            WeaponOrderHandler.saveWeaponOrderFile();
        } catch (IOException e) {
            Sentry.captureException(e);
            LOGGER.error("Error saving custom weapon orders!", e);
        }
        stop = true;
    }

    public boolean stopRequested() {
        return stop;
    }

    public void quit() {
        client.die();
        client = null;// explicit null of the MM client. Wasn't/isn't being
        System.gc();
    }

    public void createController() {
        MegaMekGUI megaMekGUI = new MegaMekGUI();
        megaMekGUI.createController();
        controller = MegaMekGUI.getKeyDispatcher();
    }

    /**
     * Adds a Princess bot to play the player's side for Auto Resolve, and hands all the player's units to it. The bot
     * takes the player's name with "@AI" on the end, and the player's team, deployment zone, colour and camouflage.
     *
     * @param player the player whose units the bot is to command
     *
     * @return the bot
     */
    protected BotClient setupPlayerBotForAutoResolve(Player player) throws InterruptedException, PrincessException {
        var botName = player.getName() + "@AI";

        Thread.sleep(MekHQ.getMHQOptions().getStartGameBotClientDelay());
        var botClient = new Princess(botName, client.getHost(), client.getPort());
        botClient.setBehaviorSettings(autoResolveBehaviorSettings.getCopy());
        try {
            botClient.connect();
            botClient.startPrecognition();
            Thread.sleep(MekHQ.getMHQOptions().getStartGameBotClientDelay());
        } catch (Exception e) {
            LOGGER.error(e, "Could not connect with Bot name {}", botName);
        }

        var retryCount = MekHQ.getMHQOptions().getStartGameBotClientRetryCount();
        while (botClient.getLocalPlayer() == null) {
            Thread.sleep(MekHQ.getMHQOptions().getStartGameBotClientDelay());
            retryCount--;
            if (retryCount <= 0) {
                break;
            }
        }
        if (retryCount <= 0) {
            LOGGER.error("Could not connect with Bot name {}", botName);
        }
        botClient.getLocalPlayer().setName(botName);
        botClient.getLocalPlayer().setStartingPos(player.getStartingPos());
        botClient.getLocalPlayer().setStartOffset(player.getStartOffset());
        botClient.getLocalPlayer().setStartWidth(player.getStartWidth());
        botClient.getLocalPlayer().setStartingAnyNWx(player.getStartingAnyNWx());
        botClient.getLocalPlayer().setStartingAnyNWy(player.getStartingAnyNWy());
        botClient.getLocalPlayer().setStartingAnySEx(player.getStartingAnySEx());
        botClient.getLocalPlayer().setStartingAnySEy(player.getStartingAnySEy());
        botClient.getLocalPlayer().setCamouflage(player.getCamouflage().clone());
        botClient.getLocalPlayer().setColour(player.getColour());
        botClient.getLocalPlayer().setTeam(player.getTeam());
        botClient.sendPlayerInfo();
        Thread.sleep(MekHQ.getMHQOptions().getStartGameBotClientDelay());

        handOverPlayerUnitsToBot(client, botClient);
        Thread.sleep(MekHQ.getMHQOptions().getStartGameBotClientDelay());
        return botClient;
    }

    /**
     * Gives every unit the player owns to their auto-resolve bot, asking over the player's own connection.
     *
     * <p>The server only lets a sender give away units it already owns (MegaMek issue #8860). The request used to go
     * out over the bot's connection, so it read as a bot taking a human's units and was refused. The units then stayed
     * with the Commander window, which never plays a turn, and the game sat at "Preparing..." for good (issue #8989).
     * Sent by the player, the same request is a player giving their own units to a bot, which is allowed.</p>
     *
     * @param playerClient the player's own client, which owns the units being handed over
     * @param botClient    the bot that is to fight with them
     */
    static void handOverPlayerUnitsToBot(Client playerClient, BotClient botClient) {
        Player player = playerClient.getLocalPlayer();
        Player botPlayer = botClient.getLocalPlayer();
        List<Entity> playerEntities = playerClient.getEntitiesVector()
                                            .stream()
                                            .filter(entity -> entity.getOwnerId() == player.getId())
                                            .collect(Collectors.toList());
        if (playerEntities.isEmpty()) {
            LOGGER.warn("[AutoResolve] {} has no units to hand to {}; the bot will have nothing to command",
                  player.getName(), botPlayer.getName());
            return;
        }
        LOGGER.info("[AutoResolve] {} hands {} unit(s) to {} over the player's own connection",
              player.getName(), playerEntities.size(), botPlayer.getName());
        playerClient.sendChangeOwner(playerEntities, botPlayer.getId());
    }
}
