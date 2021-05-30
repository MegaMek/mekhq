/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import megamek.client.ui.baseComponents.MMButton;
import megamek.common.annotations.Nullable;
import megamek.common.util.EncodeControl;
import mekhq.campaign.CampaignPreset;
import mekhq.gui.dialog.CampaignPresetCustomizationDialog;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class CampaignPresetPanel extends JPanel {
    //region Variable Declarations
    private final JFrame frame;
    private JLabel lblTitle;
    private JTextArea txtDescription;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public CampaignPresetPanel(final JFrame frame, final @Nullable CampaignPreset preset) {
        this.frame = frame;
        initialize(preset);
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

    public JLabel getLblTitle() {
        return lblTitle;
    }

    public void setLblTitle(final JLabel lblTitle) {
        this.lblTitle = lblTitle;
    }

    public JTextArea getTxtDescription() {
        return txtDescription;
    }

    public void setTxtDescription(final JTextArea txtDescription) {
        this.txtDescription = txtDescription;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize(final @Nullable CampaignPreset preset) {
        // Setup the Panel
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(Color.BLACK, 2)));
        setName("campaignPresetPanel");
        setLayout(new GridBagLayout());

        // Create the Constraints


        // Create Components and Layout
        setLblTitle(new JLabel(""));
        getLblTitle().setName("lblTitle");
        getLblTitle().setAlignmentX(Component.CENTER_ALIGNMENT);

        setTxtDescription(new JTextArea(""));
        getTxtDescription().setName("txtDescription");
        getTxtDescription().setEditable(false);
        getTxtDescription().setLineWrap(true);
        getTxtDescription().setWrapStyleWord(true);

        if ((preset != null) && preset.isUserData()) {
            final JButton btnEditPreset = new MMButton("btnEditPreset", resources.getString("Edit.text"),
                    resources.getString("btnEditPreset.toolTipText"), evt -> {
                final CampaignPresetCustomizationDialog dialog = new CampaignPresetCustomizationDialog(frame, preset);
                if (dialog.showDialog().isConfirmed()) {
                    dialog.updatePreset(preset);
                    updateFromPreset(preset);
                }
            });
        }
    }
    //endregion Initialization

    protected void updateFromPreset(final CampaignPreset preset) {
        getLblTitle().setText(preset.getTitle());
        getTxtDescription().setText(preset.getDescription());
    }
}
