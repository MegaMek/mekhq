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

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * This is the Base Dialog for a dialog with buttons in MegaMek. It extends Base Dialog, and adds a
 * button panel with base Ok and Cancel buttons. It also includes an enum tracker for the result of
 * the dialog.
 *
 * Inheriting classes must call initialize() in their constructors and override createCenterPane()
 *
 * The resources associated with this dialog need to contain at least the following keys:
 * - "Ok.text" -> text for the ok button
 * - "Ok.toolTipText" -> toolTipText for the ok button
 * - "Cancel.text" -> text for the cancel button
 * - "Cancel.toolTipText" -> toolTipText for the cancel button
 */
public abstract class AbstractMHQButtonDialog extends AbstractButtonDialog {
    //region Constructors
    /**
     * This creates a modal AbstractMHQButtonDialog using the default MHQ resource bundle. This is
     * the normal constructor to use for an AbstractMHQButtonDialog.
     */
    protected AbstractMHQButtonDialog(final JFrame frame, final String name, final String title) {
        this(frame, true, name, title);
    }

    /**
     * This creates an AbstractMHQButtonDialog using the default MHQ resource bundle. It allows one
     * to create non-modal button dialogs, which is not recommended by default.
     */
    protected AbstractMHQButtonDialog(final JFrame frame, final boolean modal, final String name,
                                      final String title) {
        this(frame, modal, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()), name, title);
    }

    /**
     * This creates an AbstractMHQButtonDialog using the specified resource bundle. This is not
     * recommended by default.
     */
    protected AbstractMHQButtonDialog(final JFrame frame, final boolean modal, final ResourceBundle resources,
                                      final String name, final String title) {
        super(frame, modal, resources, name, title);
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
