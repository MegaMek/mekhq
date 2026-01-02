/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.iconDialogs;

import javax.swing.JFrame;

import megamek.client.ui.dialogs.iconChooser.AbstractIconChooserDialog;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.gui.dialog.AbstractMHQIconChooserDialog;
import mekhq.gui.panels.AbstractMHQIconChooser;
import mekhq.gui.panels.StandardFormationIconChooser;

/**
 * StandardFormationIconDialog is an implementation of AbstractMHQIconChooserDialog that is used to select a
 * StandardFormationIcon from the Formation Icon Directory.
 *
 * @see AbstractMHQIconChooserDialog
 * @see AbstractIconChooserDialog
 */
public class StandardFormationIconDialog extends AbstractMHQIconChooserDialog {
    //region Constructors
    public StandardFormationIconDialog(final JFrame frame, final @Nullable AbstractIcon icon) {
        this(frame, "StandardFormationIconDialog", "StandardFormationIconDialog.title",
              new StandardFormationIconChooser(frame, icon));
    }

    protected StandardFormationIconDialog(final JFrame frame, final String name, final String title,
          final AbstractMHQIconChooser chooser) {
        super(frame, name, title, chooser);
    }
    //endregion Constructors

    //region Getters
    @Override
    protected StandardFormationIconChooser getChooser() {
        return (StandardFormationIconChooser) super.getChooser();
    }

    @Override
    public @Nullable StandardFormationIcon getSelectedItem() {
        return getChooser().getSelectedItem();
    }
    //endregion Getters
}
