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
package mekhq.gui.renderers;

import megamek.common.util.EncodeControl;
import mekhq.campaign.CampaignPreset;
import mekhq.gui.panels.CampaignPresetPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class CampaignPresetRenderer extends CampaignPresetPanel implements ListCellRenderer<CampaignPreset> {
    //region Constructors
    public CampaignPresetRenderer(final JFrame frame) {
        super(frame, null);
    }
    //endregion Constructors

    @Override
    public Component getListCellRendererComponent(final JList<? extends CampaignPreset> list,
                                                  final CampaignPreset value, final int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
        // JTextArea::setForeground and JTextArea::setBackground don't work properly with the
        // default return, but by recreating the colour it works properly
        final Color foreground = new Color(UIManager.getColor(isSelected
                ? "Table.selectionForeground" : "Table.foreground").getRGB());
        final Color background = new Color(UIManager.getColor(isSelected
                ? "Table.selectionBackground" : "Table.background").getRGB());
        setForeground(foreground);
        setBackground(background);

        getTxtDescription().setForeground(foreground);
        getTxtDescription().setBackground(background);

        updateFromPreset(value);

        return this;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(300, 100);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 120);
    }
}
