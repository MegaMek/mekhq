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

import java.util.ArrayList;

import megamek.client.Client;
import megamek.client.CloseClientListener;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.preference.PreferenceManager;
import mekhq.campaign.Campaign;
import mekhq.campaign.Unit;

class GameThread extends Thread implements CloseClientListener {

    // VARIABLES
    private String myname;
    private String serverip;
    private String serverName;
    private int serverport;
    private Client client;
    private ClientGUI swingGui;
    private Campaign campaign;

    private ArrayList<Unit> mechs = new ArrayList<Unit>();

    // CONSTRUCTOR
    public GameThread(String name, String servername, String ip, int port, Campaign c, ArrayList<Unit> mechs) {
        super(name);
        myname = name.trim();
        serverName = servername;
        serverip = ip;
        serverport = port;
        campaign = c;
        this.mechs = mechs;
        if (serverip.indexOf("127.0.0.1") != -1) {
            serverip = "127.0.0.1";
        }
    }

    public Client getClient() {
        return client;
    }

    @Override
    public void run() {
        client = new Client(myname, serverip, serverport);
        client.addCloseClientListener(this);

        if (swingGui != null) {
        	for (Client client2 : swingGui.getBots().values()) {
        		client2.die();
        	}
        	swingGui.getBots().clear();
        }
        swingGui = new ClientGUI(client);
        swingGui.initialize();

        try {
            client.connect();
        } catch (Exception ex) {
            client = null;
			MekHQApp.logMessage("MegaMek client failed to connect to server");
			MekHQApp.logError(ex);
            return;
        }

        try {
            while (client.getLocalPlayer() == null) {
                Thread.sleep(50);
            }

            // if game is running, shouldn't do the following, so detect the
            // phase
            for (int i = 0; (i < 1000) && (client.game.getPhase() == IGame.Phase.PHASE_UNKNOWN); i++) {
                Thread.sleep(50);
            }

            if (((client.game != null) && (client.game.getPhase() == IGame.Phase.PHASE_LOUNGE))) {
            	
            	client.getLocalPlayer().setCamoCategory(campaign.getCamoCategory());
                client.getLocalPlayer().setCamoFileName(campaign.getCamoFileName());
            	
                client.game.getOptions().loadOptions();
                client.sendGameOptions("", campaign.getGameOptionsVector());

                for (Unit unit : mechs) {
                    // Get the Entity
                    Entity entity = unit.getEntity();
                    // Set the TempID for autoreporting
                    entity.setExternalId(unit.getId());
                    // Set the owner
                    entity.setOwner(client.getLocalPlayer()); 
                    // Add Mek to game
                    client.sendAddEntity(entity);
                    // Wait a few secs to not overuse bandwith
                    Thread.sleep(125);
                }

                client.sendPlayerInfo();

            }

        } catch (Exception e) {
			MekHQApp.logError(e);
        }
    }

    /*
     * from megamek.client.CloseClientListener clientClosed() Thanks to MM for
     * adding the listener. And to MMNet for the poorly documented code change.
     */
    public void clientClosed() {

        PreferenceManager.getInstance().save();

        /*
        if (bot != null) {
            bot.die();
            bot = null;
        }
        */

       // client.die();
        client = null;// explicit null of the MM client. Wasn't/isn't being
        // GC'ed.
       // mwclient.closingGame(serverName);
        System.gc();

    }

}