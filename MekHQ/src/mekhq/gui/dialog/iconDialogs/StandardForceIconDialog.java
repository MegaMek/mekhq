/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.iconDialogs;

import megamek.client.ui.dialogs.AbstractIconChooserDialog;
import megamek.client.ui.panels.AbstractIconChooser;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.gui.panels.StandardForceIconChooser;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * StandardForceIconDialog is an implementation of AbstractIconChooserDialog that is used to select
 * a StandardForceIcon from the Force Icon Directory.
 * @see AbstractIconChooserDialog
 */
public class StandardForceIconDialog extends AbstractIconChooserDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 2690083417720266231L;
    //endregion Variable Declarations

    //region Constructors
    public StandardForceIconDialog(final JFrame frame, final @Nullable AbstractIcon icon) {
        this(frame, "StandardForceIconDialog", "StandardForceIconDialog.title",
                new StandardForceIconChooser(frame, icon));
    }

    public StandardForceIconDialog(final JFrame frame, final String name, final String title,
                                   final AbstractIconChooser chooser) {
        super(frame, true, ResourceBundle.getBundle("mekhq.resources.GUI",
                        MekHQ.getMHQOptions().getLocale(), new EncodeControl()),
                name, title, chooser, false);
    }
    //endregion Constructors

    //region Getters
    @Override
    protected StandardForceIconChooser getChooser() {
        return (StandardForceIconChooser) super.getChooser();
    }

    @Override
    public @Nullable StandardForceIcon getSelectedItem() {
        return getChooser().getSelectedItem();
    }
    //endregion Getters
}
