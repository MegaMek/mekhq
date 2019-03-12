package mekhq.gui.stratcon;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconCampaignState;

public class StratconScenarioWizard extends JDialog {
    StratconScenario currentScenario;
    Campaign campaign;
    StratconCampaignState currentCampaignState;

    JLabel lblTotalBV = new JLabel();
    JList<Force> playerForceList = new JList<>();


    public StratconScenarioWizard(Campaign campaign) {
        this.campaign = campaign;
    }

    public void setCurrentScenario(StratconScenario scenario, StratconCampaignState campaignState) {
        currentScenario = scenario;
        currentCampaignState = campaignState;
        setUI();
    }

    public void setUI() {
        setTitle("Scenario Setup Wizard");
        getContentPane().removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        getContentPane().setLayout(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy++;
        setPlayerForceSelector(gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        setNavigationButtons(gbc);
        pack();
        validate();
    }

    public void setPlayerForceSelector(GridBagConstraints gbc) {
        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign);
        playerForceList.setModel(lanceModel);
        playerForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        playerForceList.addListSelectionListener(new ListSelectionListener() { 
            @Override
            public void valueChanged(ListSelectionEvent e) {
                playerForceSelectorChanged(e);
            }
        });

        forceListContainer.setViewportView(playerForceList);

        gbc.gridx = 0;
        getContentPane().add(forceListContainer, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        lblTotalBV.setText("Selected BV: 0");
        getContentPane().add(lblTotalBV, gbc);
    }

    /**
     * Sets the navigation button
     * @param gbc
     */
    private void setNavigationButtons(GridBagConstraints gbc) {
        // you're on one of two screens:
        // the 'primary force selection' screen
        // the 'reinforcement selection' screen
        // you can re-visit the second one
        JButton btnCommit = new JButton("Commit");
        btnCommit.setActionCommand("COMMIT_CLICK");
        btnCommit.addActionListener(new ActionListener() { 
            @Override
            public void actionPerformed(ActionEvent e) {
                btnCommitClicked(e);
            }
        });

        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        getContentPane().add(btnCommit, gbc);
    }

    /**
     * Event handler for when the user clicks the 'commit' button.
     * Behaves differently depending on the state of the 
     * @param e
     */
    private void btnCommitClicked(ActionEvent e) {
        if(currentScenario.getCurrentState() == ScenarioState.UNRESOLVED) {
            currentScenario.commitPrimaryForces(campaign, currentCampaignState.getContract());
        }
    }

    /**
     * Event handler for when the user makes a selection on the player force selector.
     * Updates the "selected BV" label.
     * @param e The event fired. 
     */
    private void playerForceSelectorChanged(ListSelectionEvent e) {
        if(!(e.getSource() instanceof JList<?>)) {
            return;
        }

        int totalBV = 0;
        JList<Force> sourceList = (JList<Force>) e.getSource();
        for(Force force : sourceList.getSelectedValuesList()) {
            totalBV += force.getTotalBV(campaign);
        }

        lblTotalBV.setText(String.format("Selected BV: %d", totalBV));
    }
}