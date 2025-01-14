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
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
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
            showStratConNotice();
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



    /**
     * Displays a promo introducing users to StratCon.
     * This method shows the promo only when the Campaign Options pane is closed
     * and the current day is the first day of the campaign.
     */
    private void showStratConNotice() {
        // we don't store whether this dialog has previously appeared,
        // instead we just have it appear only when Campaign Options is closed,
        // the current day is the first day of the campaign, and StratCon is enabled
        if (!campaign.getCampaignOptions().isUseStratCon()
                || !campaign.getLocalDate().equals(campaign.getCampaignStartDate())) {
            return;
        }

        ImageIcon imageIcon = new ImageIcon("data/images/stratcon/stratConPromo.png");
        JLabel imageLabel = new JLabel(imageIcon);
        JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.add(imageLabel);

        String title = resources.getString("stratConPromo.title");

        String message = resources.getString("stratConPromo.message");
        JLabel messageLabel = new JLabel(message);
        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.add(messageLabel);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(imagePanel);
        panel.add(messagePanel);

        Object[] options = {
                resources.getString("stratConPromo.button")
        };

        JOptionPane.showOptionDialog(null, panel, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }
}
