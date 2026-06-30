/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.*;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Dialog for selecting and managing campaign option presets in MekHQ.
 *
 * <p>This dialog allows users to choose from a list of predefined campaign presets, view a description for each, and
 * optionally customize or apply a preset.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class CampaignOptionsPresetPicker extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignOptionsPresetPicker";
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptionsPresetPicker.class);

    // While these values can be local, it makes more sense to define them at a class level so that it's easier to
    // find and adjust as necessary.
    @SuppressWarnings("FieldCanBeLocal")
    private final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    @SuppressWarnings("FieldCanBeLocal")
    private final int IMAGE_HEIGHT = 160; // This is scaled elsewhere

    private final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

    @SuppressWarnings("FieldCanBeLocal")
    private final int SINGLE_LINE_HEIGHT = UIUtil.scaleForGUI(30);

    @SuppressWarnings("FieldCanBeLocal")
    private final int BUTTON_WIDTH = UIUtil.scaleForGUI(100);

    @SuppressWarnings("FieldCanBeLocal")
    private final String IMAGE_ADDRESS = "data/images/misc/MekHQ.png";

    private int returnState;
    private CampaignPreset selectedPreset;

    /**
     * Preset selection result enumerations.
     */
    private enum PresetSelection {
        CANCELLED(0),
        APPLY(1),
        CUSTOMIZE(2);

        private final int value;

        PresetSelection(int value) {
            this.value = value;
        }

        /**
         * Returns the numerical value for this selection.
         *
         * @return the value associated with the selection
         */
        public int getValue() {
            return value;
        }
    }

    /**
     * Indicates if the user canceled the dialog.
     *
     * @return {@code true} if canceled, {@code false} otherwise
     */
    public boolean wasCanceled() {
        return returnState == PresetSelection.CANCELLED.getValue();
    }

    /**
     * Indicates if the user selected the 'apply' option.
     *
     * @return {@code true} if 'apply' was selected, {@code false} otherwise
     */
    public boolean wasApplied() {
        return returnState == PresetSelection.APPLY.getValue();
    }

    /**
     * Gets the selected campaign preset.
     *
     * @return the selected {@link CampaignPreset}, or {@code null} if none selected
     */
    public CampaignPreset getSelectedPreset() {
        return selectedPreset;
    }

    /**
     * Constructs and displays the campaign options preset picker dialog.
     *
     * @param parentFrame                  The parent {@link JFrame} for modal display.
     * @param includeCustomizePresetOption If {@code true}, enables the "Customize" button.
     */
    public CampaignOptionsPresetPicker(JFrame parentFrame, boolean includeCustomizePresetOption) {
        super(parentFrame, true);

        setTitle(getTextAt(RESOURCE_BUNDLE, "CampaignOptionsPresetPicker.title"));

        JPanel contentPanel = new JPanel(new BorderLayout(INSERT_SIZE, INSERT_SIZE));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(INSERT_SIZE, INSERT_SIZE, INSERT_SIZE, INSERT_SIZE));

        JLabel picLabel = new JLabel();
        picLabel.setHorizontalAlignment(JLabel.CENTER);
        ImageIcon imageIcon = new ImageIcon(IMAGE_ADDRESS);
        imageIcon = scaleImageIcon(imageIcon, IMAGE_HEIGHT, false);
        picLabel.setIcon(imageIcon);
        contentPanel.add(picLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, INSERT_SIZE));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextPane txtInstructions = new JTextPane();
        txtInstructions.setContentType("text/html");
        txtInstructions.setText(String.format("<body><div style='width:%spx;'><center>%s</center></div></body>",
              DIALOG_WIDTH, getTextAt(RESOURCE_BUNDLE, "CampaignOptionsPresetPicker.instructions")));
        txtInstructions.setEditable(false);
        txtInstructions.setOpaque(false);
        txtInstructions.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        txtInstructions.setBorder(
              BorderFactory.createCompoundBorder(
                    RoundedLineBorder.createRoundedLineBorder(),
                    BorderFactory.createEmptyBorder(INSERT_SIZE, INSERT_SIZE, INSERT_SIZE, INSERT_SIZE)
              )
        );
        txtInstructions.setAlignmentX(Component.CENTER_ALIGNMENT);

        // This prevents the instructions from resizing vertically
        Dimension instructionsPreferredSize = txtInstructions.getPreferredSize();
        txtInstructions.setMaximumSize(new Dimension(Integer.MAX_VALUE, instructionsPreferredSize.height));
        txtInstructions.setPreferredSize(new Dimension(DIALOG_WIDTH, instructionsPreferredSize.height));

        headerPanel.add(txtInstructions);
        headerPanel.add(Box.createVerticalStrut(INSERT_SIZE));

        // The description needs to be initialized before the combobox, else we won't be able to update the text based
        // on the selected item
        JTextPane txtDescription = new JTextPane();
        txtDescription.setContentType("text/html");
        txtDescription.setText(getTextAt(RESOURCE_BUNDLE, "CampaignOptionsPresetPicker.missingDescription"));
        txtDescription.setEditable(false);
        txtDescription.setOpaque(false);
        txtDescription.setBorder(BorderFactory.createEmptyBorder(INSERT_SIZE, INSERT_SIZE, INSERT_SIZE, INSERT_SIZE));
        txtDescription.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JScrollPane paneDescription = new JScrollPane(txtDescription);
        paneDescription.setBorder(RoundedLineBorder.createRoundedLineBorder());
        paneDescription.setAlignmentX(Component.CENTER_ALIGNMENT);
        paneDescription.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        final DefaultListModel<CampaignPreset> campaignPresets = new DefaultListModel<>();
        campaignPresets.addAll(CampaignPreset.getCampaignPresetsMetadata());

        if (campaignPresets.isEmpty()) {
            LOGGER.errorDialog("Error", "No campaign presets found");
        }

        JComboBox<CampaignPreset> cboPresets = new JComboBox<>();
        cboPresets.setModel(convertPresetListModelToComboBoxModel(campaignPresets));
        cboPresets.setAlignmentX(Component.CENTER_ALIGNMENT);
        cboPresets.setPreferredSize(new Dimension(DIALOG_WIDTH, SINGLE_LINE_HEIGHT));
        cboPresets.setMaximumSize(new Dimension(DIALOG_WIDTH, SINGLE_LINE_HEIGHT));
        cboPresets.addActionListener(e -> updateDescription(cboPresets, txtDescription));

        updateDescription(cboPresets, txtDescription);

        headerPanel.add(cboPresets);

        centerPanel.add(headerPanel, BorderLayout.NORTH);
        centerPanel.add(paneDescription, BorderLayout.CENTER);

        contentPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, INSERT_SIZE, 0));

        JButton btnCancel = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "CampaignOptionsPresetPicker.button.cancel"));
        btnCancel.addActionListener(e -> {
            returnState = PresetSelection.CANCELLED.getValue();
            dispose();
        });
        buttonPanel.add(btnCancel);

        JButton btnApply = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "CampaignOptionsPresetPicker.button.apply"));
        btnApply.addActionListener(e -> {
            selectedPreset = loadFullPreset((CampaignPreset) cboPresets.getSelectedItem());
            returnState = PresetSelection.APPLY.getValue();
            dispose();
        });
        buttonPanel.add(btnApply);

        JButton btnCustomize = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "CampaignOptionsPresetPicker.button.customize"));
        btnCustomize.setEnabled(includeCustomizePresetOption);
        btnCustomize.addActionListener(e -> {
            selectedPreset = loadFullPreset((CampaignPreset) cboPresets.getSelectedItem());
            returnState = PresetSelection.CUSTOMIZE.getValue();
            dispose();
        });
        buttonPanel.add(btnCustomize);

        Dimension buttonSize = new Dimension(BUTTON_WIDTH, SINGLE_LINE_HEIGHT);
        btnCancel.setPreferredSize(buttonSize);
        btnApply.setPreferredSize(buttonSize);
        btnCustomize.setPreferredSize(buttonSize);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
        pack();
        fitToScreen(parentFrame);
        setVisible(true);
    }

    /**
     * Limits the dialog's height and vertical position so it always fits within the usable area of the screen it is
     * shown on (for example, without slipping beneath the Windows taskbar). The width is left untouched, since it is
     * driven by {@code DIALOG_WIDTH} and always fits.
     *
     * <p>The screen bounds and insets are taken from the parent's {@link GraphicsConfiguration}, so the calculation
     * honors the active display's resolution and the operating system's DPI/zoom scaling, as well as multi-monitor
     * setups. The height is clamped to the usable height, the dialog is centered on the parent, and then nudged
     * vertically so its top and bottom edges stay within the usable bounds.</p>
     *
     * @param parentFrame the parent frame the dialog is shown relative to, used to pick the correct display
     */
    private void fitToScreen(@Nullable JFrame parentFrame) {
        GraphicsConfiguration graphicsConfiguration = (parentFrame != null)
              ? parentFrame.getGraphicsConfiguration()
              : null;
        if (graphicsConfiguration == null) {
            graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                          .getDefaultScreenDevice()
                                          .getDefaultConfiguration();
        }

        final Rectangle screenBounds = graphicsConfiguration.getBounds();
        final Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);

        final int usableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;

        // Never let the dialog grow taller than the usable area of the screen (for example, under the Windows
        // taskbar). The width is driven by DIALOG_WIDTH and always fits, so only the height needs clamping.
        setSize(getWidth(), Math.min(getHeight(), usableHeight));

        // Center on the parent, then nudge it vertically so its top and bottom edges stay out from under the taskbar.
        setLocationRelativeTo(parentFrame);

        final int minY = screenBounds.y + screenInsets.top;
        final int clampedY = Math.max(minY, Math.min(getY(), minY + usableHeight - getHeight()));
        setLocation(getX(), clampedY);
    }

    /**
     * Fully loads the campaign preset backing the lightweight metadata entry selected in the picker.
     *
     * <p>For speed, the picker only parses preset titles and descriptions up front (see
     * {@link CampaignPreset#getCampaignPresetsMetadata()}). The complete preset - campaign options, game options,
     * skills, and so on - is read from disk here, when the user commits to a selection.</p>
     *
     * @param metadataPreset the lightweight preset selected in the combo box, or {@code null} if none was selected
     *
     * @return the fully parsed {@link CampaignPreset}, or {@code null} if nothing was selected or it could not be
     *       loaded
     */
    private @Nullable CampaignPreset loadFullPreset(@Nullable CampaignPreset metadataPreset) {
        if (metadataPreset == null) {
            return null;
        }

        final File presetFile = metadataPreset.getPresetFile();
        if (presetFile == null) {
            // Every preset offered by the picker is read from a file, so a missing file is unexpected.
            LOGGER.error("Selected campaign preset '{}' has no source file and cannot be loaded.", metadataPreset);
            return null;
        }

        final CampaignPreset fullPreset = CampaignPreset.parseFromFile(presetFile);
        if (fullPreset == null) {
            LOGGER.error("Failed to fully load campaign preset from {}.", presetFile);
        }

        return fullPreset;
    }

    /**
     * Updates the description text pane with information about the selected campaign preset.
     *
     * @param cboPresets     The combo box containing the list of presets.
     * @param txtDescription The text pane to display the description.
     */
    private void updateDescription(JComboBox<CampaignPreset> cboPresets, JTextPane txtDescription) {
        Object selectedItem = cboPresets.getSelectedItem();
        String description = getTextAt(RESOURCE_BUNDLE, "CampaignOptionsPresetPicker.missingDescription");
        if (selectedItem instanceof CampaignPreset preset) {
            description = preset.getDescription();
        }

        txtDescription.setText(String.format("<body><div style='width:%spx;'><center>%s</center></div></body>",
              DIALOG_WIDTH, description));

        // Reset the scroll position to the top so a newly selected description always starts at its beginning.
        txtDescription.setCaretPosition(0);
    }

    /**
     * Converts a {@link DefaultListModel} containing {@link CampaignPreset} objects to a {@link DefaultComboBoxModel}.
     *
     * @param listModel the {@link DefaultListModel} of {@link CampaignPreset}
     *
     * @return a {@link DefaultComboBoxModel} containing the same elements
     */
    private DefaultComboBoxModel<CampaignPreset> convertPresetListModelToComboBoxModel(
          DefaultListModel<CampaignPreset> listModel) {
        DefaultComboBoxModel<CampaignPreset> comboBoxModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < listModel.size(); i++) {
            comboBoxModel.addElement(listModel.get(i));
        }

        return comboBoxModel;
    }
}
