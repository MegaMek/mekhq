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

import megamek.client.ui.baseComponents.AbstractSplitPane;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * This is the default SplitPane. It handles preferences, resources, the frame, and setup.
 *
 * Inheriting classes must call initialize() in their constructors and override createLeftComponent()
 * and createRightComponent()
 */
public abstract class AbstractMHQSplitPane extends AbstractSplitPane {
    //region Constructors
    /**
     * This creates an AbstractMHQSplitPane using the default MHQ resource bundle. This is the
     * normal constructor to use for an AbstractMHQSplitPane.
     */
    protected AbstractMHQSplitPane(final JFrame frame, final String name) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()), name);
    }

    /**
     * This creates an AbstractMHQSplitPane using the specified resource bundle. This is not recommended
     * by default.
     */
    protected AbstractMHQSplitPane(final JFrame frame, final ResourceBundle resources, final String name) {
        super(frame, resources, name);
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
