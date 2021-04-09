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

import megamek.common.annotations.Nullable;
import mekhq.campaign.GamePreset;
import mekhq.gui.renderers.CampaignPresetRenderer;

import javax.swing.*;
import java.awt.*;

public class CampaignPresetSelectionPanel extends JPanel {
    //region Variable Declarations
    private JList<GamePreset> presets;
    //endregion Variable Declarations

    //region Constructors
    public CampaignPresetSelectionPanel() {
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public JList<GamePreset> getPresets() {
        return presets;
    }

    public void setPresets(final JList<GamePreset> presets) {
        this.presets = presets;
    }

    public @Nullable GamePreset getSelectedPreset() {
        return getPresets().getSelectedValue();
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize() {
        setName("campaignPresetSelectionPanel");
        setLayout(new GridLayout(1, 1));
        setMinimumSize(new Dimension(335, 130));
        setPreferredSize(new Dimension(500, 400));

        DefaultListModel<GamePreset> listModel = new DefaultListModel<>();
        listModel.addAll(GamePreset.getGamePresetsIn());

        setPresets(new JList<>(listModel));
        getPresets().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getPresets().setSelectedIndex(0);
        getPresets().setLayoutOrientation(JList.VERTICAL);
        getPresets().setCellRenderer(new CampaignPresetRenderer());

        add(new JScrollPane(getPresets(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
    }
    //endregion Initialization
}
