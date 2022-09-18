/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * This is the default Scrollable Panel implementation, designed to be used as a basic
 * implementation of AbstractMHQScrollablePanel for inline scrollable panels that can then be used
 * within a scrollpane. It handles the frame while ignoring preferences and resources.
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
     * This creates a DefaultMHQScrollablePanel using the default MHQ resource bundle and specified
     * double buffered boolean.
     */
    public DefaultMHQScrollablePanel(final JFrame frame, final String name,
                                     final boolean isDoubleBuffered) {
        super(frame, name, isDoubleBuffered);
    }

    /**
     * This creates a DefaultMHQScrollablePanel using the default MHQ resource bundle and specified
     * layout manager.
     */
    public DefaultMHQScrollablePanel(final JFrame frame, final String name,
                                     final LayoutManager layoutManager) {
        super(frame, name, layoutManager);
    }

    /**
     * This creates a DefaultMHQScrollablePanel using the default MHQ resource bundle and specified
     * layout manager and double buffered boolean.
     */
    public DefaultMHQScrollablePanel(final JFrame frame, final String name,
                                     final LayoutManager layoutManager,
                                     final boolean isDoubleBuffered) {
        super(frame, name, layoutManager, isDoubleBuffered);
    }

    /**
     * This creates a DefaultMHQScrollablePanel using the specified resource bundle, layout manager,
     * and double buffered boolean. This is not recommended by default.
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
