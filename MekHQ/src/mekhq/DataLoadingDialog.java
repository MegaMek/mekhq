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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.jdesktop.application.SingleFrameApplication;

import megamek.common.MechSummaryCache;
import mekhq.campaign.Campaign;
import mekhq.campaign.Planets;

public class DataLoadingDialog extends JDialog implements PropertyChangeListener {

    /**
     * 
     */
    private static final long serialVersionUID = -3454307876761238915L;
    private JProgressBar progressBar;
    private JLabel progressLbl;
    Task task;
    SingleFrameApplication app;
    Campaign campaign;
 
    public DataLoadingDialog(SingleFrameApplication app) {
        super(app.getMainFrame(), "Data Loading"); //$NON-NLS-1$
        this.app = app;
        
        progressBar = new JProgressBar(0, 3);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        progressBar.setVisible(true);
        progressLbl = new JLabel("Loading Planetary Data...");
        getContentPane().setLayout(new GridLayout(2, 1));
        getContentPane().add(progressLbl);
        getContentPane().add(progressBar);
        
        setSize(250, 130);
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
            campaign = new Campaign();
            setProgress(3);
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            //Toolkit.getDefaultToolkit().beep();
        	setVisible(false);
        	app.show(new MekHQView(app, campaign));
        }
    }

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
	     progressBar.setValue(task.getProgress());
	     if(Planets.getInstance().isInitialized()) {
	    	 if(MechSummaryCache.getInstance().isInitialized()) {
		    	 progressLbl.setText("Loading Campaign...");
		     } else {
		    	 progressLbl.setText("Loading Unit Data...");
		     }
	     }
	     
	}
    
}
