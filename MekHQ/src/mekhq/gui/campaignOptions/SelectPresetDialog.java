/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.swing.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.MekHQ;
import mekhq.gui.campaignOptions.components.CampaignOptionsButton;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createGroupLayout;

/**
 * A dialog for selecting campaign presets. Extends {@link JDialog}.
 * Keeps track of the selected preset and return state.
 * Provides options to select a preset, customize a preset, or cancel the operation.
 */
public class SelectPresetDialog extends JDialog {
    private static String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE,
        MekHQ.getMHQOptions().getLocale());

    private static final MMLogger logger = MMLogger.create(SelectPresetDialog.class);

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
     * @param frame                     the parent {@link JFrame} for the dialog
     * @param includePresetSelectOption whether to include the option to select a preset
     * @param includeCustomizePresetOption whether to include the option to customize a preset
     */
    public SelectPresetDialog(JFrame frame, boolean includePresetSelectOption, boolean includeCustomizePresetOption) {
        super(frame, resources.getString("presetDialog.title"), true);
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);
        final int INSERT_SIZE = UIUtil.scaleForGUI(10);
        returnState = PRESET_SELECTION_CANCELLED;

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        ImageIcon imageIcon = new ImageIcon("data/images/misc/megamek-splash.png");

        int width = UIUtil.scaleForGUI(imageIcon.getIconWidth());
        int height = UIUtil.scaleForGUI(imageIcon.getIconHeight());

        Image image = imageIcon.getImage();
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        imageIcon = new ImageIcon(scaledImage);
        JLabel imageLabel = new JLabel(imageIcon);
        add(imageLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        final GroupLayout layout = createGroupLayout(centerPanel);
        centerPanel.setLayout(layout);

        JLabel descriptionLabel = new JLabel(String.format(
            "<html><body><div style='width:%spx;'><center>%s</center></div></body></html>",
            DIALOG_WIDTH, resources.getString("presetDialog.description")));

        final DefaultListModel<CampaignPreset> campaignPresets = new DefaultListModel<>();
        campaignPresets.addAll(CampaignPreset.getCampaignPresets());

        if (campaignPresets.isEmpty()) {
            logger.error("No campaign presets found", "Error");
        }

        JComboBox<CampaignPreset> comboBox = new JComboBox<>();
        comboBox.setModel(convertPresetListModelToComboBoxModel(campaignPresets));

        DefaultListCellRenderer listRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected,
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

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(descriptionLabel)
                .addPreferredGap(ComponentPlacement.UNRELATED, INSERT_SIZE, INSERT_SIZE)
                .addComponent(comboBox)
        );

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.CENTER)
                .addComponent(descriptionLabel)
                .addComponent(comboBox)
        );

        JPanel outerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;

        // Add padding/margin to outerPanel layout using an empty border
        outerPanel.setBorder(BorderFactory.createEmptyBorder(INSERT_SIZE, INSERT_SIZE, INSERT_SIZE, INSERT_SIZE));
        outerPanel.add(centerPanel, gbc);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
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

        JPanel buttonPanel = new JPanel();

        JButton buttonSelect = new CampaignOptionsButton("PresetDialogSelect");
        buttonSelect.addActionListener(e -> {
            selectedPreset = (CampaignPreset) comboBox.getSelectedItem();
            returnState = PRESET_SELECTION_SELECT;
            dispose();
        });
        buttonSelect.setEnabled(includePresetSelectOption);
        buttonPanel.add(buttonSelect);

        JButton buttonCustomize = new CampaignOptionsButton("PresetDialogCustomize");
        buttonCustomize.addActionListener(e -> {
            selectedPreset = (CampaignPreset) comboBox.getSelectedItem();
            returnState = PRESET_SELECTION_CUSTOMIZE;
            dispose();
        });
        buttonCustomize.setEnabled(includeCustomizePresetOption);
        buttonPanel.add(buttonCustomize);

        JButton buttonCancel = new CampaignOptionsButton("PresetDialogCancel");
        buttonCancel.addActionListener(e -> {
            returnState = PRESET_SELECTION_CANCELLED;
            dispose();
        });
        buttonPanel.add(buttonCancel);

        add(buttonPanel, BorderLayout.PAGE_END);

        pack();
        setAlwaysOnTop(true);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Converts a {@link DefaultListModel} of {@link CampaignPreset} objects to a {@link DefaultComboBoxModel}.
     *
     * @param listModel The {@link DefaultListModel} to convert.
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
