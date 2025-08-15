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

import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.util.ResourceBundle;
import javax.swing.JFrame;

import megamek.client.ui.panels.abstractPanels.AbstractScrollablePanel;
import mekhq.MekHQ;

/**
 * This is the default Scrollable Panel, implementing Scrollable and designed to be used within a ScrollPane, preferably
 * AbstractMHQScrollPane. It handles preferences, resources, and the frame.
 * <p>
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
     * This creates an AbstractMHQScrollablePanel using the default MHQ resource bundle and specified double buffered
     * boolean.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final String name,
          final boolean isDoubleBuffered) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI",
                    MekHQ.getMHQOptions().getLocale()),
              name, new FlowLayout(), isDoubleBuffered);
    }

    /**
     * This creates an AbstractMHQScrollablePanel using the default MHQ resource bundle and specified layout manager.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final String name,
          final LayoutManager layoutManager) {
        this(frame, name, layoutManager, true);
    }

    /**
     * This creates an AbstractMHQScrollablePanel using the default MHQ resource bundle and specified layout manager and
     * double buffered boolean.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final String name,
          final LayoutManager layoutManager,
          final boolean isDoubleBuffered) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI",
                    MekHQ.getMHQOptions().getLocale()),
              name, layoutManager, isDoubleBuffered);
    }

    /**
     * This creates an AbstractMHQScrollablePanel using the specified resource bundle, layout manager, and double
     * buffered boolean. This is not recommended by default.
     */
    protected AbstractMHQScrollablePanel(final JFrame frame, final ResourceBundle resources,
          final String name, final LayoutManager layoutManager,
          final boolean isDoubleBuffered) {
        super(frame, resources, name, layoutManager, isDoubleBuffered);
    }
    //endregion Constructors

    //region Initialization

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     *
     * @throws Exception if there's an issue initializing the preferences. Normally this means a component has
     *                   <strong>not</strong> had its name value set.
     */
    @Override
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
    //endregion Initialization
}
