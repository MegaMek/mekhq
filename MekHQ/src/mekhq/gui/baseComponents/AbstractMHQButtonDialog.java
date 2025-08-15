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
import javax.swing.JDialog;
import javax.swing.JFrame;

import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import mekhq.MekHQ;

/**
 * This is the Base Dialog for a dialog with buttons in MegaMek. It extends Base Dialog, and adds a button panel with
 * base Ok and Cancel buttons. It also includes an enum tracker for the result of the dialog.
 * <p>
 * Inheriting classes must call initialize() in their constructors and override createCenterPane()
 * <p>
 * The resources associated with this dialog need to contain at least the following keys: - "Ok.text" - text for the ok
 * button - "Ok.toolTipText" - toolTipText for the ok button - "Cancel.text" - text for the cancel button -
 * "Cancel.toolTipText" - toolTipText for the cancel button
 */
public abstract class AbstractMHQButtonDialog extends AbstractButtonDialog {
    //region Constructors

    /**
     * This creates a modal AbstractMHQButtonDialog using the default MHQ resource bundle. This is the normal
     * constructor to use for an AbstractMHQButtonDialog.
     */
    protected AbstractMHQButtonDialog(final JFrame frame, final String name, final String title) {
        this(frame, true, name, title);
    }

    protected AbstractMHQButtonDialog(final JDialog dialog, final JFrame frame, final String name, final String title) {
        this(dialog, frame, true, name, title);
    }

    /**
     * This creates an AbstractMHQButtonDialog using the default MHQ resource bundle. It allows one to create non-modal
     * button dialogs, which is not recommended by default.
     */
    protected AbstractMHQButtonDialog(final JFrame frame, final boolean modal, final String name,
          final String title) {
        this(frame, modal, ResourceBundle.getBundle("mekhq.resources.GUI",
              MekHQ.getMHQOptions().getLocale()), name, title);
    }

    protected AbstractMHQButtonDialog(final JDialog dialog, final JFrame frame, final boolean modal, final String name,
          final String title) {
        this(dialog, frame, modal, ResourceBundle.getBundle("mekhq.resources.GUI",
              MekHQ.getMHQOptions().getLocale()), name, title);
    }

    /**
     * This creates an AbstractMHQButtonDialog using the specified resource bundle. This is not recommended by default.
     */
    protected AbstractMHQButtonDialog(final JFrame frame, final boolean modal, final ResourceBundle resources,
          final String name, final String title) {
        super(frame, modal, resources, name, title);
    }

    protected AbstractMHQButtonDialog(final JDialog dialog, final JFrame frame, final boolean modal,
          final ResourceBundle resources,
          final String name, final String title) {
        super(dialog, frame, modal, resources, name, title);
    }
    //endregion Constructors
    //endregion Constructors

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
}
