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

import java.awt.FileDialog;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import megamek.client.Client;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameCFREvent;
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
import megamek.common.event.GameVictoryEvent;
import megamek.server.Server;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.StartUpGUI;
import mekhq.gui.dialog.LaunchGameDialog;
import mekhq.gui.dialog.ResolveScenarioWizardDialog;
import mekhq.gui.dialog.RetirementDefectionDialog;

/**
 * The main class of the application.
 */
public class MekHQ implements GameListener {
	//TODO: This is intended as a debug/production type thing.
	// So it should be backed down to 1 for releases...
	// It's intended for 1 to be critical, 3 to be typical, and 5 to be debug/informational.
	public static int VERBOSITY_LEVEL = 5;
	public static String CAMPAIGN_DIRECTORY = "./campaigns/";
	public static String PROPERTIES_FILE = "mmconf/mekhq.properties";
	
	//stuff related to MM games
    private Server myServer = null;
    private GameThread gameThread = null;
    private Scenario currentScenario = null;
    private Client client = null;
    
    //the actual campaign - this is where the good stuff is  
    private Campaign campaign;
    private CampaignGUI campaigngui;
    
    private IconPackage iconPackage = new IconPackage();
    
	private Properties preferences;
	
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
    
	protected static MekHQ getInstance() {
		return new MekHQ();
	}
	
    /**
     * At startup create and show the main frame of the application.
     */
    protected void startup() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQ");
        MekHQ.logMessage(resourceMap.getString("Application.name") + " " + resourceMap.getString("Application.version"));
        //read in preferences
    	readPreferences();
    	setLookAndFeel();
        //create a start up frame and display it
        StartUpGUI sud = new StartUpGUI(this);
        sud.setVisible(true);
    }
    
    public void exit() {
    	if(JOptionPane.showConfirmDialog(null,
                "Do you really want to quit MekHQ?",
                "Quit?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) ==
                JOptionPane.YES_OPTION) {
    		if(null != campaigngui) {
        		campaigngui.getFrame().dispose();
        	}
    		savePreferences();
        	System.exit(0);
    	} 	
    }
    
    public void showNewView() {
    	campaigngui = new CampaignGUI(this);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
    	System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name","MekHQ");  
        //redirect output to log file
        redirectOutput();
        MekHQ.getInstance().startup();
    }
    
    protected static Properties setDefaultPreferences() {
    	Properties defaults = new Properties();
    	defaults.setProperty("laf", UIManager.getSystemLookAndFeelClassName());
    	return defaults;
    }
    
    protected void readPreferences() {
    	preferences = new Properties(setDefaultPreferences());
        try {
            preferences.load(new FileInputStream(PROPERTIES_FILE));
            MekHQ.logMessage("loading mekhq properties from " + PROPERTIES_FILE);
        } catch (FileNotFoundException e) {
            MekHQ.logMessage("No mekhq properties file found. Reverting to defaults.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void savePreferences() {
    	preferences.setProperty("laf", UIManager.getLookAndFeel().getClass().getName());
    	try {
			preferences.store(new FileOutputStream(PROPERTIES_FILE), "MekHQ Preferences");
		} catch (FileNotFoundException e) {
			MekHQ.logMessage("could not save preferences to " + PROPERTIES_FILE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    protected void setLookAndFeel() {
    	//TODO: we can extend this with other look and feel options
    	try {
    		UIManager.setLookAndFeel(preferences.getProperty("laf"));
    	} catch (ClassNotFoundException e) {
    		e.printStackTrace();
    	} catch (InstantiationException e) {
    		e.printStackTrace();
    	} catch (IllegalAccessException e) {
    		e.printStackTrace();
    	} catch (UnsupportedLookAndFeelException e) {
        	e.printStackTrace();
        }		
    }
    
    /**
     * This function redirects the standard error and output streams to the
     * given File name.
     *
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
    
    public void joinGame(Scenario scenario, ArrayList<Unit> meks) {
		LaunchGameDialog lgd = new LaunchGameDialog(campaigngui.getFrame(), false, campaign);
		lgd.setVisible(true);

    	try {
    		client = new Client(lgd.playerName, lgd.serverAddr, lgd.port);
        } catch (Exception ex) {
        	MekHQ.logMessage("Failed to connect to server properly");
			MekHQ.logError(ex);
            return;
        }

    	client.getGame().addGameListener(this);
        currentScenario = scenario;
        
        //Start the game thread
        gameThread = new GameThread(lgd.playerName, client, this, meks, false);
        gameThread.start();
    }
    
    public void startHost(Scenario scenario, boolean loadSavegame, ArrayList<Unit> meks) {
    	LaunchGameDialog lgd = new LaunchGameDialog(campaigngui.getFrame(), true, campaign);
		lgd.setVisible(true);

    	try {
            myServer = new Server("", lgd.port);
            if (loadSavegame) {
                FileDialog f = new FileDialog(campaigngui.getFrame(), "Load Savegame");
                f.setDirectory(System.getProperty("user.dir") + "/savegames");
                f.setVisible(true);
                myServer.loadGame(new File(f.getDirectory(), f.getFile()));
            }
        } catch (Exception ex) {
        	MekHQ.logMessage("Failed to start up server properly");
			MekHQ.logError(ex);
            return;
        }
        
        client = new Client(lgd.playerName, "127.0.0.1", lgd.port);

        client.getGame().addGameListener(this);
        currentScenario = scenario;
        
        //Start the game thread
        if (campaign.getCampaignOptions().getUseAtB() && scenario instanceof AtBScenario) {
        	gameThread = new AtBGameThread(lgd.playerName, client, this, meks, (AtBScenario)scenario);
        } else {
        	gameThread = new GameThread(lgd.playerName, client, this, meks);
        }
        gameThread.start();
    }

    // Stop & send the close game event to the Server
    public synchronized void stopHost() {
       if(null != myServer) {
    	   //myServer.getGame().removeGameListener(this);
    	   myServer.die();
    	   myServer = null;
       }
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
	}
	
	@Override
	public void gameVictory(GameVictoryEvent e) {
		if (gameThread.stopRequested()) {
			return;
		}
		try {
        	boolean control = JOptionPane.showConfirmDialog(campaigngui.getFrame(),
                    "Did your side control the battlefield at the end of the scenario?",
                    "Control of Battlefield?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) ==
                    JOptionPane.YES_OPTION;
        	ResolveScenarioTracker tracker = new ResolveScenarioTracker(currentScenario, campaign, control);
            tracker.setClient(gameThread.getClient());
        	tracker.processGame();
        	ResolveScenarioWizardDialog resolveDialog = new ResolveScenarioWizardDialog(campaigngui.getFrame(), true, tracker);
        	resolveDialog.setVisible(true);
        	if (campaigngui.getCampaign().getCampaignOptions().getUseAtB() &&
        			campaign.getMission(currentScenario.getMissionId()) instanceof AtBContract &&
        			campaigngui.getCampaign().getRetirementDefectionTracker().getRetirees().size() > 0) {
        		RetirementDefectionDialog rdd = new RetirementDefectionDialog(campaigngui,
        				(AtBContract)campaign.getMission(currentScenario.getMissionId()), false);
        		rdd.setVisible(true);
        		if (!rdd.wasAborted()) {
        			getCampaign().applyRetirement(rdd.totalPayout(), rdd.getUnitAssignments());
        		}
        	}
        	gameThread.requestStop();
            /*Megamek dumps these in the deployment phase to free memory*/
            if (getCampaign().getCampaignOptions().getUseAtB()) {
                megamek.client.RandomUnitGenerator.getInstance();
                megamek.client.RandomNameGenerator.getInstance();
            }
            campaigngui.refreshScenarioList();
            campaigngui.refreshOrganization();
            campaigngui.refreshServicedUnitList();
            campaigngui.refreshUnitList();
            campaigngui.refreshPersonnelList();
            campaigngui.filterPersonnel();
            campaigngui.refreshDoctorsList();
            campaigngui.refreshPatientList();
            campaigngui.refreshReport();
            campaigngui.changeMission();
            campaigngui.refreshFinancialTransactions();

        }// end try
        catch (Exception ex) {
            logError(ex);
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

	public void gameClientFeedbackRquest(GameCFREvent e) {
		// TODO Auto-generated method stub
		
	}
	
	public IconPackage getIconPackage() {
	    return iconPackage;
	}
}
