/*
 * AlertPopup.java
 *
 * Created on Jan 3, 2014
 */

package mekhq.gui.dialog;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.common.preference.PreferenceManager;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

public class LaunchGameDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 9094845543518155461L;
    public String playerName;
    public String serverAddr;
    public int port;
    public boolean cancelled;
    private JLabel yourNameL;
    private JLabel serverAddrL;
    private JLabel portL;
    private JTextField yourNameF;
    private JTextField serverAddrF;
    private JTextField portF;
    private JButton okayB;
    private JButton cancelB;
    private Frame parent;
    private boolean server;
    private Campaign campaign;

	/** Creates new form AlertPopup */
    public LaunchGameDialog(java.awt.Frame parent, boolean server, Campaign c) {
    	super(parent, Messages.getString("MegaMek.ConnectDialog.title"), true); //$NON-NLS-1$
    	this.parent = parent;
    	this.server = server;
    	this.campaign = c;
        initComponents();
        setUserPreferences();
    }

    public void initComponents() {
    	yourNameL = new JLabel(
                Messages.getString("MegaMek.yourNameL"), SwingConstants.RIGHT); //$NON-NLS-1$
    	if (!server) {
	        serverAddrL = new JLabel(
	                Messages.getString("MegaMek.serverAddrL"), SwingConstants.RIGHT); //$NON-NLS-1$
    	}
        portL = new JLabel(
                Messages.getString("MegaMek.portL"), SwingConstants.RIGHT); //$NON-NLS-1$
        yourNameF = new JTextField(campaign.getName(), 16);
        yourNameF.addActionListener(this);
        if (!server) {
	        serverAddrF = new JTextField(PreferenceManager.getClientPreferences()
	                .getLastConnectAddr(), 16);
	        serverAddrF.addActionListener(this);
        }
        portF = new JTextField(PreferenceManager.getClientPreferences()
                .getLastConnectPort() + "", 4); //$NON-NLS-1$
        portF.addActionListener(this);
        okayB = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        okayB.setActionCommand("done"); //$NON-NLS-1$
        okayB.addActionListener(this);
        okayB.setSize(80, 24);
        cancelB = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        cancelB.setActionCommand("cancel"); //$NON-NLS-1$
        cancelB.addActionListener(this);
        cancelB.setSize(80, 24);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(yourNameL, c);
        getContentPane().add(yourNameL);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(yourNameF, c);
        getContentPane().add(yourNameF);
        if (!server) {
	        c.gridwidth = 1;
	        c.anchor = GridBagConstraints.EAST;
	        gridbag.setConstraints(serverAddrL, c);
	        getContentPane().add(serverAddrL);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(serverAddrF, c);
        getContentPane().add(serverAddrF);
        }
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(portL, c);
        getContentPane().add(portL);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(portF, c);
        getContentPane().add(portF);
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(okayB, c);
        getContentPane().add(okayB);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancelB, c);
        getContentPane().add(cancelB);
        pack();
        setResizable(false);
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                - getSize().width / 2, parent.getLocation().y
                + parent.getSize().height / 2 - getSize().height / 2);
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(LaunchGameDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		if (!"cancel".equals(e.getActionCommand())) { //$NON-NLS-1$
            try {
                playerName = yourNameF.getText();
                if (!server) {
                	serverAddr = serverAddrF.getText();
                }
                port = Integer.decode(portF.getText().trim());
            } catch (NumberFormatException ex) {
                MekHQ.getLogger().error(getClass(), "actionPerformed", ex.getMessage());
            }
        } else {
            cancelled = true;
        }
        setVisible(false);
	}

}
