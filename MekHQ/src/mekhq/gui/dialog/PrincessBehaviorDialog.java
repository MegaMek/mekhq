/*
 * PrincessBehaviorDialog.java
 *
 * Derived from megamek.client.ui.swing.BotConfigDialog Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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

package mekhq.gui.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.ui.swing.BotConfigDialog;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;

public class PrincessBehaviorDialog extends BotConfigDialog implements ActionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1697757949925940582L;

	public PrincessBehaviorDialog(JFrame parent, BehaviorSettings princessBehavior, String name) {
	    super(parent, null);
	    
	    try {
	        this.princessBehavior = princessBehavior.getCopy();
	    } catch(Exception e) {
	        handleError("PrincessBehaviorDialog", e);
	    }
	    this.setName(name);
	    
	    this.nameField.setText(name);
	    super.setPrincessFields();
	}
	
	public BehaviorSettings getBehaviorSettings() {
		return princessBehavior;
	}
	
    private void getPrincessFields() {
        LogLevel logLevel = LogLevel.getLogLevel((String) verbosityCombo.getSelectedItem());
        if (null != logLevel) {
            princessBehavior.setVerbosity(logLevel);
        }
    	princessBehavior.setForcedWithdrawal(forcedWithdrawalCheck.isSelected());
    	princessBehavior.setAutoFlee(autoFleeCheck.isSelected());
    	princessBehavior.setSelfPreservationIndex(selfPreservationSlidebar.getValue());
    	princessBehavior.setHyperAggressionIndex(aggressionSlidebar.getValue());
    	princessBehavior.setFallShameIndex(fallShameSlidebar.getValue());
    	princessBehavior.setDestinationEdge(destinationEdgeCombo.getSelectedIndex());
    	princessBehavior.setRetreatEdge(retreatEdgeCombo.getSelectedIndex());
    	princessBehavior.setHerdMentalityIndex(herdingSlidebar.getValue());
    	princessBehavior.setBraveryIndex(braverySlidebar.getValue());
    }

    public void actionPerformed(ActionEvent e) {
        if (butOK.equals(e.getSource())) {
        	getPrincessFields();
            dialogAborted = false;
            setVisible(false);
        } else {
            super.actionPerformed(e);
        }
    }

    private void handleError(String method, Throwable t) {
        JOptionPane.showMessageDialog(this, t.getMessage(),
                                      "ERROR", JOptionPane.ERROR_MESSAGE);
        MekHQ.getLogger().error(getClass(), method, t);
    }

    @Override
    public String getBotName() {
        return nameField.getText();
    }
}
