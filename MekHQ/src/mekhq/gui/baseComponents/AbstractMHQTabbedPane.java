/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.gui.baseComponents;

import megamek.client.ui.baseComponents.AbstractTabbedPane;
import mekhq.MekHQ;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * This is the default TabbedPane. It handles preferences, resources, and the frame.
 *
 * Inheriting classes must call initialize() in their constructors and override initialize()
 */
public abstract class AbstractMHQTabbedPane extends AbstractTabbedPane {
    //region Constructors
    /**
     * This creates an AbstractMHQTabbedPane using the default MHQ resource bundle. This is the
     * normal constructor to use for an AbstractMHQTabbedPane.
     */
    protected AbstractMHQTabbedPane(final JFrame frame, final String name) {
        this(frame, ResourceBundle.getBundle("mekhq.resources.GUI",
                MekHQ.getMHQOptions().getLocale()), name);
    }

    /**
     * This creates an AbstractMHQTabbedPane using the specified resource bundle. This is not recommended
     * by default.
     */
    protected AbstractMHQTabbedPane(final JFrame frame, final ResourceBundle resources, final String name) {
        super(frame, resources, name);
    }
    //endregion Constructors

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek
     */
    @Override
    protected void setPreferences() {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
}
