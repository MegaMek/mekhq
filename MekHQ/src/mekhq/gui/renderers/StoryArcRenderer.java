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

import mekhq.campaign.storyarc.StoryArcStub;
import mekhq.gui.panels.StoryArcPanel;

import javax.swing.*;
import java.awt.*;

public class StoryArcRenderer extends StoryArcPanel implements ListCellRenderer<StoryArcStub> {
    //region Constructors
    public StoryArcRenderer(final JFrame frame) {
        super(frame, null, null);
    }
    //endregion Constructors

    @Override
    public Component getListCellRendererComponent(final JList<? extends StoryArcStub> list,
                                                  final StoryArcStub value, final int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
        // JTextArea::setForeground and JTextArea::setBackground don't work properly with the
        // default return, but by recreating the colour it works properly
        final Color foreground = new Color((isSelected
                ? list.getSelectionForeground() : list.getForeground()).getRGB());
        final Color background = new Color((isSelected
                ? list.getSelectionBackground() : list.getBackground()).getRGB());
        setForeground(foreground);
        setBackground(background);

        getTxtDetails().setForeground(foreground);
        getTxtDetails().setBackground(background);

        updateFromStoryArcStub(value);
        this.revalidate();

        return this;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(250, 75);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(250, 75);
    }
}
