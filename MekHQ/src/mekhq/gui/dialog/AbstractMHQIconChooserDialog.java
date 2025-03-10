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
 */
package mekhq.gui.dialog;

import megamek.client.ui.dialogs.AbstractIconChooserDialog;
import mekhq.MekHQ;
import mekhq.gui.panels.AbstractMHQIconChooser;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * AbstractMHQIconChooserDialog is an extension of AbstractIconChooserDialog that moves the
 * preferences and localization to MekHQ.
 * @see AbstractIconChooserDialog
 */
public abstract class AbstractMHQIconChooserDialog extends AbstractIconChooserDialog {
    //region Constructors
    protected AbstractMHQIconChooserDialog(final JFrame frame, final String name, final String title,
                                           final AbstractMHQIconChooser chooser) {
        super(frame, true, ResourceBundle.getBundle("mekhq.resources.GUI",
                        MekHQ.getMHQOptions().getLocale()),
                name, title, chooser, false);
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
    //endregion Initialization
}
