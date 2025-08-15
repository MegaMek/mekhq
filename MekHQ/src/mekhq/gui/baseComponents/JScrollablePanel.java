/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.baseComponents;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * JScrollablePanel is an extension of JPanel that implements scrollable, so that it can be properly used within a
 * JScrollPane.
 *
 * @author aarong original author
 */
public class JScrollablePanel extends JPanel implements Scrollable {
    //region Variable Declarations
    // by default, track the width, and re-size as needed.
    private boolean trackViewportWidth = true;
    //endregion Variable Declarations

    //region Constructors

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
