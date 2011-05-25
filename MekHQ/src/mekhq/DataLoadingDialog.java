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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import megamek.common.MechSummaryCache;
import mekhq.campaign.Planets;

public class DataLoadingDialog extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = -3454307876761238915L;
    private JProgressBar progressBar;
 
    public DataLoadingDialog(JFrame frame) {
        super(frame, "Please Wait test"); //$NON-NLS-1$
       
        progressBar = new JProgressBar(0, 2);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        progressBar.setVisible(true);
        JLabel testLabel = new JLabel("Loading Data, Please Wait");
        getContentPane().setLayout(new GridLayout(1, 2));
        getContentPane().add(testLabel);
        //getContentPane().add(progressBar);
        
        setSize(250, 130);
        // move to middle of screen
        Dimension screenSize = frame.getToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getSize().width / 2,
                screenSize.height / 2 - getSize().height / 2);
    }

    public void updateProgress() {
    	int value = 0;
    	if(Planets.getInstance().isInitialized()) {
    		value++;
    	}
    	if(MechSummaryCache.getInstance().isInitialized()) {
    		value++;
    	}
    	progressBar.setValue(value);
    }
}
