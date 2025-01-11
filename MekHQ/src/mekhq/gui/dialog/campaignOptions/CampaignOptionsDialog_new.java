package mekhq.gui.dialog.campaignOptions;

import mekhq.CampaignPreset;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.FileDialogs;
import mekhq.gui.dialog.CreateCampaignPresetDialog;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsButton;

import javax.swing.*;
import java.awt.*;

import static mekhq.gui.dialog.campaignOptions.SelectPresetDialog.PRESET_SELECTION_CANCELLED;

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
            btnSaveActionPerformed();
            dispose();
        });
        pnlButtons.add(btnSavePreset);

        // Load Preset
        JButton btnLoadPreset = new CampaignOptionsButton("LoadPreset");
        btnLoadPreset.addActionListener(evt -> {
            final SelectPresetDialog presetSelectionDialog =
                new SelectPresetDialog(null, true, false);
            if (presetSelectionDialog.getReturnState() != PRESET_SELECTION_CANCELLED) {
                campaignOptionsPane.applyPreset(presetSelectionDialog.getSelectedPreset());
            }
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

    private void btnSaveActionPerformed() {
        campaignOptionsPane.applyCampaignOptionsToCampaign();

        final CreateCampaignPresetDialog createCampaignPresetDialog
            = new CreateCampaignPresetDialog(null, campaign, null);
        if (!createCampaignPresetDialog.showDialog().isConfirmed()) {
            dispose();
            return;
        }
        final CampaignPreset preset = createCampaignPresetDialog.getPreset();
        if (preset == null) {
            dispose();
            return;
        }
        preset.writeToFile(null,
            FileDialogs.saveCampaignPreset(null, preset).orElse(null));
        setVisible(false);
    }
}
