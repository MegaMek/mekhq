/*
 * ClientThread - Copyright (C) 2011
 *
 * Derived from MekWars (http://www.sourceforge.net/projects/mekwars)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package mekhq;

import java.awt.KeyboardFocusManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import megamek.client.Client;
import megamek.client.CloseClientListener;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.KeyBindParser;
import megamek.common.QuirksHandler;
import megamek.common.WeaponOrderHandler;
import megamek.common.preference.PreferenceManager;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;

class GameThread extends Thread implements CloseClientListener {
    //region Variable Declarations
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
    //endregion Variable Declarations

    //region Constructors
    public GameThread(String name, String password, Client c, MekHQ app, List<Unit> units) {
        this(name, password, c, app, units, true);
    }

    public GameThread(String name, Client c, MekHQ app, List<Unit> units, boolean started) {
        this(name, "", c, app, units, started);
    }

    public GameThread(String name, String password, Client c, MekHQ app, List<Unit> units, boolean started) {
        super(name);
        myname = name.trim();
        this.password = password;
        this.client = c;
        this.app = app;
        this.units = units;
        this.started = started;
        this.campaign = app.getCampaign();
    }
    //endregion Constructors

    public Client getClient() {
        return client;
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
                MekHQ.getLogger().error("Thread in unknown stage" );
            }

            if (((client.getGame() != null) && (client.getGame().getPhase() == IGame.Phase.PHASE_LOUNGE))) {
                MekHQ.getLogger().info("Thread in lounge" );
                client.getLocalPlayer().setCamouflage(app.getCampaign().getCamouflage().clone());

                if (started) {
                    client.getGame().getOptions().loadOptions();
                    client.sendGameOptions(password, app.getCampaign().getGameOptionsVector());
                    Thread.sleep(MekHQ.getMekHQOptions().getStartGameDelay());
                }

                var entities = new ArrayList<Entity>();
                for (Unit unit : units) {
                    Entity entity = unit.getEntity();
                    // Set the TempID for autoreporting
                    entity.setExternalIdAsString(unit.getId().toString());
                    entity.setOwner(client.getLocalPlayer());
                    Force force = campaign.getForceFor(unit);
                    entity.setForceString(force.getFullMMName());
                    entities.add(entity);
                }
                client.sendAddEntity(entities);
                client.sendPlayerInfo();
            }

            while(!stop) {
                Thread.sleep(50);
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
        finally {
            client.die();
            client = null;
            swingGui = null;
            controller = null;
        }
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
            MekHQ.getLogger().error("Error saving custom weapon orders!", e);
        }

        try {
            QuirksHandler.saveCustomQuirksList();
        } catch (IOException e) {
            MekHQ.getLogger().error("Error saving quirks override!", e);
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
        controller = new MegaMekController();
        KeyboardFocusManager kbfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kbfm.addKeyEventDispatcher(controller);

        KeyBindParser.parseKeyBindings(controller);
    }
}
