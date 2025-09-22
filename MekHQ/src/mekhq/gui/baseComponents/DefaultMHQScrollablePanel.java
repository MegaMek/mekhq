/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.LayoutManager;
import java.util.ResourceBundle;
import javax.swing.JFrame;

/**
 * This is the default Scrollable Panel implementation, designed to be used as a basic implementation of
 * AbstractMHQScrollablePanel for inline scrollable panels that can then be used within a scroll pane. It handles the
 * frame while ignoring preferences and resources.
 */
public final class DefaultMHQScrollablePanel extends AbstractMHQScrollablePanel {
    //region Constructors

    /**
     * This creates a DefaultMHQScrollablePanel using the default MHQ resource bundle.
     */
    public DefaultMHQScrollablePanel(final JFrame frame, final String name) {
        super(frame, name);
    }

    /**
     * This creates a DefaultMHQScrollablePanel using the default MHQ resource bundle and specified double buffered
     * boolean.
     */
    public DefaultMHQScrollablePanel(final JFrame frame, final String name,
          final boolean isDoubleBuffered) {
        super(frame, name, isDoubleBuffered);
    }

    /**
     * This creates a DefaultMHQScrollablePanel using the default MHQ resource bundle and specified layout manager.
     */
    public DefaultMHQScrollablePanel(final JFrame frame, final String name,
          final LayoutManager layoutManager) {
        super(frame, name, layoutManager);
    }

    /**
     * This creates a DefaultMHQScrollablePanel using the default MHQ resource bundle and specified layout manager and
     * double buffered boolean.
     */
    public DefaultMHQScrollablePanel(final JFrame frame, final String name,
          final LayoutManager layoutManager,
          final boolean isDoubleBuffered) {
        super(frame, name, layoutManager, isDoubleBuffered);
    }

    /**
     * This creates a DefaultMHQScrollablePanel using the specified resource bundle, layout manager, and double buffered
     * boolean. This is not recommended by default.
     */
    public DefaultMHQScrollablePanel(final JFrame frame, final ResourceBundle resources,
          final String name, final LayoutManager layoutManager,
          final boolean isDoubleBuffered) {
        super(frame, resources, name, layoutManager, isDoubleBuffered);
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected void initialize() {
        // Ignore initialization, as that will be handled instead by the classes where this is used
    }
    //endregion Initialization
}
