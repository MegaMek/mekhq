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
package mekhq.gui.baseComponents;

import megamek.client.ui.baseComponents.AbstractScrollPane;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * This is the default ScrollPane. It handles preferences, resources, and the frame.
 *
 * Inheriting classes must call initialize() in their constructors and override initialize().
 */
public abstract class AbstractMHQScrollPane extends AbstractScrollPane {
    //region Constructors
    /**
     * This creates an AbstractMHQScrollPane using the default MHQ resource bundle and using the
     * default of vertical and horizontal scrollbars as required.
     */
    protected AbstractMHQScrollPane(final JFrame frame, final String name) {
        this(frame, name, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * This creates an AbstractMHQScrollPane using the default MHQ resource bundle and using the
     * specified scrollbar policies.
     */
    protected AbstractMHQScrollPane(final JFrame frame, final String name,
                                    final int verticalScrollBarPolicy, final int horizontalScrollBarPolicy) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()),
                name, verticalScrollBarPolicy, horizontalScrollBarPolicy);
    }

    /**
     * This creates an AbstractMHQScrollPane using the specified resource bundle and using the
     * default of vertical and horizontal scrollbars as required. This is not recommended by default.
     */
    protected AbstractMHQScrollPane(final JFrame frame, final ResourceBundle resources, final String name,
                                    final int verticalScrollBarPolicy, final int horizontalScrollBarPolicy) {
        super(frame, resources, name, verticalScrollBarPolicy, horizontalScrollBarPolicy);
    }
    //endregion Constructors

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek
     */
    @Override
    protected void setPreferences() {
        setPreferences(MekHQ.getPreferences().forClass(getClass()));
    }
}
