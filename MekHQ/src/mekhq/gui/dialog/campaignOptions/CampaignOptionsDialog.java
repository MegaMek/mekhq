package mekhq.gui.dialog.campaignOptions;

import megamek.common.annotations.Nullable;
import mekhq.CampaignPreset;
import mekhq.campaign.Campaign;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.CampaignOptionsButton;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.gui.dialog.campaignOptions.SelectPresetDialog.PRESET_SELECTION_CANCELLED;

public class CampaignOptionsDialog extends AbstractMHQButtonDialog {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private final Campaign campaign;
    private final CampaignOptionsPane campaignOptionsPane;
    private final boolean isStartUp;

    private boolean wasCanceled = true;

    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign) {
        super(frame, true, resources, "CampaignOptionsDialog", "campaignOptions.title");
        this.campaign = campaign;
        this.campaignOptionsPane = new CampaignOptionsPane(frame, campaign);
        this.isStartUp = false;
        initialize();

        setLocationRelativeTo(frame);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign, @Nullable CampaignPreset preset,
                                 boolean isStartUp) {
        super(frame, true, resources, "CampaignOptionsDialog", "campaignOptions.title");
        this.campaign = campaign;
        this.campaignOptionsPane = new CampaignOptionsPane(frame, campaign);
        this.isStartUp = isStartUp;
        initialize();

        if (preset != null) {
            applyPreset(preset);
        }

        setLocationRelativeTo(frame);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public boolean wasCanceled() {
        return wasCanceled;
    }

    @Override
    protected Container createCenterPane() {
        return campaignOptionsPane;
    }

    @Override
    protected JPanel createButtonPanel() {
        final JPanel pnlButtons = new JPanel(new GridLayout(1, 0));

        // Apply Settings
        JButton btnApplySettings = new CampaignOptionsButton("ApplySettings");
        btnApplySettings.addActionListener(evt -> {
            wasCanceled = false;
            campaignOptionsPane.applyCampaignOptionsToCampaign(null, isStartUp);
            dispose();
        });
        pnlButtons.add(btnApplySettings);

        // Save Preset
        JButton btnSavePreset = new CampaignOptionsButton("SavePreset");
        btnSavePreset.addActionListener(evt -> btnSaveActionPerformed());
        pnlButtons.add(btnSavePreset);

        // Load Preset
        JButton btnLoadPreset = new CampaignOptionsButton("LoadPreset");
        btnLoadPreset.addActionListener(evt -> btnLoadActionPerformed());
        pnlButtons.add(btnLoadPreset);

        // Cancel
        JButton btnCancel = new CampaignOptionsButton("Cancel");
        btnCancel.addActionListener(evt -> dispose());
        pnlButtons.add(btnCancel);

        return pnlButtons;
    }

    private void btnSaveActionPerformed() {
        final CreateCampaignPreset createCampaignPresetDialog
            = new CreateCampaignPreset(null, campaign, null);
        if (!createCampaignPresetDialog.showDialog().isConfirmed()) {
            return;
        }
        final CampaignPreset preset = createCampaignPresetDialog.getPreset();
        if (preset == null) {
            return;
        }

        campaignOptionsPane.applyCampaignOptionsToCampaign(preset, isStartUp);

        preset.writeToFile(null,
            FileDialogs.saveCampaignPreset(null, preset).orElse(null));
    }

    private void btnLoadActionPerformed() {
        final SelectPresetDialog presetSelectionDialog =
            new SelectPresetDialog(null, true, false);
        if (presetSelectionDialog.getReturnState() != PRESET_SELECTION_CANCELLED) {
            campaignOptionsPane.applyPreset(presetSelectionDialog.getSelectedPreset());
        }
    }

    public void applyPreset(CampaignPreset preset) {
        campaignOptionsPane.applyPreset(preset);
    }
}