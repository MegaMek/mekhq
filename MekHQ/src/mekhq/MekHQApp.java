/*
 * MekBayApp.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

import megamek.common.IGame;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityNewOffboardEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameMapQueryEvent;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerConnectedEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.server.Server;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.Unit;
import mekhq.campaign.mission.Scenario;

/**
 * The main class of the application.
 */
public class MekHQApp extends SingleFrameApplication implements GameListener {
	//TODO: This is intended as a debug/production type thing.
	// So it should be backed down to 1 for releases...
	// It's intended for 1 to be critical, 3 to be typical, and 5 to be debug/informational.
	public static int VERBOSITY_LEVEL = 5;
	
    private Server myServer = null;
    private Scenario currentScenario = null;
    
    private Campaign campaign;
    
	
	/**
	 * Designed to centralize output and logging.
	 * Purely a pass-through to the version with a log level.
	 * Default to log level 3.
	 * 
	 * @param msg The message you want to log.
	 */
	public static void logMessage(String msg) {
		logMessage(msg, 3);
	}
	
	/**
	 * Designed to centralize output and logging.
	 * 
	 * @param msg The message you want to log.
	 * @param logLevel The log level of the message.
	 */
	public static void logMessage(String msg, int logLevel) {
		if (logLevel <= VERBOSITY_LEVEL)
			System.out.println(msg);
	}

	public static void logError(String err) {
		System.err.println(err);
	}
	
	public static void logError(Exception ex) {
		System.err.println(ex);
		ex.printStackTrace();
	}
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        
        //redirect output to log file
        redirectOutput();
        
        addExitListener(new ExitListener() {
        	@Override
            public boolean canExit(EventObject e) {
                boolean bOkToExit = false;
                Component source = (Component) e.getSource();
                bOkToExit = JOptionPane.showConfirmDialog(source,
                                "Do you really want to quit MekHQ?",
                                "Quit?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE) ==
                                JOptionPane.YES_OPTION;
                return bOkToExit;
            }
        	@Override
            public void willExit(EventObject event) {

            }
        });
        
        show(new StartUpDialog(this));

    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MekBayApp
     */
    public static MekHQApp getApplication() {
        return Application.getInstance(MekHQApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {

    	System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name","MekHQ");
        
        // this should also help to make MekHQ look more system-specific
        try {
        	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        } catch (InstantiationException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        } catch (IllegalAccessException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        launch(MekHQApp.class, args);
        
    }
    
    /**
     * This function redirects the standard error and output streams to the
     * given File name.
     *
     * @param logFileName The file name to redirect to.
     */
    private static void redirectOutput() {
        try {
            System.out.println("Redirecting output to mekhqlog.txt"); //$NON-NLS-1$
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream("logs" + File.separator + "mekhqlog.txt"), 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception e) {
            System.err.println("Unable to redirect output to mekhqlog.txt"); //$NON-NLS-1$
            e.printStackTrace();
        }
    }
    
    public Server getMyServer() {
    	return myServer;
    }
    
    public Campaign getCampaign() {
    	return campaign;
    }
    
    public void setCampaign(Campaign c) {
    	campaign = c;
    }
    
    public void startHost(Scenario scenario, boolean loadSavegame, ArrayList<Unit> meks) {

        try {
            myServer = new Server("", 2346);
            if (loadSavegame) {
                FileDialog f = new FileDialog(getMainFrame(), "Load Savegame");
                f.setDirectory(System.getProperty("user.dir") + "/savegames");
                f.setVisible(true);
                myServer.loadGame(new File(f.getDirectory(), f.getFile()));
            }
        } catch (Exception ex) {
        	MekHQApp.logMessage("Failed to start up server properly");
			MekHQApp.logError(ex);
            return;
        }

        myServer.getGame().addGameListener(this);
        currentScenario = scenario;
        //Start the game thread
        GameThread MMGameThread = new GameThread(campaign.getName(), "", "127.0.0.1", 2346, campaign, meks);
        MMGameThread.start();
    }

    // Stop & send the close game event to the Server
    public void stopHost() {

       /* serverSend("CG");// send close game to server
        try {
            myServer.die();
        } catch (Exception ex) {
            CampaignData.mwlog.errLog(ex);
            CampaignData.mwlog.errLog("Megamek Error:");
        }
        */
        myServer = null;
        currentScenario = null;
    }

	@Override
	public void gameBoardChanged(GameBoardChangeEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameBoardNew(GameBoardNewEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameEnd(GameEndEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameEntityChange(GameEntityChangeEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameEntityNew(GameEntityNewEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameEntityRemove(GameEntityRemoveEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameMapQuery(GameMapQueryEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameNewAction(GameNewActionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gamePhaseChange(GamePhaseChangeEvent e) {
		try {

            if (myServer.getGame().getPhase() == IGame.Phase.PHASE_VICTORY) {
            	ResolveScenarioTracker tracker = new ResolveScenarioTracker(currentScenario, campaign);
            	tracker.processGame(myServer.getGame());
            	ResolveWizardMissingUnitsDialog resolveDialog = new ResolveWizardMissingUnitsDialog(getMainFrame(), true, tracker);
            	show(resolveDialog);
            }

        }// end try
        catch (Exception ex) {
            
        }
	}

	@Override
	public void gamePlayerChange(GamePlayerChangeEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gamePlayerChat(GamePlayerChatEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gamePlayerConnected(GamePlayerConnectedEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameReport(GameReportEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameSettingsChange(GameSettingsChangeEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameTurnChange(GameTurnChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
}
