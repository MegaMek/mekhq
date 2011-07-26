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

package mekhq;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.jdesktop.application.SingleFrameApplication;

import megamek.client.ui.swing.MechTileset;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.MechSummaryCache;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.Campaign;
import mekhq.campaign.Planets;

public class DataLoadingDialog extends JDialog implements PropertyChangeListener {

    /**
     * 
     */
    private static final long serialVersionUID = -3454307876761238915L;
    private JProgressBar progressBar;
    Task task;
    MekHQApp app;
    Campaign campaign;
    File fileCampaign;
    //the various directory items we need to access
	private DirectoryItems portraits;
    private DirectoryItems camos;
    private DirectoryItems forceIcons;
	protected static MechTileset mt;
 
    public DataLoadingDialog(MekHQApp app, File f) {
        super(app.getMainFrame(), "Data Loading"); //$NON-NLS-1$
        this.app = app;
        this.fileCampaign = f;
        
        setUndecorated(true);
        progressBar = new JProgressBar(0, 4);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        progressBar.setVisible(true);
        progressBar.setString("Loading Planetary Data...");
        
        // initialize splash image
        Image imgSplash = app.getMainFrame().getToolkit().getImage("data/images/misc/mekhq-load.jpg"); //$NON-NLS-1$

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(app.getMainFrame());
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
        
        setSize(500, 300);
        // move to middle of screen
        Dimension screenSize = app.getMainFrame().getToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getSize().width / 2,
                screenSize.height / 2 - getSize().height / 2);
        
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }
    
    class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            //Initialize progress property.
            setProgress(0);
            while (!Planets.getInstance().isInitialized()) {
                //Sleep for up to one second.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                	
                }
            }
            setProgress(1);
            while (!MechSummaryCache.getInstance().isInitialized()) {
                //Sleep for up to one second.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                	
                }
            }
            setProgress(2);
    		//load in directory items and tilesets
    		app.loadDirectories();
            setProgress(3);
            boolean newCampaign = false;
            if(null == fileCampaign) {
            	newCampaign = true;
            	campaign = new Campaign();
            } else {
            	MekHQApp.logMessage("Loading campaign file from XML...");

        		// And then load the campaign object from it.
        		FileInputStream fis = null;

        		try {
        			fis = new FileInputStream(fileCampaign);
        			campaign = Campaign.createCampaignFromXMLFileInputStream(fis);
        			// Restores all transient attributes from serialized objects
        			campaign.restore();
        			fis.close();
        		} catch (Exception ex) {
        			ex.printStackTrace();
        			JOptionPane.showMessageDialog(app.getMainFrame(), 
        					"The campaign file could not be loaded.\nPlease check the log file for details.",
        					"Campaign Loading Error",
        				    JOptionPane.ERROR_MESSAGE);
        			campaign = new Campaign();
        		}
            }
            setProgress(4);
            if(newCampaign) {
            	setVisible(false);
            	CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(app.getMainFrame(), true, campaign, camos);
            	optionsDialog.setVisible(true);
        		campaign.addReport("<b>" + campaign.getDateAsString() + "</b>");
            }
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            //Toolkit.getDefaultToolkit().beep();
        	app.setCampaign(campaign);
        	app.show(new MekHQView(app));
        	setVisible(false);
        }
    }

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		int progress = task.getProgress();
		progressBar.setValue(progress);
		switch(progress) {
		case(0):
			progressBar.setString("Loading Planetary Data...");
			break;
		case(1):
			progressBar.setString("Loading Unit Data...");
			break;
		case(2):
			progressBar.setString("Loading Image Data...");
			break;
		case(3):
			progressBar.setString("Loading Campaign...");
			break;
		}
	}
    
}
