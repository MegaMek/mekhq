/*
 * Copyright (c) 2025 The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.campaignOptions;

import megamek.common.annotations.Nullable;
import mekhq.CampaignPreset;
import mekhq.campaign.Campaign;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.campaignOptions.components.CampaignOptionsButton;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.ABRIDGED;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.NORMAL;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP;
import static mekhq.gui.campaignOptions.SelectPresetDialog.PRESET_SELECTION_CANCELLED;

/**
 * The {@code CampaignOptionsDialog} class represents a dialog window for presenting
 * and modifying the campaign options in MekHQ. It provides a user interface
 * for accessing, editing, and applying various gameplay-related settings for a campaign.
 * The dialog also supports applying presets and saving settings for future use.
 * <p>
 * This dialog is an extension of {@link AbstractMHQButtonDialog} and integrates closely
 * with a {@link Campaign} instance, representing the current or a predefined campaign setup.
 * It facilitates user interaction for fine-tuning the game campaign experience.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Initialization in multiple modes, such as NORMAL, STARTUP, and ABRIDGED.</li>
 *   <li>Ability to load and save presets for recurring configurations.</li>
 *   <li>Application of campaign settings directly to the active {@link Campaign} instance.</li>
 *   <li>Visual notifications, such as a notice about StratCon activation during the campaign.</li>
 * </ul>
 */
public class CampaignOptionsDialog extends AbstractMHQButtonDialog {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private final Campaign campaign;
    private final CampaignOptionsPane campaignOptionsPane;
    private final CampaignOptionsDialogMode mode;

    private boolean wasCanceled = true;

    public enum CampaignOptionsDialogMode {
        NORMAL,
        STARTUP,
        ABRIDGED
    }

    /**
     * Constructs a {@code CampaignOptionsDialog} with the specified parent frame and
     * campaign instance. Initializes the dialog using the default {@code NORMAL} mode.
     *
     * @param frame    the parent {@link JFrame} for this dialog
     * @param campaign the {@link Campaign} instance representing the current campaign
     */
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign) {
        super(frame, true, resources, "CampaignOptionsDialog", "campaignOptions.title");
        this.campaign = campaign;
        this.campaignOptionsPane = new CampaignOptionsPane(frame, campaign, NORMAL);
        this.mode = NORMAL;
        initialize();

        setLocationRelativeTo(frame);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Constructs a {@code CampaignOptionsDialog} with the specified parent frame,
     * campaign instance, campaign preset, and mode. If a preset is provided, it is
     * automatically applied to the campaign.
     *
     * @param frame    the parent {@link JFrame} for this dialog
     * @param campaign the {@link Campaign} instance for the current or preconfigured campaign
     * @param preset   an optional {@link CampaignPreset} to initialize the campaign (can be null)
     * @param mode     the {@link CampaignOptionsDialogMode} defining the behavior of the dialog
     */
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign, @Nullable CampaignPreset preset,
                                 CampaignOptionsDialogMode mode) {
        super(frame, true, resources, "CampaignOptionsDialog", "campaignOptions.title");
        this.campaign = campaign;
        this.campaignOptionsPane = new CampaignOptionsPane(frame, campaign, mode);
        this.mode = mode;
        initialize();

        if (preset != null) {
            applyPreset(preset);
        }

        setLocationRelativeTo(frame);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Indicates whether the dialog was canceled by the user.
     *
     * @return {@code true} if the user canceled the dialog, {@code false} otherwise
     */
    public boolean wasCanceled() {
        return wasCanceled;
    }

    /**
     * Creates the central pane of the dialog, which contains the campaign options UI.
     *
     * @return a {@link Container} representing the center pane of the dialog
     */
    @Override
    protected Container createCenterPane() {
        return campaignOptionsPane;
    }

    /**
     * Creates the button panel for the dialog, allowing the user to apply settings,
     * load and save presets, or cancel the dialog.
     *
     * @return a {@link JPanel} representing the button panel
     */
    @Override
    protected JPanel createButtonPanel() {
        final JPanel pnlButtons = new JPanel(new GridLayout(1, 0));

        // Apply Settings
        JButton btnApplySettings = new CampaignOptionsButton("ApplySettings");
        btnApplySettings.addActionListener(evt -> {
            wasCanceled = false;
            campaignOptionsPane.applyCampaignOptionsToCampaign(null, mode == STARTUP,
                false);
            dispose();
            showStratConNotice();
        });
        pnlButtons.add(btnApplySettings);

        // Save Preset
        if (mode != ABRIDGED) {
            JButton btnSavePreset = new CampaignOptionsButton("SavePreset");
            btnSavePreset.addActionListener(evt -> btnSaveActionPerformed());
            pnlButtons.add(btnSavePreset);
        }

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

    /**
     * Handles the "Save Preset" button action. Opens a dialog to create a new preset and
     * save the campaign configuration to a file if confirmed.
     */
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

        campaignOptionsPane.applyCampaignOptionsToCampaign(preset, mode == STARTUP, true);

        preset.writeToFile(null,
            FileDialogs.saveCampaignPreset(null, preset).orElse(null));
    }

    /**
     * Handles the "Load Preset" button action. Opens a preset selection dialog,
     * and applies the selected preset to the current campaign options if a preset
     * is selected and not canceled.
     */
    private void btnLoadActionPerformed() {
        final SelectPresetDialog presetSelectionDialog =
            new SelectPresetDialog(null, true, false);
        if (presetSelectionDialog.getReturnState() != PRESET_SELECTION_CANCELLED) {
            campaignOptionsPane.applyPreset(presetSelectionDialog.getSelectedPreset());
        }
    }

    /**
     * Applies a preset to the campaign options pane. This allows the user to quickly
     * configure the campaign settings based on predefined presets.
     *
     * @param preset the {@link CampaignPreset} instance to apply
     */
    public void applyPreset(CampaignPreset preset) {
        campaignOptionsPane.applyPreset(preset);
    }

    /**
     * Displays a promo notice for StratCon functionality when the campaign options dialog
     * is closed if the campaign is starting on the first day, and StratCon is enabled.
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
