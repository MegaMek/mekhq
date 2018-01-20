/*
 * RatingReport.java
 *
 * Copyright (c) 2013 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
package mekhq.campaign.report;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JSplitPane;
import javax.swing.JTextPane;

import mekhq.campaign.Campaign;



/**
 * @author Jay Lawson
 * @version %I% %G%
 * @since 3/12/2012
 */
public class PersonnelReport extends Report {


    public PersonnelReport(Campaign c) {
        super(c);
    }

    public String getTitle() {
        return "Personnel Report";
    }

    public JTextPane getCombatPersonnelReport() {
    	// Load combat personnel
        JTextPane txtCombat = new JTextPane();
        txtCombat.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtCombat.setText(getCampaign().getCombatPersonnelDetails());
        return txtCombat;
    }

    public JTextPane getSupportPersonnelReport() {
    	// Load support personnel
        JTextPane txtSupport = new JTextPane();
        txtSupport.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtSupport.setText(getCampaign().getSupportPersonnelDetails());
        return txtSupport;
    }

    public JTextPane getReport() {
        // SplitPane them
        JSplitPane splitOverviewPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getCombatPersonnelReport(), getSupportPersonnelReport());
		splitOverviewPersonnel.setName("splitOverviewPersonnel");
		splitOverviewPersonnel.setOneTouchExpandable(true);
		splitOverviewPersonnel.setResizeWeight(0.5);

		// Actual report pane
		JTextPane txtReport = new JTextPane();
        txtReport.setMinimumSize(new Dimension(800, 500));
        txtReport.insertComponent(splitOverviewPersonnel);
        return txtReport;
    }

}
