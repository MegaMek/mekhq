package mekhq.gui.dialog.campaignOptions;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsButton;

import javax.swing.*;
import java.awt.*;

public class CampaignOptionsDialog_new extends JDialog {
    final JFrame frame;
    private final Campaign campaign;
    private final MekHQ application;
    private final CampaignOptionsPane campaignOptionsPane;

    private boolean wasCanceled = false;

    public CampaignOptionsDialog_new(final JFrame frame, final Campaign campaign) {
        this.frame = frame;
        this.campaign = campaign;
        this.campaignOptionsPane = new CampaignOptionsPane(frame, campaign);
        this.application = null;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(campaignOptionsPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.PAGE_END);

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setModal(true);
        setVisible(true);
    }

    public boolean wasCanceled() {
        return wasCanceled;
    }

    protected JPanel createButtonPanel() {
        final JPanel pnlButtons = new JPanel(new GridLayout(1, 0));

        // Apply Settings
        JButton btnApplySettings = new CampaignOptionsButton("ApplySettings");
        btnApplySettings.addActionListener(evt -> {
            campaignOptionsPane.applyCampaignOptionsToCampaign();
            dispose();
        });
        pnlButtons.add(btnApplySettings);

        // Save Preset
        JButton btnSavePreset = new CampaignOptionsButton("SavePreset");
        btnSavePreset.addActionListener(evt -> {
            dispose();
        });
        pnlButtons.add(btnSavePreset);

        // Load Preset
        JButton btnLoadPreset = new CampaignOptionsButton("LoadPreset");
        btnLoadPreset.addActionListener(evt -> {
            dispose();
        });
        pnlButtons.add(btnLoadPreset);

        // Cancel
        JButton btnCancel = new CampaignOptionsButton("Cancel");
        btnCancel.addActionListener(evt -> {
            wasCanceled = true;
            dispose();
        });
        pnlButtons.add(btnCancel);

        return pnlButtons;
    }
}
