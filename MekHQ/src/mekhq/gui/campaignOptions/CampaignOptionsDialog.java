/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.campaignOptions;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ResourceBundle;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import megamek.common.annotations.Nullable;
import mekhq.CampaignPreset;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.campaignOptions.components.CampaignOptionsButton;

/**
 * The {@code CampaignOptionsDialog} class represents a dialog window for presenting and modifying the campaign options
 * in MekHQ. It provides a user interface for accessing, editing, and applying various gameplay-related settings for a
 * campaign. The dialog also supports applying presets and saving settings for future use.
 * <p>
 * This dialog is an extension of {@link AbstractMHQButtonDialog} and integrates closely with a {@link Campaign}
 * instance, representing the current or a predefined campaign setup. It facilitates user interaction for fine-tuning
 * the game campaign experience.
 * </p>
 *
 * <strong>Key Features:</strong>
 * <ul>
 *   <li>Initialization in multiple modes, such as NORMAL, STARTUP, and ABRIDGED.</li>
 *   <li>Ability to load and save presets for recurring configurations.</li>
 *   <li>Application of campaign settings directly to the active {@link Campaign} instance.</li>
 *   <li>Visual notifications, such as a notice about StratCon activation during the campaign.</li>
 * </ul>
 */
public class CampaignOptionsDialog extends AbstractMHQButtonDialog {
    private final Campaign campaign;
    private final CampaignOptionsPane campaignOptionsPane;
    private final CampaignOptionsDialogMode mode;

    private boolean wasCanceled = true;

    public enum CampaignOptionsDialogMode {
        NORMAL, STARTUP, STARTUP_ABRIDGED, ABRIDGED
    }

    /**
     * Constructs a {@code CampaignOptionsDialog} with the specified parent frame and campaign instance. Initializes the
     * dialog using the default {@code NORMAL} mode.
     *
     * @param frame    the parent {@link JFrame} for this dialog
     * @param campaign the {@link Campaign} instance representing the current campaign
     */
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign) {
        super(frame,
              true,
              ResourceBundle.getBundle(getCampaignOptionsResourceBundle()),
              "CampaignOptionsDialog",
              "campaignOptions.title");
        this.campaign = campaign;
        this.campaignOptionsPane = new CampaignOptionsPane(frame, campaign, CampaignOptionsDialogMode.NORMAL);
        this.mode = CampaignOptionsDialogMode.NORMAL;
        initialize();

        setLocationRelativeTo(frame);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Constructs a {@code CampaignOptionsDialog} with the specified parent frame, campaign instance, campaign preset,
     * and mode. If a preset is provided, it is automatically applied to the campaign.
     *
     * @param frame    the parent {@link JFrame} for this dialog
     * @param campaign the {@link Campaign} instance for the current or preconfigured campaign
     * @param preset   an optional {@link CampaignPreset} to initialize the campaign (can be null)
     * @param mode     the {@link CampaignOptionsDialogMode} defining the behavior of the dialog
     */
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign, @Nullable CampaignPreset preset,
          CampaignOptionsDialogMode mode) {
        super(frame,
              true,
              ResourceBundle.getBundle(getCampaignOptionsResourceBundle()),
              "CampaignOptionsDialog",
              "campaignOptions.title");
        this.campaign = campaign;
        this.campaignOptionsPane = new CampaignOptionsPane(frame, campaign, mode);
        this.mode = mode;
        initialize();

        if (preset != null) {
            applyPreset(preset, true);
        }

        setLocationRelativeTo(frame);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Constructs a {@code CampaignOptionsDialog} for the specified campaign and applies the given preset if provided.
     *
     * <p>This constructor initializes the dialog using the {@code NORMAL} mode, providing a user interface for
     * viewing and modifying campaign options. If a {@link CampaignPreset} is supplied (i.e., {@code preset} is not
     * {@code null}), the options from the preset are automatically applied to the dialog upon creation. </p>
     *
     * @param campaign the {@link Campaign} instance whose options will be displayed and edited
     * @param preset   an optional {@link CampaignPreset} to apply initial settings (may be {@code null})
     */
    public CampaignOptionsDialog(final Campaign campaign, @Nullable CampaignPreset preset) {
        super(null,
              false,
              ResourceBundle.getBundle(getCampaignOptionsResourceBundle()),
              "CampaignOptionsDialog",
              "campaignOptions.title");
        this.campaign = campaign;
        this.campaignOptionsPane = new CampaignOptionsPane(null, campaign, CampaignOptionsDialogMode.NORMAL);
        this.mode = CampaignOptionsDialogMode.NORMAL;
        initialize();

        if (preset != null) {
            applyPreset(preset, false);
        }

        setLocationRelativeTo(null);
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
     * Creates the button panel for the dialog, allowing the user to apply settings, load and save presets, or cancel
     * the dialog.
     *
     * @return a {@link JPanel} representing the button panel
     */
    @Override
    protected JPanel createButtonPanel() {
        final JPanel pnlButtons = new JPanel(new GridLayout(1, 0));

        // Apply Settings
        JButton btnApplySettings = new CampaignOptionsButton("ApplySettings");
        btnApplySettings.addActionListener(evt -> processApplyAction());
        pnlButtons.add(btnApplySettings);

        // Save Preset
        if (mode != CampaignOptionsDialogMode.ABRIDGED && mode != CampaignOptionsDialogMode.STARTUP_ABRIDGED) {
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
     * Applies the selected campaign options to the current campaign and closes the dialog.
     *
     * <p>This method is typically called when the user confirms their option changes. It updates the campaign's
     * configuration using the current selections in the options pane, taking into account whether the dialog is being
     * used during campaign startup.</p>
     * <p>If the dialog is in a startup mode and the "StratCon" setting is enabled for the campaign, an additional
     * notice is displayed to inform the user of important details about this feature.</p>
     *
     * <p>The dialog is closed after applying the options. The result is recorded as not canceled.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void processApplyAction() {
        wasCanceled = false;
        boolean isStartup = mode == CampaignOptionsDialogMode.STARTUP ||
                                  mode == CampaignOptionsDialogMode.STARTUP_ABRIDGED;
        campaignOptionsPane.applyCampaignOptionsToCampaign(null, isStartup, false);
        dispose();

        if (isStartup) {
            final CampaignOptions campaignOptions = campaign.getCampaignOptions();
            if (campaignOptions.isUseStratCon()) {
                showStratConNotice();
            }
        }
    }

    /**
     * Handles the "Save Preset" button action. Opens a dialog to create a new preset and save the campaign
     * configuration to a file if confirmed.
     */
    private void btnSaveActionPerformed() {
        final CreateCampaignPreset createCampaignPresetDialog = new CreateCampaignPreset(null, campaign, null);

        if (!createCampaignPresetDialog.showDialog().isConfirmed()) {
            return;
        }

        final CampaignPreset preset = createCampaignPresetDialog.getPreset();
        if (preset == null) {
            return;
        }

        campaignOptionsPane.applyCampaignOptionsToCampaign(preset,
              mode == CampaignOptionsDialogMode.STARTUP || mode == CampaignOptionsDialogMode.STARTUP_ABRIDGED,
              true);

        preset.writeToFile(null, FileDialogs.saveCampaignPreset(null, preset).orElse(null));
    }

    /**
     * Handles the "Load Preset" button action. Opens a preset selection dialog, and applies the selected preset to the
     * current campaign options if a preset is selected and not canceled.
     */
    private void btnLoadActionPerformed() {
        final CampaignOptionsPresetPicker campaignOptionsPresetPicker = new CampaignOptionsPresetPicker(getFrame(),
              false);
        if (!campaignOptionsPresetPicker.wasCanceled()) {
            campaignOptionsPane.applyPreset(campaignOptionsPresetPicker.getSelectedPreset(), false);
        }
    }

    /**
     * Applies a preset to the campaign options pane. This allows the user to quickly configure the campaign settings
     * based on predefined presets.
     *
     * @param preset the {@link CampaignPreset} instance to apply
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void applyPreset(CampaignPreset preset) {
        campaignOptionsPane.applyPreset(preset, true);
    }

    /**
     * Applies the specified campaign preset to the options pane, optionally indicating if the dialog is being used
     * during campaign startup.
     *
     * <p>This method updates the available campaign option settings in the dialog based on the values defined in the
     * given {@link CampaignPreset}. The {@code isStartup} parameter allows for different preset application logic or
     * behavior when initializing campaign options for a new campaign.</p>
     *
     * @param preset    the {@link CampaignPreset} whose settings will be applied to the options pane
     * @param isStartup {@code true} if the dialog is being used during the startup of a new campaign
     */
    public void applyPreset(CampaignPreset preset, boolean isStartup) {
        campaignOptionsPane.applyPreset(preset, isStartup);
    }

    /**
     * Displays the StratCon promotional notice dialog, if applicable.
     *
     * <p>This method shows a modal dialog to present promotional information about the StratCon feature
     * in the form of an image and a message. The notice is presented with a title, an optional image, and a descriptive
     * message, ensuring users are informed of the feature's introduction or importance. The dialog also includes a
     * single button for the user to acknowledge and dismiss the notice.</p>
     */
    private void showStratConNotice() {
        ImageIcon imageIcon = new ImageIcon("data/images/stratcon/stratConPromo.png");
        JLabel imageLabel = new JLabel(imageIcon);
        JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.add(imageLabel);

        String title = getTextAt(getCampaignOptionsResourceBundle(), "stratConPromo.title");

        String message = getTextAt(getCampaignOptionsResourceBundle(), "stratConPromo.message");
        JLabel messageLabel = new JLabel(message);
        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.add(messageLabel);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(imagePanel);
        panel.add(messagePanel);

        Object[] options = { getTextAt(getCampaignOptionsResourceBundle(), "stratConPromo.button") };

        JOptionPane.showOptionDialog(null,
              panel,
              title,
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.INFORMATION_MESSAGE,
              null,
              options,
              options[0]);
    }
}
