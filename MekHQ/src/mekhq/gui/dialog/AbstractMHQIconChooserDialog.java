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
package mekhq.gui.dialog;

import megamek.client.ui.dialogs.AbstractIconChooserDialog;
import megamek.common.util.EncodeControl;
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
                        MekHQ.getMHQOptions().getLocale(), new EncodeControl()),
                name, title, chooser, false);
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected void setPreferences() {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }
    //endregion Initialization
}
