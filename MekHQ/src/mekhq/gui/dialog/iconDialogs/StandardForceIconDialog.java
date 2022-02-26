/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.gui.dialog.AbstractMHQIconChooserDialog;
import mekhq.gui.panels.AbstractMHQIconChooser;
import mekhq.gui.panels.StandardForceIconChooser;

import javax.swing.*;

/**
 * StandardForceIconDialog is an implementation of AbstractMHQIconChooserDialog that is used to
 * select a StandardForceIcon from the Force Icon Directory.
 * @see AbstractMHQIconChooserDialog
 * @see AbstractIconChooserDialog
 */
public class StandardForceIconDialog extends AbstractMHQIconChooserDialog {
    //region Constructors
    public StandardForceIconDialog(final JFrame frame, final @Nullable AbstractIcon icon) {
        this(frame, "StandardForceIconDialog", "StandardForceIconDialog.title",
                new StandardForceIconChooser(frame, icon));
    }

    protected StandardForceIconDialog(final JFrame frame, final String name, final String title,
                                      final AbstractMHQIconChooser chooser) {
        super(frame, name, title, chooser);
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
