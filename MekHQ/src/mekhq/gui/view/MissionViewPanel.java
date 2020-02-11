/*
 * MissionViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq.gui.view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import megamek.common.util.EncodeControl;
import mekhq.campaign.mission.Mission;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;
import mekhq.gui.utilities.MarkdownRenderer;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissionViewPanel extends ScrollablePanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Mission mission;
	private CampaignGUI gui;

	private JPanel pnlStats;
	private JTextPane txtDesc;

	private JLabel lblStatus;
	private JLabel lblLocation;
	private JLabel txtLocation;
	private JLabel lblType;
	private JLabel txtType;

	public MissionViewPanel(Mission m, CampaignGUI gui) {
		this.mission = m;
		this.gui = gui;
		initComponents();
	}

	private void initComponents() {
		GridBagConstraints gridBagConstraints;

		pnlStats = new JPanel();
		txtDesc = new JTextPane();

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
		gridBagConstraints.insets = new Insets(5, 5, 5, 5);
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		add(pnlStats, gridBagConstraints);

	}

    private void fillStats() {

    	ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractViewPanel", new EncodeControl()); //$NON-NLS-1$

    	lblStatus = new JLabel();
    	lblLocation = new JLabel();
    	txtLocation = new JLabel();
    	lblType = new JLabel();
    	txtType = new JLabel();

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

		if(null != mission.getSystemName(null) && !mission.getSystemName(null).isEmpty()) {
    		lblLocation.setName("lblLocation"); // NOI18N
    		lblLocation.setText(resourceMap.getString("lblLocation.text"));
    		gridBagConstraints = new GridBagConstraints();
    		gridBagConstraints.gridx = 0;
    		gridBagConstraints.gridy = 1;
    		gridBagConstraints.fill = GridBagConstraints.NONE;
    		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblLocation, gridBagConstraints);

    		txtLocation.setName("txtLocation"); // NOI18N
            String systemName = mission.getSystemName(null);
            txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
            txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            txtLocation.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Display where it is on the interstellar map
                    gui.getMapTab().switchSystemsMap(mission.getSystem());
                    gui.setSelectedTab(GuiTabType.MAP);
                }
            });
    		gridBagConstraints = new GridBagConstraints();
    		gridBagConstraints.gridx = 1;
    		gridBagConstraints.gridy = 1;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtLocation, gridBagConstraints);
		}

		if(null != mission.getType() && !mission.getType().isEmpty()) {
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
    		gridBagConstraints = new GridBagConstraints();
    		gridBagConstraints.gridx = 1;
    		gridBagConstraints.gridy = 2;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtType, gridBagConstraints);
		}

		txtDesc.setName("txtDesc");
		txtDesc.setEditable(false);
		txtDesc.setContentType("text/html");
		txtDesc.setText(MarkdownRenderer.getRenderedHtml(mission.getDescription()));
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new Insets(0, 0, 5, 0);
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		pnlStats.add(txtDesc, gridBagConstraints);

    }
}
