/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.baseComponents;

import java.awt.*;

import javax.swing.*;

/**
 * JScrollablePanel is an an extension of JPanel that implements scrollable, so that it can be
 * properly used within a JScrollPane.
 *
 * @author aarong original author
 */
public class JScrollablePanel extends JPanel implements Scrollable {
    //region Variable Declarations
    private static final long serialVersionUID = -1422419969984249050L;

    // by default, track the width, and re-size as needed.
    private boolean trackViewportWidth = true;
    //endregion Variable Declarations

    //region Constructors
    /**
     * @see JPanel#JPanel(LayoutManager, boolean)
     */
    public JScrollablePanel(final LayoutManager layout, final boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    /**
     * @see JPanel#JPanel(LayoutManager)
     */
    public JScrollablePanel(final LayoutManager layout) {
        super(layout);
    }

    /**
     * @see JPanel#JPanel(boolean)
     */
    public JScrollablePanel(final boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    /**
     * @see JPanel#JPanel()
     */
    public JScrollablePanel() {
        super();
    }
    //endregion Constructors

    //region Setters
    public void setTracksViewportWidth(final boolean trackViewportWidth) {
        this.trackViewportWidth = trackViewportWidth;
    }
    //endregion Setters

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        // tell the JScrollPane that we want to be our 'preferredSize' - but later, we'll say that
        // vertically, it should scroll.
        return super.getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation,
                                          final int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(final Rectangle visible, final int orientation,
                                           final int direction) {
        return (SwingConstants.VERTICAL == orientation) ? visible.height : visible.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return trackViewportWidth;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false; //we don't want to track the height, because we want to scroll vertically.
    }
}
