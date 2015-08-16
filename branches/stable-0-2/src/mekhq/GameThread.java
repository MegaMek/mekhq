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
import mekhq.campaign.Unit;

class GameThread extends Thread implements CloseClientListener {

    // VARIABLES
    private String myname;
    private Client client;
    private ClientGUI swingGui;
    private MekHQ app;

    private ArrayList<Unit> mechs = new ArrayList<Unit>();

    private volatile boolean stop = false;
    
    // CONSTRUCTOR
    public GameThread(String name, Client c, MekHQ app, ArrayList<Unit> mechs) {
        super(name);
        myname = name.trim();
        this.client = c;
        this.app = app;
        this.mechs = mechs;
    }

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
        swingGui = new ClientGUI(client);
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
            for (int i = 0; (i < 1000) && (client.game.getPhase() == IGame.Phase.PHASE_UNKNOWN); i++) {
                Thread.sleep(50);
            	System.out.println("Thread in unknown stage" );
            }

            if (((client.game != null) && (client.game.getPhase() == IGame.Phase.PHASE_LOUNGE))) {
            	System.out.println("Thread in lounge" );
            	client.getLocalPlayer().setCamoCategory(app.getCampaign().getCamoCategory());
                client.getLocalPlayer().setCamoFileName(app.getCampaign().getCamoFileName());
            	
                client.game.getOptions().loadOptions();
                client.sendGameOptions("", app.getCampaign().getGameOptionsVector());
                for (Unit unit : mechs) {
                    // Get the Entity
                    Entity entity = unit.getEntity();
                    // Set the TempID for autoreporting
                    entity.setExternalIdAsString(unit.getId().toString());
                    // Set the owner
                    entity.setOwner(client.getLocalPlayer()); 
                    // Add Mek to game
                    client.sendAddEntity(entity);
                    // Wait a few secs to not overuse bandwith
                    Thread.sleep(125);
                }

                client.sendPlayerInfo();

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
        }
    }

    /*
     * from megamek.client.CloseClientListener clientClosed() Thanks to MM for
     * adding the listener. And to MMNet for the poorly documented code change.
     */
    public void clientClosed() {
    	requestStop();
    	app.stopHost();
    }
    
    public void requestStop() {
    	PreferenceManager.getInstance().save();
    	stop = true;
    }
    
    public void quit() {
    	client.die();
        client = null;// explicit null of the MM client. Wasn't/isn't being
        // GC'ed.
        System.gc();
    }

}