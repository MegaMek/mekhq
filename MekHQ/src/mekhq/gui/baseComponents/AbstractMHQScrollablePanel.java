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

import megamek.client.ui.baseComponents.AbstractScrollablePanel;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * This is the default Scrollable Panel, implementing Scrollable and designed to be used within a
 * ScrollPane, preferably AbstractMHQScrollPane. It handles preferences, resources, and the frame.
 *
 * Inheriting classes must call initialize() in their constructors and override initialize().
 */
public abstract class AbstractMHQScrollablePanel extends AbstractScrollablePanel {
    //region Constructors
    /**
     * This creates an AbstractMHQScrollablePanel using the default MHQ resource bundle.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final String name) {
        this(frame, name, true);
    }

    /**
     * This creates an AbstractMHQScrollablePanel using the default MHQ resource bundle and
     * specified double buffered boolean.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final String name,
                                         final boolean isDoubleBuffered) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI",
                        MekHQ.getMHQOptions().getLocale(), new EncodeControl()),
                name, new FlowLayout(), isDoubleBuffered);
    }

    /**
     * This creates an AbstractMHQScrollablePanel using the default MHQ resource bundle and
     * specified layout manager.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final String name,
                                         final LayoutManager layoutManager) {
        this(frame, name, layoutManager, true);
    }

    /**
     * This creates an AbstractMHQScrollablePanel using the default MHQ resource bundle and
     * specified layout manager and double buffered boolean.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final String name,
                                         final LayoutManager layoutManager,
                                         final boolean isDoubleBuffered) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI",
                        MekHQ.getMHQOptions().getLocale(), new EncodeControl()),
                name, layoutManager, isDoubleBuffered);
    }

    /**
     * This creates an AbstractMHQScrollablePanel using the specified resource bundle, layout
     * manager, and double buffered boolean. This is not recommended by default.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final ResourceBundle resources,
                                         final String name, final LayoutManager layoutManager,
                                         final boolean isDoubleBuffered) {
        super(frame, resources, name, layoutManager, isDoubleBuffered);
    }
    //endregion Constructors

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     * @throws Exception if there's an issue initializing the preferences. Normally this means
     * a component has <strong>not</strong> had its name value set.
     */
    @Override
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
}
