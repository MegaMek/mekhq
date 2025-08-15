/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import megamek.client.ui.panes.AbstractScrollPane;
import mekhq.MekHQ;

/**
 * This is the default ScrollPane. It handles preferences, resources, and the frame.
 * <p>
 * Inheriting classes must call initialize() in their constructors and override initialize().
 */
public abstract class AbstractMHQScrollPane extends AbstractScrollPane {
    //region Constructors

    /**
     * This creates an AbstractMHQScrollPane using the default MHQ resource bundle and using the default of vertical and
     * horizontal scrollbars as required.
     */
    protected AbstractMHQScrollPane(final JFrame frame, final String name) {
        this(frame, name, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * This creates an AbstractMHQScrollPane using the default MHQ resource bundle and using the specified scrollbar
     * policies.
     */
    protected AbstractMHQScrollPane(final JFrame frame, final String name,
          final int verticalScrollBarPolicy, final int horizontalScrollBarPolicy) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI",
                    MekHQ.getMHQOptions().getLocale()),
              name, verticalScrollBarPolicy, horizontalScrollBarPolicy);
    }

    /**
     * This creates an AbstractMHQScrollPane using the specified resource bundle and using the default of vertical and
     * horizontal scrollbars as required. This is not recommended by default.
     */
    protected AbstractMHQScrollPane(final JFrame frame, final ResourceBundle resources, final String name,
          final int verticalScrollBarPolicy, final int horizontalScrollBarPolicy) {
        super(frame, resources, name, verticalScrollBarPolicy, horizontalScrollBarPolicy);
    }
    //endregion Constructors

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek
     *
     * @throws Exception if there's an issue initializing the preferences. Normally this means a component has
     *                   <strong>not</strong> had its name value set.
     */
    @Override
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
}
