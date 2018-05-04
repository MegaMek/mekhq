/*
 * MissionViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq.gui.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.common.util.EncodeControl;
import mekhq.campaign.mission.Mission;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissionViewPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Mission mission;
	
	private JPanel pnlStats;
	private JTextArea txtDesc;
	
	private JLabel lblStatus;
	private JLabel lblLocation;
	private JTextArea txtLocation;
	private JLabel lblType;
	private JTextArea txtType;
	
	public MissionViewPanel(Mission m) {
		this.mission = m;
		initComponents();
	}
	
	private void initComponents() {
		GridBagConstraints gridBagConstraints;

		pnlStats = new JPanel();
		txtDesc = new JTextArea();
		       
		setLayout(new GridBagLayout());

		setBackground(Color.WHITE);

		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder(mission.getName()));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new Insets(5, 5, 5, 20);
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
	}

    private void fillStats() {
    	
    	ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractViewPanel", new EncodeControl()); //$NON-NLS-1$
    	
    	lblStatus = new JLabel();
    	lblLocation = new JLabel();
    	txtLocation = new JTextArea();
    	lblType = new JLabel();
    	txtType = new JTextArea();

    	GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new GridBagLayout());
		
		lblStatus.setName("lblOwner"); // NOI18N
		lblStatus.setText("<html><b>" + mission.getStatusName() + "</b></html>");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new Insets(0, 0, 5, 0);
		gridBagConstraints.fill = GridBagConstraints.NONE;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStatus, gridBagConstraints);
		
		lblLocation.setName("lblLocation"); // NOI18N
		lblLocation.setText(resourceMap.getString("lblLocation.text"));
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = GridBagConstraints.NONE;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		pnlStats.add(lblLocation, gridBagConstraints);
		
		txtLocation.setName("txtLocation"); // NOI18N
        txtLocation.setText(mission.getPlanetName(null));
		txtLocation.setEditable(false);
		txtLocation.setLineWrap(true);
		txtLocation.setWrapStyleWord(true);
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new Insets(0, 10, 0, 0);
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		pnlStats.add(txtLocation, gridBagConstraints);
		
		lblType.setName("lblType"); // NOI18N
		lblType.setText(resourceMap.getString("lblType.text"));
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = GridBagConstraints.NONE;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		pnlStats.add(lblType, gridBagConstraints);
		
		txtType.setName("txtType"); // NOI18N
		txtType.setText(mission.getType());
		txtType.setEditable(false);
		txtType.setLineWrap(true);
		txtType.setWrapStyleWord(true);
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new Insets(0, 10, 0, 0);
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		pnlStats.add(txtType, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(mission.getDescription());
		txtDesc.setEditable(false);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new Insets(5, 5, 5, 20);
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		pnlStats.add(txtDesc, gridBagConstraints);
		
    }
}