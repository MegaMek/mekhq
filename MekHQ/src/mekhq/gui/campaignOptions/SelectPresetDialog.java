/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createGroupLayout;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.components.CampaignOptionsButton;

/**
 * A dialog for selecting campaign presets. Extends {@link JDialog}. Keeps track of the selected preset and return
 * state. Provides options to select a preset, customize a preset, or cancel the operation.
 */
@Deprecated(since = "0.50.07", forRemoval = true)
public class SelectPresetDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SelectPresetDialog.class);

    private int returnState;
    public static final int PRESET_SELECTION_CANCELLED = 0;
    public static final int PRESET_SELECTION_SELECT = 1;
    public static final int PRESET_SELECTION_CUSTOMIZE = 2;
    private CampaignPreset selectedPreset;

    /**
     * Returns the current return state of the dialog.
     * <p>{@code PRESET_SELECTION_CANCELLED} = 0
     * <p>{@code PRESET_SELECTION_SELECT} = 1
     * <p>{@code PRESET_SELECTION_CUSTOMIZE} = 2
     *
     * @return An integer representing the return state of the dialog.
     */
    public int getReturnState() {
        return returnState;
    }

    /**
     * @return The {@link CampaignPreset} that was selected.
     */
    public CampaignPreset getSelectedPreset() {
        return selectedPreset;
    }

    /**
     * Constructs a dialog window for selecting a campaign preset.
     *
     * @param frame                        the parent {@link JFrame} for the dialog
     * @param includePresetSelectOption    whether to include the option to select a preset
     * @param includeCustomizePresetOption whether to include the option to customize a preset
     */
    public SelectPresetDialog(JFrame frame, boolean includePresetSelectOption, boolean includeCustomizePresetOption) {
        super(frame, getTextAt(getCampaignOptionsResourceBundle(), "presetDialog.title"), true);
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);
        final int INSERT_SIZE = UIUtil.scaleForGUI(10);
        returnState = PRESET_SELECTION_CANCELLED;

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        ImageIcon imageIcon = new ImageIcon("data/images/misc/megamek-splash.png");
        imageIcon = scaleImageIcon(imageIcon, 400, true);

        JLabel imageLabel = new JLabel(imageIcon);
        add(imageLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        final GroupLayout layout = createGroupLayout(centerPanel);
        centerPanel.setLayout(layout);

        JLabel descriptionLabel = new JLabel(String.format(
              "<html><body><div style='width:%spx;'><center>%s</center></div></body></html>",
              DIALOG_WIDTH, getTextAt(getCampaignOptionsResourceBundle(), "presetDialog.description")));

        final DefaultListModel<CampaignPreset> campaignPresets = new DefaultListModel<>();
        campaignPresets.addAll(CampaignPreset.getCampaignPresets());

        if (campaignPresets.isEmpty()) {
            LOGGER.errorDialog("Error", "No campaign presets found");
        }

        JComboBox<CampaignPreset> comboBox = new JComboBox<>();
        comboBox.setModel(convertPresetListModelToComboBoxModel(campaignPresets));

        DefaultListCellRenderer listRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                if (value instanceof CampaignPreset preset) {
                    setText(preset.getTitle());
                }

                setHorizontalAlignment(JLabel.CENTER);

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                return this;
            }
        };
        comboBox.setRenderer(listRenderer);

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addComponent(descriptionLabel)
                                      .addPreferredGap(ComponentPlacement.UNRELATED, INSERT_SIZE, INSERT_SIZE)
                                      .addComponent(comboBox));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
                                        .addComponent(descriptionLabel)
                                        .addComponent(comboBox));

        JPanel outerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;

        // Add padding/margin to outerPanel layout using an empty border
        outerPanel.setBorder(BorderFactory.createEmptyBorder(INSERT_SIZE, INSERT_SIZE, INSERT_SIZE, INSERT_SIZE));
        outerPanel.add(centerPanel, gbc);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        JLabel newLabel = new JLabel();
        newLabel.setHorizontalAlignment(JLabel.CENTER);
        bottomPanel.add(newLabel);

        // Add 10px gap between outerPanel and bottomPanel
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 0, 0, 0);
        outerPanel.add(bottomPanel, gbc);

        comboBox.addActionListener(e -> {
            Object selectedItem = comboBox.getSelectedItem();
            if (selectedItem instanceof CampaignPreset preset) {
                newLabel.setText(String.format(
                      "<html><body><div style='width:%spx;'><center>%s</center></div></body></html>",
                      DIALOG_WIDTH * 0.9,
                      preset.getDescription()));
            } else {
                newLabel.setText(String.format(
                      "<html><body><div style='width:%spx;'><center>%s</center></div></body></html>",
                      DIALOG_WIDTH * 0.9,
                      "No description available."));
            }
            revalidate();
            repaint();
        });

        // Set the initial text in newLabel based on the currently selected item in comboBox
        Object initialItem = comboBox.getSelectedItem();
        if (initialItem instanceof CampaignPreset preset) {
            newLabel.setText(String.format(
                  "<html><body><div style='width:%spx;'><center>%s</center></div></body></html>",
                  DIALOG_WIDTH * 0.9,
                  preset.getDescription()));
        } else {
            newLabel.setText(String.format(
                  "<html><body><div style='width:%spx;'><center>%s</center></div></body></html>",
                  DIALOG_WIDTH * 0.9,
                  "No description available."));
        }

        add(outerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = getButtonPanel(includePresetSelectOption, includeCustomizePresetOption, comboBox);

        add(buttonPanel, BorderLayout.PAGE_END);

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screen = toolkit.getScreenSize();
        Insets insets = getToolkit().getScreenInsets(getGraphicsConfiguration());

        int maxHeight = screen.height - insets.top - insets.bottom;

        pack();
        Dimension size = getSize();
        if (size.height > maxHeight) {
            setSize(new Dimension(size.width, maxHeight));
        }

        setAlwaysOnTop(true);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel getButtonPanel(boolean includePresetSelectOption, boolean includeCustomizePresetOption,
          JComboBox<CampaignPreset> comboBox) {
        JPanel buttonPanel = new JPanel();

        RoundedJButton buttonSelect = new CampaignOptionsButton("PresetDialogSelect");
        buttonSelect.addActionListener(e -> {
            selectedPreset = (CampaignPreset) comboBox.getSelectedItem();
            returnState = PRESET_SELECTION_SELECT;
            dispose();
        });
        buttonSelect.setEnabled(includePresetSelectOption);
        buttonPanel.add(buttonSelect);

        RoundedJButton buttonCustomize = new CampaignOptionsButton("PresetDialogCustomize");
        buttonCustomize.addActionListener(e -> {
            selectedPreset = (CampaignPreset) comboBox.getSelectedItem();
            returnState = PRESET_SELECTION_CUSTOMIZE;
            dispose();
        });
        buttonCustomize.setEnabled(includeCustomizePresetOption);
        buttonPanel.add(buttonCustomize);

        RoundedJButton buttonCancel = new CampaignOptionsButton("PresetDialogCancel");
        buttonCancel.addActionListener(e -> {
            returnState = PRESET_SELECTION_CANCELLED;
            dispose();
        });
        buttonPanel.add(buttonCancel);
        return buttonPanel;
    }

    /**
     * Converts a {@link DefaultListModel} of {@link CampaignPreset} objects to a {@link DefaultComboBoxModel}.
     *
     * @param listModel The {@link DefaultListModel} to convert.
     *
     * @return The converted {@link DefaultComboBoxModel}.
     */
    private DefaultComboBoxModel<CampaignPreset> convertPresetListModelToComboBoxModel(
          DefaultListModel<CampaignPreset> listModel) {

        // Create a new DefaultComboBoxModel
        DefaultComboBoxModel<CampaignPreset> comboBoxModel = new DefaultComboBoxModel<>();

        // Populate the DefaultComboBoxModel with the elements from the DefaultListModel
        for (int i = 0; i < listModel.size(); i++) {
            comboBoxModel.addElement(listModel.get(i));
        }

        return comboBoxModel;
    }
}
