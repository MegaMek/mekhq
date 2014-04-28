/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * My own version of a UnitLoadingDialog using a progress bar
 *  based on the one in MegaMek
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import megamek.common.DefaultQuirksHandler;
import megamek.common.MechSummaryCache;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planets;

public class DataLoadingDialog extends JDialog implements PropertyChangeListener {

    /**
     * 
     */
    private static final long serialVersionUID = -3454307876761238915L;
    private JProgressBar progressBar;
    Task task;
    MekHQ app;
    JFrame frame;
    Campaign campaign;
    File fileCampaign;
    ResourceBundle resourceMap;
 
    public DataLoadingDialog(MekHQ app, JFrame frame, File f) {
        super(frame, "Data Loading"); //$NON-NLS-1$
        this.frame = frame;
        this.app = app;
        this.fileCampaign = f;
        
        resourceMap = ResourceBundle.getBundle("mekhq.resources.DataLoadingDialog");

        setUndecorated(true);
        progressBar = new JProgressBar(0, 4);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        progressBar.setVisible(true);
        progressBar.setString(resourceMap.getString("loadPlanet.text"));
        
        // initialize splash image
        Image imgSplash = getToolkit().getImage("data/images/misc/mekhq-load.png"); //$NON-NLS-1$

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            // really should never come here
        }
        // make splash image panel
        ImageIcon icon = new ImageIcon(imgSplash);
        JLabel splash = new JLabel(icon);      
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splash, BorderLayout.CENTER);
        getContentPane().add(progressBar, BorderLayout.PAGE_END);
        
        setSize(500, 350);
        this.setLocationRelativeTo(frame);
       
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }
    
    class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */    	
    	private boolean cancelled = false;
    	
        @Override
        public Void doInBackground() {
            //Initialize progress property.
            setProgress(0);
            try {
            	Faction.generateFactions();
            } catch (Exception ex) {
    			ex.printStackTrace();
            }
            while (!Planets.getInstance().isInitialized()) {
                //Sleep for up to one second.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                	
                }
            }
            setProgress(1);
            try {
                DefaultQuirksHandler.initQuirksList();
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!MechSummaryCache.getInstance().isInitialized()) {
                //Sleep for up to one second.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                	ignore.printStackTrace();
                }
            }
            setProgress(2);
    		//load in directory items and tilesets
    		app.getIconPackage().loadDirectories();
            setProgress(3);
            boolean newCampaign = false;
            if(null == fileCampaign) {
            	try {
					newCampaign = true;
					campaign = new Campaign();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } else {
            	MekHQ.logMessage("Loading campaign file from XML...");

        		// And then load the campaign object from it.
        		FileInputStream fis = null;

        		try {
        			fis = new FileInputStream(fileCampaign);
        			campaign = Campaign.createCampaignFromXMLFileInputStream(fis);
        			// Restores all transient attributes from serialized objects
        			campaign.restore();
        			fis.close();
        		} catch (NullEntityException ex) {
        			JOptionPane.showMessageDialog(null, 
        					"The following units could not be loaded by the campaign:\n" + ex.getError() + "\n\nPlease be sure to copy over any custom units before starting a new version of MekHQ.\nIf you believe the units listed are not customs, then try deleting the file data/mechfiles/units.cache and restarting MekHQ.\nIt is also possible that unit chassi and model names have changed across versions of MegaMek. You can check this by\nopening up MegaMek and searching for the units. Chassis and models can be edited in your MekHQ save file with a text editor." ,
        					"Unit Loading Error",
        					JOptionPane.ERROR_MESSAGE);
        			cancelled = true;
        			cancel(true);
        		} catch (Exception ex) {
        			ex.printStackTrace();
        			JOptionPane.showMessageDialog(null, 
        					"The campaign file could not be loaded.\nPlease check the log file for details.",
        					"Campaign Loading Error",
        				    JOptionPane.ERROR_MESSAGE);
        			//setVisible(false);
        			cancelled = true;
        			cancel(true);
        		} catch(OutOfMemoryError e) { 
        		    JOptionPane.showMessageDialog(null, 
                            "MekHQ ran out of memory attempting to load the campaign file. \nTry increasing the memory allocated to MekHQ and reloading.\nSee the FAQ at http://megamek.info for details.",
                            "Not Enough Memory",
                            JOptionPane.ERROR_MESSAGE);
                    //setVisible(false);
                    cancelled = true;
                    cancel(true);
        		}
            }
            setProgress(4);
            if(newCampaign) {
            	setVisible(false);
            	// show the date chooser
                DateChooser dc = new DateChooser(frame, campaign.calendar);
                // user can eiter choose a date or cancel by closing
                if (dc.showDateChooser() == DateChooser.OK_OPTION) {
                	campaign.calendar = dc.getDate();
                }
            	CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(frame, true, campaign, app.getIconPackage().getCamos());
            	optionsDialog.setVisible(true);
        		if(optionsDialog.wasCancelled()) {
        			cancelled = true;
        			cancel(true);
        		} else {
        			campaign.setStartingPlanet();
        			campaign.generateNewPersonnelMarket();
        			campaign.reloadNews();
        			campaign.addReport("<b>" + campaign.getDateAsString() + "</b>");
        		}
            }
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
        	setVisible(false);
        	if(!cancelled) {
        		app.setCampaign(campaign);
        		frame.setVisible(false);
        		frame.dispose();
        		app.showNewView();
        	}	
        }
    }

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		int progress = task.getProgress();
		progressBar.setValue(progress);
		switch(progress) {
		case(0):
			progressBar.setString(resourceMap.getString("loadPlanet.text"));
			break;
		case(1):
			progressBar.setString(resourceMap.getString("loadUnits.text"));
			break;
		case(2):
			progressBar.setString(resourceMap.getString("loadImages.text"));
			break;
		case(3):
			progressBar.setString(resourceMap.getString("loadCampaign.text"));
			break;
		}
	}
    
}
