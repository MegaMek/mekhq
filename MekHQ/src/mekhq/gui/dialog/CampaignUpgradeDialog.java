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
package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsDialog;

/**
 * Provides a user interface dialog for upgrading a {@link Campaign} across versions.
 *
 * <p>The {@link CampaignUpgradeDialog} guides the user through upgrading their campaign via interactive dialogs. The
 * dialog offers several options, such as selecting a predefined campaign preset, customizing options before upgrade, or
 * canceling the process. It manages all aspects of the dialog flow, including preset selection, application of
 * settings, handling user cancellation, and displaying a loading indicator during long-running operations.</p>
 *
 * <p><b>Functionality</b></p>
 * <ul>
 *   <li>Lists available campaign presets for upgrade and allows user selection.</li>
 *   <li>Supports campaign customization via a manual campaign options dialog.</li>
 *   <li>Performs the upgrade operation in a background thread, showing a loading UI to the user.</li>
 *   <li>Handles error cases, such as missing presets or failed upgrades, with proper messaging and application exit
 *   handling.</li>
 * </ul>
 *
 * <p><b>Thread Safety</b></p>
 * <p>This class encapsulates all user interactions on the Event Dispatch Thread and handles background work using
 * {@link SwingWorker}.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class CampaignUpgradeDialog {
    private static final MMLogger LOGGER = MMLogger.create(CampaignUpgradeDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignUpgradeDialog";

    private static final List<ImmersiveDialogCore.ButtonLabelTooltipPair> BUTTONS = List.of(
          new ImmersiveDialogCore.ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE,
                "CampaignUpgradeDialog.button.cancel"), null),
          new ImmersiveDialogCore.ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE,
                "CampaignUpgradeDialog.button.confirm"), null),
          new ImmersiveDialogCore.ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE,
                "CampaignUpgradeDialog.button.manual"), null));

    public static final int PRESET_SELECTION_CANCELLED = 0;
    public static final int PRESET_SELECTION_SELECT = 1;
    public static final int PRESET_SELECTION_CUSTOMIZE = 2;

    private static final List<CampaignPreset> presets = CampaignPreset.getCampaignPresets();

    /**
     * Shows and manages the campaign upgrade dialog for the given campaign.
     *
     * <p>The dialog displays available presets and options for customizing or canceling. On selection, it applies
     * the chosen preset, optionally runs a completion callback, or allows further customization via a campaign options
     * dialog.</p>
     *
     * @param campaign The campaign being upgraded; must not be {@code null}.
     * @param runnable An optional {@link Runnable} to trigger after a successful upgrade.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void campaignUpgradeDialog(Campaign campaign, Runnable runnable) {
        JPanel supplementalPanel = createSupplementalPanel();

        // This will occur if there are no presets available. This should never occur under normal circumstances as
        // MekHQ always ships with a battery of presets.
        if (supplementalPanel == null) {
            exitApp(campaign.getApp());
            return;
        }

        ImmersiveDialogCore upgradeDialog = new ImmersiveDialogCore(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, "CampaignUpgradeDialog.inCharacter", campaign.getCommanderAddress()),
              BUTTONS,
              getFormattedTextAt(RESOURCE_BUNDLE, "CampaignUpgradeDialog.outOfCharacter"),
              ImmersiveDialogWidth.LARGE.getWidth(),
              false,
              supplementalPanel,
              null,
              true);

        int dialogChoiceIndex = upgradeDialog.getDialogChoice();
        int comboChoiceIndex = upgradeDialog.getComboBoxChoiceIndex();

        switch (dialogChoiceIndex) {
            case PRESET_SELECTION_CANCELLED -> exitApp(campaign.getApp());
            case PRESET_SELECTION_SELECT -> {
                if (comboChoiceIndex < 0 || comboChoiceIndex >= presets.size()) {
                    LOGGER.errorDialog("Error",
                          "Invalid campaign preset index {}. Please report to the MekHQ team",
                          comboChoiceIndex);
                    exitApp(campaign.getApp());
                    // This is wholly unnecessary as the app will have already been closed.
                    // We include it because otherwise the IDE complains.
                    return;
                }

                CampaignPreset chosenPreset = presets.get(comboChoiceIndex);
                LOGGER.info("Applying {} during upgrade process", chosenPreset.getTitle());

                triggerLoadingDialog(campaign, chosenPreset, runnable);
            }
            case PRESET_SELECTION_CUSTOMIZE -> {
                CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(null, campaign);
                optionsDialog.setVisible(true);
                if (optionsDialog.wasCanceled()) {
                    exitApp(campaign.getApp());
                }
                SwingUtilities.invokeLater(runnable);
            }
        }
    }

    /**
     * Triggers a loading dialog while the provided campaign is upgraded using the chosen preset.
     *
     * <p>This method performs the upgrade operation on a background thread, displaying a modal overlay to the user.
     * Upon completion, the dialog is closed and, if specified, a {@link Runnable} completion task is executed.</p>
     *
     * @param campaign          The campaign being upgraded.
     * @param chosenPreset      The campaign preset to apply during the upgrade.
     * @param onUpgradeComplete A task to execute once the upgrade is complete.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void triggerLoadingDialog(Campaign campaign, CampaignPreset chosenPreset,
          Runnable onUpgradeComplete) {
        JDialog loadingDialog = new JDialog((Frame) null, true);
        loadingDialog.setUndecorated(true);

        JLabel loadingLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "CampaignUpgradeDialog.upgrading",
              MHQConstants.VERSION.toString()));
        loadingLabel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        loadingDialog.add(loadingLabel);
        loadingDialog.pack();
        loadingDialog.setLocationRelativeTo(null);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(campaign, chosenPreset);
                optionsDialog.processApplyAction();
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.setVisible(false);
                loadingDialog.dispose();
                onUpgradeComplete.run(); // trigger the external options update
            }
        };

        SwingUtilities.invokeLater(() -> {
            worker.execute();
            loadingDialog.setVisible(true);
        });
    }

    /**
     * Exits the application gracefully.
     *
     * <p><b>Notes:</b> This is used to ensure that any campaign passing through the upgrader is upgraded. If the
     * player performs an action that would result in the upgrade failing, such as canceling out of a manual Campaign
     * Options dialog, we want the application to exit.</p>
     *
     * @param mekHQ The instance of MekHQ to close.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void exitApp(MekHQ mekHQ) {
        mekHQ.exit(false);
    }

    /**
     * Creates a panel for displaying and selecting available campaign presets.
     *
     * <p>The panel shows a drop-down with all known campaign presets. If no presets are available, this method
     * returns {@code null} (and an error is logged).</p>
     *
     * @return the supplemental panel for preset selection, or {@code null} if no presets found.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static @Nullable JPanel createSupplementalPanel() {
        final DefaultListModel<CampaignPreset> campaignPresets = new DefaultListModel<>();
        campaignPresets.addAll(presets);

        if (campaignPresets.isEmpty()) {
            LOGGER.errorDialog("Error", "No campaign presets found");
            return null;
        }

        JLabel lblPresetName = new JLabel(getTextAt(RESOURCE_BUNDLE, "CampaignUpgradeDialog.label.presetPicker"));
        MMComboBox<String> comboBox = new MMComboBox<>("cboPresets", convertPresetListModelToComboBoxModel());

        JPanel panel = new JPanel();
        panel.add(lblPresetName, BorderLayout.WEST);
        panel.add(comboBox, BorderLayout.CENTER);

        return panel;
    }


    /**
     * Converts a list of {@link CampaignPreset} objects into a combo box model of their display names.
     *
     * @return a {@link DefaultComboBoxModel} holding the titles of available campaign presets.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static DefaultComboBoxModel<String> convertPresetListModelToComboBoxModel() {
        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();

        for (CampaignPreset preset : presets) {
            comboBoxModel.addElement(preset.getTitle());
        }

        return comboBoxModel;
    }
}
